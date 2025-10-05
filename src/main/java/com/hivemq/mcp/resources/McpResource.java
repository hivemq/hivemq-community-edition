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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

/**
 * Represents an MCP resource descriptor.
 *
 * @author HiveMQ
 */
public class McpResource {

    private final @NotNull String uri;
    private final @NotNull String name;
    private final @Nullable String description;
    private final @Nullable String mimeType;

    public McpResource(
            final @NotNull String uri,
            final @NotNull String name,
            final @Nullable String description,
            final @Nullable String mimeType) {
        this.uri = uri;
        this.name = name;
        this.description = description;
        this.mimeType = mimeType;
    }

    @JsonProperty("uri")
    public @NotNull String getUri() {
        return uri;
    }

    @JsonProperty("name")
    public @NotNull String getName() {
        return name;
    }

    @JsonProperty("description")
    public @Nullable String getDescription() {
        return description;
    }

    @JsonProperty("mimeType")
    public @Nullable String getMimeType() {
        return mimeType;
    }
}
