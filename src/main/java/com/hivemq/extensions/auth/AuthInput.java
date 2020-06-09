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
package com.hivemq.extensions.auth;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.parameter.EnhancedAuthInput;
import com.hivemq.extension.sdk.api.packets.auth.AuthPacket;
import com.hivemq.extensions.packets.auth.AuthPacketImpl;
import com.hivemq.extensions.parameter.ClientBasedInputImpl;
import com.hivemq.mqtt.message.auth.AUTH;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Daniel Krüger
 * @author Florian Limpöck
 */
public class AuthInput extends ClientBasedInputImpl implements EnhancedAuthInput, Supplier<AuthInput> {

    private final @NotNull AUTH auth;
    private final boolean isReAuth;
    private @Nullable AuthPacketImpl authPacket;

    public AuthInput(
            final @NotNull String clientId,
            final @NotNull Channel channel,
            final @NotNull AUTH auth,
            final boolean isReAuth) {

        super(clientId, channel);
        this.auth = auth;
        this.isReAuth = isReAuth;
    }

    @Override
    public @NotNull AuthPacket getAuthPacket() {
        if (authPacket == null) {
            authPacket = new AuthPacketImpl(auth);
        }
        return authPacket;
    }

    @Override
    public boolean isReAuthentication() {
        return isReAuth;
    }

    @Override
    public @NotNull AuthInput get() {
        return this;
    }
}