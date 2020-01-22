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

package com.hivemq.extensions.packets.subscribe;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.subscribe.ModifiableSubscribePacket;
import com.hivemq.extension.sdk.api.packets.subscribe.SubscribePacket;
import com.hivemq.extension.sdk.api.packets.subscribe.Subscription;
import com.hivemq.mqtt.message.subscribe.Mqtt5SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class SubscribePacketImpl implements SubscribePacket {

    private final @NotNull List<Subscription> subscriptionList;
    private final @NotNull UserProperties userProperties;
    private final int subscriptionIdentifier;
    private final int packetIdentifier;

    public SubscribePacketImpl(final @NotNull SUBSCRIBE subscribe) {

        subscriptionList = subscribe.getTopics().stream()
                .map((Function<Topic, Subscription>) topic -> new SubscriptionImpl(topic))
                .collect(Collectors.toList());
        userProperties = subscribe.getUserProperties().getPluginUserProperties();
        subscriptionIdentifier = subscribe.getSubscriptionIdentifier();
        packetIdentifier = subscribe.getPacketIdentifier();
    }

    public SubscribePacketImpl(final @NotNull ModifiableSubscribePacket subscribe) {

        subscriptionList = new ArrayList<>(subscribe.getSubscriptions());
        userProperties = subscribe.getUserProperties();
        subscriptionIdentifier = subscribe.getSubscriptionIdentifier().orElse(Mqtt5SUBSCRIBE.DEFAULT_NO_SUBSCRIPTION_IDENTIFIER);
        packetIdentifier = subscribe.getPacketId();
    }


    @NotNull
    @Override
    @Immutable
    public List<Subscription> getSubscriptions() {
        return subscriptionList;
    }

    @Override
    public @NotNull UserProperties getUserProperties() {
        return userProperties;
    }

    @NotNull
    @Override
    public Optional<Integer> getSubscriptionIdentifier() {
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
}
