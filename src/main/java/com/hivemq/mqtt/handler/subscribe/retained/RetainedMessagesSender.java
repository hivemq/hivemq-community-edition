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

package com.hivemq.mqtt.handler.subscribe.retained;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.*;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.configuration.HivemqId;
import com.hivemq.mqtt.callback.PublishChannelInactiveCallback;
import com.hivemq.mqtt.callback.PublishStoredInPersistenceCallback;
import com.hivemq.mqtt.handler.publish.ChannelInactiveHandler;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.MessageIDPools;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.pool.MessageIDPool;
import com.hivemq.mqtt.message.pool.exception.NoMessageIdAvailableException;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.PublishUtil;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility methods for using retained messages
 *
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 */
@Singleton
public class RetainedMessagesSender {

    private static final Logger log = LoggerFactory.getLogger(RetainedMessagesSender.class);
    private static final ClosedChannelException CLOSED_CHANNEL_EXCEPTION = new ClosedChannelException();

    static {
        CLOSED_CHANNEL_EXCEPTION.setStackTrace(new StackTraceElement[0]);
    }

    private final @NotNull HivemqId hiveMQId;
    private final @NotNull PublishPayloadPersistence publishPayloadPersistence;
    private final @NotNull RetainedMessagePersistence retainedMessagePersistence;
    private final @NotNull MessageIDPools messageIDPools;
    private final @NotNull PublishPollService publishPollService;

    @Inject
    public RetainedMessagesSender(
            final @NotNull HivemqId hiveMQId,
            final @NotNull PublishPayloadPersistence publishPayloadPersistence,
            final @NotNull RetainedMessagePersistence retainedMessagePersistence,
            final @NotNull MessageIDPools messageIDPools,
            final @NotNull PublishPollService publishPollService) {

        this.hiveMQId = hiveMQId;
        this.publishPayloadPersistence = publishPayloadPersistence;
        this.retainedMessagePersistence = retainedMessagePersistence;
        this.messageIDPools = messageIDPools;
        this.publishPollService = publishPollService;
    }

