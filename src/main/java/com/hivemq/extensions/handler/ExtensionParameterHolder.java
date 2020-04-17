package com.hivemq.extensions.handler;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class ExtensionParameterHolder<T> implements Supplier<T> {

    private @NotNull T t;

    public ExtensionParameterHolder(final @NotNull T t) {
        this.t = t;
    }

    @Override
    public @NotNull T get() {
        return this.t;
    }

    public void set(final @NotNull T t) {
        this.t = t;
    }
}
