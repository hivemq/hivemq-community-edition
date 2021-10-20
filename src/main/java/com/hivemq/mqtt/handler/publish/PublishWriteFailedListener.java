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
package com.hivemq.mqtt.handler.publish;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.util.Exceptions;
import io.netty.handler.codec.EncoderException;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Florian Limpöck
 */
public class PublishWriteFailedListener implements GenericFutureListener<Future<? super Void>> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(PublishWriteFailedListener.class);

    private final @NotNull SettableFuture<PublishStatus> statusFuture;

    public PublishWriteFailedListener(final @NotNull SettableFuture<PublishStatus> statusFuture) {
        this.statusFuture = statusFuture;
    }

    @Override
    public void operationComplete(final Future<? super Void> future) throws Exception {
        if (!future.isSuccess()) {
            final Throwable cause = future.cause();
            if (Exceptions.isConnectionClosedException(cause)) {
                log.trace("Failed to write publish. Client not connected anymore");
                statusFuture.set(PublishStatus.NOT_CONNECTED);

            } else if (cause instanceof EncoderException) {
                Exceptions.rethrowError("Failed to write publish. Encoding Failure.", cause);
                final Throwable rootCause = cause.getCause();
                if (cause != rootCause) {
                    Exceptions.rethrowError("Failed to write publish. Encoding Failure, root cause:", rootCause);
                }
                statusFuture.set(PublishStatus.FAILED);
            } else {
                Exceptions.rethrowError("Failed to write publish.", cause);
                statusFuture.set(PublishStatus.FAILED);
            }
        }
    }
}