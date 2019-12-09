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
package com.hivemq.extensions.interceptor.puback;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackOutboundOutput;
import com.hivemq.extension.sdk.api.packets.puback.PubackPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.puback.ModifiablePubackPacketImpl;
import com.hivemq.mqtt.message.puback.PUBACK;

import java.util.function.Supplier;

/**
 * @author Yannick Weber
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class PubackOutboundOutputImpl extends AbstractSimpleAsyncOutput<PubackOutboundOutput>
        implements PubackOutboundOutput, Supplier<PubackOutboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private @NotNull ModifiablePubackPacketImpl pubackPacket;

    public PubackOutboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull PUBACK puback) {

        super(asyncer);
        this.configurationService = configurationService;
        pubackPacket = new ModifiablePubackPacketImpl(configurationService, puback);
    }

    @Override
    public @Immutable @NotNull ModifiablePubackPacketImpl getPubackPacket() {
        return pubackPacket;
    }

    @Override
    public @NotNull PubackOutboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull PubackPacket pubackPacket) {
        this.pubackPacket = new ModifiablePubackPacketImpl(configurationService, pubackPacket);
    }
}
