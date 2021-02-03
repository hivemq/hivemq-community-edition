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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.handler.IncomingPublishHandler;
import com.hivemq.mqtt.event.PublishDroppedEvent;
import com.hivemq.mqtt.event.PubrelDroppedEvent;
import com.hivemq.mqtt.message.MessageIDPools;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.pool.MessageIDPool;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistence;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.hivemq.configuration.service.InternalConfigurations.DROP_MESSAGES_QOS_0;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRE_ON_DISCONNECT;

/**
 * @author Florian Limpöck
 */
public class PublishFlowHandler extends ChannelDuplexHandler {

    @NotNull
    private static final Logger log = LoggerFactory.getLogger(PublishFlowHandler.class);

    //must be static as a new instance of qos receiver handler is in every channel
    private static final @NotNull AtomicLong UNACKNOWLEDGED_PUBLISHES_COUNTER = new AtomicLong();

    private final @NotNull IncomingMessageFlowPersistence persistence;
    private final @NotNull OrderedTopicService orderedTopicService;
    private final @NotNull PublishPollService publishPollService;
    private final @NotNull MessageIDPools messageIDPools;
    private final @NotNull IncomingPublishHandler incomingPublishHandler;
    private final @NotNull DropOutgoingPublishesHandler dropOutgoingPublishesHandler;

    private final @NotNull Map<Integer, Boolean> qos1And2AlreadySentMap;

    @VisibleForTesting
    @Inject
    public PublishFlowHandler(final @NotNull PublishPollService publishPollService,
                              final @NotNull IncomingMessageFlowPersistence persistence,
                              final @NotNull OrderedTopicService orderedTopicService,
                              final @NotNull MessageIDPools messageIDPools,
                              final @NotNull IncomingPublishHandler incomingPublishHandler,
                              final @NotNull DropOutgoingPublishesHandler dropOutgoingPublishesHandler) {
        this.publishPollService = publishPollService;
        this.persistence = persistence;
        this.orderedTopicService = orderedTopicService;
        this.qos1And2AlreadySentMap = new HashMap<>();
        this.messageIDPools = messageIDPools;
        this.incomingPublishHandler = incomingPublishHandler;
        this.dropOutgoingPublishesHandler = dropOutgoingPublishesHandler;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, @NotNull final Object msg) throws Exception {

        final String client = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();

        if (msg instanceof PUBLISH) {

            handlePublish(ctx, (PUBLISH) msg, client);

        } else if (msg instanceof PUBACK) {

            handlePuback(ctx, (PUBACK) msg, client);

        } else if (msg instanceof PUBREC) {

            handlePubrec(ctx, (PUBREC) msg, client);

        } else if (msg instanceof PUBREL) {

            handlePubrel(ctx, (PUBREL) msg);

        } else if (msg instanceof PUBCOMP) {

            handlePubcomp(ctx, (PUBCOMP) msg, client);

        } else {
            super.channelRead(ctx, msg);

        }
    }

