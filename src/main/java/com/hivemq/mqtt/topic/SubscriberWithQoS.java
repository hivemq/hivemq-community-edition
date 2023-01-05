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
package com.hivemq.mqtt.topic;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.util.Bytes;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This represents a subscriber (client ID) with a Quality of Service Level
 */
@Immutable
public class SubscriberWithQoS implements Comparable<SubscriberWithQoS> {

    private final @NotNull String subscriber;
    private final int qos;
    private final @Nullable String sharedName;
    private final @Nullable Integer subscriptionIdentifier;
    // The topic filter is only present for shared subscription
    private final @Nullable String topicFilter;

    private byte flags;

    public SubscriberWithQoS(
            final @NotNull String subscriber,
            final int qos,
            final byte flags,
            final @Nullable Integer subscriptionIdentifier) {

        this(subscriber, qos, flags, null, subscriptionIdentifier, null);
    }

    public SubscriberWithQoS(
            final @NotNull String subscriber,
            final int qos,
            final byte flags,
            final @Nullable String sharedName,
            final @Nullable Integer subscriptionIdentifier,
            final @Nullable String topicFilter) {

        checkNotNull(subscriber, "Subscriber must not be null");
        checkArgument((qos <= 2 && qos >= 0), "Quality of Service level must be between 0 and 2");

        this.subscriber = subscriber;
        this.qos = qos;
        this.flags = flags;
        this.sharedName = sharedName;
        this.subscriptionIdentifier = subscriptionIdentifier;
        this.topicFilter = topicFilter;
    }

    @NotNull
    public String getSubscriber() {
        return subscriber;
    }

    public int getQos() {
        return qos;
    }

    public byte getFlags() {
        return flags;
    }

    public void addFlags(final @NotNull SubscriptionFlag... subscriptionFlags) {
        for (final SubscriptionFlag flag : subscriptionFlags) {
            flags = Bytes.setBit(flags, flag.getFlagIndex());
        }
    }

    public void removeFlags(final @NotNull SubscriptionFlag... subscriptionFlags) {
        for (final SubscriptionFlag flag : subscriptionFlags) {
            flags = Bytes.unsetBit(flags, flag.getFlagIndex());
        }
    }

    public boolean isSharedSubscription() {
        return Bytes.isBitSet(flags, SubscriptionFlag.SHARED_SUBSCRIPTION.getFlagIndex());
    }

    public boolean isRetainAsPublished() {
        return Bytes.isBitSet(flags, SubscriptionFlag.RETAIN_AS_PUBLISHED.getFlagIndex());
    }

    public boolean isNoLocal() {
        return Bytes.isBitSet(flags, SubscriptionFlag.NO_LOCAL.getFlagIndex());
    }

    @Nullable
    public String getSharedName() {
        return sharedName;
    }

    @Nullable
    public Integer getSubscriptionIdentifier() {
        return subscriptionIdentifier;
    }

    @Nullable
    public String getTopicFilter() {
        return topicFilter;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final SubscriberWithQoS that = (SubscriberWithQoS) o;
        return qos == that.qos &&
                flags == that.flags &&
                Objects.equals(subscriber, that.subscriber) &&
                Objects.equals(sharedName, that.sharedName) &&
                Objects.equals(subscriptionIdentifier, that.subscriptionIdentifier) &&
                Objects.equals(topicFilter, that.topicFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriber, qos, flags, sharedName, subscriptionIdentifier, topicFilter);
    }

    @Override
    public int compareTo(@Nullable final SubscriberWithQoS o) {
        // Subscription are sorted by client id first and qos after.
        // This allows us to determine the highest qos for each subscriber
        if (o == null) {
            return -1;
        }
        final int subscriberCompare = subscriber.compareTo(o.getSubscriber());
        if (subscriberCompare == 0) {
            final int qosCompare = Integer.compare(qos, o.getQos());
            if (qosCompare == 0 && subscriptionIdentifier != null && o.subscriptionIdentifier != null) {
                return Integer.compare(subscriptionIdentifier, o.subscriptionIdentifier);
            }
            return qosCompare;
        }
        return subscriberCompare;
    }

    @NotNull
    @Override
    public String toString() {
        return "SubscriberWithQoS{" +
                "subscriber='" + subscriber + '\'' +
                ", qos=" + qos +
                '}';
    }
}
