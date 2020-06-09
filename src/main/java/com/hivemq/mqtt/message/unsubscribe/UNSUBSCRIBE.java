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
package com.hivemq.mqtt.message.unsubscribe;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.packets.unsubscribe.UnsubscribePacketImpl;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttMessageWithUserProperties;

import java.util.List;

/**
 * The MQTT UNSUBSCRIBE message
 *
 * @author Dominik Obermaier
 * @author Florian Limpöck
 * @since 1.4
 */
@Immutable
public class UNSUBSCRIBE extends MqttMessageWithUserProperties implements Mqtt3UNSUBSCRIBE, Mqtt5UNSUBSCRIBE {

    private final ImmutableList<String> topics;

    //MQTT 3
    public UNSUBSCRIBE(@NotNull final ImmutableList<String> topics) {
        this(topics, 0);
    }

    //MQTT 3
    public UNSUBSCRIBE(@NotNull final ImmutableList<String> topics, final int packetIdentifier) {
        this(topics, packetIdentifier, Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    //MQTT 3
    public UNSUBSCRIBE(@NotNull final List<String> topics, final int packetIdentifier) {
        this(ImmutableList.copyOf(topics), packetIdentifier, Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    //MQTT 5
    public UNSUBSCRIBE(
            @NotNull final ImmutableList<String> topicFilters, final int packetIdentifier,
            final Mqtt5UserProperties userProperties) {
        super(userProperties);

        Preconditions.checkNotNull(topicFilters);
        Preconditions.checkArgument(!topicFilters.isEmpty(), "topics may never be empty");

        this.topics = topicFilters;
        setPacketIdentifier(packetIdentifier);
    }

    public static @NotNull UNSUBSCRIBE from(final @NotNull UnsubscribePacketImpl packet) {
        return new UNSUBSCRIBE(
                packet.getTopicFilters(),
                packet.getPacketIdentifier(),
                Mqtt5UserProperties.of(packet.getUserProperties().asInternalList()));
    }

    /**
     * @return a list of topic the client wants to unsubscribe to
     */
    @Override
    @NotNull
    public ImmutableList<String> getTopics() {
        return topics;
    }

    @NotNull
    @Override
    public MessageType getType() {
        return MessageType.UNSUBSCRIBE;
    }
}
