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
import com.google.inject.Inject;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static com.hivemq.configuration.service.InternalConfigurations.NOT_WRITABLE_QUEUE_SIZE;

@LazySingleton
public class DropOutgoingPublishesHandler {

    private static final Logger log = LoggerFactory.getLogger(DropOutgoingPublishesHandler.class);

    private final @NotNull PublishPayloadPersistence publishPayloadPersistence;
    private final @NotNull AtomicInteger notWritableMessages = new AtomicInteger();
    private final @NotNull DecrementCounterListener decrementCounterListener = new DecrementCounterListener();
    private final @NotNull MessageDroppedService messageDroppedService;
    private final int notWritableQueueSize;

    @Inject
    public DropOutgoingPublishesHandler(final @NotNull PublishPayloadPersistence publishPayloadPersistence,
                                        final @NotNull MessageDroppedService messageDroppedService) {
        this.publishPayloadPersistence = publishPayloadPersistence;
        this.messageDroppedService = messageDroppedService;
        this.notWritableQueueSize = NOT_WRITABLE_QUEUE_SIZE.get();
    }

    public boolean checkChannelNotWritable(final ChannelHandlerContext ctx, final @NotNull Object msg, final @NotNull ChannelPromise promise) throws Exception {
        if (!ctx.channel().isWritable()) {

            if (msg instanceof PUBLISH) {
                if (notWritableMessages.get() < notWritableQueueSize) {
                    notWritableMessages.incrementAndGet();
                    promise.addListeners(decrementCounterListener);
                    return false;
                }

                final PUBLISH publish = (PUBLISH) msg;
                if ((publish).getQoS() == QoS.AT_MOST_ONCE) {
                    if (msg instanceof PublishWithFuture) {
                        final SettableFuture<PublishStatus> future = ((PublishWithFuture) msg).getFuture();
                        future.set(PublishStatus.CHANNEL_NOT_WRITABLE);
                    }
                    //Drop message
                    final String clientId = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();
                    log.trace("Dropped qos 0 message for client {} on topic {} because the channel was not writable", clientId, publish.getTopic());
                    messageDroppedService.notWritable(clientId, publish.getTopic(), publish.getQoS().getQosNumber());
                    promise.setSuccess();
                    if (publish instanceof PublishWithFuture) {
                        //Don't decrement the reference count here because the message might be resent to an other client
                        //The shared subscription handling will take care of the reference counting
                        if (!((PublishWithFuture) publish).isShared()) {
                            publishPayloadPersistence.decrementReferenceCounter(publish.getPublishId());
                        }
                    } else {
                        publishPayloadPersistence.decrementReferenceCounter(publish.getPublishId());
                    }
                    return true;
                }
            }
        }

        return false;
    }


    private class DecrementCounterListener implements GenericFutureListener<Future<? super Void>> {

        @Override
        public void operationComplete(final @NotNull Future<? super Void> future) throws Exception {
            notWritableMessages.decrementAndGet();
        }
    }
}