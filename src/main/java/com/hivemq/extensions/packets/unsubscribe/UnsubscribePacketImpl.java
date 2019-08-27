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
package com.hivemq.extensions.packets.unsubscribe;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.unsubscribe.UnsubscribePacket;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;

import java.util.List;

/**
 * @author Robin Atherton
 */
@Immutable
public class UnsubscribePacketImpl implements UnsubscribePacket {

    private final @NotNull List<String> topicFilters;
    private final @NotNull UserProperties userProperties;
    private final int packetIdentifier;

    public UnsubscribePacketImpl(final @NotNull UNSUBSCRIBE unsubscribe) {
        topicFilters = unsubscribe.getTopics();
        userProperties = unsubscribe.getUserProperties().getPluginUserProperties();
        packetIdentifier = unsubscribe.getPacketIdentifier();
    }

    public UnsubscribePacketImpl(final @NotNull UnsubscribePacket unsubscribe) {
        topicFilters = unsubscribe.getTopicFilters();
        userProperties = unsubscribe.getUserProperties();
        packetIdentifier = unsubscribe.getPacketIdentifier();
    }

    @Override
    public @Immutable @NotNull List<@NotNull String> getTopicFilters() {
        return topicFilters;
    }

    @Override
    public @Immutable @NotNull UserProperties getUserProperties() {
        return userProperties;
    }

    @Override
    public int getPacketIdentifier() {
        return this.packetIdentifier;
    }
}
