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
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.util.FutureUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.hivemq.mqtt.handler.publish.PublishStatus.DELIVERED;
import static com.hivemq.mqtt.handler.publish.PublishStatus.FAILED;
import static com.hivemq.mqtt.handler.publish.PublishStatus.NOT_CONNECTED;

@Singleton
public class PublishDistributorImpl implements PublishDistributor {

    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull ClientSessionPersistence clientSessionPersistence;
    private final SingleWriterService singleWriterService;
    private final @NotNull MqttConfigurationService mqttConfigurationService;

    @Inject
    public PublishDistributorImpl(
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull ClientSessionPersistence clientSessionPersistence,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull MqttConfigurationService mqttConfigurationService) {
        this.clientQueuePersistence = clientQueuePersistence;
        this.clientSessionPersistence = clientSessionPersistence;
        this.singleWriterService = singleWriterService;
        this.mqttConfigurationService = mqttConfigurationService;
    }

    @Override
    public @NotNull ListenableFuture<Void> distributeToNonSharedSubscribers(
            final @NotNull Map<String, SubscriberWithIdentifiers> subscribers,
            final @NotNull PUBLISH publish,
            final @NotNull ExecutorService executorService) {

        final ImmutableList.Builder<ListenableFuture<Void>> publishResultFutureBuilder = ImmutableList.builder();

        for (final Map.Entry<String, SubscriberWithIdentifiers> entry : subscribers.entrySet()) {
            final SubscriberWithIdentifiers subscriber = entry.getValue();

            final ListenableFuture<PublishStatus> publishFuture = sendMessageToSubscriber(publish,
                    entry.getKey(),
                    subscriber.getQos(),
                    false,
                    subscriber.isRetainAsPublished(),
                    subscriber.getSubscriptionIdentifier());

            final SettableFuture<Void> publishFinishedFuture = SettableFuture.create();
            publishResultFutureBuilder.add(publishFinishedFuture);
            Futures.addCallback(publishFuture,
                    new StandardPublishCallback(entry.getKey(), publish, publishFinishedFuture),
                    executorService);
        }

        return FutureUtils.voidFutureFromList(publishResultFutureBuilder.build());
    }

    @Override
    public @NotNull ListenableFuture<Void> distributeToSharedSubscribers(
            final @NotNull Set<String> sharedSubscribers,
            final @NotNull PUBLISH publish,
            final @NotNull ExecutorService executorService) {

        final ImmutableList.Builder<ListenableFuture<Void>> publishResultFutureBuilder = ImmutableList.builder();

        for (final String sharedSubscriber : sharedSubscribers) {
            final SettableFuture<Void> publishFinishedFuture = SettableFuture.create();
            final ListenableFuture<PublishStatus> future = sendMessageToSubscriber(publish,
                    sharedSubscriber,
                    publish.getQoS().getQosNumber(),
                    true,
                    true,
                    null);
            publishResultFutureBuilder.add(publishFinishedFuture);
            Futures.addCallback(future,
                    new StandardPublishCallback(sharedSubscriber, publish, publishFinishedFuture),
                    executorService);
        }

        return FutureUtils.voidFutureFromList(publishResultFutureBuilder.build());
    }

    @Override
    public @NotNull ListenableFuture<PublishStatus> sendMessageToSubscriber(
            final @NotNull PUBLISH publish,
            final @NotNull String clientId,
            final int subscriptionQos,
            final boolean sharedSubscription,
            final boolean retainAsPublished,
            final @Nullable ImmutableIntArray subscriptionIdentifier) {

        return handlePublish(publish,
                clientId,
                subscriptionQos,
                sharedSubscription,
                retainAsPublished,
                subscriptionIdentifier);
    }

    private @NotNull ListenableFuture<PublishStatus> handlePublish(
            final @NotNull PUBLISH publish,
            final @NotNull String client,
            final int subscriptionQos,
            final boolean sharedSubscription,
            final boolean retainAsPublished,
            final @Nullable ImmutableIntArray subscriptionIdentifier) {

        if (sharedSubscription) {
            return queuePublish(client,
                    publish,
                    subscriptionQos,
                    true,
                    retainAsPublished,
                    subscriptionIdentifier,
                    null);
        }

        final boolean qos0Message = Math.min(subscriptionQos, publish.getQoS().getQosNumber()) == 0;
        final ClientSession clientSession = clientSessionPersistence.getSession(client, false);
        final boolean clientConnected = clientSession != null && clientSession.isConnected();

        if ((qos0Message && !clientConnected)) {
            return Futures.immediateFuture(NOT_CONNECTED);
        }

        //no session present or session already expired
        if (clientSession == null) {
            return Futures.immediateFuture(NOT_CONNECTED);
        }

        return queuePublish(client,
                publish,
                subscriptionQos,
                false,
                retainAsPublished,
                subscriptionIdentifier,
                clientSession.getQueueLimit());
    }

    private @NotNull SettableFuture<PublishStatus> queuePublish(
            final @NotNull String client,
            final @NotNull PUBLISH publish,
            final int subscriptionQos,
            final boolean shared,
            final boolean retainAsPublished,
            final @Nullable ImmutableIntArray subscriptionIdentifier,
            final @Nullable Long queueLimit) {

        final ListenableFuture<Void> future = clientQueuePersistence.add(client,
                shared,
                createPublish(publish, subscriptionQos, retainAsPublished, subscriptionIdentifier),
                false,
                Objects.requireNonNullElseGet(queueLimit, mqttConfigurationService::maxQueuedMessages));

        final SettableFuture<PublishStatus> statusFuture = SettableFuture.create();

        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(final Void result) {
                statusFuture.set(DELIVERED);
            }

            @Override
            public void onFailure(final Throwable t) {
                statusFuture.set(FAILED);
            }
        }, MoreExecutors.directExecutor());
        return statusFuture;
    }

    private@NotNull  PUBLISH createPublish(
            final @NotNull PUBLISH publish,
            final int subscriptionQos,
            final boolean retainAsPublished,
            final @Nullable ImmutableIntArray subscriptionIdentifier) {
        final ImmutableIntArray identifiers;
        if (subscriptionIdentifier == null) {
            identifiers = ImmutableIntArray.of();
        } else {
            identifiers = subscriptionIdentifier;
        }

        final PUBLISHFactory.Mqtt5Builder builder = new PUBLISHFactory.Mqtt5Builder().fromPublish(publish)
                .withRetain(publish.isRetain() && retainAsPublished)
                .withSubscriptionIdentifiers(identifiers);

        final int qos = Math.min(publish.getOnwardQoS().getQosNumber(), subscriptionQos);
        builder.withQoS(QoS.valueOf(qos));

        if (qos == 0) {
            builder.withPacketIdentifier(0);
        }

        return builder.build();
    }
}
