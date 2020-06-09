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
package com.hivemq.mqtt.message.subscribe;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.subscribe.Subscription;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.persistence.Sizable;
import com.hivemq.util.ObjectMemoryEstimation;

import java.io.Serializable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a MQTT topic with a string for the topic name and a quality of service.
 *
 * @author Dominik Obermaier
 * @author Florian Limpöck
 * @since 1.4
 */
public class Topic implements Serializable, Comparable<Topic>, Mqtt3Topic, Mqtt5Topic, Sizable {

    /**
     * The default qos to work with.
     */
    public static final QoS DEFAULT_QOS = QoS.AT_LEAST_ONCE;

    //MQTT 3 & 5
    private final @NotNull String topic;
    private @NotNull QoS qoS;

    //MQTT 5
    private final boolean noLocal;
    private final boolean retainAsPublished;
    private final @NotNull Mqtt5RetainHandling retainHandling;
    private final @Nullable Integer subscriptionIdentifier;

    private int sizeInMemory = SIZE_NOT_CALCULATED;

    //MQTT 5 Topic
    public Topic(final @NotNull String topic,
                 final @NotNull QoS qoS,
                 final boolean noLocal,
                 final boolean retainAsPublished,
                 final @NotNull Mqtt5RetainHandling retainHandling,
                 final @Nullable Integer subscriptionIdentifier) {

        checkNotNull(topic, "A Topic must not be null");
        checkNotNull(qoS, "A QoS must not be null");
        checkNotNull(retainHandling, "A RetainHandling must not be null");
        checkArgument((subscriptionIdentifier == null) || ((subscriptionIdentifier >= 1) && (subscriptionIdentifier <= 268_435_455)),
                "Subscription identifier must be between 1 and 268_435_455");

        this.topic = topic;
        this.qoS = qoS;
        this.noLocal = noLocal;
        this.retainAsPublished = retainAsPublished;
        this.retainHandling = retainHandling;
        this.subscriptionIdentifier = subscriptionIdentifier;
    }

    public Topic(final @NotNull String topic, final @NotNull QoS qoS,
                 final boolean noLocal, final boolean retainAsPublished) {

        this(topic, qoS, noLocal, retainAsPublished, DEFAULT_RETAIN_HANDLING, null);
    }

    //MQTT 3 Topic
    public Topic(final @NotNull String topic, final @NotNull QoS qoS) {
        this(topic, qoS, DEFAULT_NO_LOCAL, DEFAULT_RETAIN_AS_PUBLISHED, DEFAULT_RETAIN_HANDLING, null);
    }

    public static Topic topicFromString(@NotNull final String string) {
        return new Topic(string, DEFAULT_QOS);
    }

    @NotNull
    public static Topic topicFromSubscription(@NotNull final Subscription subscription, final @Nullable Integer subscriptionIdentifier) {
        return new Topic(subscription.getTopicFilter(),
                Objects.requireNonNull(QoS.valueOf(subscription.getQos().getQosNumber())),
                subscription.getNoLocal(),
                subscription.getRetainAsPublished(),
                Objects.requireNonNull(Mqtt5RetainHandling.fromCode(subscription.getRetainHandling().getCode())),
                subscriptionIdentifier);
    }

    /**
     * @return the topic as String representation
     */
    @NotNull
    public String getTopic() {
        return topic;
    }

    /**
     * @return the QoS of a Topic
     */
    @NotNull
    public QoS getQoS() {
        return qoS;
    }


    public void setQoS(@NotNull final QoS qos) {
        checkNotNull(qos, "QoS must not be null");
        this.qoS = qos;
    }

    /**
     * @return whether the client must not receive messages published by itself. The default is {@link
     * #DEFAULT_NO_LOCAL}.
     */
    public boolean isNoLocal() {
        return noLocal;
    }

    /**
     * @return the handling of retained message for this subscription. The default is {@link #DEFAULT_RETAIN_HANDLING}.
     */
    @NotNull
    public Mqtt5RetainHandling getRetainHandling() {
        return retainHandling;
    }

    /**
     * @return whether the retain flag for incoming publishes must be set to its original value.
     */
    public boolean isRetainAsPublished() {
        return retainAsPublished;
    }

    @Nullable
    public Integer getSubscriptionIdentifier() {
        return subscriptionIdentifier;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Topic topic1 = (Topic) o;

        return topic.equals(topic1.topic);
    }

    @Override
    public int hashCode() {
        return topic.hashCode();
    }


    @Override
    public @NotNull String toString() {
        return "Topic{" +
                "topic='" + topic + '\'' +
                ", qoS=" + qoS +
                '}';
    }

    @Override
    public int compareTo(final @NotNull Topic o) {
        return this.topic.compareTo(o.getTopic());
    }


    @Override
    public int getEstimatedSize() {

        if (sizeInMemory != SIZE_NOT_CALCULATED) {
            return sizeInMemory;
        }
        int size = 0;

        size += ObjectMemoryEstimation.objectShellSize();
        size += ObjectMemoryEstimation.stringSize(topic);
        size += ObjectMemoryEstimation.enumSize(); // QoS
        size += ObjectMemoryEstimation.booleanSize(); // no local
        size += ObjectMemoryEstimation.booleanSize(); // retain as published
        size += ObjectMemoryEstimation.enumSize(); // retain handling
        size += ObjectMemoryEstimation.intWrapperSize(); // sub id

        sizeInMemory = size;
        return sizeInMemory;
    }
}
