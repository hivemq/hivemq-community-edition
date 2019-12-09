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
package com.hivemq.extensions.interceptor.disconnect;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundOutput;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.disconnect.ModifiableOutboundDisconnectPacketImpl;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class DisconnectOutboundOutputImpl extends AbstractSimpleAsyncOutput<DisconnectOutboundOutput>
        implements DisconnectOutboundOutput, Supplier<DisconnectOutboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private @NotNull ModifiableOutboundDisconnectPacketImpl disconnectPacket;

    public DisconnectOutboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull DISCONNECT disconnect) {

        super(asyncer);
        this.configurationService = configurationService;
        disconnectPacket = new ModifiableOutboundDisconnectPacketImpl(configurationService, disconnect);
    }

    @Override
    public @NotNull ModifiableOutboundDisconnectPacketImpl getDisconnectPacket() {
        return disconnectPacket;
    }

    @Override
    public @NotNull DisconnectOutboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull DisconnectPacket disconnectPacket) {
        this.disconnectPacket = new ModifiableOutboundDisconnectPacketImpl(configurationService, disconnectPacket);
    }
}
