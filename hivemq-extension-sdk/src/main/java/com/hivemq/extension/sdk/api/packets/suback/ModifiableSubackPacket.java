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
 * Represents a SUBACK packet that can be modified.
 *
 * @author Robin Atherton
 */
@DoNotImplement
public interface ModifiableSubackPacket extends SubackPacket {

    /**
     * Sets the list of {@link SubackReasonCode reason codes} of the SUBACK packet.
     *
     * @param reasonCodes the list of reason codes.
     * @throws NullPointerException     If the list or an individual reason code is <code>null</code>.
     * @throws IllegalArgumentException If the amount of reason codes passed differs from that contained in the packet
     *                                  being manipulated.
     */
    void setReasonCodes(@NotNull List<@NotNull SubackReasonCode> reasonCodes);

    /**
     * Sets the reason string of the SUBACK packet.
     *
     * @param reasonString the reason string or <code>null</code> to remove the reason string.
     * @throws IllegalArgumentException If the reason string is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the reason string exceeds the UTF-8 string length limit.
     */
    void setReasonString(@Nullable String reasonString);

    /**
     * The modifiable {@link UserProperties} of the SUBACK packet.
     *
     * @return Modifiable user properties.
     */
    @Override
    @NotNull ModifiableUserProperties getUserProperties();
}
