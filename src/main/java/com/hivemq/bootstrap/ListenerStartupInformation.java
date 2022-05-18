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

    private final int port;

    private final boolean successful;
    private final Listener originalListener;

    private final Optional<Throwable> exception;

    private ListenerStartupInformation(final int port, final boolean successful,
                                       @NotNull final Listener originalListener,
                                       @Nullable final Throwable exception) {

        checkNotNull(originalListener, "Original Listener must not be null");

        this.port = port;
        this.successful = successful;
        this.originalListener = originalListener;
        this.exception = Optional.ofNullable(exception);
    }

    public int getPort() {
        return port;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public Optional<Throwable> getException() {
        return exception;
    }

    public Listener getOriginalListener() {
        return originalListener;
    }

    public static ListenerStartupInformation successfulListenerStartup(final int port, @NotNull final Listener originalListener) {
        return new ListenerStartupInformation(port, true, originalListener, null);
    }

    public static ListenerStartupInformation failedListenerStartup(final int port, @NotNull final Listener originalListener, @Nullable final Throwable exception) {
        return new ListenerStartupInformation(port, false, originalListener, exception);
    }
}
