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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.packets.subscribe.ModifiableSubscribePacket;
import com.hivemq.extension.sdk.api.packets.subscribe.ModifiableSubscription;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.mqtt.message.subscribe.Mqtt5SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
@ThreadSafe
public class ModifiableSubscribePacketImpl implements ModifiableSubscribePacket {

    private final @NotNull ModifiableUserPropertiesImpl userProperties;
    private final int subscriptionIdentifier;
    private final int packetIdentifier;

    private final @NotNull List<ModifiableSubscription> modifiableSubscriptionList;

    public ModifiableSubscribePacketImpl(final @NotNull FullConfigurationService configurationService, final @NotNull SUBSCRIBE subscribe) {

        Preconditions.checkNotNull(subscribe, "subscribe must never be null");
        Preconditions.checkNotNull(configurationService, "config must never be null");

        subscriptionIdentifier = subscribe.getSubscriptionIdentifier();
        packetIdentifier = subscribe.getPacketIdentifier();

        modifiableSubscriptionList = subscribe.getTopics().stream()
                .map((Function<Topic, ModifiableSubscriptionImpl>) topic -> new ModifiableSubscriptionImpl(configurationService, Objects.requireNonNull(topic)))
                .collect(Collectors.toList());

        this.userProperties = new ModifiableUserPropertiesImpl(subscribe.getUserProperties().getPluginUserProperties(), configurationService.securityConfiguration().validateUTF8());
    }

    @NotNull
    @Override
    public synchronized Optional<Integer> getSubscriptionIdentifier() {
        if (subscriptionIdentifier == Mqtt5SUBSCRIBE.DEFAULT_NO_SUBSCRIPTION_IDENTIFIER) {
            return Optional.empty();
        } else {
            return Optional.of(subscriptionIdentifier);
        }
    }

    @NotNull
    @Override
    @Immutable
    public synchronized List<ModifiableSubscription> getSubscriptions() {
        return modifiableSubscriptionList;
    }

    @Override
    public synchronized @NotNull ModifiableUserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    @Override
    public synchronized int getPacketId() {
        return packetIdentifier;
    }

    public synchronized boolean isModified() {
        for (final ModifiableSubscription modifiableSubscription : modifiableSubscriptionList) {
            if (((ModifiableSubscriptionImpl) modifiableSubscription).isModified()) {
                return true;
            }
        }
        return userProperties.isModified();
    }
}
