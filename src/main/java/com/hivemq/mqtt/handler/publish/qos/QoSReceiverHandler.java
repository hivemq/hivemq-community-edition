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

package com.hivemq.mqtt.handler.publish.qos;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistence;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRE_ON_DISCONNECT;

/**
 * The Quality of service handler for incoming QoS message flows.
 *
 * @author Dominik Obermaier
 */
public class QoSReceiverHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(QoSReceiverHandler.class);

    //must be static as a new instance of qos receiver handler is in every channel
    private static final AtomicLong UNACKNOWLEDGED_PUBLISHES_COUNTER = new AtomicLong();

    private final IncomingMessageFlowPersistence persistence;
    private final Map<Integer, Boolean> qos1And2AlreadySentMap;

    @Inject
    QoSReceiverHandler(final IncomingMessageFlowPersistence persistence) {
        this.persistence = persistence;
        qos1And2AlreadySentMap = new HashMap<>();
    }


    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof PUBLISH) {
            handlePublish(ctx, (PUBLISH) msg);
        }
        //QoS2 - receiver of publish
        else if (msg instanceof PUBREL) {
            handlePubrel(ctx, (PUBREL) msg);
        } else {
            //We're not interested in the message
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {

        if (msg instanceof PUBACK) {
            final PUBACK puback = (PUBACK) msg;
            final String client = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();
            final int messageId = puback.getPacketIdentifier();
            persistence.addOrReplace(client, messageId, puback);
            promise.addListener(new PUBLISHFlowCompleteListener(messageId, client, qos1And2AlreadySentMap, persistence));
        }

        super.write(ctx, msg, promise);
    }

    private void handlePublish(final ChannelHandlerContext ctx, final PUBLISH publish) throws Exception {
        switch (publish.getQoS()) {

            case AT_MOST_ONCE:
                // do nothing
                super.channelRead(ctx, publish);
                break;

            // QoS 1 or 2 duplicate delivery handling
            default:
                UNACKNOWLEDGED_PUBLISHES_COUNTER.incrementAndGet();
                final String client = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();
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
                break;
        }
    }

    private void firstPublishForMessageIdReceived(final ChannelHandlerContext ctx, final PUBLISH publish, final String client, final int messageId) throws Exception {
        persistence.addOrReplace(client, messageId, publish);
        super.channelRead(ctx, publish);
        qos1And2AlreadySentMap.put(messageId, true);
        log.trace("Client {} sent a publish message with id {} which was not forwarded before. This message is processed normally", client, messageId);
    }

    private void resentWithDUPFlag(final ChannelHandlerContext ctx, final PUBLISH publish, final String client) throws Exception {
        final Boolean alreadySent = qos1And2AlreadySentMap.get(publish.getPacketIdentifier());
        if (alreadySent != null && alreadySent) {

            log.debug("Client {} sent a duplicate publish message with id {}. This message is ignored", client, publish.getPacketIdentifier());
        } else {
            super.channelRead(ctx, publish);
            log.debug("Client {} sent a duplicate publish message with id {} which was not forwarded before. This message is processed normally", client, publish.getPacketIdentifier());
        }
        qos1And2AlreadySentMap.put(publish.getPacketIdentifier(), true);
    }

    private void resentWithoutDUPFlag(final ChannelHandlerContext ctx, final PUBLISH publish, final String client) throws Exception {
        log.debug("Client {} sent a new PUBLISH with QoS {} and a message identifier which is already in process ({}) by another flow! Starting new flow",
                client, publish.getQoS().getQosNumber(), publish.getPacketIdentifier());
        persistence.addOrReplace(client, publish.getPacketIdentifier(), publish);
        super.channelRead(ctx, publish);
        qos1And2AlreadySentMap.put(publish.getPacketIdentifier(), true);
    }

    private void handlePubrel(final ChannelHandlerContext ctx, final PUBREL pubrel) {
        final String client = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();

        final int messageId = pubrel.getPacketIdentifier();

        persistence.addOrReplace(client, messageId, pubrel);
        ctx.writeAndFlush(new PUBCOMP(messageId)).addListener(
                new PUBLISHFlowCompleteListener(messageId, client, qos1And2AlreadySentMap, persistence));
    }


    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {

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


    @Immutable
    private static class PUBLISHFlowCompleteListener implements ChannelFutureListener {
        private final int messageId;
        private final String client;
        private final Map<Integer, Boolean> qos1And2AlreadySentMap;
        private final IncomingMessageFlowPersistence persistence;

        PUBLISHFlowCompleteListener(final int messageId, final String client,
                                    final Map<Integer, Boolean> qos1And2AlreadySentMap,
                                    final IncomingMessageFlowPersistence persistence) {
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
