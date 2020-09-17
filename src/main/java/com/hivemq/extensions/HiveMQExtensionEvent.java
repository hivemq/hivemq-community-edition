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
package com.hivemq.extensions;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.nio.file.Path;

/**
 * @author Georg Held
 */
public class HiveMQExtensionEvent {
    private final Change change;
    private final String pluginId;
    private final int priority;
    private final Path pluginFolder;
    private final boolean embedded;

    public HiveMQExtensionEvent(
            @NotNull final Change change,
            @NotNull final String pluginId,
            final int priority,
            @NotNull final Path pluginFolder,
            final boolean embedded) {
        this.change = change;
        this.pluginId = pluginId;
        this.priority = priority;
        this.pluginFolder = pluginFolder;
        this.embedded = embedded;
    }

    @NotNull
    public Change getChange() {
        return change;
    }

    @NotNull
    public String getPluginId() {
        return pluginId;
    }

    public int getPriority() {
        return priority;
    }

    @NotNull
    public Path getPluginFolder() {
        return pluginFolder;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public enum Change {ENABLE, DISABLE}
}
