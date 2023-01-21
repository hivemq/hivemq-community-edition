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
package com.hivemq.extensions.events.client.parameters;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.events.client.parameters.ConnectionStartInput;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extensions.ExtensionInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.connect.ConnectPacketImpl;
import com.hivemq.mqtt.message.connect.CONNECT;
import io.netty.channel.Channel;

import java.util.Objects;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ConnectionStartInputImpl implements ConnectionStartInput, PluginTaskInput, Supplier<ConnectionStartInputImpl> {

    private final @NotNull CONNECT connect;
    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;
    private @Nullable ConnectPacket connectPacket;
    private final long connectTimestamp;

    public ConnectionStartInputImpl(final @NotNull CONNECT connect, final @NotNull Channel channel) {
        Preconditions.checkNotNull(connect, "connect message must never be null");
        Preconditions.checkNotNull(channel, "channel must never be null");
        this.connect = connect;
        this.connectionInformation = ExtensionInformationUtil.getAndSetConnectionInformation(channel);
        this.clientInformation = ExtensionInformationUtil.getAndSetClientInformation(channel, connect.getClientIdentifier());
        this.connectTimestamp = Objects.requireNonNullElse(channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().getConnectReceivedTimestamp(),
                System.currentTimeMillis());
    }

    @Override
    public @NotNull ConnectionInformation getConnectionInformation() {
        return connectionInformation;
    }

    @Override
    public @NotNull ClientInformation getClientInformation() {
        return clientInformation;
    }

    @Override
    public @NotNull ConnectPacket getConnectPacket() {
        if (connectPacket == null) {
            connectPacket = new ConnectPacketImpl(connect, connectTimestamp);
        }
        return connectPacket;
    }

    @Override
    public @NotNull ConnectionStartInputImpl get() {
        return this;
    }
}
