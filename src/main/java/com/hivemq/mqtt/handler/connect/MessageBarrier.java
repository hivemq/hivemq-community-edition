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
package com.hivemq.mqtt.handler.connect;

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A message barrier which blocks (or queues) messages if they are sent before the connection was approved
 */
public class MessageBarrier extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(MessageBarrier.class);

    private static final ChannelFutureListener ENABLE_AUTO_READ_LISTENER = future -> {
        if (future.isSuccess()) {
            resumeRead(future.channel());
        }
    };

    private final @NotNull MqttServerDisconnector serverDisconnector;
    private final @NotNull Queue<Message> messageQueue = new LinkedList<>();

    private boolean connectReceived;
    private boolean connackSent;

    public MessageBarrier(final @NotNull MqttServerDisconnector serverDisconnector) {
        this.serverDisconnector = serverDisconnector;
    }

    @Override
    public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) {

        if (msg instanceof Message) {
            if (msg instanceof CONNECT) {
                connectReceived = true;
                suspendRead(ctx.channel());
            } else if (!connectReceived) {
                serverDisconnector.logAndClose(ctx.channel(),
                        "A client (IP: {}) sent other message before CONNECT. Disconnecting client.",
                        "Sent other message before CONNECT");
                return;
            } else if (msg instanceof AUTH) {
                suspendRead(ctx.channel());
            } else if (!connackSent) {
                messageQueue.add((Message) msg);
                return;
            }
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull Object msg,
            final @NotNull ChannelPromise promise) {

        if ((msg instanceof CONNACK) && (((CONNACK) msg).getReasonCode() == Mqtt5ConnAckReasonCode.SUCCESS)) {
            promise.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    future.channel().pipeline().remove(this);
                    connackSent = true;
                    releaseQueuedMessages(ctx);
                }
            });
            promise.addListener(ENABLE_AUTO_READ_LISTENER);
        } else if (msg instanceof AUTH) {
            promise.addListener(ENABLE_AUTO_READ_LISTENER);
        }
        ctx.write(msg, promise);
    }

    private void releaseQueuedMessages(final @NotNull ChannelHandlerContext ctx) {
        for (final Message message : messageQueue) {
            ctx.fireChannelRead(message);
        }
    }

    private static void suspendRead(final @NotNull Channel channel) {
        if (log.isTraceEnabled()) {
            final ClientConnection clientConnection = channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
            final Optional<String> channelIP = (clientConnection == null)
                    ? Optional.empty()
                    : clientConnection.getChannelIP();

            log.trace("Suspending read operations for MQTT client with id {} and IP {}",
                    clientConnection.getClientId(),
                    channelIP.orElse("UNKNOWN"));
        }
        channel.config().setAutoRead(false);
    }

    private static void resumeRead(final @NotNull Channel channel) {
        if (log.isTraceEnabled()) {
            final ClientConnection clientConnection = channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
            final Optional<String> channelIP = (clientConnection == null)
                    ? Optional.empty()
                    : clientConnection.getChannelIP();

            log.trace("Restarting read operations for MQTT client with id {} and IP {}",
                    clientConnection.getClientId(),
                    channelIP.orElse("UNKNOWN"));
        }
        channel.config().setAutoRead(true);
    }

    @VisibleForTesting
    boolean getConnectReceived() {
        return connectReceived;
    }

    @VisibleForTesting
    @NotNull Collection<Message> getQueue() {
        return Collections.unmodifiableCollection(messageQueue);
    }
}
