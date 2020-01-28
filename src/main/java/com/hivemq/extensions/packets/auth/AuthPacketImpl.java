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

package com.hivemq.extensions.packets.auth;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.auth.AuthPacket;
import com.hivemq.extension.sdk.api.packets.auth.AuthReasonCode;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author Daniel Krüger
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
public class AuthPacketImpl implements AuthPacket {

    private final @NotNull Mqtt5AuthReasonCode reasonCode;
    private final @NotNull String method;
    private final @Nullable byte[] data;
    private final @Nullable String reasonString;
    private final @NotNull Mqtt5UserProperties userProperties;

    public AuthPacketImpl(final @NotNull AUTH auth) {
        reasonCode = auth.getReasonCode();
        method = auth.getAuthMethod();
        data = auth.getAuthData();
        reasonString = auth.getReasonString();
        userProperties = auth.getUserProperties();
    }

    @Override
    public @NotNull AuthReasonCode getReasonCode() {
        return reasonCode.toAuthReasonCode();
    }

    @Override
    public @NotNull String getAuthenticationMethod() {
        return method;
    }

    @Override
    public @NotNull Optional<ByteBuffer> getAuthenticationData() {
        if (data == null) {
            return Optional.empty();
        }
        return Optional.of(ByteBuffer.wrap(data).asReadOnlyBuffer());
    }

    @Override
    public @NotNull Optional<byte[]> getAuthenticationDataAsArray() {
        if (data == null) {
            return Optional.empty();
        }
        return Optional.of(Arrays.copyOf(data, data.length));
    }

    @Override
    public @NotNull Optional<String> getReasonString() {
        return Optional.ofNullable(reasonString);
    }

    @Override
    public @Immutable @NotNull UserProperties getUserProperties() {
        return userProperties.getPluginUserProperties();
    }
}
