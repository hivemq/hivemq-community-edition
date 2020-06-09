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

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;

/**
 * The event to fire when client auth failed.
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
@Immutable
public class OnAuthFailedEvent {

    private final @Nullable DisconnectedReasonCode reasonCode;
    private final @Nullable String reasonString;
    private final @Nullable UserPropertiesImpl userProperties;

    public OnAuthFailedEvent(
            final @Nullable DisconnectedReasonCode reasonCode,
            final @Nullable String reasonString,
            final @Nullable Mqtt5UserProperties userProperties) {

        this.reasonCode = reasonCode;
        this.reasonString = reasonString;
        this.userProperties = (userProperties == null) ? null : UserPropertiesImpl.of(userProperties.asList());
    }

    public @Nullable DisconnectedReasonCode getReasonCode() {
        return reasonCode;
    }

    public @Nullable String getReasonString() {
        return reasonString;
    }

    public @Nullable UserPropertiesImpl getUserProperties() {
        return userProperties;
    }
}
