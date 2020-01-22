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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import io.netty.channel.*;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * After a subscribe message arrived, we have to queue all messages until the subscribe was handled.
 * Otherwise the subscription would be ignored for publishes that are sent shortly after the subscribe message.
 *
 * @author Lukas Brandl
 */
public class SubscribeMessageBarrier extends ChannelDuplexHandler {

    private final AtomicBoolean subscribeInProcess = new AtomicBoolean(false);

    private final Queue<Message> messageQueue = new LinkedList<>();

    @Override
    public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) throws Exception {

        if (msg instanceof Message && !(msg instanceof PINGREQ)) {

            if ((msg instanceof SUBSCRIBE || msg instanceof UNSUBSCRIBE) &&
                    subscribeInProcess.compareAndSet(false, true)) {
                ctx.channel().config().setAutoRead(false);
                super.channelRead(ctx, msg);
                return;
            }

            if (subscribeInProcess.get()) {
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

        if (msg instanceof SUBACK || msg instanceof UNSUBACK) {
            promise.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(final @NotNull ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        final boolean allMessagesReleased = releaseQueuedMessages(ctx);
                        subscribeInProcess.set(!allMessagesReleased);
                        ctx.channel().config().setAutoRead(allMessagesReleased);
                    }
                }

                private boolean releaseQueuedMessages(final @NotNull ChannelHandlerContext ctx) {
                    while (messageQueue.size() > 0) {
                        final Message message = messageQueue.poll();
                        ctx.fireChannelRead(message);
                        if (message instanceof SUBSCRIBE || message instanceof UNSUBSCRIBE) {
                            return false;
                        }
                    }
                    return true;
                }
            });
        }

        super.write(ctx, msg, promise);
    }


    @VisibleForTesting
    boolean getSubscribeInProcess() {
        return subscribeInProcess.get();
    }


    @VisibleForTesting
    @NotNull Collection<Message> getQueue() {
        return Collections.unmodifiableCollection(messageQueue);
    }

}
