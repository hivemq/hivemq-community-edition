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

package com.hivemq.mqtt.services;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
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
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.util.FutureUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.hivemq.mqtt.handler.publish.PublishStatus.*;

/**
 * @author Christoph Schäbel
 */
@Singleton
public class PublishDistributorImpl implements PublishDistributor {


    @NotNull
    private final PublishPayloadPersistence payloadPersistence;
    @NotNull
    private final ClientQueuePersistence clientQueuePersistence;
    @NotNull
    private final ClientSessionPersistence clientSessionPersistence;
    @NotNull
    private final SingleWriterService singleWriterService;

    @Inject
    public PublishDistributorImpl(@NotNull final PublishPayloadPersistence payloadPersistence,
                                  @NotNull final ClientQueuePersistence clientQueuePersistence,
                                  @NotNull final ClientSessionPersistence clientSessionPersistence,
                                  @NotNull final SingleWriterService singleWriterService) {
        this.payloadPersistence = payloadPersistence;
        this.clientQueuePersistence = clientQueuePersistence;
        this.clientSessionPersistence = clientSessionPersistence;
        this.singleWriterService = singleWriterService;
    }

    @NotNull
    @Override
    public ListenableFuture<Void> distributeToNonSharedSubscribers(@NotNull final Map<String, SubscriberWithIdentifiers> subscribers,
                                                                   @NotNull final PUBLISH publish, @NotNull final ExecutorService executorService) {

        final ImmutableList.Builder<ListenableFuture<Void>> publishResultFutureBuilder = ImmutableList.builder();

        for (final Map.Entry<String, SubscriberWithIdentifiers> entry : subscribers.entrySet()) {
            final SubscriberWithIdentifiers subscriber = entry.getValue();

            final ListenableFuture<PublishStatus> publishFuture = sendMessageToSubscriber(publish, entry.getKey(), subscriber.getQos(),
                    false, subscriber.isRetainAsPublished(), subscriber.getSubscriptionIdentifier());

            final SettableFuture<Void> publishFinishedFuture = SettableFuture.create();
            publishResultFutureBuilder.add(publishFinishedFuture);
            Futures.addCallback(publishFuture, new StandardPublishCallback(entry.getKey(), publish, publishFinishedFuture), executorService);
        }

        return FutureUtils.voidFutureFromList(publishResultFutureBuilder.build());
    }

    @NotNull
    @Override
    public ListenableFuture<Void> distributeToSharedSubscribers(@NotNull final Set<String> sharedSubscribers, @NotNull final PUBLISH publish,
                                                                @NotNull final ExecutorService executorService) {

        final ImmutableList.Builder<ListenableFuture<Void>> publishResultFutureBuilder = ImmutableList.builder();

        for (final String sharedSubscriber : sharedSubscribers) {
            final SettableFuture<Void> publishFinishedFuture = SettableFuture.create();
            final ListenableFuture<PublishStatus> future = sendMessageToSubscriber(publish, sharedSubscriber, publish.getQoS().getQosNumber(), true, false, null);
            publishResultFutureBuilder.add(publishFinishedFuture);
            Futures.addCallback(future, new StandardPublishCallback(sharedSubscriber, publish, publishFinishedFuture), executorService);
        }

        return FutureUtils.voidFutureFromList(publishResultFutureBuilder.build());
    }

    @NotNull
    @Override
    public ListenableFuture<PublishStatus> sendMessageToSubscriber(@NotNull final PUBLISH publish, @NotNull final String clientId, final int subscriptionQos,
                                                                   final boolean sharedSubscription, final boolean retainAsPublished,
                                                                   @Nullable final List<Integer> subscriptionIdentifier) {

        return handlePublish(publish, clientId, subscriptionQos, sharedSubscription, retainAsPublished, subscriptionIdentifier);
    }

    @NotNull
    private ListenableFuture<PublishStatus> handlePublish(@NotNull final PUBLISH publish, @NotNull final String client, final int subscriptionQos,
                                                          final boolean sharedSubscription, final boolean retainAsPublished,
                                                          @Nullable final List<Integer> subscriptionIdentifier) {

        if (sharedSubscription) {
            return queuePublish(client, publish, subscriptionQos, true, retainAsPublished, subscriptionIdentifier);
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

        return queuePublish(client, publish, subscriptionQos, false, retainAsPublished, subscriptionIdentifier);
    }

    @NotNull
    private SettableFuture<PublishStatus> queuePublish(@NotNull final String client, @NotNull final PUBLISH publish,
                                                       final int subscriptionQos, final boolean shared, final boolean retainAsPublished,
                                                       @Nullable final List<Integer> subscriptionIdentifier) {

        final ListenableFuture<Void> future = clientQueuePersistence.add(client, shared, createPublish(publish, subscriptionQos, retainAsPublished, subscriptionIdentifier));
        final SettableFuture<PublishStatus> statusFuture = SettableFuture.create();

        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                statusFuture.set(DELIVERED);
            }

            @Override
            public void onFailure(final Throwable t) {
                statusFuture.set(FAILED);
            }
        }, singleWriterService.callbackExecutor(client));
        return statusFuture;
    }

    @NotNull
    private PUBLISH createPublish(@NotNull final PUBLISH publish, final int subscriptionQos, final boolean retainAsPublished, @Nullable final List<Integer> subscriptionIdentifier) {
        final long payloadId = payloadPersistence.add(publish.getPayload(), 1);
        final ImmutableList<Integer> identifiers;
        if (subscriptionIdentifier == null) {
            identifiers = ImmutableList.of();
        } else {
            identifiers = ImmutableList.copyOf(subscriptionIdentifier);
        }

        final PUBLISHFactory.Mqtt5Builder builder = new PUBLISHFactory.Mqtt5Builder()
                .fromPublish(publish)
                .withPayloadId(payloadId)
                .withPersistence(payloadPersistence)
                .withRetain(publish.isRetain() && retainAsPublished)
                .withSubscriptionIdentifiers(ImmutableList.copyOf(identifiers));

        final int qos = Math.min(publish.getQoS().getQosNumber(), subscriptionQos);
        builder.withQoS(QoS.valueOf(qos));

        if (qos == 0) {
            builder.withPacketIdentifier(0);
        }

        return builder.build();
    }
}
