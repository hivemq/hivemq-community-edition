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

package com.hivemq.extensions.events;

import com.hivemq.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;

/**
 * The event to fire when the server sends a disconnect to a client or closes a clients channel.
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class OnServerDisconnectEvent {

    private final @Nullable DisconnectedReasonCode reasonCode;
    private final @Nullable String reasonString;
    private final @Nullable UserProperties userProperties;

    public OnServerDisconnectEvent() {
        this(null);
    }

    public OnServerDisconnectEvent(final @Nullable DisconnectedReasonCode disconnectReasonCode,
                                   final @Nullable String reasonString,
                                   final @Nullable Mqtt5UserProperties userProperties) {
        this.reasonCode = disconnectReasonCode;
        this.reasonString = reasonString;
        if (userProperties == null) {
            this.userProperties = null;
        } else {
            this.userProperties = userProperties.getPluginUserProperties();
        }
    }

    public OnServerDisconnectEvent(final @Nullable DISCONNECT disconnect) {
        if (disconnect != null) {
            this.reasonCode = Mqtt5DisconnectReasonCode.toPluginDisconnectedCode(disconnect.getReasonCode());
            this.reasonString = disconnect.getReasonString();
            this.userProperties = disconnect.getUserProperties().getPluginUserProperties();
        } else {
            this.reasonCode = null;
            this.reasonString = null;
            this.userProperties = null;
        }
    }

    public @Nullable DisconnectedReasonCode getReasonCode() {
        return reasonCode;
    }

    public @Nullable String getReasonString() {
        return reasonString;
    }

    public @Nullable UserProperties getUserProperties() {
        return userProperties;
    }

}
