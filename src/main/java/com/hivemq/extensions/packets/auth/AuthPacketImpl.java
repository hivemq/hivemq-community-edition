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

import com.hivemq.annotations.Immutable;
import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.auth.AuthPacket;
import com.hivemq.extension.sdk.api.packets.auth.AuthReasonCode;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * @author Daniel Krüger
 * @author Florian Limpöck
*/
public class AuthPacketImpl implements AuthPacket {

    private final @NotNull AUTH auth;

    public AuthPacketImpl(final @NotNull AUTH auth) {
        this.auth = auth;
    }

    @NotNull
    @Override
    public String getAuthenticationMethod() {
        return auth.getAuthMethod();
    }

    @NotNull
    @Override
    @Immutable
    public Optional<ByteBuffer> getAuthenticationData() {
        final byte[] authData = auth.getAuthData();
        if (authData == null) {
            return Optional.empty();
        }
        return Optional.of(ByteBuffer.wrap(authData).asReadOnlyBuffer());
    }

    @NotNull
    @Override
    public Optional<byte[]> getAuthenticationDataAsBytes() {
        final byte[] authData = auth.getAuthData();
        if (authData == null) {
            return Optional.empty();
        }
        return Optional.of(authData);
    }

    @NotNull
    @Override
    public AuthReasonCode getReasonCode() {
        final Mqtt5AuthReasonCode reasonCode = auth.getReasonCode();
        return toAuthReasonCode(reasonCode);
    }

    @NotNull
    private AuthReasonCode toAuthReasonCode(final @NotNull Mqtt5AuthReasonCode reasonCode) {
        switch(reasonCode){
            case SUCCESS:
                return AuthReasonCode.SUCCESS;
            case CONTINUE_AUTHENTICATION:
                return AuthReasonCode.CONTINUE_AUTHENTICATION;
            case REAUTHENTICATE:
            default:
                return AuthReasonCode.REAUTHENTICATE;
        }
    }

    @NotNull
    @Override
    public Optional<String> getReasonString() {
        return Optional.ofNullable(auth.getReasonString());
    }

    @NotNull
    @Override
    @Immutable
    public UserProperties getUserProperties() {
        return auth.getUserProperties().getPluginUserProperties();
    }
}
