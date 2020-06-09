/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.events;

import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

/**
 * The event to fire when a client disconnects gracefully or ungracefully.
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class OnClientDisconnectEvent {

    private final @Nullable DisconnectedReasonCode reasonCode;
    private final @Nullable String reasonString;
    private final @Nullable UserProperties userProperties;
    private final boolean graceful;

    public OnClientDisconnectEvent(
            final @Nullable DisconnectedReasonCode reasonCode,
            final @Nullable String reasonString,
            final @Nullable UserProperties userProperties,
            final boolean graceful) {

        this.reasonCode = reasonCode;
        this.reasonString = reasonString;
        this.userProperties = userProperties;
        this.graceful = graceful;
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

    public boolean isGraceful() {
        return graceful;
    }
}
