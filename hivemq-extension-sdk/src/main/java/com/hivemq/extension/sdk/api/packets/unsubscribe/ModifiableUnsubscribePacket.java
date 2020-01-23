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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;

import java.util.List;

/**
 * A copy of an {@link UnsubscribePacket} that can be modified before it is processed by HiveMQ.
 *
 * @author Robin Atherton
 */
@DoNotImplement
public interface ModifiableUnsubscribePacket extends UnsubscribePacket {

    /**
     * Sets the list of Topics to be unsubscribed from.
     *
     * @param topics The list of Topics to unsubscribe from.
     * @throws NullPointerException     If the list of topics or one of its elements is <code>null</code>.
     * @throws IllegalArgumentException If the amount of topics passed differs from that contained in the packet being
     *                                  manipulated.
     */
    void setTopicFilters(@NotNull List<@NotNull String> topics);

    /**
     * Gets the modifiable {@link ModifiableUserProperties} of the UNSUBSCRIBE packet.
     * <p>
     * This setting is respected for MQTT 5 and MQTT 3.x clients when the UNSUBSCRIBE is processed by HiveMQ, this
     * allows to enrich MQTT 3.x UNSUBSCRIBEs with this MQTT 5 property.
     *
     * @return Modifiable user properties.
     */
    @Override
    @NotNull ModifiableUserProperties getUserProperties();
}
