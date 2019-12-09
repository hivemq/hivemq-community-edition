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

package com.hivemq.extensions.services.auth;

import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.parameter.EnhancedAuthInput;
import com.hivemq.extension.sdk.api.packets.auth.AuthPacket;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.auth.AuthPacketImpl;
import com.hivemq.mqtt.message.auth.AUTH;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Daniel Krüger
 * @author Florian Limpöck
*/
public class AuthTaskInput extends AbstractAuthTaskInput<AuthTaskInput> implements PluginTaskInput, EnhancedAuthInput {

    private final @NotNull AUTH auth;
    private final boolean isReAuth;
    private @Nullable AuthPacketImpl authPacket;

    public AuthTaskInput(@NotNull final AUTH auth,
                         @NotNull final String clientId,
                         final boolean isReAuth,
                         @NotNull final ChannelHandlerContext ctx) {
        super(clientId, ctx);
        this.auth = auth;
        this.isReAuth = isReAuth;
    }

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

    @NotNull
    @Override
    public AuthTaskInput get() {
        return this;
    }
}