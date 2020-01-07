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
package com.hivemq.extension.sdk.api.packets.suback;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;

import java.util.List;

/**
 * A {@link SubackPacket} that can be modified before it is sent to the client.
 * <p>
 * For MQTT 3 clients you should only modify the reason codes, as the other properties exists only since MQTT 5 and
 * will therefore not be send to an MQTT 3 client.
 *
 * @author Robin Atherton
 */
@DoNotImplement
public interface ModifiableSubackPacket extends SubackPacket {

    /**
     * Sets the list of {@link SubackReasonCode reason codes} of the SUBACK packet.
     * <p>
     * MQTT 3.x clients know less SUBACK reason codes than MQTT 5 clients. See {@link SubackReasonCode} how HiveMQ will
     * transform the unknown reason codes for MQTT 3.x clients.
     *
     * @param reasonCodes The list of reason codes to be written into the SUBACK packet.
     * @throws NullPointerException     If the list or an individual reason code is <code>null</code>.
     * @throws IllegalArgumentException If the amount of reason codes passed differs from that contained in the packet
     *                                  being manipulated.
     * @throws IllegalStateException    If switching from successful reason code to unsuccessful reason code or vice
     *                                  versa. Check out {@link SubackReasonCode} to see what reason code counts
     *                                  as a success or unsuccessful code.
     */
    void setReasonCodes(@NotNull List<@NotNull SubackReasonCode> reasonCodes);

    /**
     * Sets the reason string of the SUBACK packet.
     * <p>
     * This setting is only respected for MQTT 5 clients and ignored for MQTT
     * 3.x clients when the SUBACK is sent to the client (as MQTT 3.x clients don't know this property).
     *
     * @param reasonString The reason string or <code>null</code> to remove the reason string.
     * @throws IllegalArgumentException If the reason string is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the reason string exceeds the UTF-8 string length limit.
     */
    void setReasonString(@Nullable String reasonString);

    /**
     * The modifiable {@link UserProperties} of the SUBACK packet.
     * <p>
     * This setting is only respected for MQTT 5 clients and ignored for MQTT 3.x clients
     * when the SUBACK is sent to the client (as MQTT 3.x clients don't know this property).
     *
     * @return Modifiable user properties.
     */
    @Override
    @NotNull ModifiableUserProperties getUserProperties();
}
