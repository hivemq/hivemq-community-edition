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
package com.hivemq.persistence.clientsession;

import com.google.common.collect.ImmutableSet;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import com.hivemq.persistence.clientsession.SharedSubscriptionServiceImpl.SharedSubscription;

import java.util.concurrent.ExecutionException;

/**
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 */
@ThreadSafe
public interface SharedSubscriptionService {

    /**
     * This check is only for the shared subscription service, the subscription topic validation happens for
     * performance reasons in com.hivemq.util.Topics. Changes to the shared subscription syntax need to be reflected
     * there as well.
     *
     * @param topic a validated subscription topic string
     * @return a SharedSubscription or null if $share keyword not present
     */
    @Nullable
    SharedSubscription checkForSharedSubscription(@NotNull String topic);

    /**
     * Create a {@link Subscription} from a specific {@link Topic}.
     *
     * @param topic the topic to create a subscription for.
     * @return The created subscription.
     */
    @NotNull
    Subscription createSubscription(@NotNull Topic topic);

    /**
     * Requests all shared subscribers for a given shared subscription.
     *
     * @param sharedSubscription is the share name and the topic filter separated by a '/'
     * @return a set of subscribers
     */
    @NotNull
    ImmutableSet<SubscriberWithQoS> getSharedSubscriber(@NotNull final String sharedSubscription);

    /**
     * Requests all shared subscriptions for a given client id.
     *
     * @param client of which the subscriptions are requested.
     * @return a set of subscriptions
     */
    @NotNull
    ImmutableSet<Topic> getSharedSubscriptions(@NotNull final String client) throws ExecutionException;

    /**
     * Invalidate the shared subscriber cache for a specific shared subscription.
     *
     * @param sharedSubscription The shared subscription.
     */
    void invalidateSharedSubscriberCache(@NotNull final String sharedSubscription);

    /**
     * Invalidate the shared subscription cache for a specific shared subscriber (client).
     *
     * @param clientId The client id of the shared subscriber.
     */
    void invalidateSharedSubscriptionCache(@NotNull final String clientId);

    /**
     * Removes the '$share/' from a given topic.
     * The remaining string has the same pattern that is used for she shared subscription message queue.
     *
     * @param topic from which the prefix will be removed
     * @return the topic without the leading '$share/' or the original topic if it does't start with '$share/'
     */
    @NotNull String removePrefix(@NotNull final String topic);

}
