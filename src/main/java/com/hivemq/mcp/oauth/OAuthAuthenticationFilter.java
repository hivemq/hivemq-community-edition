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
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hivemq.configuration.service.InternalConfigurations;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * JAX-RS filter that validates OAuth 2.0 Bearer tokens on protected resources.
 * Implements RFC 6750 (Bearer Token Usage).
 *
 * @author HiveMQ
 */
@Provider
public class OAuthAuthenticationFilter implements ContainerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OAuthAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BASE_URL = "https://localhost:" + InternalConfigurations.MCP_SERVER_PORT.get();

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final String path = requestContext.getUriInfo().getPath();
        final String method = requestContext.getMethod();
        final String clientAddress = requestContext.getHeaderString("X-Forwarded-For");

        log.info("MCP request: {} {} (path: '{}') from {}", method, requestContext.getUriInfo().getRequestUri(), path, clientAddress != null ? clientAddress : "unknown");

        // Skip authentication for OAuth and metadata endpoints
        if (path.startsWith("/.well-known") || path.startsWith("/oauth")) {
            log.debug("Skipping authentication for public endpoint: {}", path);
            return;
        }

        final String authHeader = requestContext.getHeaderString(AUTHORIZATION_HEADER);

        if (authHeader == null) {
            log.warn("MCP authentication failed for {}: Missing Authorization header", path);
            unauthorized(requestContext, "missing_auth_header");
            return;
        }

        if (!authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("MCP authentication failed for {}: Invalid Authorization header format", path);
            unauthorized(requestContext, "invalid_auth_format");
            return;
        }

        final String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // Validate JWT token
            final Algorithm algorithm = Algorithm.HMAC256(OAuthEndpoint.JWT_SECRET);
            final JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("hivemq-mcp")
                    .withAudience("hivemq-mcp-resources")
                    .build();

            final DecodedJWT jwt = verifier.verify(token);
            log.debug("MCP authentication successful for {}: client_id={}", path, jwt.getSubject());

            // Token is valid, request continues
        } catch (JWTVerificationException e) {
            log.warn("MCP authentication failed for {}: Invalid JWT token - {}", path, e.getMessage());
            unauthorized(requestContext, "invalid_token");
        }
    }

    private void unauthorized(ContainerRequestContext requestContext, String reason) {
        // RFC 9728 - WWW-Authenticate header with resource metadata URL
        final String wwwAuthenticate = "Bearer " +
                "realm=\"HiveMQ MCP Server\", " +
                "error=\"" + reason + "\", " +
                "resource_metadata_uri=\"" + BASE_URL + "/.well-known/oauth-protected-resource\"";

        log.info("Returning 401 Unauthorized for {}: {}", requestContext.getUriInfo().getPath(), reason);

        requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .header("WWW-Authenticate", wwwAuthenticate)
                        .build()
        );
    }
}
