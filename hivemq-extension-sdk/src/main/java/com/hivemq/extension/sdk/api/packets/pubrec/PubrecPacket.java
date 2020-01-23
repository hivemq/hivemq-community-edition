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
package com.hivemq.extension.sdk.api.packets.pubrec;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;

import java.util.Optional;

/**
 * Represents a PUBREC packet.
 * <p>
 * Contains all values of an MQTT 5 PUBREC, but will also used to represent MQTT 3 PUBREC packets.
 *
 * @author Yannick Weber
 */
@Immutable
@DoNotImplement
public interface PubrecPacket {

    /**
     * The packet identifier of the PUBREC.
     *
     * @return The packet identifier.
     */
    int getPacketIdentifier();

    /**
     * The reason code from the PUBREC packet.
     * <p>
     *
     * @return The pubrec reason code.
     * @see AckReasonCode How reason codes are translated from MQTT 5 to MQTT 3.
     */
    @NotNull AckReasonCode getReasonCode();

    /**
     * The reason string of the PUBREC packet.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty (if not modified by a
     * previous {@link PubrecInboundInterceptor} or {@link PubrecOutboundInterceptor}).
     *
     * @return An {@link Optional} containing the pubrec reason string if present.
     */
    @NotNull Optional<String> getReasonString();

    /**
     * The user properties from the PUBREC packet.
     * <p>
     * For an MQTT 3 client this MQTT 5 property will always be empty (if not modified by a
     * previous {@link PubrecInboundInterceptor} or {@link PubrecOutboundInterceptor}).
     *
     * @return The user properties.
     */
    @NotNull UserProperties getUserProperties();
}
