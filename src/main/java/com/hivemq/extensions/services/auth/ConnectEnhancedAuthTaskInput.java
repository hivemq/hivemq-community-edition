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
import com.hivemq.extension.sdk.api.auth.parameter.ConnectEnhancedAuthInput;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.connect.ConnectPacketImpl;
import com.hivemq.mqtt.message.connect.CONNECT;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Daniel Krüger
 * @author Florian Limpöck
*/
public class ConnectEnhancedAuthTaskInput extends AbstractAuthTaskInput<ConnectEnhancedAuthTaskInput> implements PluginTaskInput, ConnectEnhancedAuthInput {

    private final @NotNull CONNECT connect;
    private @Nullable ConnectPacketImpl connectPacket;

    public ConnectEnhancedAuthTaskInput(@NotNull final CONNECT connect,
                                        @NotNull final ChannelHandlerContext ctx) {
        super(connect.getClientIdentifier(), ctx);
        this.connect = connect;
    }

    public @NotNull ConnectPacket getConnectPacket() {
        if (connectPacket == null) {
            connectPacket = new ConnectPacketImpl(connect);
        }
        return connectPacket;
    }

    @NotNull
    @Override
    public ConnectEnhancedAuthTaskInput get() {
        return this;
    }

}