    @Override
    public void write(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg, final @NotNull ChannelPromise promise) throws Exception {

        if(!(msg instanceof PUBLISH || msg instanceof PUBACK || msg instanceof PUBREL)){
            super.write(ctx, msg, promise);
            return;
        }

        if (msg instanceof PUBACK) {
            final PUBACK puback = (PUBACK) msg;
            final String client = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();
            final int messageId = puback.getPacketIdentifier();
            persistence.addOrReplace(client, messageId, puback);
            promise.addListener(new PUBLISHFlowCompleteListener(messageId, client, qos1And2AlreadySentMap, persistence));
        }

        final boolean flowComplete = orderedTopicService.handlePublish(ctx.channel(), msg, promise);
        if (flowComplete) {
            return;
        }

        if(DROP_MESSAGES_QOS_0){
            final boolean messageDropped = dropOutgoingPublishesHandler.checkChannelNotWritable(ctx, msg, promise);
            if(messageDropped){
                return;
            }
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void userEventTriggered(@NotNull final ChannelHandlerContext ctx, @NotNull final Object evt) throws Exception {
        if (evt instanceof PublishDroppedEvent) {
            final PublishDroppedEvent publishDroppedEvent = (PublishDroppedEvent) evt;
            // Already logged, just proceeded with with the next message
            orderedTopicService.messageFlowComplete(ctx, publishDroppedEvent.getMessage().getPacketIdentifier());
            return;
        }
        if (evt instanceof PubrelDroppedEvent) {
            final PubrelDroppedEvent pubrelDroppedEvent = (PubrelDroppedEvent) evt;
            orderedTopicService.messageFlowComplete(ctx, pubrelDroppedEvent.getMessage().getPacketIdentifier());
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {

        orderedTopicService.handleInactive();

        final Long sessionExpiryInterval = ctx.channel().attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).get();

        //remove incoming message flow for not persisted client
        if (sessionExpiryInterval != null && sessionExpiryInterval == SESSION_EXPIRE_ON_DISCONNECT) {
            final String clientId = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();
            if (clientId != null) {   //Just to be save. The client id should never be null, if the persistent session is not null.
                persistence.delete(clientId);
            }
        }
        super.channelInactive(ctx);
    }

    private void handlePublish(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBLISH publish, final @NotNull String client) throws Exception {
        if (publish.getQoS() == QoS.AT_MOST_ONCE) {// do nothing
            incomingPublishHandler.interceptOrDelegate(ctx, publish, client);
            // QoS 1 or 2 duplicate delivery handling
        } else {
            UNACKNOWLEDGED_PUBLISHES_COUNTER.incrementAndGet();
            final int messageId = publish.getPacketIdentifier();
            final MessageWithID savedMessage = persistence.get(client, messageId);

            //No PUBLISH message was found in persistence. This is the standard case since we don't know this message yet
            if (!(savedMessage instanceof PUBLISH)) {
                firstPublishForMessageIdReceived(ctx, publish, client, messageId);
                //The publish was resent with the DUP flag
            } else if (publish.isDuplicateDelivery()) {
                resentWithDUPFlag(ctx, publish, client);
                //The publish was resent without DUP flag!
            } else {
                resentWithoutDUPFlag(ctx, publish, client);
            }
        }
    }

    private void firstPublishForMessageIdReceived(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBLISH publish, final @NotNull String client, final int messageId) throws Exception {
        persistence.addOrReplace(client, messageId, publish);
        incomingPublishHandler.interceptOrDelegate(ctx, publish, client);
        qos1And2AlreadySentMap.put(messageId, true);
        log.trace("Client {} sent a publish message with id {} which was not forwarded before. This message is processed normally", client, messageId);
    }

    private void resentWithDUPFlag(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBLISH publish, final @NotNull String client) throws Exception {
        final Boolean alreadySent = qos1And2AlreadySentMap.get(publish.getPacketIdentifier());
        if (alreadySent != null && alreadySent) {

            log.debug("Client {} sent a duplicate publish message with id {}. This message is ignored", client, publish.getPacketIdentifier());
        } else {
            super.channelRead(ctx, publish);
            log.debug("Client {} sent a duplicate publish message with id {} which was not forwarded before. This message is processed normally", client, publish.getPacketIdentifier());
        }
        qos1And2AlreadySentMap.put(publish.getPacketIdentifier(), true);
    }

    private void resentWithoutDUPFlag(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBLISH publish, final @NotNull String client) throws Exception {
        log.debug("Client {} sent a new PUBLISH with QoS {} and a message identifier which is already in process ({}) by another flow! Starting new flow",
                client, publish.getQoS().getQosNumber(), publish.getPacketIdentifier());
        persistence.addOrReplace(client, publish.getPacketIdentifier(), publish);
        incomingPublishHandler.interceptOrDelegate(ctx, publish, client);
        qos1And2AlreadySentMap.put(publish.getPacketIdentifier(), true);
    }


    private void handlePuback(final @NotNull ChannelHandlerContext ctx, final PUBACK msg, @NotNull final String client) {

        log.trace("Client {}: Received PUBACK", client);
        final int messageId = msg.getPacketIdentifier();
        orderedTopicService.messageFlowComplete(ctx, messageId);
        returnMessageId(msg, client);

        if (log.isTraceEnabled()) {
            log.trace("Client {}: Received PUBACK remove message id:[{}] ", client, messageId);
        }
    }

    private void handlePubrec(@NotNull final ChannelHandlerContext ctx, @NotNull final PUBREC msg, @NotNull final String client) {
        log.trace("Client {}: Received pubrec", client);

        if (msg.getReasonCode() != Mqtt5PubRecReasonCode.SUCCESS &&
                msg.getReasonCode() != Mqtt5PubRecReasonCode.NO_MATCHING_SUBSCRIBERS) {
            orderedTopicService.messageFlowComplete(ctx, ((MessageWithID) msg).getPacketIdentifier());
        }

        final ListenableFuture<Void> future = publishPollService.putPubrelInQueue(client, msg.getPacketIdentifier());
        FutureUtils.addExceptionLogger(future);
        if (log.isTraceEnabled()) {
            log.trace("Client {}: Received PUBREC remove message id:[{}]", client, msg.getPacketIdentifier());
        }
        //We send it with channel instead of context because otherwise we can't intercept the write in this handler
        ctx.channel().writeAndFlush(new PUBREL(msg.getPacketIdentifier()));
    }

    private void handlePubrel(final ChannelHandlerContext ctx, final PUBREL pubrel) {
        final String client = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();

        final int messageId = pubrel.getPacketIdentifier();

        persistence.addOrReplace(client, messageId, pubrel);
        ctx.writeAndFlush(new PUBCOMP(messageId)).addListener(
                new PUBLISHFlowCompleteListener(messageId, client, qos1And2AlreadySentMap, persistence));
    }

    private void handlePubcomp(final @NotNull ChannelHandlerContext ctx, @NotNull final PUBCOMP msg, @NotNull final String client) {
        log.trace("Client {}: Received PUBCOMP", client);

        orderedTopicService.messageFlowComplete(ctx, msg.getPacketIdentifier());
        returnMessageId(msg, client);

        if (log.isTraceEnabled()) {
            log.trace("Client {}: Received PUBCOMP remove message id:[{}]", client, msg.getPacketIdentifier());
        }

    }

    private void returnMessageId(final @NotNull MessageWithID msg, final @NotNull String clientId) {

        final int messageId = msg.getPacketIdentifier();

        //Such a message ID must never be zero, but better be safe than sorry
        if (messageId > 0) {
            final MessageIDPool messageIDPool = messageIDPools.forClientOrNull(clientId);
            if (messageIDPool != null) {
                messageIDPool.returnId(messageId);
            }
            if (log.isTraceEnabled()) {
                log.trace("Returning Message ID {} for client {} because of a {} message was received", messageId, clientId, msg.getClass().getSimpleName());
            }
        }

    }

    @Immutable
    private static class PUBLISHFlowCompleteListener implements ChannelFutureListener {

        private final int messageId;
        private final @NotNull String client;
        private final @NotNull Map<Integer, Boolean> qos1And2AlreadySentMap;
        private final @NotNull IncomingMessageFlowPersistence persistence;

        PUBLISHFlowCompleteListener(final int messageId,
                                    final @NotNull String client,
                                    final @NotNull Map<Integer, Boolean> qos1And2AlreadySentMap,
                                    final @NotNull IncomingMessageFlowPersistence persistence) {
            this.messageId = messageId;
            this.client = client;
            this.qos1And2AlreadySentMap = qos1And2AlreadySentMap;
            this.persistence = persistence;
        }

        @Override
        public void operationComplete(final ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                UNACKNOWLEDGED_PUBLISHES_COUNTER.decrementAndGet();
                qos1And2AlreadySentMap.remove(messageId);
                persistence.remove(client, messageId);
                log.trace("Client '{}' completed a PUBLISH flow with QoS 1 or 2 for packet identifier '{}'", client, messageId);
            }
        }
    }
}
