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
package com.hivemq.extensions.services.subscription;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.services.subscription.TopicSubscription;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.subscribe.Topic;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class TopicSubscriptionImpl implements TopicSubscription {

    private final @NotNull String topicFilter;
    private final @NotNull Qos qos;
    private final boolean retainAsPublished;
    private final boolean noLocal;
    private final @Nullable Integer subscriptionIdentifier;

    public TopicSubscriptionImpl(final @NotNull String topicFilter, final @NotNull Qos qos, final boolean retainAsPublished,
                                 final boolean noLocal, @Nullable final Integer subscriptionIdentifier) {
        Preconditions.checkNotNull(topicFilter, "Topic filter must never be null");
        Preconditions.checkNotNull(qos, "QoS must never be null");
        this.topicFilter = topicFilter;
        this.qos = qos;
        this.retainAsPublished = retainAsPublished;
        this.noLocal = noLocal;
        this.subscriptionIdentifier = subscriptionIdentifier;
    }

    public TopicSubscriptionImpl(final @NotNull Topic topic) {
        Preconditions.checkNotNull(topic, "Topic must never be null");
        this.topicFilter = topic.getTopic();
        this.qos = topic.getQoS().toQos();
        this.retainAsPublished = topic.isRetainAsPublished();
        this.noLocal = topic.isNoLocal();
        this.subscriptionIdentifier = topic.getSubscriptionIdentifier();
    }

    @Override
    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    @Override
    public @NotNull Qos getQos() {
        return qos;
    }

    @Override
    public boolean getRetainAsPublished() {
        return retainAsPublished;
    }

    @Override
    public boolean getNoLocal() {
        return noLocal;
    }

    @Override
    public @NotNull Optional<Integer> getSubscriptionIdentifier() {
        return Optional.ofNullable(subscriptionIdentifier);
    }

    public static @NotNull Topic convertToTopic(@NotNull final TopicSubscription topicSubscription) {
        Preconditions.checkNotNull(topicSubscription, "TopicSubscription must never be null");
        return new Topic(topicSubscription.getTopicFilter(),
                Objects.requireNonNull(QoS.valueOf(topicSubscription.getQos().getQosNumber())),
                topicSubscription.getNoLocal(),
                topicSubscription.getRetainAsPublished(),
                Objects.requireNonNull(Mqtt5RetainHandling.DO_NOT_SEND),
                topicSubscription.getSubscriptionIdentifier().orElse(null));
    }

    @Override
    public String toString() {
        return "TopicSubscription{" +
                "topicFilter='" + topicFilter + '\'' +
                ", qos=" + qos +
                ", retainAsPublished=" + retainAsPublished +
                ", noLocal=" + noLocal +
                ", subscriptionIdentifier=" + subscriptionIdentifier +
                '}';
    }
}
