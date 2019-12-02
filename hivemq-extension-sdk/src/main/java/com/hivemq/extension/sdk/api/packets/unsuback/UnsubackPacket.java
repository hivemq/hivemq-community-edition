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

package com.hivemq.extension.sdk.api.packets.unsuback;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.unsuback.UnsubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

import java.util.List;
import java.util.Optional;

/**
 * Represents an UNSUBACK packet.
 * <p>
 * Contains all values of an MQTT 5 UNSUBACK, but will also be used to represent MQTT 3 UNSUBACK messages.
 *
 * @author Robin Atherton
 * @author Silvio Giebl
 */
@Immutable
@DoNotImplement
public interface UnsubackPacket {

    /**
     * The packet identifier of the UNSUBACK packet.
     *
     * @return The packet identifier.
     */
    int getPacketIdentifier();

    /**
     * Represents the return codes for the QoS levels of the different Topics contained in the corresponding UNSUBSCRIBE
     * message as well as potential failure codes. For an MQTT 3 client this {@link List} for the MQTT 5 property will
     * always be empty (if not modified by a previous {@link UnsubackOutboundInterceptor}).
     *
     * @return The reason codes for the unsubscribed topics.
     */
    @Immutable
    @NotNull List<@NotNull UnsubackReasonCode> getReasonCodes();

    /**
     * The reason string of the UNSUBACK packet.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty (if not modified by a
     * previous {@link UnsubackOutboundInterceptor}).
     *
     * @return An {@link Optional} containing the unsuback reason string if present.
     */
    @NotNull Optional<String> getReasonString();

    /**
     * The user properties from the UNSUBACK packet.
     * <p>
     * For an MQTT 3 client this MQTT 5 property will always be empty (if not modified by a previous {@link
     * UnsubackOutboundInterceptor}).
     *
     * @return The {@link UserProperties} of the UNSUBACK packet.
     */
    @Immutable
    @NotNull UserProperties getUserProperties();
}
