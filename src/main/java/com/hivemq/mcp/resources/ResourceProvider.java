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
package com.hivemq.mcp.resources;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * Interface for MCP resource providers.
 * Resource providers expose data through the MCP protocol.
 *
 * @author HiveMQ
 */
public interface ResourceProvider {

    /**
     * Gets the descriptor for this resource.
     * The descriptor contains metadata about the resource (uri, name, description, mimeType).
     *
     * @return the MCP resource descriptor
     */
    @NotNull McpResource getDescriptor();

    /**
     * Gets the current content for this resource.
     * The content contains the actual data exposed by this resource.
     *
     * @return the MCP resource content
     */
    @NotNull McpResourceContent getContent();
}
