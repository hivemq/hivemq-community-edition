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
package com.hivemq.bootstrap;

import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

@Immutable
public class ListenerStartupInformation {

    private final boolean successful;
    private final @NotNull Listener listener;
    private final @Nullable Throwable exception;

    private ListenerStartupInformation(
            final boolean successful, final @NotNull Listener listener, final @Nullable Throwable exception) {

        checkNotNull(listener, "Original Listener must not be null");

        this.successful = successful;
        this.listener = listener;
        this.exception = exception;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public @NotNull Listener getListener() {
        return listener;
    }

    public @NotNull Optional<Throwable> getException() {
        return Optional.ofNullable(exception);
    }

    public static ListenerStartupInformation successfulListenerStartup(final @NotNull Listener listener) {
        return new ListenerStartupInformation(true, listener, null);
    }

    public static ListenerStartupInformation failedListenerStartup(
            final @NotNull Listener listener, final @Nullable Throwable exception) {
        return new ListenerStartupInformation(false, listener, exception);
    }
}
