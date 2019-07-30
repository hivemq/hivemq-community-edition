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

package com.hivemq.extension.sdk.api.packets.subscribe;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.services.subscription.SubscriptionStore;

import java.util.List;
import java.util.Optional;

/**
 * A copy of an {@link SubscribePacket} that can be modified for onward delivery.
 *
 * @author Florian Limpöck
 * @since 4.2.0
 */
@DoNotImplement
public interface ModifiableSubscribePacket {

    /**
     * If this property is present, it contains the subscription identifier for the SUBSCRIBE packet.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty.
     * <p>
     * It is not possible to change the subscription identifier.
     *
     * @return An {@link Optional} containing the subscription identifier of the SUBSCRIBE packet if present.
     * @since 4.2.0
     */
    @NotNull
    Optional<Integer> getSubscriptionIdentifier();

    /**
     * Get the modifiable {@link UserProperties} of the SUBSCRIBE packet.
     *
     * @return Modifiable user properties.
     * @since 4.2.0
     */
    @NotNull
    ModifiableUserProperties getUserProperties();

    /**
     * Get the modifiable subscriptions of the SUBSCRIBE packet.
     * <p>
     * The list itself is immutable, so you cannot add or remove subscriptions from the SUBSCRIBE.
     * To do this please use the {@link SubscriptionStore}.
     *
     * @return An unmodifiable list of all {@link ModifiableSubscription}s of the SUBSCRIBE.
     * @since 4.2.0
     */
    @NotNull
    @Immutable
    List<ModifiableSubscription> getSubscriptions();

    /**
     * The packet identifier of the SUBSCRIBE packet.
     * <p>
     * It is not possible to change the packet identifier.
     *
     * @return The packet identifier.
     * @since 4.2.0
     */
    int getPacketId();
}