    /**
     * Writes out the retained message for a given topic to the given {@link io.netty.channel.Channel}
     * <p>
     *
     * @param subscribedTopic the topic to get the retained message for. Must not include wildcards.
     * @param channel         the channel to write the retained message to.
     */
    public @NotNull ListenableFuture<Void> writeRetainedMessage(
            final @Nullable Topic subscribedTopic, final @NotNull Channel channel) {

        if (subscribedTopic == null) {
            return Futures.immediateFuture(null);
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        final ListenableFuture<RetainedMessage> future = retainedMessagePersistence.get(subscribedTopic.getTopic());
        final SettableFuture<Void> resultFuture = SettableFuture.create();
        Futures.addCallback(future, new SendRetainedMessageCallback(subscribedTopic, hiveMQId,
                publishPayloadPersistence, messageIDPools, clientId, resultFuture, channel, publishPollService), channel.eventLoop());

        return resultFuture;

    }

    private static class SendRetainedMessageCallback implements FutureCallback<RetainedMessage> {
        private final @NotNull Topic subscribedTopic;
        private final @NotNull HivemqId hivemqId;
        private final @NotNull PublishPayloadPersistence payloadPersistence;
        private final @NotNull MessageIDPools messageIDPools;
        private final @NotNull String clientId;
        private final @NotNull SettableFuture<Void> resultFuture;
        private final @NotNull Channel channel;
        private final @NotNull PublishPollService publishPollService;

        SendRetainedMessageCallback(
                final @NotNull Topic subscribedTopic,
                final @NotNull HivemqId hivemqId,
                final @NotNull PublishPayloadPersistence payloadPersistence,
                final @NotNull MessageIDPools messageIDPools,
                final @NotNull String clientId,
                final @NotNull SettableFuture<Void> resultFuture,
                final @NotNull Channel channel,
                final @NotNull PublishPollService publishPollService) {

            this.subscribedTopic = subscribedTopic;
            this.hivemqId = hivemqId;
            this.payloadPersistence = payloadPersistence;
            this.messageIDPools = messageIDPools;
            this.clientId = clientId;
            this.resultFuture = resultFuture;
            this.channel = channel;
            this.publishPollService = publishPollService;
        }

        @Override
        public void onSuccess(final @Nullable RetainedMessage retainedMessage) {
            if (retainedMessage != null) {
                final QoS qos = PublishUtil.getMinQoS(subscribedTopic.getQoS(), retainedMessage.getQos());
                final ImmutableList<Integer> subscriptionIdentifier;
                if (subscribedTopic.getSubscriptionIdentifier() != null) {
                    subscriptionIdentifier = ImmutableList.of(subscribedTopic.getSubscriptionIdentifier());
                } else {
                    subscriptionIdentifier = ImmutableList.of();
                }

                final PUBLISHFactory.Mqtt5Builder publishBuilder = new PUBLISHFactory.Mqtt5Builder()
                        .withTimestamp(System.currentTimeMillis())
                        .withHivemqId(hivemqId.get())
                        .withPayload(retainedMessage.getMessage())
                        .withPayloadId(retainedMessage.getPayloadId())
                        .withPersistence(payloadPersistence)
                        .withMessageExpiryInterval(retainedMessage.getMessageExpiryInterval())
                        .withTopic(subscribedTopic.getTopic())
                        .withRetain(true)
                        .withDuplicateDelivery(false)
                        .withQoS(qos)
                        .withUserProperties(retainedMessage.getUserProperties())
                        .withResponseTopic(retainedMessage.getResponseTopic())
                        .withContentType(retainedMessage.getContentType())
                        .withCorrelationData(retainedMessage.getCorrelationData())
                        .withPayloadFormatIndicator(retainedMessage.getPayloadFormatIndicator())
                        .withSubscriptionIdentifiers(subscriptionIdentifier);

                //Message ID must not be zero for QoS 1 and 2
                if (qos.getQosNumber() > 0) {
                    try {
                        final int messageId = messageIDPools.forClient(clientId).takeNextId();
                        publishBuilder.withPacketIdentifier(messageId);
                        sendOutMessage(publishBuilder.build());
                    } catch (final NoMessageIdAvailableException e) {
                        resultFuture.setException(e);
                    }
                } else {
                    sendOutMessage(publishBuilder.build());
                }

            }
        }

        private void sendOutMessage(final @NotNull PUBLISH publish) {
            if (!channel.isActive()) {
                payloadPersistence.decrementReferenceCounter(publish.getPayloadId());
                resultFuture.setException(CLOSED_CHANNEL_EXCEPTION);
                return;
            }
            log.trace("Sending retained message with topic [{}] for client [{}]", subscribedTopic, clientId);

            final SettableFuture<PublishStatus> publishFuture = SettableFuture.create();

            Futures.addCallback(publishFuture, new FutureCallback<>() {
                @Override
                public void onSuccess(final @Nullable PublishStatus status) {
                    // remove PubRel from client queue
                    if (publish.getQoS() == QoS.EXACTLY_ONCE) {
                        final ListenableFuture<Void> future = publishPollService.removeMessageFromQueue(clientId, publish.getPacketIdentifier());
                        FutureUtils.addExceptionLogger(future);
                        // check for new messages as the queue was not empty because of the PubRel
                        if (status != PublishStatus.NOT_CONNECTED) {
                            checkForNewMessages();
                        }
                    }

                    if (status == PublishStatus.DELIVERED) {
                        resultFuture.set(null);
                    } else {
                        resultFuture.setException(new ClosedChannelException());
                    }

                    payloadPersistence.decrementReferenceCounter(publish.getPayloadId());
                    if (publish.getPacketIdentifier() != 0) {
                        final MessageIDPool messageIDPool = messageIDPools.forClientOrNull(clientId);
                        if (messageIDPool != null) {
                            messageIDPool.returnId(publish.getPacketIdentifier());
                        }
                    }
                }

                private void checkForNewMessages() {
                    final AtomicInteger inFlightMessages = channel.attr(ChannelAttributes.IN_FLIGHT_MESSAGES).get();
                    if (inFlightMessages != null && inFlightMessages.decrementAndGet() > 0) {
                        return;
                    }
                    publishPollService.pollMessages(clientId, channel);
                }

                @Override
                public void onFailure(final @NotNull Throwable t) {
                    if (t instanceof CancellationException) {
                        //ignore because task was cancelled because channel became inactive and
                        //response has already been sent by callback from ChannelInactiveHandler
                        return;
                    }
                    payloadPersistence.decrementReferenceCounter(publish.getPayloadId());
                }
            }, MoreExecutors.directExecutor());

            final ChannelInactiveHandler channelInactiveHandler = channel.pipeline().get(ChannelInactiveHandler.class);
            final ChannelInactiveHandler.ChannelInactiveCallback channelInactiveCallback = new PublishChannelInactiveCallback(publishFuture);
            if (channelInactiveHandler != null) {
                Futures.addCallback(publishFuture, new PublishStoredInPersistenceCallback(channelInactiveHandler, publish), MoreExecutors.directExecutor());
                channelInactiveHandler.addCallback(publish.getUniqueId(), channelInactiveCallback);
            }
            if (!channel.isActive()) {
                channelInactiveCallback.channelInactive();
                return;
            }

            final PublishWithFuture event = new PublishWithFuture(publish, publishFuture, publish.getPayloadId(), false, payloadPersistence);
            channel.pipeline().fireUserEventTriggered(event);
        }

        @Override
        public void onFailure(final @NotNull Throwable throwable) {
            resultFuture.setException(throwable);
        }
    }
}
