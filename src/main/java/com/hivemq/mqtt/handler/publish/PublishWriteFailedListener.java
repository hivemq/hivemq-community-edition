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

    private final @NotNull SettableFuture<PublishStatus> inStatusFuture;
    private final @NotNull SettableFuture<PublishStatus> outStatusFuture;
    private final boolean sameFuture;

    public PublishWriteFailedListener(final @NotNull SettableFuture<PublishStatus> inStatusFuture) {
        this(inStatusFuture, inStatusFuture, true);
    }

    public PublishWriteFailedListener(final @NotNull SettableFuture<PublishStatus> inStatusFuture,
                                      final @NotNull SettableFuture<PublishStatus> outStatusFuture) {
        this(inStatusFuture, outStatusFuture, false);
    }

    public PublishWriteFailedListener(final @NotNull SettableFuture<PublishStatus> inStatusFuture,
                                      final @NotNull SettableFuture<PublishStatus> outStatusFuture,
                                      final boolean sameFuture) {
        this.inStatusFuture = inStatusFuture;
        this.outStatusFuture = outStatusFuture; // Since SettableFuture can only be set once, we need a new future to account for failure cases in operationComplete
        this.sameFuture = sameFuture; // To avoid waiting indefinitely on inStatusFuture
    }

    @Override
    public void operationComplete(final Future<? super Void> future) throws Exception {
        if (!future.isSuccess()) {
            final Throwable cause = future.cause();
            if (Exceptions.isConnectionClosedException(cause)) {
                log.trace("Failed to write publish. Client not connected anymore");
                outStatusFuture.set(PublishStatus.NOT_CONNECTED);
                return;
            } else if (cause instanceof EncoderException) {
                Exceptions.rethrowError("Failed to write publish. Encoding Failure.", cause);
                final Throwable rootCause = cause.getCause();
                if (cause != rootCause) {
                    Exceptions.rethrowError("Failed to write publish. Encoding Failure, root cause:", rootCause);
                }
                outStatusFuture.set(PublishStatus.FAILED);
                return;
            } else {
                Exceptions.rethrowError("Failed to write publish.", cause);
                outStatusFuture.set(PublishStatus.FAILED);
                return;
            }
        } else if(!sameFuture && inStatusFuture.isDone()) {
            // see RetainedMessagesSender for QoS0 publishes
            outStatusFuture.set(inStatusFuture.get());
        }
    }
}