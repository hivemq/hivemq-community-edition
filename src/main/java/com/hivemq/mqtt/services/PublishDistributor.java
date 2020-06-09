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

import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * @author Christoph Schäbel
 */
public interface PublishDistributor {

    /**
     * Sends a message to the specified non-shared subscribers
     *
     * @param subscribers     map with all subscribers, key is client identifier, value is the subscription information
     * @param publish         the message to send
     * @param executorService the executor service in which all callbacks are executed
     */
    @NotNull
    ListenableFuture<Void> distributeToNonSharedSubscribers(@NotNull Map<String, SubscriberWithIdentifiers> subscribers,
                                                            @NotNull PUBLISH publish, @NotNull ExecutorService executorService);

    /**
     * Sends a message to the specified shared subscribers
     *
     * @param sharedSubscriptions is a set of all shared subscriptions (group + '/'+ topic-filter) that have matching
     *                            subscriptions for this topic
     * @param publish             the message to send
     * @param executorService     the executor service in which all callbacks are executed
     */
    @NotNull
    ListenableFuture<Void> distributeToSharedSubscribers(@NotNull Set<String> sharedSubscriptions, @NotNull PUBLISH publish,
                                                         @NotNull ExecutorService executorService);

    /**
     * Sends a message to a discrete subscriber
     *
     * @param publish            the message to send
     * @param clientId           client identifier to send to
     * @param subscriptionQos    the QoS from the subscription
     * @param sharedSubscription if the subscription is part of a shared subscription
     * @param retainAsPublished  if the retain flag should be forwarded to the subscriber
     * @return a future with the result for this publish
     */
    @NotNull
    ListenableFuture<PublishStatus> sendMessageToSubscriber(@NotNull PUBLISH publish, @NotNull String clientId, final int subscriptionQos,
                                                            boolean sharedSubscription, boolean retainAsPublished,
                                                            @Nullable ImmutableIntArray subscriptionIdentifier);

}
