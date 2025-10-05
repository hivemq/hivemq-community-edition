/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.mcp.oauth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth 2.0 endpoints for MCP server.
 * Implements Dynamic Client Registration (RFC 7591) and OAuth 2.0 authorization flow.
 *
 * @author HiveMQ
 */
@Path("/oauth")
@Singleton
public class OAuthEndpoint {

    private static final Logger log = LoggerFactory.getLogger(OAuthEndpoint.class);

    static final String JWT_SECRET = generateSecret();
    private static final Algorithm JWT_ALGORITHM = Algorithm.HMAC256(JWT_SECRET);

    private final @NotNull Map<String, RegisteredClient> registeredClients = new ConcurrentHashMap<>();
    private final @NotNull Map<String, AuthorizationCode> authorizationCodes = new ConcurrentHashMap<>();

    /**
     * Dynamic Client Registration endpoint (RFC 7591).
     * Allows Claude to register as an OAuth client.
     */
    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(Map<String, Object> request) {
        log.info("OAuth client registration request received: client_name={}, redirect_uris={}",
                request.get("client_name"), request.get("redirect_uris"));

        final String clientId = UUID.randomUUID().toString();
        final String clientSecret = generateSecret();

        final RegisteredClient client = new RegisteredClient(
                clientId,
                clientSecret,
                (String) request.get("client_name"),
                (List<String>) request.get("redirect_uris")
        );

        registeredClients.put(clientId, client);

        final Map<String, Object> response = new HashMap<>();
        response.put("client_id", clientId);
        response.put("client_secret", clientSecret);
        response.put("client_name", client.clientName);
        response.put("redirect_uris", client.redirectUris);

        log.info("OAuth client registered successfully: client_id={}, client_name={}", clientId, client.clientName);

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    /**
     * Authorization endpoint.
     * For development, auto-approves authorization requests.
     */
    @GET
    @Path("/authorize")
    public Response authorize(
            @QueryParam("client_id") String clientId,
            @QueryParam("redirect_uri") String redirectUri,
            @QueryParam("response_type") String responseType,
            @QueryParam("state") String state,
            @QueryParam("code_challenge") String codeChallenge,
            @QueryParam("code_challenge_method") String codeChallengeMethod) {

        log.info("OAuth authorization request: client_id={}, redirect_uri={}, response_type={}",
                clientId, redirectUri, responseType);

        if (!"code".equals(responseType)) {
            log.warn("OAuth authorization failed: unsupported_response_type={}", responseType);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "unsupported_response_type"))
                    .build();
        }

        final RegisteredClient client = registeredClients.get(clientId);
        if (client == null) {
            log.warn("OAuth authorization failed: unknown client_id={}", clientId);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "invalid_client"))
                    .build();
        }

        if (!client.redirectUris.contains(redirectUri)) {
            log.warn("OAuth authorization failed: redirect_uri={} not registered for client_id={}",
                    redirectUri, clientId);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "invalid_redirect_uri"))
                    .build();
        }

        // Generate authorization code
        final String code = UUID.randomUUID().toString();
        authorizationCodes.put(code, new AuthorizationCode(
                clientId, redirectUri, codeChallenge, Instant.now().plusSeconds(600)));

        log.info("OAuth authorization code generated for client_id={}", clientId);

        // Redirect back to client with authorization code
        final String redirectUrl = redirectUri + "?code=" + code +
                (state != null ? "&state=" + state : "");

        return Response.seeOther(java.net.URI.create(redirectUrl)).build();
    }

    /**
     * Token endpoint.
     * Exchanges authorization code for access token.
     */
    @POST
    @Path("/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response token(
            @FormParam("grant_type") String grantType,
            @FormParam("code") String code,
            @FormParam("redirect_uri") String redirectUri,
            @FormParam("client_id") String clientId,
            @FormParam("client_secret") String clientSecret,
            @FormParam("code_verifier") String codeVerifier) {

        log.info("OAuth token request: grant_type={}, client_id={}", grantType, clientId);

        if (!"authorization_code".equals(grantType)) {
            log.warn("OAuth token failed: unsupported_grant_type={}", grantType);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "unsupported_grant_type"))
                    .build();
        }

        // Validate client
        final RegisteredClient client = registeredClients.get(clientId);
        if (client == null || !client.clientSecret.equals(clientSecret)) {
            log.warn("OAuth token failed: invalid client credentials for client_id={}", clientId);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "invalid_client"))
                    .build();
        }

        // Validate authorization code
        final AuthorizationCode authCode = authorizationCodes.remove(code);
        if (authCode == null) {
            log.warn("OAuth token failed: invalid or expired authorization code");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "invalid_grant", "error_description", "Authorization code not found"))
                    .build();
        }

        if (!authCode.clientId.equals(clientId)) {
            log.warn("OAuth token failed: authorization code client_id mismatch");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "invalid_grant", "error_description", "Client ID mismatch"))
                    .build();
        }

        if (!authCode.redirectUri.equals(redirectUri)) {
            log.warn("OAuth token failed: redirect_uri mismatch");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "invalid_grant", "error_description", "Redirect URI mismatch"))
                    .build();
        }

        if (authCode.expiresAt.isBefore(Instant.now())) {
            log.warn("OAuth token failed: authorization code expired");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "invalid_grant", "error_description", "Authorization code expired"))
                    .build();
        }

        // Generate access token
        final String accessToken = JWT.create()
                .withIssuer("hivemq-mcp")
                .withSubject(clientId)
                .withAudience("hivemq-mcp-resources")
                .withIssuedAt(new Date())
                .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
                .sign(JWT_ALGORITHM);

        log.info("OAuth access token issued for client_id={}", clientId);

        final Map<String, Object> response = new HashMap<>();
        response.put("access_token", accessToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", 3600);

        return Response.ok(response).build();
    }

    private static String generateSecret() {
        final byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static class RegisteredClient {
        final String clientId;
        final String clientSecret;
        final String clientName;
        final List<String> redirectUris;

        RegisteredClient(String clientId, String clientSecret, String clientName, List<String> redirectUris) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.clientName = clientName;
            this.redirectUris = redirectUris != null ? redirectUris : Collections.emptyList();
        }
    }

    private static class AuthorizationCode {
        final String clientId;
        final String redirectUri;
        final String codeChallenge;
        final Instant expiresAt;

        AuthorizationCode(String clientId, String redirectUri, String codeChallenge, Instant expiresAt) {
            this.clientId = clientId;
            this.redirectUri = redirectUri;
            this.codeChallenge = codeChallenge;
            this.expiresAt = expiresAt;
        }
    }
}
