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
package com.hivemq.extensions.interceptor.unsuback.parameter;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.unsuback.parameter.UnsubackOutboundOutput;
import com.hivemq.extension.sdk.api.packets.unsuback.UnsubackPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.unsuback.ModifiableUnsubackPacketImpl;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class UnsubackOutboundOutputImpl extends AbstractSimpleAsyncOutput<UnsubackOutboundOutput>
        implements UnsubackOutboundOutput, Supplier<UnsubackOutboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private @NotNull ModifiableUnsubackPacketImpl unsubackPacket;

    public UnsubackOutboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull UNSUBACK unsuback) {

        super(asyncer);
        this.configurationService = configurationService;
        this.unsubackPacket = new ModifiableUnsubackPacketImpl(configurationService, unsuback);
    }

    @Override
    public @NotNull ModifiableUnsubackPacketImpl getUnsubackPacket() {
        return this.unsubackPacket;
    }

    @Override
    public @NotNull UnsubackOutboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull UnsubackPacket unsubackPacket) {
        this.unsubackPacket = new ModifiableUnsubackPacketImpl(configurationService, unsubackPacket);
    }
}
