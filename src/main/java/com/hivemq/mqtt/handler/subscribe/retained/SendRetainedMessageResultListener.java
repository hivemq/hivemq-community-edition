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
package com.hivemq.mqtt.handler.subscribe.retained;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.pool.exception.NoMessageIdAvailableException;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.util.Exceptions;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class SendRetainedMessageResultListener implements FutureCallback<Void> {

    private static final Logger log = LoggerFactory.getLogger(SendRetainedMessageResultListener.class);

    private final @NotNull Channel channel;
    private final @NotNull Topic subscription;
    private final @NotNull RetainedMessagesSender retainedMessagesSender;

    SendRetainedMessageResultListener(
            final @NotNull Channel channel,
            final @NotNull Topic subscription,
            final @NotNull RetainedMessagesSender retainedMessagesSender) {

        this.channel = channel;
        this.subscription = subscription;
        this.retainedMessagesSender = retainedMessagesSender;
    }

    @Override
    public void onSuccess(final @Nullable Void aVoid) {
        //noop: everything is good
    }

    @Override
    public void onFailure(final @NotNull Throwable throwable) {

        if (Exceptions.isConnectionClosedException(throwable)) {
            // There is no need to send the retained message, if the client is disconnected.
            return;
        }

        if (throwable instanceof NoMessageIdAvailableException) {
            if (!channel.isActive()) {
                return;
            }

            //retry
            channel.eventLoop().schedule(() -> {
                if (log.isTraceEnabled()) {
                    log.trace("Retrying retained message for client '{}' on topic '{}'.",
                            channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().getClientId(), subscription.getTopic());
                }
                final ListenableFuture<Void> sentFuture =
                        retainedMessagesSender.writeRetainedMessages(channel, subscription);
                Futures.addCallback(sentFuture, this, channel.eventLoop());
            }, 1, TimeUnit.SECONDS);

        } else {
            Exceptions.rethrowError("Unable to send retained message on topic " + subscription.getTopic() +
                    " to client " + channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().getClientId() + ".", throwable);
        }
    }
}