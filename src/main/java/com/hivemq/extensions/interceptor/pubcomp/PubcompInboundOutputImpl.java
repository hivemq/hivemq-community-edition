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
package com.hivemq.extensions.interceptor.pubcomp;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter.PubcompInboundOutput;
import com.hivemq.extension.sdk.api.packets.pubcomp.PubcompPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.pubcomp.ModifiablePubcompPacketImpl;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;

import java.util.function.Supplier;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class PubcompInboundOutputImpl extends AbstractSimpleAsyncOutput<PubcompInboundOutput>
        implements PubcompInboundOutput, Supplier<PubcompInboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private @NotNull ModifiablePubcompPacketImpl pubcompPacket;

    public PubcompInboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull PUBCOMP pubcomp) {

        super(asyncer);
        this.configurationService = configurationService;
        pubcompPacket = new ModifiablePubcompPacketImpl(configurationService, pubcomp);
    }

    @Override
    public @NotNull ModifiablePubcompPacketImpl getPubcompPacket() {
        return pubcompPacket;
    }

    @Override
    public @NotNull PubcompInboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull PubcompPacket pubcompPacket) {
        this.pubcompPacket = new ModifiablePubcompPacketImpl(configurationService, pubcompPacket);
    }
}
