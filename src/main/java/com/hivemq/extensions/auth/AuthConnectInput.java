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

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.parameter.EnhancedAuthConnectInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extensions.packets.connect.ConnectPacketImpl;
import com.hivemq.extensions.parameter.ClientBasedInputImpl;
import com.hivemq.mqtt.message.connect.CONNECT;
import io.netty.channel.Channel;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author Georg Held
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
public class AuthConnectInput extends ClientBasedInputImpl
        implements SimpleAuthInput, EnhancedAuthConnectInput, Supplier<AuthConnectInput> {

    private final @NotNull CONNECT connect;
    private @Nullable ConnectPacketImpl connectPacket;
    private final long connectTimestamp;

    public AuthConnectInput(final @NotNull CONNECT connect, final @NotNull Channel channel) {
        super(connect.getClientIdentifier(), channel);
        this.connect = connect;
        this.connectTimestamp = Objects.requireNonNullElse(channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().getConnectReceivedTimestamp(),
                System.currentTimeMillis());
    }

    @Override
    public @NotNull ConnectPacket getConnectPacket() {
        if (connectPacket == null) {
            connectPacket = new ConnectPacketImpl(connect, connectTimestamp);
        }
        return connectPacket;
    }

    @Override
    public @NotNull AuthConnectInput get() {
        return this;
    }
}
