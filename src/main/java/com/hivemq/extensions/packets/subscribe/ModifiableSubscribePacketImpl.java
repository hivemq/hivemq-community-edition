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
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.packets.subscribe.ModifiableSubscribePacket;
import com.hivemq.extension.sdk.api.packets.subscribe.ModifiableSubscription;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;

import java.util.Optional;

import static com.hivemq.mqtt.message.subscribe.Mqtt5SUBSCRIBE.DEFAULT_NO_SUBSCRIPTION_IDENTIFIER;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 * @since 4.2.0
 */
@ThreadSafe
public class ModifiableSubscribePacketImpl implements ModifiableSubscribePacket {

    private final @NotNull ImmutableList<ModifiableSubscriptionImpl> subscriptions;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;
    private final int subscriptionIdentifier;
    private final int packetIdentifier;

    private final @NotNull FullConfigurationService configurationService;

    public ModifiableSubscribePacketImpl(
            final @NotNull SubscribePacketImpl packet,
            final @NotNull FullConfigurationService configurationService) {

        final ImmutableList.Builder<ModifiableSubscriptionImpl> builder = ImmutableList.builder();
        packet.subscriptions.forEach(
                subscription -> builder.add(new ModifiableSubscriptionImpl(subscription, configurationService)));
        subscriptions = builder.build();
        userProperties = new ModifiableUserPropertiesImpl(
                packet.userProperties.asInternalList(), configurationService.securityConfiguration().validateUTF8());
        subscriptionIdentifier = packet.subscriptionIdentifier;
        packetIdentifier = packet.packetIdentifier;

        this.configurationService = configurationService;
    }

    @Override
    public @NotNull ImmutableList<ModifiableSubscription> getSubscriptions() {
        return ImmutableList.copyOf(subscriptions);
    }

    @Override
    public @NotNull ModifiableUserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    @Override
    public @NotNull Optional<Integer> getSubscriptionIdentifier() {
        if (subscriptionIdentifier == DEFAULT_NO_SUBSCRIPTION_IDENTIFIER) {
            return Optional.empty();
        } else {
            return Optional.of(subscriptionIdentifier);
        }
    }

    @Override
    public int getPacketId() {
        return packetIdentifier;
    }

    public boolean isModified() {
        for (final ModifiableSubscription modifiableSubscription : subscriptions) {
            if (((ModifiableSubscriptionImpl) modifiableSubscription).isModified()) {
                return true;
            }
        }
        return userProperties.isModified();
    }

    public @NotNull SubscribePacketImpl copy() {
        final ImmutableList.Builder<SubscriptionImpl> builder = ImmutableList.builder();
        subscriptions.forEach(subscription -> builder.add(subscription.copy()));
        return new SubscribePacketImpl(
                builder.build(), userProperties.copy(), subscriptionIdentifier, packetIdentifier);
    }

    public @NotNull ModifiableSubscribePacketImpl update(final @NotNull SubscribePacketImpl packet) {
        return new ModifiableSubscribePacketImpl(packet, configurationService);
    }
}
