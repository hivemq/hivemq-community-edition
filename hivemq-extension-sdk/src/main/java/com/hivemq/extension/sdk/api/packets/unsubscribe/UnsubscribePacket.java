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

package com.hivemq.extension.sdk.api.packets.unsubscribe;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.UnsubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

import java.util.List;

/**
 * Represents an UNSUBSCRIBE packet.
 * <p>
 * Contains all values of an MQTT 5 UNSUBSCRIBE, but will also be used to represent an MQTT 3 UNSUBSCRIBE.
 *
 * @author Robin Atherton
 */
@Immutable
@DoNotImplement
public interface UnsubscribePacket {

    /**
     * The packet identifier of the UNSUBSCRIBE packet.
     *
     * @return The packet identifier.
     */
    int getPacketIdentifier();

    /**
     * Gets the list of topics to be unsubscribed from.
     *
     * @return The list of topics to be unsubscribed from.
     */
    @Immutable
    @NotNull List<@NotNull String> getTopicFilters();

    /**
     * The user properties from the UNSUBSCRIBE packet.
     * <p>
     * For an MQTT 3 client this MQTT 5 property will always be empty (if not modified by a previous {@link
     * UnsubscribeInboundInterceptor}).
     *
     * @return The {@link UserProperties} of the UNSUBSCRIBE packet.
     */
    @Immutable
    @NotNull UserProperties getUserProperties();

}
