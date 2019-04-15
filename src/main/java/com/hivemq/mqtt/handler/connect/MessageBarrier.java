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

package com.hivemq.mqtt.handler.connect;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.hivemq.annotations.NotNull;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelUtils;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A message barrier which blocks (or queues) messages if they are sent before the connection was approved
 *
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 */
public class MessageBarrier extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(MessageBarrier.class);

    private final AtomicBoolean connectAlreadySent = new AtomicBoolean(false);
    private final AtomicBoolean connackAlreadySent = new AtomicBoolean(false);

    private final Queue<Message> messageQueue = new LinkedList<>();
    private final @NotNull EventLog eventLog;

    @Inject
    public MessageBarrier(final @NotNull EventLog eventLog) {
        this.eventLog = eventLog;
    }

    @Override
    public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) throws Exception {

        if (msg instanceof CONNECT) {
            connectAlreadySent.set(true);

        } else if (msg instanceof Message) {

            if (!connectAlreadySent.get()) {
                //Disconnect, because we must receive CONNECTs first
                eventLog.clientWasDisconnected(ctx.channel(), "Sent other message before CONNECT");
                ctx.channel().close();

                if (log.isDebugEnabled()) {
                    log.debug("Disconnecting client with IP [{}] because it sent another message before a CONNECT message", ChannelUtils.getChannelIP(ctx.channel()).or("UNKNOWN"));
                }
                return;

            } else if (connectAlreadySent.get() && !connackAlreadySent.get()) {
                messageQueue.add((Message) msg);
                return;
            }
        }

        super.channelRead(ctx, msg);
    }

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg, final @NotNull ChannelPromise promise)
            throws Exception {

        if (msg instanceof CONNACK) {
            final CONNACK connack = (CONNACK) msg;
            if (connack.getReasonCode() == Mqtt5ConnAckReasonCode.SUCCESS) {
                promise.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(final @NotNull ChannelFuture future) {
                        if (future.isSuccess()) {
                            removeSelf(ctx);
                            connackAlreadySent.set(true);
                            releaseQueuedMessages(ctx);
                        }
                    }

                    private void releaseQueuedMessages(final @NotNull ChannelHandlerContext ctx) {
                        for (final Message message : messageQueue) {
                            ctx.fireChannelRead(message);
                        }
                    }
                });
            }
        }

        super.write(ctx, msg, promise);
    }

    private void removeSelf(final ChannelHandlerContext ctx) {
        ctx.pipeline().remove(this);
    }

    @VisibleForTesting
    boolean getConnectAlreadySent() {
        return connectAlreadySent.get();
    }

    @VisibleForTesting
    @NotNull Collection<Message> getQueue() {
        return Collections.unmodifiableCollection(messageQueue);
    }
}
