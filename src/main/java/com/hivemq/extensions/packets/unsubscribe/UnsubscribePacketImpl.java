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
package com.hivemq.extensions.packets.unsubscribe;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.unsubscribe.UnsubscribePacket;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;

import java.util.Objects;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
@Immutable
public class UnsubscribePacketImpl implements UnsubscribePacket {

    final @NotNull ImmutableList<String> topicFilters;
    final @NotNull UserPropertiesImpl userProperties;
    final int packetIdentifier;

    public UnsubscribePacketImpl(
            final @NotNull ImmutableList<String> topicFilters,
            final @NotNull UserPropertiesImpl userProperties,
            final int packetIdentifier) {

        this.topicFilters = topicFilters;
        this.userProperties = userProperties;
        this.packetIdentifier = packetIdentifier;
    }

    public UnsubscribePacketImpl(final @NotNull UNSUBSCRIBE unsubscribe) {
        this(
                unsubscribe.getTopics(),
                UserPropertiesImpl.of(unsubscribe.getUserProperties().asList()),
                unsubscribe.getPacketIdentifier());
    }

    @Override
    public @NotNull ImmutableList<String> getTopicFilters() {
        return topicFilters;
    }

    @Override
    public @NotNull UserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    @Override
    public int getPacketIdentifier() {
        return this.packetIdentifier;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnsubscribePacketImpl)) {
            return false;
        }
        final UnsubscribePacketImpl that = (UnsubscribePacketImpl) o;
        return topicFilters.equals(that.topicFilters) &&
                userProperties.equals(that.userProperties) &&
                (packetIdentifier == that.packetIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicFilters, userProperties, packetIdentifier);
    }
}
