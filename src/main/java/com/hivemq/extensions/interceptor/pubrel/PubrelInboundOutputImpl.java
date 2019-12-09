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
package com.hivemq.extensions.interceptor.pubrel;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelInboundOutput;
import com.hivemq.extension.sdk.api.packets.pubrel.PubrelPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.pubrel.ModifiablePubrelPacketImpl;
import com.hivemq.mqtt.message.pubrel.PUBREL;

import java.util.function.Supplier;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class PubrelInboundOutputImpl extends AbstractSimpleAsyncOutput<PubrelInboundOutput>
        implements PubrelInboundOutput, Supplier<PubrelInboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private @NotNull ModifiablePubrelPacketImpl pubrelPacket;

    public PubrelInboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull PUBREL pubrel) {

        super(asyncer);
        this.configurationService = configurationService;
        pubrelPacket = new ModifiablePubrelPacketImpl(configurationService, pubrel);
    }

    @Override
    public @Immutable @NotNull ModifiablePubrelPacketImpl getPubrelPacket() {
        return pubrelPacket;
    }

    @Override
    public @NotNull PubrelInboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull PubrelPacket pubrelPacket) {
        this.pubrelPacket = new ModifiablePubrelPacketImpl(configurationService, pubrelPacket);
    }
}
