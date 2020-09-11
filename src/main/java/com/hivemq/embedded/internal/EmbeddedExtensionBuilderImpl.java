package com.hivemq.embedded.internal;

import com.google.common.base.Preconditions;
import com.hivemq.embedded.EmbeddedExtension;
import com.hivemq.embedded.EmbeddedExtensionBuilder;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

public class EmbeddedExtensionBuilderImpl implements EmbeddedExtensionBuilder {

    private @Nullable String id;
    private @Nullable String name;
    private @Nullable String version;
    private @Nullable String author;
    private int priority = 0;
    private int startPriority = 1000;
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
        Preconditions.checkNotNull(id, "id must never be null");
        Preconditions.checkNotNull(name, "name must never be null");
        Preconditions.checkNotNull(version, "version must never be null");
        Preconditions.checkNotNull(extensionMain, "extensionMain must never be null");

        return new EmbeddedExtensionImpl(id, name, version, author, priority, startPriority, extensionMain);
    }
}
