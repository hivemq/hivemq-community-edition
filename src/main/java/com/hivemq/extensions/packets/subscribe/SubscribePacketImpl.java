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
package com.hivemq.extensions.packets.subscribe;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.subscribe.SubscribePacket;
import com.hivemq.extension.sdk.api.packets.subscribe.Subscription;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.subscribe.Mqtt5SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 * @since 4.0.0
 */
@Immutable
public class SubscribePacketImpl implements SubscribePacket {

    final @NotNull ImmutableList<SubscriptionImpl> subscriptions;
    final @NotNull UserPropertiesImpl userProperties;
    final int subscriptionIdentifier;
    final int packetIdentifier;

    public SubscribePacketImpl(
            final @NotNull ImmutableList<SubscriptionImpl> subscriptions,
            final @NotNull UserPropertiesImpl userProperties,
            final int subscriptionIdentifier,
            final int packetIdentifier) {

        this.subscriptions = subscriptions;
        this.userProperties = userProperties;
        this.subscriptionIdentifier = subscriptionIdentifier;
        this.packetIdentifier = packetIdentifier;
    }

    public SubscribePacketImpl(final @NotNull SUBSCRIBE subscribe) {
        final ImmutableList.Builder<SubscriptionImpl> builder = ImmutableList.builder();
        subscribe.getTopics().forEach(topic -> builder.add(new SubscriptionImpl(topic)));
        subscriptions = builder.build();
        userProperties = UserPropertiesImpl.of(subscribe.getUserProperties().asList());
        subscriptionIdentifier = subscribe.getSubscriptionIdentifier();
        packetIdentifier = subscribe.getPacketIdentifier();
    }

    @Override
    public @NotNull ImmutableList<Subscription> getSubscriptions() {
        return ImmutableList.copyOf(subscriptions);
    }

    @Override
    public @NotNull UserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    @Override
    public @NotNull Optional<Integer> getSubscriptionIdentifier() {
        if (subscriptionIdentifier == Mqtt5SUBSCRIBE.DEFAULT_NO_SUBSCRIPTION_IDENTIFIER) {
            return Optional.empty();
        } else {
            return Optional.of(subscriptionIdentifier);
        }
    }

    @Override
    public int getPacketId() {
        return packetIdentifier;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubscribePacketImpl)) {
            return false;
        }
        final SubscribePacketImpl that = (SubscribePacketImpl) o;
        return subscriptions.equals(that.subscriptions) &&
                userProperties.equals(that.userProperties) &&
                (subscriptionIdentifier == that.subscriptionIdentifier) &&
                (packetIdentifier == that.packetIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriptions, userProperties, subscriptionIdentifier, packetIdentifier);
    }
}
