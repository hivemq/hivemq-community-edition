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

import com.google.common.base.Preconditions;
import com.hivemq.embedded.EmbeddedExtension;
import com.hivemq.embedded.EmbeddedExtensionBuilder;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import static com.hivemq.extensions.HiveMQExtensionEntity.DEFAULT_PRIORITY;
import static com.hivemq.extensions.HiveMQExtensionEntity.DEFAULT_START_PRIORITY;

public class EmbeddedExtensionBuilderImpl implements EmbeddedExtensionBuilder {

    private @Nullable String id;
    private @Nullable String name;
    private @Nullable String version;
    private @Nullable String author;
    private int priority = DEFAULT_PRIORITY;
    private int startPriority = DEFAULT_START_PRIORITY;
    private @Nullable ExtensionMain extensionMain;

    @Override
    public @NotNull EmbeddedExtensionBuilder withId(final @NotNull String id) {
        this.id = id;
        return this;
    }

    @Override
    public @NotNull EmbeddedExtensionBuilder withName(final @NotNull String name) {
        this.name = name;
        return this;
    }

    @Override
    public @NotNull EmbeddedExtensionBuilder withVersion(final @NotNull String version) {
        this.version = version;
        return this;
    }

    @Override
    public @NotNull EmbeddedExtensionBuilder withAuthor(final @Nullable String author) {
        this.author = author;
        return this;
    }

    @Override
    public @NotNull EmbeddedExtensionBuilder withPriority(final int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public @NotNull EmbeddedExtensionBuilder withStartPriority(final int startPriority) {
        this.startPriority = startPriority;
        return this;
    }


    @Override
    public @NotNull EmbeddedExtensionBuilder withExtensionMain(final @NotNull ExtensionMain extensionMain) {
        this.extensionMain = extensionMain;
        return this;
    }

    @Override
    public @NotNull EmbeddedExtension build() {
        Preconditions.checkState(id != null, "id must never be null");
        Preconditions.checkState(name != null, "name must never be null");
        Preconditions.checkState(version != null, "version must never be null");
        Preconditions.checkState(extensionMain != null, "extensionMain must never be null");

        return new EmbeddedExtensionImpl(id, name, version, author, priority, startPriority, extensionMain);
    }
}
