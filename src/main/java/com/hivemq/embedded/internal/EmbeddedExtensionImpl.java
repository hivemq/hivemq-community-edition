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

package com.hivemq.embedded.internal;

import com.hivemq.embedded.EmbeddedExtension;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

class EmbeddedExtensionImpl implements EmbeddedExtension {

    private final @NotNull String id;
    private final @NotNull String name;
    private final @NotNull String version;
    private final @Nullable String author;
    private final int priority;
    private final int startPriority;
    private final @NotNull ExtensionMain extensionMain;

    EmbeddedExtensionImpl(
            final @NotNull String id,
            final @NotNull String name,
            final @NotNull String version,
            final @Nullable String author,
            final int priority,
            final int startPriority,
            final @NotNull ExtensionMain extensionMain) {

        this.id = id;
        this.name = name;
        this.version = version;
        this.author = author;
        this.priority = priority;
        this.startPriority = startPriority;
        this.extensionMain = extensionMain;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getVersion() {
        return version;
    }

    @Nullable
    public String getAuthor() {
        return author;
    }

    public int getPriority() {
        return priority;
    }

    public int getStartPriority() {
        return startPriority;
    }

    @NotNull
    public ExtensionMain getExtensionMain() {
        return extensionMain;
    }

    @NotNull
    @Override
    public String toString() {
        return "EmbeddedExtension{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", version='" + version +
                '\'' + ", author='" + author + '\'' + ", priority=" + priority + ", startPriority=" + startPriority +
                ", extensionMain=" + extensionMain.getClass().getSimpleName() + '}';
    }
}
