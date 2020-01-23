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

package com.hivemq.extension.sdk.api.packets.disconnect;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

import java.util.Optional;

/**
 * Represents a DISCONNECT packet.
 * <p>
 * Contains all values of an MQTT 5 DISCONNECT packet, but will also be used to represent MQTT 3.x DISCONNECT packets.
 *
 * @author Robin Atherton
 * @author Silvio Giebl
 */
@Immutable
@DoNotImplement
public interface DisconnectPacket {

    /**
     * The reason code of the DISCONNECT packet.
     * <p>
     * It will be {@link DisconnectReasonCode#NORMAL_DISCONNECTION} for MQTT 3.x clients (if not modified by an
     * interceptor).
     *
     * @return An enum containing the reason for disconnecting.
     */
    @NotNull DisconnectReasonCode getReasonCode();

    /**
     * The optional reason string of the DISCONNECT packet.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty (if not modified by a
     * previous {@link DisconnectInboundInterceptor} or {@link DisconnectOutboundInterceptor}).
     *
     * @return A string containing the disconnect reason if present.
     */
    @NotNull Optional<String> getReasonString();

    /**
     * The optional session expiry interval in seconds (duration for which the clients session is stored) of the
     * DISCONNECT packet.
     * <p>
     * If absent, the session expiry interval of the CONNECT/CONNACK handshake will be used.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty (if not modified by a
     * previous {@link DisconnectInboundInterceptor} or {@link DisconnectOutboundInterceptor}).
     *
     * @return A long representing the session expiry interval if present.
     */
    @NotNull Optional<Long> getSessionExpiryInterval();

    /**
     * The optional server reference of the DISCONNECT packet.
     * <p>
     * It is only present for outbound DISCONNECT packets from the server to a client.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty (if not modified by a
     * previous {@link DisconnectInboundInterceptor} or {@link DisconnectOutboundInterceptor}).
     *
     * @return A string representing the server reference if present.
     */
    @NotNull Optional<String> getServerReference();

    /**
     * The user properties from the DISCONNECT packet.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty (if not modified by a
     * previous {@link DisconnectInboundInterceptor} or {@link DisconnectOutboundInterceptor}).
     *
     * @return The {@link UserProperties} of the DISCONNECT packet.
     */
    @NotNull UserProperties getUserProperties();
}
