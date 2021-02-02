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
package com.hivemq.mqtt.handler.unsubscribe;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.connect.SubscribeMessageBarrier;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.reason.Mqtt5UnsubAckReasonCode;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.clientsession.SharedSubscriptionService;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static com.hivemq.persistence.clientsession.SharedSubscriptionServiceImpl.SharedSubscription;

/**
 * The Unsubscribe handler which is responsible for handling unsubscription of MQTT clients
 *
 * @author Florian Limpoeck
 * @author Dominik Obermaier
 */
public class UnsubscribeHandler extends SimpleChannelInboundHandler<UNSUBSCRIBE> {


    private static final Logger log = LoggerFactory.getLogger(UnsubscribeHandler.class);

    private final @NotNull ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence;
    private final @NotNull SharedSubscriptionService sharedSubscriptionService;

    @Inject
    public UnsubscribeHandler(final @NotNull ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence,
                              final @NotNull SharedSubscriptionService sharedSubscriptionService) {
        this.clientSessionSubscriptionPersistence = clientSessionSubscriptionPersistence;
        this.sharedSubscriptionService = sharedSubscriptionService;
    }


    @Override
    protected void channelRead0(@NotNull final ChannelHandlerContext ctx, @NotNull final UNSUBSCRIBE msg) throws Exception {

        SubscribeMessageBarrier.addToPipeline(ctx);

        final String clientId = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();
        final ProtocolVersion protocolVersion = ctx.channel().attr(ChannelAttributes.MQTT_VERSION).get();
        final ImmutableList.Builder<ListenableFuture<Void>> builder = ImmutableList.builder();

        final Mqtt5UnsubAckReasonCode[] reasonCodes = new Mqtt5UnsubAckReasonCode[msg.getTopics().size()];

        final ListenableFuture<Void> future;
        if (batch(msg)) {
            future = clientSessionSubscriptionPersistence.removeSubscriptions(clientId, ImmutableSet.copyOf(msg.getTopics()));
            for (int i = 0; i < msg.getTopics().size(); i++) {
                reasonCodes[i] = Mqtt5UnsubAckReasonCode.SUCCESS;
            }
        } else {
            for (int i = 0; i < msg.getTopics().size(); i++) {
                final String topic = msg.getTopics().get(i);
                builder.add(clientSessionSubscriptionPersistence.remove(clientId, topic));
                reasonCodes[i] = Mqtt5UnsubAckReasonCode.SUCCESS;
                log.trace("Unsubscribed from topic [{}] for client [{}]", topic, clientId);
            }
            future = FutureUtils.voidFutureFromList(builder.build());
        }
        log.trace("Applied all unsubscriptions for client [{}]", clientId);


        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(@NotNull final Void aVoid) {

                for (final String topic : msg.getTopics()) {
                    final SharedSubscription sharedSubscription = sharedSubscriptionService.checkForSharedSubscription(topic);
                    if (sharedSubscription != null) {
                        sharedSubscriptionService.invalidateSharedSubscriberCache(sharedSubscription.getShareName() + "/" + sharedSubscription.getTopicFilter());
                        sharedSubscriptionService.invalidateSharedSubscriptionCache(clientId);
                    }
                }

                if (ProtocolVersion.MQTTv5 == protocolVersion) {
                    ctx.writeAndFlush(new UNSUBACK(msg.getPacketIdentifier(), reasonCodes));
                } else {
                    ctx.writeAndFlush(new UNSUBACK(msg.getPacketIdentifier()));
                }
            }

            @Override
            public void onFailure(@NotNull final Throwable throwable) {

                //DON'T ACK for MQTT 3
                if (ProtocolVersion.MQTTv5 == protocolVersion) {
                    for (int i = 0; i < msg.getTopics().size(); i++) {
                        reasonCodes[i] = Mqtt5UnsubAckReasonCode.UNSPECIFIED_ERROR;
                    }
                    ctx.writeAndFlush(new UNSUBACK(msg.getPacketIdentifier(), reasonCodes));
                }

                Exceptions.rethrowError("Unable to unsubscribe client " + clientId + ".", throwable);
            }
        }, ctx.executor());

    }

    @VisibleForTesting
    boolean batch(@NotNull final UNSUBSCRIBE msg) {
        return msg.getTopics().size() >= 2;
    }
}
