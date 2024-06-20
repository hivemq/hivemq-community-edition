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
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ClientConnectionContext;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.handler.IncomingPublishHandler;
import com.hivemq.mqtt.event.PublishDroppedEvent;
import com.hivemq.mqtt.event.PubrelDroppedEvent;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.pool.FreePacketIdRanges;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistence;
import com.hivemq.persistence.util.FutureUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.hivemq.configuration.service.InternalConfigurations.DROP_MESSAGES_QOS_0_ENABLED;
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
    private final @NotNull IncomingPublishHandler incomingPublishHandler;
    private final @NotNull DropOutgoingPublishesHandler dropOutgoingPublishesHandler;

    private final @NotNull Map<Integer, Boolean> qos1AlreadySentMap;

    @VisibleForTesting
    @Inject
    public PublishFlowHandler(
            final @NotNull PublishPollService publishPollService,
            final @NotNull IncomingMessageFlowPersistence persistence,
            final @NotNull OrderedTopicService orderedTopicService,
            final @NotNull IncomingPublishHandler incomingPublishHandler,
            final @NotNull DropOutgoingPublishesHandler dropOutgoingPublishesHandler) {
        this.publishPollService = publishPollService;
        this.persistence = persistence;
        this.orderedTopicService = orderedTopicService;
        this.qos1AlreadySentMap = new HashMap<>();
        this.incomingPublishHandler = incomingPublishHandler;
        this.dropOutgoingPublishesHandler = dropOutgoingPublishesHandler;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, @NotNull final Object msg) throws Exception {
        if (msg instanceof PUBLISH) {
            handlePublish(ctx, (PUBLISH) msg);
        } else if (msg instanceof PUBACK) {
            handlePuback(ctx, (PUBACK) msg);
        } else if (msg instanceof PUBREC) {
            handlePubrec(ctx, (PUBREC) msg);
        } else if (msg instanceof PUBREL) {
            handlePubrel(ctx, (PUBREL) msg);
        } else if (msg instanceof PUBCOMP) {
            handlePubcomp(ctx, (PUBCOMP) msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg, final @NotNull ChannelPromise promise)
            throws Exception {

        if (!(msg instanceof PUBLISH || msg instanceof PUBACK || msg instanceof PUBREL)) {
            super.write(ctx, msg, promise);
            return;
        }

        if (msg instanceof PUBACK) {
            final PUBACK puback = (PUBACK) msg;
            final int messageId = puback.getPacketIdentifier();
            promise.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    qos1AlreadySentMap.remove(messageId);
                    if (log.isTraceEnabled()) {
                        log.trace("Client '{}' completed a PUBLISH flow with QoS 1 for packet identifier '{}'",
                                ctx,
                                messageId);
                    }
                }
            });
        }

        final boolean flowComplete = orderedTopicService.handlePublish(ctx.channel(), msg, promise);
        if (flowComplete) {
            return;
        }

        if (DROP_MESSAGES_QOS_0_ENABLED) {
            final boolean messageDropped = dropOutgoingPublishesHandler.checkChannelNotWritable(ctx, msg, promise);
            if (messageDropped) {
                return;
            }
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void userEventTriggered(@NotNull final ChannelHandlerContext ctx, @NotNull final Object evt)
            throws Exception {
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

        final ClientConnectionContext clientConnectionContext = ClientConnectionContext.of(ctx.channel());
        final Long sessionExpiryInterval = clientConnectionContext.getClientSessionExpiryInterval();

        //remove incoming message flow for not persisted client
        if (sessionExpiryInterval != null && sessionExpiryInterval == SESSION_EXPIRE_ON_DISCONNECT) {
            final String clientId = clientConnectionContext.getClientId();
            if (clientId !=
                    null) {   //Just to be save. The client id should never be null, if the persistent session is not null.
                persistence.delete(clientId);
            }
        }
        super.channelInactive(ctx);
    }

    private void handlePublish(
            final @NotNull ChannelHandlerContext ctx, final @NotNull PUBLISH publish) throws Exception {

        final String clientId = ClientConnection.of(ctx.channel()).getClientId();

        if (publish.getQoS() == QoS.AT_MOST_ONCE) {// do nothing
            incomingPublishHandler.interceptOrDelegate(ctx, publish, clientId);
            // QoS 1 delivery handling
        } else if (publish.getQoS() == QoS.AT_LEAST_ONCE) {
            UNACKNOWLEDGED_PUBLISHES_COUNTER.incrementAndGet();
            if (publish.isDuplicateDelivery() && qos1AlreadySentMap.get(publish.getPacketIdentifier()) != null) {
                log.debug("Client {} sent a duplicate publish message with id {}. This message is ignored",
                        clientId,
                        publish.getPacketIdentifier());
            } else {
                final int packetId = publish.getPacketIdentifier();
                qos1AlreadySentMap.put(publish.getPacketIdentifier(), true);
                firstPublishForMessageIdReceived(ctx, publish, clientId, packetId);
            }
            // QoS 2 duplicate delivery handling
        } else {
            final int messageId = publish.getPacketIdentifier();
            final MessageWithID savedMessage = persistence.get(clientId, messageId);
            if (!(savedMessage instanceof PUBLISH)) {
                persistence.addOrReplace(clientId, messageId, publish);
                firstPublishForMessageIdReceived(ctx, publish, clientId, messageId);
            } else
                ctx.writeAndFlush(new PUBREC(messageId));
        }
    }

    private void firstPublishForMessageIdReceived(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PUBLISH publish,
            final @NotNull String client,
            final int messageId) throws Exception {
        incomingPublishHandler.interceptOrDelegate(ctx, publish, client);
        log.trace(
                "Client {} sent a publish message with id {} which was not forwarded before. This message is processed normally",
                client,
                messageId);
    }


    private void handlePuback(final @NotNull ChannelHandlerContext ctx, final PUBACK msg) {

        final String clientId = ClientConnection.of(ctx.channel()).getClientId();

        log.trace("Client {}: Received PUBACK", clientId);
        final int messageId = msg.getPacketIdentifier();
        orderedTopicService.messageFlowComplete(ctx, messageId);
        returnMessageId(ctx.channel(), msg, clientId);

        if (log.isTraceEnabled()) {
            log.trace("Client {}: Received PUBACK remove message id:[{}] ", clientId, messageId);
        }
    }

    private void handlePubrec(@NotNull final ChannelHandlerContext ctx, @NotNull final PUBREC msg) {

        final String clientId = ClientConnection.of(ctx.channel()).getClientId();

        log.trace("Client {}: Received pubrec", clientId);

        if (msg.getReasonCode() != Mqtt5PubRecReasonCode.SUCCESS &&
                msg.getReasonCode() != Mqtt5PubRecReasonCode.NO_MATCHING_SUBSCRIBERS) {
            orderedTopicService.messageFlowComplete(ctx, ((MessageWithID) msg).getPacketIdentifier());
        }

        final ListenableFuture<Void> future = publishPollService.putPubrelInQueue(clientId, msg.getPacketIdentifier());
        FutureUtils.addExceptionLogger(future);
        if (log.isTraceEnabled()) {
            log.trace("Client {}: Received PUBREC remove message id:[{}]", clientId, msg.getPacketIdentifier());
        }
        //We send it with channel instead of context because otherwise we can't intercept the write in this handler
        ctx.channel().writeAndFlush(new PUBREL(msg.getPacketIdentifier()));
    }

    private void handlePubrel(final ChannelHandlerContext ctx, final PUBREL pubrel) {

        final String client = ClientConnection.of(ctx.channel()).getClientId();

        final int messageId = pubrel.getPacketIdentifier();

        persistence.addOrReplace(client, messageId, pubrel);
        ctx.writeAndFlush(new PUBCOMP(messageId))
                .addListener(new PubcompSentListener(messageId, client, persistence));
    }

    private void handlePubcomp(final @NotNull ChannelHandlerContext ctx, @NotNull final PUBCOMP msg) {

        final String clientId = ClientConnection.of(ctx.channel()).getClientId();

        log.trace("Client {}: Received PUBCOMP", clientId);

        orderedTopicService.messageFlowComplete(ctx, msg.getPacketIdentifier());
        returnMessageId(ctx.channel(), msg, clientId);

        if (log.isTraceEnabled()) {
            log.trace("Client {}: Received PUBCOMP remove message id:[{}]", clientId, msg.getPacketIdentifier());
        }

    }

    private void returnMessageId(
            final @NotNull Channel channel, final @NotNull MessageWithID msg, final @NotNull String clientId) {

        final int messageId = msg.getPacketIdentifier();

        //Such a message ID must never be zero, but better be safe than sorry
        if (messageId > 0) {
            final FreePacketIdRanges freePacketIdRanges = ClientConnection.of(channel).getFreePacketIdRanges();
            freePacketIdRanges.returnId(messageId);
            if (log.isTraceEnabled()) {
                log.trace("Returning Message ID {} for client {} because of a {} message was received",
                        messageId,
                        clientId,
                        msg.getClass().getSimpleName());
            }
        }

    }

    @Immutable
    private static class PubcompSentListener implements ChannelFutureListener {

        private final int messageId;
        private final @NotNull String client;
        private final @NotNull IncomingMessageFlowPersistence persistence;

        PubcompSentListener(
                final int messageId,
                final @NotNull String client,
                final @NotNull IncomingMessageFlowPersistence persistence) {
            this.messageId = messageId;
            this.client = client;
            this.persistence = persistence;
        }

        @Override
        public void operationComplete(final ChannelFuture future)  {
            if (future.isSuccess()) {
                UNACKNOWLEDGED_PUBLISHES_COUNTER.decrementAndGet();
                persistence.remove(client, messageId);
                log.trace("Client '{}' completed a PUBLISH flow with QoS 2 for packet identifier '{}'",
                        client,
                        messageId);
            }
        }
    }
}
