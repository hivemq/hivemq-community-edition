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
 * A copy of an {@link UnsubscribePacket} that can be modified for onward delivery.
 *
 * @author Robin Atherton
 */
@DoNotImplement
public interface ModifiableUnsubscribePacket extends UnsubscribePacket {

    /**
     * Sets the list of Topics to be unsubscribed from.
     *
     * @param topics the list of Topics to unsubscribe from.
     * @throws NullPointerException If the passed topic filter is <null>.
     */
    void setTopicFilters(@NotNull List<@NotNull String> topics);

    /**
     * Gets the modifiable {@link ModifiableUserProperties} of the UNSUBSCRIBE packet.
     */
    @Override
    @NotNull ModifiableUserProperties getUserProperties();
}
