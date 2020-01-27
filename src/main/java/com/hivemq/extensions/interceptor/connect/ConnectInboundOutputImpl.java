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
package com.hivemq.extensions.interceptor.connect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.ModifiableConnectPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.extensions.packets.connect.ModifiableConnectPacketImpl;
import com.hivemq.mqtt.message.connect.CONNECT;

import java.util.function.Supplier;

/**
 * @author Lukas Brandl
 * @since 4.2.0
 */
public class ConnectInboundOutputImpl extends AbstractAsyncOutput<ConnectInboundOutput> implements ConnectInboundOutput, PluginTaskOutput, Supplier<ConnectInboundOutputImpl> {

    private final @NotNull ModifiableConnectPacketImpl connectPacket;

    public ConnectInboundOutputImpl(final @NotNull FullConfigurationService configurationService, final @NotNull PluginOutPutAsyncer asyncer, final @NotNull CONNECT connect) {
        super(asyncer);
        this.connectPacket = new ModifiableConnectPacketImpl(configurationService, connect);
    }

    @Override
    public @NotNull ModifiableConnectPacketImpl getConnectPacket() {
        return connectPacket;
    }


    @Override
    public @NotNull ConnectInboundOutputImpl get() {
        return this;
    }

}
