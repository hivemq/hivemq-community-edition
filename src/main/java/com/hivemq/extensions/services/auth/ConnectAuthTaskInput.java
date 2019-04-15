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
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.connect.ConnectPacketImpl;
import com.hivemq.mqtt.message.connect.CONNECT;
import io.netty.channel.ChannelHandlerContext;

import java.util.function.Supplier;

/**
 * @author Georg Held
 */
public class ConnectAuthTaskInput implements Supplier<ConnectAuthTaskInput>, PluginTaskInput, SimpleAuthInput {


    private final @NotNull CONNECT connect;
    private @Nullable ConnectPacket connectPacket;
    private final @NotNull ConnectionInformation connectionInformation;
    private final @NotNull ClientInformation clientInformation;

    public ConnectAuthTaskInput(@NotNull final CONNECT connect,
                                @NotNull final ChannelHandlerContext ctx) {

        this.connect = connect;
        this.connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(ctx.channel());
        this.clientInformation = PluginInformationUtil.getAndSetClientInformation(ctx.channel(), connect.getClientIdentifier());
    }

    @NotNull
    @Override
    public ConnectAuthTaskInput get() {
        return this;
    }

    @NotNull
    @Override
    public ConnectPacket getConnectPacket() {
        if (connectPacket == null) {
            connectPacket = new ConnectPacketImpl(connect);
        }
        return connectPacket;
    }

    @NotNull
    @Override
    public ConnectionInformation getConnectionInformation() {
        return connectionInformation;
    }

    @NotNull
    @Override
    public ClientInformation getClientInformation() {
        return clientInformation;
    }

}
