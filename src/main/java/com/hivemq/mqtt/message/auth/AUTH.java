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
package com.hivemq.mqtt.message.auth;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttMessageWithUserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;

/**
 * AUTH Packet is used for the authentication.
 *
 * @author Waldemar Ruck
 * @since 4.0
 */
public class AUTH extends MqttMessageWithUserProperties.MqttMessageWithReasonCode<Mqtt5AuthReasonCode> {

    /**
     * Method by which the authentication is performed.
     * Authentication Method is UTF-8 Encoded. It is a Protocol Error to omit the Authentication Method or to include it more than once.
     */
    @NotNull
    private final String authMethod;

    /**
     * Necessary Data for the authentication.
     * It is a Protocol Error to include Authentication Data more than once.
     */
    @Nullable
    private final byte[] authData;

    public AUTH(@NotNull final String authMethod,
                @Nullable final byte[] authData,
                @NotNull final Mqtt5AuthReasonCode reasonCode,
                @NotNull final Mqtt5UserProperties userProperties,
                @Nullable final String reasonString) {

        super(reasonCode, reasonString, userProperties);

        Preconditions.checkNotNull(reasonCode, "Reason code must never be null");
        Preconditions.checkNotNull(authMethod, "Auth method must never be null");

        this.authMethod = authMethod;
        this.authData = authData;
    }

    @Nullable
    public byte[] getAuthData() {
        return authData;
    }

    @NotNull
    public String getAuthMethod() {
        return authMethod;
    }

    /**
     * AUTH if the Reason Code is SUCCESS
     *
     * @return The successful {@link AUTH}
     */
    public static AUTH getSuccessAUTH() {
        return new AUTH("", null, Mqtt5AuthReasonCode.SUCCESS, Mqtt5UserProperties.NO_USER_PROPERTIES, null);
    }

    @NotNull
    @Override
    public MessageType getType() {
        return MessageType.AUTH;
    }
}
