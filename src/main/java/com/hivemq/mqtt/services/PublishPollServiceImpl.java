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
package com.hivemq.mqtt.services;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.*;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.callback.PublishStatusFutureCallback;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.handler.publish.PublishWriteFailedListener;
import com.hivemq.mqtt.message.MessageIDPools;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.pool.MessageIDPool;
import com.hivemq.mqtt.message.pool.exception.NoMessageIdAvailableException;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import com.hivemq.mqtt.message.publish.PubrelWithFuture;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import com.hivemq.persistence.ChannelPersistence;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientsession.SharedSubscriptionService;
import com.hivemq.persistence.payload.PayloadPersistenceException;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ChannelUtils;
import com.hivemq.util.Exceptions;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.configuration.service.InternalConfigurations.PUBLISH_POLL_BATCH_MEMORY;

/**
 * @author Lukas Brandl
 */
@LazySingleton
public class PublishPollServiceImpl implements PublishPollService {

    @NotNull
    private static final Logger log = LoggerFactory.getLogger(PublishPollService.class);

    @NotNull
    private final MessageIDPools messageIDPools;
    @NotNull
    private final ClientQueuePersistence clientQueuePersistence;
    @NotNull
    private final ChannelPersistence channelPersistence;
    @NotNull
    private final PublishPayloadPersistence payloadPersistence;
    @NotNull
    private final MessageDroppedService messageDroppedService;
    @NotNull
    private final SharedSubscriptionService sharedSubscriptionService;
    @NotNull
    private final SingleWriterService singleWriterService;

