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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.connect.SubscribeMessageBarrier;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.reason.Mqtt5UnsubAckReasonCode;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.clientsession.SharedSubscriptionService;
import com.hivemq.util.Exceptions;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.bootstrap.ClientConnection.CHANNEL_ATTRIBUTE_NAME;

@Singleton
@ChannelHandler.Sharable
public class UnsubscribeHandler extends SimpleChannelInboundHandler<UNSUBSCRIBE> {

    private static final Logger log = LoggerFactory.getLogger(UnsubscribeHandler.class);

    private final @NotNull ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence;
    private final @NotNull SharedSubscriptionService sharedSubscriptionService;

    @Inject
    public UnsubscribeHandler(
            final @NotNull ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence,
            final @NotNull SharedSubscriptionService sharedSubscriptionService) {
        this.clientSessionSubscriptionPersistence = clientSessionSubscriptionPersistence;
        this.sharedSubscriptionService = sharedSubscriptionService;
    }

    @Override
    protected void channelRead0(
            final @NotNull ChannelHandlerContext ctx, final @NotNull UNSUBSCRIBE msg) throws Exception {
        SubscribeMessageBarrier.addToPipeline(ctx);

        final ClientConnection clientConnection = ctx.channel().attr(CHANNEL_ATTRIBUTE_NAME).get();
        final String clientId = checkNotNull(clientConnection.getClientId());

        final UnsubscribeOperationCompletionCallback unsubscribeOperationCompletionCallback =
                new UnsubscribeOperationCompletionCallback(ctx,
                        sharedSubscriptionService,
                        clientConnection.getProtocolVersion(),
                        clientId,
                        msg.getTopics(),
                        msg.getPacketIdentifier());

        if (msg.getTopics().size() == 1) { // Single unsubscribe.
            final String topic = msg.getTopics().get(0);
            final ListenableFuture<Void> future = clientSessionSubscriptionPersistence.remove(clientId, topic);

            future.addListener(() -> {
                if (log.isTraceEnabled()) {
                    log.trace("Unsubscribed from topic [{}] for client [{}]", topic, clientId);
                }
            }, MoreExecutors.directExecutor());

            Futures.addCallback(future, unsubscribeOperationCompletionCallback, ctx.executor());

            if (log.isTraceEnabled()) {
                log.trace("Applied all unsubscriptions for client [{}]", clientId);
            }

            return;
        }

        // Batch unsubscribe. The decoded UNSUBSCRIBE message is guaranteed to have at least one topic filter.
        final ListenableFuture<Void> future = clientSessionSubscriptionPersistence.removeSubscriptions(clientId,
                ImmutableSet.copyOf(msg.getTopics()));

        future.addListener(() -> msg.getTopics().forEach(topic -> {
            if (log.isTraceEnabled()) {
                log.trace("Unsubscribed from topic [{}] for client [{}]", topic, clientId);
            }
        }), MoreExecutors.directExecutor());

        Futures.addCallback(future, unsubscribeOperationCompletionCallback, ctx.executor());

        if (log.isTraceEnabled()) {
            log.trace("Applied all unsubscriptions for client [{}]", clientId);
        }
    }

    private static class UnsubscribeOperationCompletionCallback implements FutureCallback<Void> {

        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull SharedSubscriptionService sharedSubscriptionService;
        private final @NotNull ProtocolVersion protocolVersion;
        private final @NotNull String clientId;
        private final @NotNull ImmutableList<String> topicFilters;
        private final int packetIdentifier;

        UnsubscribeOperationCompletionCallback(
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull SharedSubscriptionService sharedSubscriptionService,
                final @NotNull ProtocolVersion protocolVersion,
                final @NotNull String clientId,
                final @NotNull ImmutableList<String> topicFilters,
                final int packetIdentifier) {
            this.ctx = ctx;
            this.sharedSubscriptionService = sharedSubscriptionService;
            this.protocolVersion = protocolVersion;
            this.clientId = clientId;
            this.topicFilters = topicFilters;
            this.packetIdentifier = packetIdentifier;
        }

        @Override
        public void onSuccess(final @NotNull Void aVoid) {
            // We only need to invalidate the caches for the node at which the client is connected,
            // since this node decides which client will receive messages for the shared subscriptions
            for (final String topicFilter : topicFilters) {
                final SharedSubscriptionService.SharedSubscription sharedSubscription =
                        SharedSubscriptionService.checkForSharedSubscription(topicFilter);
                if (sharedSubscription != null) {
                    sharedSubscriptionService.invalidateSharedSubscriberCache(sharedSubscription.getShareName() +
                            "/" +
                            sharedSubscription.getTopicFilter());
                    sharedSubscriptionService.invalidateSharedSubscriptionCache(clientId);
                }
            }

            if (ProtocolVersion.MQTTv5 == protocolVersion) {
                final Mqtt5UnsubAckReasonCode[] reasonCodes = new Mqtt5UnsubAckReasonCode[topicFilters.size()];
                Arrays.fill(reasonCodes, Mqtt5UnsubAckReasonCode.SUCCESS);
                ctx.writeAndFlush(new UNSUBACK(packetIdentifier, reasonCodes));
            } else {
                ctx.writeAndFlush(new UNSUBACK(packetIdentifier));
            }
        }

        @Override
        public void onFailure(final @NotNull Throwable throwable) {
            //DON'T ACK for MQTT 3
            if (ProtocolVersion.MQTTv5 == protocolVersion) {
                final Mqtt5UnsubAckReasonCode[] reasonCodes = new Mqtt5UnsubAckReasonCode[topicFilters.size()];
                Arrays.fill(reasonCodes, Mqtt5UnsubAckReasonCode.UNSPECIFIED_ERROR);
                ctx.writeAndFlush(new UNSUBACK(packetIdentifier, reasonCodes));
            }

            Exceptions.rethrowError("Unable to unsubscribe client " + clientId + ".", throwable);
        }
    }
}
