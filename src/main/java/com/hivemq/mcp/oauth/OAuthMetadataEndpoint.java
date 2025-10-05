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

import com.hivemq.configuration.service.InternalConfigurations;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OAuth 2.0 metadata endpoints for MCP server.
 * Implements RFC 8414 (Authorization Server Metadata) and RFC 9728 (Protected Resource Metadata).
 *
 * @author HiveMQ
 */
@Path("/.well-known")
public class OAuthMetadataEndpoint {

    private static final Logger log = LoggerFactory.getLogger(OAuthMetadataEndpoint.class);
    private static final String BASE_URL = "https://localhost:" + InternalConfigurations.MCP_SERVER_PORT.get();

    /**
     * OAuth 2.0 Protected Resource Metadata (RFC 9728).
     * Returns metadata about the protected resource and its authorization servers.
     */
    @GET
    @Path("/oauth-protected-resource")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProtectedResourceMetadata() {
        log.info("OAuth Protected Resource Metadata requested");

        final Map<String, Object> metadata = new HashMap<>();
        metadata.put("resource", BASE_URL);
        metadata.put("authorization_servers", List.of(BASE_URL));

        return Response.ok(metadata).build();
    }

    /**
     * OAuth 2.0 Authorization Server Metadata (RFC 8414).
     * Returns configuration details about the authorization server.
     */
    @GET
    @Path("/oauth-authorization-server")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAuthorizationServerMetadata() {
        log.info("OAuth Authorization Server Metadata requested");

        final Map<String, Object> metadata = new HashMap<>();

        metadata.put("issuer", BASE_URL);
        metadata.put("authorization_endpoint", BASE_URL + "/oauth/authorize");
        metadata.put("token_endpoint", BASE_URL + "/oauth/token");
        metadata.put("registration_endpoint", BASE_URL + "/oauth/register");

        metadata.put("response_types_supported", List.of("code"));
        metadata.put("grant_types_supported", List.of("authorization_code", "refresh_token"));
        metadata.put("token_endpoint_auth_methods_supported", List.of("client_secret_basic", "client_secret_post"));
        metadata.put("code_challenge_methods_supported", List.of("S256"));

        return Response.ok(metadata).build();
    }
}
