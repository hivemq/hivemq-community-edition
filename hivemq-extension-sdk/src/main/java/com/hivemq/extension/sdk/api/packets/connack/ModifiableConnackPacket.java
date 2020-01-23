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

package com.hivemq.extension.sdk.api.packets.connack;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

/**
 * A {@link ConnackPacket} that can be modified before it is sent to the client.
 *
 * @author Florian Limpöck
 * @since 4.2.0
 */
public interface ModifiableConnackPacket extends ConnackPacket {

    /**
     * Set a {@link ConnackReasonCode} to the CONNACK packet.
     * <p>
     * Switching from successful to unsuccessful and vice versa is not supported.
     *
     * @param reasonCode The reason code to set.
     * @throws NullPointerException  If reason code is <b>null</b>.
     * @throws IllegalStateException If switching from successful reason code to unsuccessful reason code or vice versa.
     *                               Check out {@link ConnackReasonCode} to see what reason code counts as a success or
     *                               unsuccessful code.
     * @see ConnackReasonCode How reason codes are translated from MQTT 5 to MQTT 3.
     * @since 4.2.0
     */
    void setReasonCode(@NotNull ConnackReasonCode reasonCode);

    /**
     * Set the reason string.
     * <p>
     * A reason must not be set for a successful connack.
     * <p>
     * This setting is only respected for MQTT 5 clients. For MQTT 3.x clients this setting is ignored.
     *
     * @param reasonString The reason string to set.
     * @throws IllegalStateException    If reason code is {@link ConnackReasonCode#SUCCESS}.
     * @throws IllegalArgumentException If the reason string is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the reason string exceeds the UTF-8 string length limit.
     * @since 4.2.0
     */
    void setReasonString(@Nullable String reasonString);

    /**
     * Get the modifiable {@link UserProperties} of the CONNACK packet.
     *
     * @return Modifiable user properties.
     * @since 4.2.0
     */
    @Override
    @NotNull
    ModifiableUserProperties getUserProperties();

    /**
     * Set the response information.
     * <p>
     * This setting is only respected for MQTT 5 clients. For MQTT 3.x clients this setting is ignored.
     *
     * @param responseInformation The new response information for the CONNACK.
     * @throws IllegalArgumentException If the response information is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the response information exceeds the UTF-8 string length limit.
     * @throws IllegalStateException    If the response information was not requested in the connect packet.
     * @since 4.2.0
     */
    void setResponseInformation(@Nullable String responseInformation);

    /**
     * Set the server reference.
     * <p>
     * This setting is only respected for MQTT 5 clients. For MQTT 3.x clients this setting is ignored.
     *
     * @param serverReference The new server reference for the CONNACK.
     * @throws IllegalArgumentException If the server reference is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the server reference exceeds the UTF-8 string length limit.
     * @since 4.2.0
     */
    void setServerReference(@Nullable String serverReference);

}
