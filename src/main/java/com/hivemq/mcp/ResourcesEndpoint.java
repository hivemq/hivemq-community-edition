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
package com.hivemq.mcp;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mcp.resources.ConnectionsResource;
import com.hivemq.mcp.resources.McpResource;
import com.hivemq.mcp.resources.McpResourceContent;
import com.hivemq.mcp.resources.ResourceProvider;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JAX-RS resource for MCP resources/list and resources/read endpoints.
 *
 * @author HiveMQ
 */
@Path("/resources")
public class ResourcesEndpoint {

    private final @NotNull Map<String, ResourceProvider> resourceProviders;

    @Inject
    public ResourcesEndpoint(final @NotNull ConnectionsResource connectionsResource) {
        // Register all available resource providers by their URI
        this.resourceProviders = new HashMap<>();
        registerResourceProvider(connectionsResource);
    }

    private void registerResourceProvider(final @NotNull ResourceProvider provider) {
        resourceProviders.put(provider.getDescriptor().getUri(), provider);
    }

    /**
     * MCP resources/list endpoint - returns list of available resources.
     *
     * @return JSON response with resources array
     */
    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listResources() {
        final List<McpResource> resources = new ArrayList<>();
        for (final ResourceProvider provider : resourceProviders.values()) {
            resources.add(provider.getDescriptor());
        }

        final Map<String, Object> response = new HashMap<>();
        response.put("resources", resources);

        return Response.ok(response).build();
    }

    /**
     * MCP resources/read endpoint - returns content of a specific resource.
     *
     * @param uri the URI of the resource to read
     * @return JSON response with resource content
     */
    @GET
    @Path("/read")
    @Produces(MediaType.APPLICATION_JSON)
    public Response readResource(@QueryParam("uri") final String uri) {
        if (uri == null || uri.isEmpty()) {
            final Map<String, String> error = Map.of("error", "Missing required parameter: uri");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        final ResourceProvider provider = resourceProviders.get(uri);
        if (provider == null) {
            final Map<String, String> error = Map.of("error", "Resource not found: " + uri);
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        final McpResourceContent content = provider.getContent();
        final Map<String, Object> response = new HashMap<>();
        response.put("contents", List.of(content));

        return Response.ok(response).build();
    }
}
