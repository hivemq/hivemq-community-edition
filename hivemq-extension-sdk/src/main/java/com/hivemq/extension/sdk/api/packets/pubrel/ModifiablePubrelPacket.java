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
package com.hivemq.extension.sdk.api.packets.pubrel;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

/**
 * A {@link PubrelPacket} that can be modified before it is sent to the client (for {@link PubrelOutboundInterceptor})
 * or to the server (for {@link PubrelInboundInterceptor}).
 *
 * @author Yannick Weber
 */
@DoNotImplement
public interface ModifiablePubrelPacket extends PubrelPacket {

    /**
     * Set the reason string.
     * <p>
     * A reason must not be set for a successful publish.
     * <p>
     * For an {@link PubrelOutboundInterceptor} this setting is only respected for MQTT 5 clients and ignored for MQTT
     * 3.x clients when the PUBREL is sent to the client (as MQTT 3.x clients don't know this property).
     * <p>
     * For an {@link PubrelInboundInterceptor} this setting is respected for MQTT 5 and MQTT 3.x clients when the
     * PUBREL is sent to HiveMQ, this allows to enrich MQTT 3.x PUBRELs with this MQTT 5 property.
     *
     * @param reasonString The reason string to set.
     * @throws IllegalArgumentException If the reason string is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the reason string exceeds the UTF-8 string length limit.
     */
    void setReasonString(@Nullable String reasonString);

    /**
     * Get the modifiable {@link UserProperties} of the PUBREL packet.
     * <p>
     * For an {@link PubrelOutboundInterceptor} this setting is only respected for MQTT 5 clients and ignored for MQTT
     * 3.x clients when the PUBREL is sent to the client (as MQTT 3.x clients don't know this property).
     * <p>
     * For an {@link PubrelInboundInterceptor} this setting is respected for MQTT 5 and MQTT 3.x clients when the
     * PUBREL is sent to HiveMQ, this allows to enrich MQTT 3.x PUBRELs with this MQTT 5 property.
     *
     * @return Modifiable user properties.
     */
    @Override
    @NotNull ModifiableUserProperties getUserProperties();
}
