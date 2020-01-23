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
package com.hivemq.extension.sdk.api.services.builder;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.subscribe.Subscription;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.subscription.TopicSubscription;

/**
 * This builder must be used to create a {@link TopicSubscription}.
 * <p>
 * Either from values or from a {@link Subscription}.
 * <p>
 * Every TopicSubscription built by this builder is fully validated against HiveMQ configuration.
 *
 * @author Christoph Schäbel
 * @author Florian Limpöck
 * @since 4.0.0
 */
@DoNotImplement
public interface TopicSubscriptionBuilder {

    /**
     * Create a {@link TopicSubscription} from the values of a {@link Subscription}.
     *
     * @param subscription The subscription to build a {@link TopicSubscription} from.
     * @return The {@link TopicSubscriptionBuilder}.
     * @throws NullPointerException    If the subscription is null.
     * @throws DoNotImplementException If the {@link Subscription} is implemented by the extension.
     * @since 4.0.0
     */
    @NotNull TopicSubscriptionBuilder fromSubscription(@NotNull Subscription subscription);

    /**
     * Sets the topic filter.
     * <p>
     * This value has no default and must be set.
     *
     * @param topicFilter The topic filter to set.
     * @return The {@link TopicSubscriptionBuilder}.
     * @throws NullPointerException     If the topic filter is null.
     * @throws IllegalArgumentException If the topic filter is empty.
     * @throws IllegalArgumentException If the topic filter contains invalid UTF-8 characters.
     * @throws IllegalArgumentException If the topic filter is longer than the configured maximum. Default
     *                                  maximum length is 65535.
     * @throws IllegalArgumentException If the topic filter contains a wildcard and wildcards are disabled by HiveMQ.
     *                                  Default is enabled.
     * @throws IllegalArgumentException If the topic filter is a shared subscription and shared subscriptions are
     *                                  disabled by HiveMQ. Default is enabled.
     * @since 4.0.0
     */
    @NotNull TopicSubscriptionBuilder topicFilter(@NotNull String topicFilter);

    /**
     * Sets the quality of service level.
     * <p>
     * DEFAULT: <code>QoS 0</code>.
     *
     * @param qos The {@link Qos} to set.
     * @return The {@link TopicSubscriptionBuilder}.
     * @throws NullPointerException If qos is null.
     * @since 4.0.0
     */
    @NotNull TopicSubscriptionBuilder qos(@NotNull Qos qos);

    /**
     * Sets the retain as published flag.
     * <p>
     * DEFAULT: <code>false</code>.
     *
     * @param retainAsPublished The retain as published flag to set.
     * @return The {@link TopicSubscriptionBuilder}.
     * @since 4.0.0
     */
    @NotNull TopicSubscriptionBuilder retainAsPublished(boolean retainAsPublished);

    /**
     * Sets the no local flag. Do not set the no local flag to <b>true</b> if the {@link TopicSubscription} is a
     * shared subscription.
     * <p>
     * DEFAULT: <code>false</code>.
     *
     * @param noLocal The no local flag to set.
     * @return The {@link TopicSubscriptionBuilder}.
     * @since 4.0.0
     */
    @NotNull TopicSubscriptionBuilder noLocal(boolean noLocal);

    /**
     * Sets the subscription identifier.
     * <p>
     * DEFAULT: <code>null</code>.
     *
     * @param subscriptionIdentifier The subscription identifier to set.
     * @return The {@link TopicSubscriptionBuilder}.
     * @throws IllegalArgumentException If the subscription identifier is zero or greater than the protocol limit
     *                                  (268435455).
     * @throws IllegalArgumentException If the subscription identifier are disabled by HiveMQ. Default is enabled.
     * @since 4.0.0
     */
    @NotNull TopicSubscriptionBuilder subscriptionIdentifier(int subscriptionIdentifier);

    /**
     * Builds the {@link TopicSubscription} with the provided values or default values.
     *
     * @return A {@link TopicSubscription} with the set parameters.
     * @throws NullPointerException     If the topic filter is null.
     * @throws IllegalArgumentException If the noLocal flag is set for a shared subscription.
     * @since 4.0.0
     */
    @NotNull TopicSubscription build();
}
