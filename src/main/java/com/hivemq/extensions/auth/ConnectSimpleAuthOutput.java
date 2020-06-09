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
package com.hivemq.extensions.auth;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.auth.parameter.ModifiableClientSettings;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;

import java.time.Duration;

/**
 * @author Silvio Giebl
 */
class ConnectSimpleAuthOutput implements SimpleAuthOutput {

    private final @NotNull ConnectAuthOutput delegate;

    ConnectSimpleAuthOutput(final @NotNull ConnectAuthOutput delegate) {
        this.delegate = delegate;
    }

    @Override
    public void authenticateSuccessfully() {
        delegate.authenticateSuccessfully();
    }

    @Override
    public void failAuthentication() {
        delegate.failAuthentication();
    }

    @Override
    public void failAuthentication(final @NotNull ConnackReasonCode reasonCode) {
        delegate.failAuthentication(reasonCode);
    }

    @Override
    public void failAuthentication(final @Nullable String reasonString) {
        delegate.failAuthentication(reasonString);
    }

    @Override
    public void failAuthentication(final @NotNull ConnackReasonCode reasonCode, final @Nullable String reasonString) {
        delegate.failAuthentication(reasonCode, reasonString);
    }

    @Override
    public void nextExtensionOrDefault() {
        delegate.nextExtensionOrDefault();
    }

    @Override
    public @NotNull ModifiableUserProperties getOutboundUserProperties() {
        return delegate.getOutboundUserProperties();
    }

    @Override
    public @NotNull ModifiableDefaultPermissions getDefaultPermissions() {
        return delegate.getDefaultPermissions();
    }

    @Override
    public @NotNull ModifiableClientSettings getClientSettings() {
        return delegate.getClientSettings();
    }

    @Override
    public @NotNull Async<SimpleAuthOutput> async(final @NotNull Duration timeout) {
        return new AsyncWrapper<>(delegate.async(timeout), this);
    }

    @Override
    public @NotNull Async<SimpleAuthOutput> async(
            final @NotNull Duration timeout,
            final @NotNull TimeoutFallback fallback) {

        return new AsyncWrapper<>(delegate.async(timeout, fallback), this);
    }

    @Override
    public @NotNull Async<SimpleAuthOutput> async(
            final @NotNull Duration timeout,
            final @NotNull TimeoutFallback fallback,
            final @NotNull ConnackReasonCode reasonCode) {

        return new AsyncWrapper<>(delegate.async(timeout, fallback, reasonCode), this);
    }

    @Override
    public @NotNull Async<SimpleAuthOutput> async(
            final @NotNull Duration timeout,
            final @NotNull TimeoutFallback fallback,
            final @Nullable String reasonString) {

        return new AsyncWrapper<>(delegate.async(timeout, fallback, reasonString), this);
    }

    @Override
    public @NotNull Async<SimpleAuthOutput> async(
            final @NotNull Duration timeout,
            final @NotNull TimeoutFallback fallback,
            final @NotNull ConnackReasonCode reasonCode,
            final @Nullable String reasonString) {

        return new AsyncWrapper<>(delegate.async(timeout, fallback, reasonCode, reasonString), this);
    }

    private static class AsyncWrapper<T> implements Async<T> {

        private final @NotNull Async<?> delegate;
        private final @NotNull T output;

        public AsyncWrapper(final @NotNull Async<?> delegate, final @NotNull T output) {
            this.delegate = delegate;
            this.output = output;
        }

        @Override
        public void resume() {
            delegate.resume();
        }

        @NotNull
        @Override
        public T getOutput() {
            return output;
        }

        @Override
        public @NotNull Status getStatus() {
            return delegate.getStatus();
        }
    }
}