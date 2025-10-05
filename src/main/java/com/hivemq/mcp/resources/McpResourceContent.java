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
 * Represents the content of an MCP resource.
 *
 * @author HiveMQ
 */
public class McpResourceContent {

    private final @NotNull String uri;
    private final @Nullable String mimeType;
    private final @NotNull String text;

    public McpResourceContent(
            final @NotNull String uri,
            final @Nullable String mimeType,
            final @NotNull String text) {
        this.uri = uri;
        this.mimeType = mimeType;
        this.text = text;
    }

    @JsonProperty("uri")
    public @NotNull String getUri() {
        return uri;
    }

    @JsonProperty("mimeType")
    public @Nullable String getMimeType() {
        return mimeType;
    }

    @JsonProperty("text")
    public @NotNull String getText() {
        return text;
    }
}
