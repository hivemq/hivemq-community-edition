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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.unsubscribe.ModifiableUnsubscribePacket;
import com.hivemq.extension.sdk.api.packets.unsubscribe.UnsubscribePacket;
import com.hivemq.extensions.packets.general.InternalUserProperties;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;

import java.util.List;
import java.util.Objects;

/**
 * @author Robin Atherton
 */
public class ModifiableUnsubscribePacketImpl implements ModifiableUnsubscribePacket {

    private @NotNull ImmutableList<String> topicFilters;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;
    private final int packetIdentifier;

    private boolean modified = false;

    public ModifiableUnsubscribePacketImpl(
            final @NotNull FullConfigurationService fullConfigurationService,
            final @NotNull UNSUBSCRIBE unsubscribe) {

        topicFilters = unsubscribe.getTopics();
        userProperties = new ModifiableUserPropertiesImpl(
                unsubscribe.getUserProperties().getPluginUserProperties(),
                fullConfigurationService.securityConfiguration().validateUTF8());
        packetIdentifier = unsubscribe.getPacketIdentifier();
    }

    public ModifiableUnsubscribePacketImpl(
            final @NotNull FullConfigurationService fullConfigurationService,
            final @NotNull UnsubscribePacket unsubscribe) {

        topicFilters = ImmutableList.copyOf(unsubscribe.getTopicFilters());
        userProperties = new ModifiableUserPropertiesImpl(
                (InternalUserProperties) unsubscribe.getUserProperties(),
                fullConfigurationService.securityConfiguration().validateUTF8());
        packetIdentifier = unsubscribe.getPacketIdentifier();
    }

    @Override
    public @Immutable @NotNull List<@NotNull String> getTopicFilters() {
        return topicFilters;
    }

    @Override
    public void setTopicFilters(final @NotNull List<@NotNull String> topicFilters) {
        Preconditions.checkNotNull(topicFilters, "Topic filters must never be null.");
        if (topicFilters.size() != this.topicFilters.size()) {
            throw new IllegalArgumentException("The amount of topic filters must not be changed.");
        }
        for (int i = 0; i < topicFilters.size(); i++) {
            Preconditions.checkNotNull(topicFilters.get(i), "Topic filter (at index %s) must never be null.", i);
        }
        if (Objects.equals(this.topicFilters, topicFilters)) {
            return;
        }
        this.topicFilters = ImmutableList.copyOf(topicFilters);
        modified = true;
    }

    @Override
    public @NotNull ModifiableUserProperties getUserProperties() {
        return userProperties;
    }

    @Override
    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    public boolean isModified() {
        return modified;
    }
}
