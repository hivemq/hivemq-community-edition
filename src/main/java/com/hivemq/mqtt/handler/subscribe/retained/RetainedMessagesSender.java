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
package com.hivemq.mqtt.handler.subscribe.retained;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.*;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.handler.publish.PublishWriteFailedListener;
import com.hivemq.mqtt.message.MessageIDPools;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.pool.MessageIDPool;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;

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
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull MessageIDPools messageIDPools;
    private final @NotNull MqttConfigurationService mqttConfigurationService;

    @Inject
    public RetainedMessagesSender(
            final @NotNull HivemqId hiveMQId,
            final @NotNull PublishPayloadPersistence publishPayloadPersistence,
            final @NotNull RetainedMessagePersistence retainedMessagePersistence,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull MessageIDPools messageIDPools,
            final @NotNull MqttConfigurationService mqttConfigurationService) {

        this.hiveMQId = hiveMQId;
        this.publishPayloadPersistence = publishPayloadPersistence;
        this.retainedMessagePersistence = retainedMessagePersistence;
        this.clientQueuePersistence = clientQueuePersistence;
        this.messageIDPools = messageIDPools;
        this.mqttConfigurationService = mqttConfigurationService;
    }

    /**
     * Writes out the retained message for a given topic to the given {@link io.netty.channel.Channel}
     * <p>
     *
     * @param subscribedTopics the topic to get the retained message for. Must not include wildcards.
     * @param channel          the channel to write the retained message to.
     */
    public @NotNull ListenableFuture<Void> writeRetainedMessages(
            final @NotNull Channel channel, final @Nullable Topic... subscribedTopics) {

        if (subscribedTopics == null) {
            return Futures.immediateFuture(null);
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        final ImmutableList.Builder<ListenableFuture<RetainedMessage>> retainedMessageFutures = ImmutableList.builder();
        for (final Topic topic : subscribedTopics) {
            retainedMessageFutures.add(retainedMessagePersistence.get(topic.getTopic()));
        }
        // Futures.allAsList preserves the original order
        final ListenableFuture<List<RetainedMessage>> retainedMessagesFuture =
                Futures.allAsList(retainedMessageFutures.build());

        final SettableFuture<Void> resultFuture = SettableFuture.create();
        Futures.addCallback(retainedMessagesFuture, new SendRetainedMessageCallback(subscribedTopics, hiveMQId,
                publishPayloadPersistence, messageIDPools, clientId, resultFuture, channel, clientQueuePersistence,
                mqttConfigurationService), channel.eventLoop());

        return resultFuture;

    }

    private static class SendRetainedMessageCallback implements FutureCallback<List<RetainedMessage>> {

        private final @NotNull Topic[] subscribedTopics;
        private final @NotNull HivemqId hivemqId;
        private final @NotNull PublishPayloadPersistence payloadPersistence;
        private final @NotNull MessageIDPools messageIDPools;
        private final @NotNull String clientId;
        private final @NotNull SettableFuture<Void> resultFuture;
        private final @NotNull Channel channel;
        private final @NotNull ClientQueuePersistence clientQueuePersistence;
        private final @NotNull MqttConfigurationService mqttConfigurationService;

        SendRetainedMessageCallback(
                final @NotNull Topic[] subscribedTopics,
                final @NotNull HivemqId hivemqId,
                final @NotNull PublishPayloadPersistence payloadPersistence,
                final @NotNull MessageIDPools messageIDPools,
                final @NotNull String clientId,
                final @NotNull SettableFuture<Void> resultFuture,
                final @NotNull Channel channel,
                final @NotNull ClientQueuePersistence clientQueuePersistence,
                final @NotNull MqttConfigurationService mqttConfigurationService) {

            this.subscribedTopics = subscribedTopics;
            this.hivemqId = hivemqId;
            this.payloadPersistence = payloadPersistence;
            this.messageIDPools = messageIDPools;
            this.clientId = clientId;
            this.resultFuture = resultFuture;
            this.channel = channel;
            this.clientQueuePersistence = clientQueuePersistence;
            this.mqttConfigurationService = mqttConfigurationService;
        }

        @Override
        public void onSuccess(final List<RetainedMessage> retainedMessages) {

            final ImmutableList.Builder<PUBLISH> builder = ImmutableList.builder();
            for (int i = 0; i < retainedMessages.size(); i++) {
                final RetainedMessage retainedMessage = retainedMessages.get(i);

                //list can contain null entries
                if (retainedMessage == null) {
                    continue;
                }

                // Topics and retained messages have the same order
                final Topic subscribedTopic = subscribedTopics[i];

                final QoS qos = PublishUtil.getMinQoS(subscribedTopic.getQoS(), retainedMessage.getQos());

                final ImmutableIntArray subscriptionIdentifiers;

                if (subscribedTopic.getSubscriptionIdentifier() != null) {
                    subscriptionIdentifiers = ImmutableIntArray.of(subscribedTopic.getSubscriptionIdentifier());
                } else {
                    subscriptionIdentifiers = ImmutableIntArray.of();
                }

                final PUBLISHFactory.Mqtt5Builder publishBuilder = new PUBLISHFactory.Mqtt5Builder()
                        .withTimestamp(System.currentTimeMillis())
                        .withHivemqId(hivemqId.get())
                        .withPayload(retainedMessage.getMessage())
                        .withPublishId(retainedMessage.getPublishId())
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
                        .withSubscriptionIdentifiers(subscriptionIdentifiers);
                builder.add(publishBuilder.build());

            }
            sendOutMessages(builder.build());
        }

        private void sendOutMessages(final @NotNull List<PUBLISH> retainedPublishes) {
            if (!channel.isActive()) {
                for (final PUBLISH publish : retainedPublishes) {
                    payloadPersistence.decrementReferenceCounter(publish.getPublishId());
                }
                resultFuture.setException(CLOSED_CHANNEL_EXCEPTION);
                return;
            }
            if (log.isTraceEnabled()) {
                for (final Topic topic : subscribedTopics) {
                    log.trace("Sending retained message with topic [{}] for client [{}]", topic.getQoS(), clientId);
                }
            }
            final ImmutableList.Builder<PUBLISH> builder = ImmutableList.builder();
            final ImmutableList.Builder<ListenableFuture<Void>> futures = ImmutableList.builder();
            for (final PUBLISH publish : retainedPublishes) {
                if (publish.getQoS() == QoS.AT_MOST_ONCE) {
                    futures.add(sendQos0PublishDirectly(publish));
                    continue;
                }
                builder.add(publish);
            }
            final ImmutableList<PUBLISH> qos1and2Messages = builder.build();
            if (qos1and2Messages.isEmpty()) {
                resultFuture.setFuture(FutureUtils.voidFutureFromList(futures.build()));
                return;
            }
            final Long queueLimit = channel.attr(ChannelAttributes.QUEUE_SIZE_MAXIMUM).get();
            futures.add(clientQueuePersistence.add(clientId, false, qos1and2Messages, true,
                    Objects.requireNonNullElseGet(queueLimit, mqttConfigurationService::maxQueuedMessages)));
            resultFuture.setFuture(FutureUtils.voidFutureFromList(futures.build()));
        }

        private ListenableFuture<Void> sendQos0PublishDirectly(final @NotNull PUBLISH qos0Publish) {
            final SettableFuture<Void> resultFuture = SettableFuture.create();
            final SettableFuture<PublishStatus> publishFuture = SettableFuture.create();

            Futures.addCallback(publishFuture, new FutureCallback<>() {
                @Override
                public void onSuccess(final @Nullable PublishStatus status) {

                    if (status == PublishStatus.DELIVERED) {
                        resultFuture.set(null);
                    } else {
                        resultFuture.setException(new ClosedChannelException());
                    }

                    payloadPersistence.decrementReferenceCounter(qos0Publish.getPublishId());
                    if (qos0Publish.getPacketIdentifier() != 0) {
                        final MessageIDPool messageIDPool = messageIDPools.forClientOrNull(clientId);
                        if (messageIDPool != null) {
                            messageIDPool.returnId(qos0Publish.getPacketIdentifier());
                        }
                    }
                }

                @Override
                public void onFailure(final @NotNull Throwable t) {
                    if (t instanceof CancellationException) {
                        //ignore because task was cancelled because channel became inactive and
                        //response has already been sent by callback from ChannelInactiveHandler
                        return;
                    }
                    payloadPersistence.decrementReferenceCounter(qos0Publish.getPublishId());
                }
            }, MoreExecutors.directExecutor());

            final PublishWithFuture message = new PublishWithFuture(qos0Publish, publishFuture, false, payloadPersistence);
            channel.writeAndFlush(message).addListener(new PublishWriteFailedListener(publishFuture));

            return resultFuture;
        }

        @Override
        public void onFailure(final @NotNull Throwable throwable) {
            resultFuture.setException(throwable);
        }
    }
}
