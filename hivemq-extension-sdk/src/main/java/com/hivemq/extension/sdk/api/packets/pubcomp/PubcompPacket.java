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
package com.hivemq.extension.sdk.api.packets.pubcomp;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

import java.util.Optional;

/**
 * Represents a PUBCOMP packet.
 * <p>
 * Contains all values of an MQTT 5 PUBCOMP, but will also used to represent MQTT 3 PUBCOMP packets.
 *
 * @author Yannick Weber
 */
@Immutable
@DoNotImplement
public interface PubcompPacket {

    /**
     * The packet identifier of the PUBCOMP.
     *
     * @return The packet identifier.
     */
    int getPacketIdentifier();

    /**
     * The reason code from the PUBCOMP packet.
     *
     * @return The pubcomp reason code.
     * @see PubcompReasonCode How reason codes are translated from MQTT 5 to MQTT 3.
     */
    @NotNull PubcompReasonCode getReasonCode();

    /**
     * The reason string of the PUBCOMP packet.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty (if not modified by a
     * previous {@link PubcompInboundInterceptor} or {@link PubcompOutboundInterceptor}).
     *
     * @return An {@link Optional} containing the pubcomp reason string if present.
     */
    @NotNull Optional<String> getReasonString();

    /**
     * The user properties from the PUBCOMP packet.
     * <p>
     * For an MQTT 3 client this MQTT 5 property will always be empty (if not modified by a
     * previous {@link PubcompInboundInterceptor} or {@link PubcompOutboundInterceptor}).
     *
     * @return The user properties.
     */
    @NotNull UserProperties getUserProperties();
}
