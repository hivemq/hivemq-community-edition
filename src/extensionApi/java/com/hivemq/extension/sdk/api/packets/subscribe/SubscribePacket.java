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
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

import java.util.List;
import java.util.Optional;

/**
 * Represents a SUBSCRIBE packet.
 * <p>
 * Contains all values of an MQTT 5 SUBSCRIBE, but will also be used to represent MQTT 3 subscribe messages.
 *
 * @author Florian Limpöck
 * @since 4.2.0
 */
@DoNotImplement
public interface SubscribePacket {

    /**
     * The list of unmodifiable subscriptions of the SUBSCRIBE.
     *
     * @return The list of {@link Subscription}s.
     * @since 4.2.0
     */
    @NotNull
    @Immutable
    List<Subscription> getSubscriptions();

    /**
     * The user properties from the SUBSCRIBE packet.
     * <p>
     * For an MQTT 3 client this MQTT 5 property will always contain an empty list as the user properties.
     *
     * @return The {@link UserProperties} of the SUBSCRIBE packet.
     * @since 4.2.0
     */
    @NotNull
    @Immutable
    UserProperties getUserProperties();

    /**
     * If this property is present, it contains the subscription identifier for the SUBSCRIBE packet.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty.
     *
     * @return An {@link Optional} containing the subscription identifier of the SUBSCRIBE packet if present.
     * @since 4.2.0
     */
    @NotNull
    Optional<Integer> getSubscriptionIdentifier();

    /**
     * The packet identifier of the SUBSCRIBE packet.
     *
     * @return The packet identifier.
     * @since 4.2.0
     */
    int getPacketId();

}
