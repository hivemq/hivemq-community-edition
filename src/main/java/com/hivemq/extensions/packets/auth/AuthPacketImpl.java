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
package com.hivemq.extensions.packets.auth;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.auth.AuthPacket;
import com.hivemq.extension.sdk.api.packets.auth.AuthReasonCode;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.auth.AUTH;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Daniel Krüger
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
@Immutable
public class AuthPacketImpl implements AuthPacket {

    private final @NotNull AuthReasonCode reasonCode;
    private final @NotNull String method;
    private final @Nullable byte[] data;
    private final @Nullable String reasonString;
    private final @NotNull UserPropertiesImpl userProperties;

    public AuthPacketImpl(
            final @NotNull AuthReasonCode reasonCode,
            final @NotNull String method,
            final @Nullable byte[] data,
            final @Nullable String reasonString,
            final @NotNull UserPropertiesImpl userProperties) {

        this.reasonCode = reasonCode;
        this.method = method;
        this.data = data;
        this.reasonString = reasonString;
        this.userProperties = userProperties;
    }

    public AuthPacketImpl(final @NotNull AUTH auth) {
        this(
                auth.getReasonCode().toAuthReasonCode(),
                auth.getAuthMethod(),
                auth.getAuthData(),
                auth.getReasonString(),
                UserPropertiesImpl.of(auth.getUserProperties().asList()));
    }

    @Override
    public @NotNull AuthReasonCode getReasonCode() {
        return reasonCode;
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
    public @NotNull UserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthPacketImpl)) {
            return false;
        }
        final AuthPacketImpl that = (AuthPacketImpl) o;
        return (reasonCode == that.reasonCode) &&
                method.equals(that.method) &&
                Arrays.equals(data, that.data) &&
                Objects.equals(reasonString, that.reasonString) &&
                userProperties.equals(that.userProperties);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(reasonCode, method, reasonString, userProperties);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
