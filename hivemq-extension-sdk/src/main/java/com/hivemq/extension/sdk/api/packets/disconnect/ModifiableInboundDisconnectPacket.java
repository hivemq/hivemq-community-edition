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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

/**
 * An inbound {@link DisconnectPacket} that can be modified before it is sent to the server.
 *
 * @author Robin Atherton
 * @author Silvio Giebl
 */
@DoNotImplement
public interface ModifiableInboundDisconnectPacket extends DisconnectPacket {

    /**
     * Set the {@link DisconnectReasonCode} of the DISCONNECT packet.
     *
     * @param reasonCode The reason code to set.
     * @throws NullPointerException     If reason code is <code>null</code>.
     * @throws IllegalArgumentException If the disconnect reason code must not be used for inbound disconnect packets
     *                                  from a client to the server.
     * @see DisconnectReasonCode What reason codes exist for inbound disconnect packets from a client to the server.
     */
    void setReasonCode(@NotNull DisconnectReasonCode reasonCode);

    /**
     * Set the reason string of the DISCONNECT packet.
     * <p>
     * This setting is only respected for MQTT 5 clients. For MQTT 3.x clients this setting is ignored.
     *
     * @param reasonString The reason string to set or <code>null</code> to remove the reason string.
     * @throws IllegalArgumentException If the reason string is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the reason string exceeds the UTF-8 string length limit.
     */
    void setReasonString(@Nullable String reasonString);

    /**
     * Sets the session expiry interval of the DISCONNECT packet.
     *
     * @param sessionExpiryInterval The session expiry interval to set or <code>null</code> to use the session expiry
     *                              interval of the CONNECT/CONNACK handshake.
     * @throws IllegalStateException    If the session expiry interval is modified if the session expiry interval of the
     *                                  CONNECT packet was 0.
     * @throws IllegalArgumentException If the session expiry interval is less than 0.
     * @throws IllegalArgumentException If the session expiry interval is greater than the configured maximum.
     */
    void setSessionExpiryInterval(@Nullable Long sessionExpiryInterval);

    /**
     * The modifiable {@link UserProperties} of the DISCONNECT packet.
     *
     * @return The modifiable user properties.
     */
    @Override
    @NotNull ModifiableUserProperties getUserProperties();
}
