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
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectInboundOutput;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.disconnect.ModifiableInboundDisconnectPacketImpl;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class DisconnectInboundOutputImpl extends AbstractSimpleAsyncOutput<DisconnectInboundOutput>
        implements DisconnectInboundOutput, Supplier<DisconnectInboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private final long originalSessionExpiryInterval;
    private @NotNull ModifiableInboundDisconnectPacketImpl disconnectPacket;

    public DisconnectInboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull DISCONNECT disconnect,
            final long originalSessionExpiryInterval) {

        super(asyncer);
        this.configurationService = configurationService;
        this.originalSessionExpiryInterval = originalSessionExpiryInterval;
        disconnectPacket = new ModifiableInboundDisconnectPacketImpl(configurationService, disconnect,
                originalSessionExpiryInterval);
    }

    @Override
    public @NotNull ModifiableInboundDisconnectPacketImpl getDisconnectPacket() {
        return disconnectPacket;
    }

    @Override
    public @NotNull DisconnectInboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull DisconnectPacket disconnectPacket) {
        this.disconnectPacket = new ModifiableInboundDisconnectPacketImpl(configurationService, disconnectPacket,
                originalSessionExpiryInterval);
    }
}
