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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.hivemq.configuration.service.InternalConfigurations.ACKNOWLEDGE_AFTER_PERSIST;

/**
 * @author Christoph Schäbel
 * @author Dominik Obermaier
 */
@LazySingleton
public class InternalPublishServiceImpl implements InternalPublishService {

    private static final Logger log = LoggerFactory.getLogger(InternalPublishServiceImpl.class);

    private final RetainedMessagePersistence retainedMessagePersistence;
    private final LocalTopicTree topicTree;
    private final PublishDistributor publishDistributor;

    private final boolean acknowledgeAfterPersist;

    @Inject
    public InternalPublishServiceImpl(final RetainedMessagePersistence retainedMessagePersistence,
                                      final LocalTopicTree topicTree,
                                      final PublishDistributor publishDistributor) {

        this.retainedMessagePersistence = retainedMessagePersistence;
        this.topicTree = topicTree;
        this.publishDistributor = publishDistributor;
        this.acknowledgeAfterPersist = ACKNOWLEDGE_AFTER_PERSIST.get();
    }

    @NotNull
    public ListenableFuture<PublishReturnCode> publish(@NotNull final PUBLISH publish, @NotNull final ExecutorService executorService, @Nullable final String sender) {

        Preconditions.checkNotNull(publish, "PUBLISH can not be null");
        Preconditions.checkNotNull(executorService, "executorService can not be null");

        //reset dup-flag
        publish.setDuplicateDelivery(false);

        final ListenableFuture<Void> persistFuture = persistRetainedMessage(publish, executorService);
        final ListenableFuture<PublishReturnCode> publishReturnCodeFuture = handlePublish(publish, executorService, sender);

        return Futures.whenAllComplete(publishReturnCodeFuture, persistFuture).call(() -> publishReturnCodeFuture.get(), executorService);
    }

    private ListenableFuture<Void> persistRetainedMessage(final PUBLISH publish, final ExecutorService executorService) {

        //Retained messages need to be persisted and thus we need to make that non-blocking
        if (publish.isRetain()) {

            final SettableFuture<Void> persistSettableFuture = SettableFuture.create();
            final ListenableFuture<Void> persistFuture;
            if (publish.getPayload().length > 0) {
                //pass payloadId null here, because we don't know yet if the message must be stored in the payload persistence
                final RetainedMessage retainedMessage = new RetainedMessage(publish, publish.getMessageExpiryInterval());
                log.trace("Adding retained message on topic {}", publish.getTopic());
                persistFuture = retainedMessagePersistence.persist(publish.getTopic(), retainedMessage);

            } else {
                log.trace("Deleting retained message on topic {}", publish.getTopic());
                persistFuture = retainedMessagePersistence.remove(publish.getTopic());
            }

            if (!acknowledgeAfterPersist) {
                persistSettableFuture.set(null);
            } else {

                Futures.addCallback(persistFuture, new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable final Void aVoid) {
                        persistSettableFuture.set(null);
                    }

                    @Override
                    public void onFailure(@NotNull final Throwable throwable) {
                        Exceptions.rethrowError("Unable able to store retained message for topic " + publish.getTopic()
                                + " with message id " + publish.getUniqueId() + ".", throwable);
                        persistSettableFuture.set(null);
                    }

                }, executorService);
            }

            return persistSettableFuture;
        }

        return Futures.immediateFuture(null);
    }

    @NotNull
    private ListenableFuture<PublishReturnCode> handlePublish(@NotNull final PUBLISH publish, @NotNull final ExecutorService executorService, @Nullable final String sender) {

        final ImmutableSet<SubscriberWithIdentifiers> subscribers = topicTree.getSubscribers(publish.getTopic());

        if (subscribers.size() < 1) {
            return Futures.immediateFuture(PublishReturnCode.NO_MATCHING_SUBSCRIBERS);
        }


        if (!acknowledgeAfterPersist) {
            deliverPublish(subscribers, sender, publish, executorService, null);
            return Futures.immediateFuture(PublishReturnCode.DELIVERED);
        }
        final SettableFuture<PublishReturnCode> returnCodeFuture = SettableFuture.create();
        deliverPublish(subscribers, sender, publish, executorService, returnCodeFuture);
        return returnCodeFuture;
    }

    private void deliverPublish(final ImmutableSet<SubscriberWithIdentifiers> subscribers,
                                @Nullable final String sender,
                                @NotNull final PUBLISH publish,
                                @NotNull final ExecutorService executorService,
                                @Nullable final SettableFuture<PublishReturnCode> returnCodeFuture) {
        Set<String> sharedSubscriptions = null;
        final Map<String, SubscriberWithIdentifiers> notSharedSubscribers = new HashMap<>(subscribers.size());

        for (final SubscriberWithIdentifiers subscriber : subscribers) {
            if (!subscriber.isSharedSubscription()) {

                if (subscriber.isNoLocal() && sender != null && sender.equals(subscriber.getSubscriber())) {
                    //do not send to this subscriber, because NoLocal Option is set and subscriber == sender
                    continue;
                }

                notSharedSubscribers.put(subscriber.getSubscriber(), subscriber);
                continue;
            }

            //only instantiate list if shared subscribers are available
            if (sharedSubscriptions == null) {
                sharedSubscriptions = new HashSet<>(subscribers.size());
            }

            sharedSubscriptions.add(subscriber.getSharedName() + "/" + subscriber.getTopicFilter());
        }

        //Send out the messages to the channel of the subscribers
        final ListenableFuture<Void> publishFinishedFutureNonShared = publishDistributor.distributeToNonSharedSubscribers(notSharedSubscribers, publish, executorService);

        final ListenableFuture<Void> publishFinishedFutureShared;
        //Shared subscriptions are currently not batched, since it is unlikely that there are many groups of shared subscribers for the same topic.
        if (sharedSubscriptions != null) {
            publishFinishedFutureShared = publishDistributor.distributeToSharedSubscribers(sharedSubscriptions, publish, executorService);
        } else {
            publishFinishedFutureShared = Futures.immediateFuture(null);
        }

        Futures.addCallback(FutureUtils.mergeVoidFutures(publishFinishedFutureNonShared, publishFinishedFutureShared), new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable final Void result) {
                if (returnCodeFuture != null) {
                    returnCodeFuture.set(PublishReturnCode.DELIVERED);
                }
            }

            @Override
            public void onFailure(@NotNull final Throwable throwable) {
                Exceptions.rethrowError("Unable to publish message for topic " + publish.getTopic() + " with message id" + publish.getUniqueId() + ".", throwable);
                if (returnCodeFuture != null) {
                    returnCodeFuture.set(PublishReturnCode.FAILED);
                }
            }
        }, executorService);
    }
}
