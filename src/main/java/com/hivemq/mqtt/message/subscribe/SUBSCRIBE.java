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

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.packets.subscribe.SubscribePacketImpl;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttMessageWithUserProperties;

/**
 * The MQTT SUBSCRIBE message
 *
 * @author Dominik Obermaier
 * @author Florian Limpöck
 * @since 1.4
 */
@Immutable
public class SUBSCRIBE extends MqttMessageWithUserProperties implements Mqtt3SUBSCRIBE, Mqtt5SUBSCRIBE {

    private final @NotNull ImmutableList<Topic> topics;
    private final int subscriptionIdentifier;

    /**
     * Creates a new MQTT 5 SUBSCRIBE message
     */
    public SUBSCRIBE(final @NotNull Mqtt5UserProperties userProperties,
                     final @NotNull ImmutableList<Topic> topics,
                     final int packetIdentifier,
                     final int subscriptionIdentifier) {
        super(userProperties);
        this.topics = topics;
        this.subscriptionIdentifier = subscriptionIdentifier;
        super.setPacketIdentifier(packetIdentifier);
    }

    /**
     * Creates a new MQTT 3 SUBSCRIBE message
     */
    public SUBSCRIBE(final @NotNull ImmutableList<Topic> topics, final int packetIdentifier) {
        this(Mqtt5UserProperties.NO_USER_PROPERTIES, topics, packetIdentifier, DEFAULT_NO_SUBSCRIPTION_IDENTIFIER);
    }

    /**
     * Creates a new MQTT 3 SUBSCRIBE message
     */
    public SUBSCRIBE(final int packetIdentifier, final Topic... topics) {
        this(Mqtt5UserProperties.NO_USER_PROPERTIES, ImmutableList.copyOf(topics), packetIdentifier, DEFAULT_NO_SUBSCRIPTION_IDENTIFIER);
    }

    @NotNull
    public static SUBSCRIBE from(final @NotNull SubscribePacketImpl packet) {
        final ImmutableList.Builder<Topic> subscriptionBuilder = ImmutableList.builder();
        packet.getSubscriptions().forEach(subscription -> subscriptionBuilder.add(
                Topic.topicFromSubscription(subscription, packet.getSubscriptionIdentifier().orElse(null))));

        return new SUBSCRIBE(
                Mqtt5UserProperties.of(packet.getUserProperties().asInternalList()),
                subscriptionBuilder.build(),
                packet.getPacketId(),
                packet.getSubscriptionIdentifier().orElse(DEFAULT_NO_SUBSCRIPTION_IDENTIFIER));
    }

    @Override
    public @NotNull ImmutableList<Topic> getTopics() {
        return topics;
    }

    @Override
    public int getSubscriptionIdentifier() {
        return subscriptionIdentifier;
    }

    @NotNull
    @Override
    public MessageType getType() {
        return MessageType.SUBSCRIBE;
    }
}