    @Inject
    public PublishPollServiceImpl(@NotNull final MessageIDPools messageIDPools,
                                  @NotNull final ClientQueuePersistence clientQueuePersistence,
                                  @NotNull final ChannelPersistence channelPersistence,
                                  @NotNull final PublishPayloadPersistence payloadPersistence,
                                  @NotNull final MessageDroppedService messageDroppedService,
                                  @NotNull final SharedSubscriptionService sharedSubscriptionService,
                                  @NotNull final SingleWriterService singleWriterService) {
        this.messageIDPools = messageIDPools;
        this.clientQueuePersistence = clientQueuePersistence;
        this.channelPersistence = channelPersistence;
        this.payloadPersistence = payloadPersistence;
        this.messageDroppedService = messageDroppedService;
        this.sharedSubscriptionService = sharedSubscriptionService;
        this.singleWriterService = singleWriterService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pollMessages(@NotNull final String client, @NotNull final Channel channel) {
        checkNotNull(client, "Client must not be null");
        checkNotNull(channel, "Channel must not be null");
        // Null equal false, true will never be set
        final boolean inflightMessagesSent = channel.attr(ChannelAttributes.IN_FLIGHT_MESSAGES_SENT).get() != null;
        if (inflightMessagesSent) {
            pollNewMessages(client, channel);
            final Boolean noSharedSubscriptions = channel.attr(ChannelAttributes.NO_SHARED_SUBSCRIPTION).get();
            if (noSharedSubscriptions != null && noSharedSubscriptions) {
                return;
            }
            try {
                final ImmutableSet<Topic> topics = sharedSubscriptionService.getSharedSubscriptions(client);
                if (topics.isEmpty()) {
                    channel.attr(ChannelAttributes.NO_SHARED_SUBSCRIPTION).setIfAbsent(true);
                    return;
                }
                for (final Topic topic : topics) {
                    final String sharedSubscriptions = sharedSubscriptionService.removePrefix(topic.getTopic());
                    pollSharedPublishesForClient(client, sharedSubscriptions, topic.getQoS().getQosNumber(),
                            topic.getSubscriptionIdentifier(), channel);
                }
            } catch (final ExecutionException e) {
                log.error("Exception while reading shared subscriptions for client " + client, e);
            }

        } else {
            pollInflightMessages(client, channel);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pollNewMessages(@NotNull final String client) {
        final Channel channel = channelPersistence.get(client);
        if (channel == null) {
            return; // client is disconnected
        }
        pollNewMessages(client, channel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pollNewMessages(@NotNull final String client, @NotNull final Channel channel) {
        final MessageIDPool messageIDPool = messageIDPools.forClient(client);
        final ImmutableIntArray messageIds;
        try {
            messageIds = createMessageIds(messageIDPool, pollMessageLimit(channel));
        } catch (final NoMessageIdAvailableException e) {
            // This should never happen if the limit for the poll message limit is set correctly
            log.error("No message id available for client " + client, e);
            return;
        }

        final ListenableFuture<ImmutableList<PUBLISH>> future = clientQueuePersistence.readNew(client, false, messageIds, PUBLISH_POLL_BATCH_MEMORY);

        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(final ImmutableList<PUBLISH> publishes) {
                // Return unused ID's
                int usedIds = 0;
                for (int i = 0; i < publishes.size(); i++) {
                    if (publishes.get(i).getQoS() != QoS.AT_MOST_ONCE) {
                        usedIds++;
                    }
                }
                for (int i = usedIds; i < messageIds.length(); i++) {
                    messageIDPool.returnId(messageIds.get(i));
                }

                final AtomicInteger inFlightMessages = inFlightMessageCount(channel);
                for (final PUBLISH publish : publishes) {
                    inFlightMessages.incrementAndGet();
                    try {
                        sendOutPublish(publish, false, channel, client, messageIDPool, client);
                    } catch (final PayloadPersistenceException e) {
                        // We don't prevent other messages form being published in case the reference is missing
                        log.error("Payload reference error for publish on topic: " + publish.getTopic(), e);
                        if (publish.getQoS().getQosNumber() > 0) {
                            removeMessageFromQueue(client, publish.getPacketIdentifier());
                        }
                        inFlightMessages.decrementAndGet();
                        messageDroppedService.failed(client, publish.getTopic(), publish.getQoS().getQosNumber());
                    }
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                Exceptions.rethrowError("Exception in new messages handling", t);
                channel.disconnect();
            }
        }, singleWriterService.callbackExecutor(client));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pollInflightMessages(@NotNull final String client, @NotNull final Channel channel) {
        final ListenableFuture<ImmutableList<MessageWithID>> future = clientQueuePersistence.readInflight(client, PUBLISH_POLL_BATCH_MEMORY, pollMessageLimit(channel));
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(final ImmutableList<MessageWithID> messages) {
                if (messages.isEmpty()) {
                    channel.attr(ChannelAttributes.IN_FLIGHT_MESSAGES_SENT).set(true);
                    channel.eventLoop().submit(() -> pollMessages(client, channel)); // No more inflight messages
                    return;
                }

                final AtomicInteger inFlightMessageCount = inFlightMessageCount(channel);
                for (final MessageWithID message : messages) {
                    inFlightMessageCount.incrementAndGet();
                    final MessageIDPool messageIDPool = messageIDPools.forClient(client);
                    try {
                        final int packetId = messageIDPool.takeIfAvailable(message.getPacketIdentifier());
                        if (message.getPacketIdentifier() != packetId) {
                            // This should never happen but we need to make sure the packet ID is returned in case this is the result of a retry.
                            messageIDPool.returnId(packetId);
                        }
                    } catch (final NoMessageIdAvailableException e) {
                        // This should never happen if the limit for the poll message limit is set correctly
                        log.error("No message id available for client ." + client, e);
                        if (message instanceof PUBLISH) {
                            messageDroppedService.queueFull(client, ((PUBLISH) message).getTopic(), ((PUBLISH) message).getQoS().getQosNumber());
                        }
                        return;
                    }
                    if (message instanceof PUBLISH) {
                        final PUBLISH publish = (PUBLISH) message;
                        try {
                            sendOutPublish(publish, false, channel, client, messageIDPool, client);
                        } catch (final PayloadPersistenceException e) {
                            // We don't prevent other messages form being published in case on reference is missing
                            log.error("Payload reference error for publish on topic: " + publish.getTopic(), e);
                            if (publish.getQoS().getQosNumber() > 0) {
                                removeMessageFromQueue(client, publish.getPacketIdentifier());
                            }
                            inFlightMessageCount.decrementAndGet();
                            messageDroppedService.failed(client, publish.getTopic(), publish.getQoS().getQosNumber());
                        }
                    } else if (message instanceof PUBREL) {
                        // We don't care if the message is delivered successfully here.
                        // If the client disconnects before we receive a PUBCOMP we will retry anyways.
                        final SettableFuture<PublishStatus> settableFuture = SettableFuture.create();
                        channel.writeAndFlush(new PubrelWithFuture((PUBREL) message, settableFuture));
                        Futures.addCallback(settableFuture, new PubrelResendCallback(client, message, messageIDPool, channel), MoreExecutors.directExecutor());
                    }
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                Exceptions.rethrowError("Exception in inflight messages handling", t);
            }
        }, singleWriterService.callbackExecutor(client));
    }

    private AtomicInteger inFlightMessageCount(@NotNull final Channel channel) {
        AtomicInteger qos0InFlightMessages = channel.attr(ChannelAttributes.IN_FLIGHT_MESSAGES).get();
        if (qos0InFlightMessages == null) {
            qos0InFlightMessages = new AtomicInteger(0);
            channel.attr(ChannelAttributes.IN_FLIGHT_MESSAGES).set(qos0InFlightMessages);
        }
        return qos0InFlightMessages;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pollSharedPublishes(@NotNull final String sharedSubscription) {
        final List<SubscriberWithQoS> subscribers = new ArrayList<>(sharedSubscriptionService.getSharedSubscriber(sharedSubscription));

        // We should shuffle here because otherwise one client could consume all messages if it is fast enough
        Collections.shuffle(subscribers);
        for (final SubscriberWithQoS subscriber : subscribers) {
            final Channel channel = channelPersistence.get(subscriber.getSubscriber());
            if (channel == null) {
                continue; // client is disconnected
            }
            pollSharedPublishesForClient(subscriber.getSubscriber(), sharedSubscription, subscriber.getQos(), subscriber.getSubscriptionIdentifier(), channel);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pollSharedPublishesForClient(@NotNull final String client,
                                             @NotNull final String sharedSubscription,
                                             final int qos,
                                             @Nullable final Integer subscriptionIdentifier,
                                             @NotNull final Channel channel) {
        if (ChannelUtils.messagesInFlight(channel)) {
            return;
        }

        final ListenableFuture<ImmutableList<PUBLISH>> future = clientQueuePersistence.readShared(sharedSubscription, pollMessageLimit(channel), PUBLISH_POLL_BATCH_MEMORY);

        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(@NotNull final ImmutableList<PUBLISH> publishes) {
                if (publishes.isEmpty()) {
                    return;
                }
                final MessageIDPool messageIDPool = messageIDPools.forClient(client);
                final AtomicInteger inFlightMessages = inFlightMessageCount(channel);
                for (PUBLISH publish : publishes) {
                    try {
                        inFlightMessages.incrementAndGet();
                        if (publish.getQoS().getQosNumber() > 0 && qos == 0) {
                            // In case the messages gets downgraded to qos 0, it can be removed.
                            removeMessageFromSharedQueue(sharedSubscription, publish.getUniqueId());
                        }
                        // We can't sent the qos when the message is queue, because we don't know the which client is will be sent
                        final QoS minQos = QoS.valueOf(Math.min(qos, publish.getQoS().getQosNumber()));
                        // There can only be one subscription ID for this message, because there are no overlapping shared subscriptions
                        final ImmutableIntArray subscriptionIdentifiers = subscriptionIdentifier != null ?
                                ImmutableIntArray.of(subscriptionIdentifier) : ImmutableIntArray.of();
                        int packetId = 0;
                        if (minQos.getQosNumber() > 0) {
                            packetId = messageIDPool.takeNextId();
                        }
                        publish = new PUBLISHFactory.Mqtt5Builder().fromPublish(publish)
                                .withPacketIdentifier(packetId)
                                .withQoS(minQos)
                                .withSubscriptionIdentifiers(subscriptionIdentifiers)
                                .build();
                    } catch (final NoMessageIdAvailableException e) {
                        // This should never happen if the limit for the poll message limit is set correctly
                        log.error("No message id available for client: " + client + ", shared subscription " + sharedSubscription, e);
                        messageDroppedService.queueFullShared(sharedSubscription, publish.getTopic(), publish.getQoS().getQosNumber());
                        inFlightMessages.decrementAndGet();
                        return;
                    }
                    try {
                        sendOutPublish(publish, true, channel, sharedSubscription, messageIDPool, client);
                    } catch (final PayloadPersistenceException e) {
                        // We don't prevent other messages form being published in case on reference is missing
                        log.error("Payload reference error for publish on topic: " + publish.getTopic(), e);
                        if (publish.getQoS().getQosNumber() > 0) {
                            removeMessageFromSharedQueue(sharedSubscription, publish.getUniqueId());
                        }
                        inFlightMessages.decrementAndGet();
                        messageDroppedService.failed(client, publish.getTopic(), publish.getQoS().getQosNumber());
                    }
                }
            }

            @Override
            public void onFailure(@NotNull final Throwable t) {
                Exceptions.rethrowError("Exception in shared publishes poll handling for client " + client +
                        "for shared subscription " + sharedSubscription, t);
            }
        }, singleWriterService.callbackExecutor(client));
    }

    private void sendOutPublish(PUBLISH publish, final boolean shared, @NotNull final Channel channel, @NotNull final String queueId,
                                @NotNull final MessageIDPool messageIDPool, @NotNull final String client) {

        payloadPersistence.add(publish.getPayload(), 1, publish.getPublishId());
        publish = new PUBLISHFactory.Mqtt5Builder().fromPublish(publish).withPersistence(payloadPersistence).build();

        //The client is connected locally
        final SettableFuture<PublishStatus> publishFuture = SettableFuture.create();

        Futures.addCallback(publishFuture, new PublishStatusFutureCallback(payloadPersistence,
                this, shared, queueId, publish, messageIDPool, channel, client), MoreExecutors.directExecutor());

        final PublishWithFuture message = new PublishWithFuture(publish, publishFuture, shared, payloadPersistence);
        channel.writeAndFlush(message).addListener(new PublishWriteFailedListener(publishFuture));
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public ListenableFuture<Void> removeMessageFromQueue(@NotNull final String client, final int packetId) {
        return clientQueuePersistence.remove(client, packetId);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public ListenableFuture<Void> removeMessageFromSharedQueue(@NotNull final String sharedSubscription, @NotNull final String uniqueId) {
        return clientQueuePersistence.removeShared(sharedSubscription, uniqueId);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public ListenableFuture<Void> putPubrelInQueue(@NotNull final String client, final int packetId) {
        return clientQueuePersistence.putPubrel(client, packetId);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public ListenableFuture<Void> removeInflightMarker(@NotNull final String sharedSubscription, @NotNull final String uniqueId) {
        return clientQueuePersistence.removeInFlightMarker(sharedSubscription, uniqueId);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    private ImmutableIntArray createMessageIds(@NotNull final MessageIDPool messageIDPool, final int pollMessageLimit) throws NoMessageIdAvailableException {
        final ImmutableIntArray.Builder builder = ImmutableIntArray.builder(pollMessageLimit);
        for (int i = 0; i < pollMessageLimit; i++) {
            final int nextId = messageIDPool.takeNextId();
            builder.add(nextId);
        }
        return builder.build();
    }

    private int pollMessageLimit(@NotNull final Channel channel) {
        final int min = InternalConfigurations.PUBLISH_POLL_BATCH_SIZE;
        final int inflightWindow = ChannelUtils.maxInflightWindow(channel);
        return Math.max(min, inflightWindow);
    }

    private class PubrelResendCallback implements FutureCallback<PublishStatus> {
        @NotNull
        private final String client;
        @NotNull
        private final MessageWithID message;
        @NotNull
        private final MessageIDPool messageIDPool;
        @NotNull
        private final Channel channel;

        PubrelResendCallback(@NotNull final String client,
                             @NotNull final MessageWithID message,
                             @NotNull final MessageIDPool messageIDPool,
                             @NotNull final Channel channel) {
            this.client = client;
            this.message = message;
            this.messageIDPool = messageIDPool;
            this.channel = channel;
        }

        @Override
        public void onSuccess(@NotNull final PublishStatus result) {
            messageIDPool.returnId(message.getPacketIdentifier());
            if (result != PublishStatus.NOT_CONNECTED) {
                final ListenableFuture<Void> future = removeMessageFromQueue(client, message.getPacketIdentifier());
                FutureUtils.addExceptionLogger(future);
            }

            final AtomicInteger inFlightMessages = channel.attr(ChannelAttributes.IN_FLIGHT_MESSAGES).get();
            if (inFlightMessages != null && inFlightMessages.decrementAndGet() > 0) {
                return;
            }
            pollMessages(client, channel);
        }

        @Override
        public void onFailure(final Throwable t) {
            Exceptions.rethrowError("Pubrel delivery failed", t);
            messageIDPool.returnId(message.getPacketIdentifier());
            final AtomicInteger inFlightMessages = channel.attr(ChannelAttributes.IN_FLIGHT_MESSAGES).get();
            if (inFlightMessages != null) {
                inFlightMessages.decrementAndGet();
            }
        }
    }
}
