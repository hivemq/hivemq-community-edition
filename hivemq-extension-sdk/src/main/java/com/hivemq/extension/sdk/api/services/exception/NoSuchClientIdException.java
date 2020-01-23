/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extension.sdk.api.services.exception;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * This exception is used to signal that a given MQTT ClientId is unknown to the broker in the given context.
 *
 * @author Georg Held
 * @since 4.0.0
 */
public class NoSuchClientIdException extends Exception {

    /**
     * The client id that caused the exception.
     *
     * @since 4.0.0
     */
    private final @NotNull String clientId;

    /**
     * Flag that if <b>true</b> will also fill in the stack trace for the exception.
     *
     * @since 4.0.0
     */
    private final boolean fillInStacktrace;

    /**
     * Creates a new NoSuchClientException.
     *
     * @param clientId         The not available MQTT ClientId.
     * @param fillInStacktrace Whether the created exception should fill in a stacktrace
     * @since 4.0.0
     */
    public NoSuchClientIdException(final @NotNull String clientId, final boolean fillInStacktrace) {
        this.clientId = clientId;
        this.fillInStacktrace = fillInStacktrace;
    }

    /**
     * Creates a new NoSuchClientException that will not contain a stacktrace.
     *
     * @param clientId The not available MQTT ClientId.
     * @since 4.0.0
     */
    public NoSuchClientIdException(final @NotNull String clientId) {
        this(clientId, false);
    }

    @Override
    public synchronized @NotNull Throwable fillInStackTrace() {
        if (fillInStacktrace) {
            return super.fillInStackTrace();
        }
        return this;
    }

    /**
     * Returns the unknown MQTT ClientId.
     *
     * @return The not available MQTT ClientId.
     * @since 4.0.0
     */
    public @NotNull String getClientId() {
        return clientId;
    }
}
