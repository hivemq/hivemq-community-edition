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

package com.hivemq.extension.sdk.api.packets.subscribe;


import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.Qos;

/**
 * A copy of a {@link Subscription} that can be modified for onward delivery.
 *
 * @author Florian Limpöck
 * @since 4.2.0
 */
@DoNotImplement
public interface ModifiableSubscription extends Subscription {

    /**
     * Sets the topic filter.
     *
     * @param topicFilter The topic filter to set.
     * @throws NullPointerException     If the topic filter is null.
     * @throws IllegalArgumentException If the topic filter is empty.
     * @throws IllegalArgumentException If the topic filter contains invalid UTF-8 characters.
     * @throws IllegalArgumentException If the topic filter is longer than the configured maximum. Default
     *                                  maximum length is 65535.
     * @throws IllegalArgumentException If the topic filter contains a wildcard and wildcards are disabled by HiveMQ.
     *                                  Default is enabled.
     * @throws IllegalArgumentException If the topic filter is a shared subscription and shared subscriptions are
     *                                  disabled by HiveMQ. Default is enabled.
     * @throws IllegalArgumentException If the topic filter is a shared subscription and the no local flag is set to
     *                                  true.
     * @since 4.2.0
     */
    void setTopicFilter(@NotNull String topicFilter);

    /**
     * Set the QoS of the subscription.
     *
     * @param qos The {@link Qos} to set.
     * @throws NullPointerException     If qos is null.
     * @throws IllegalArgumentException If qos is greater than the configured maximum.
     * @since 4.2.0
     */
    void setQos(@NotNull Qos qos);

    /**
     * Set the retain handling of the subscription.
     *
     * @param retainHandling The {@link RetainHandling} to set.
     * @throws NullPointerException If the retain handling is null.
     * @since 4.2.0
     */
    void setRetainHandling(@NotNull RetainHandling retainHandling);

    /**
     * Set the retain as published flag of the subscription.
     *
     * @param retainAsPublished The retain as published flag to set.
     * @since 4.2.0
     */
    void setRetainAsPublished(boolean retainAsPublished);

    /**
     * Set the no local flag of the subscription.
     *
     * @param noLocal The no local flag to set.
     * @throws IllegalArgumentException If true and the subscription is a shared subscription.
     * @since 4.2.0
     */
    void setNoLocal(boolean noLocal);

}
