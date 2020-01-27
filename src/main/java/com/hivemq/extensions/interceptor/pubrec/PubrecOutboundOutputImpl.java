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
package com.hivemq.extensions.interceptor.pubrec;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecOutboundOutput;
import com.hivemq.extension.sdk.api.packets.pubrec.PubrecPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.pubrec.ModifiablePubrecPacketImpl;
import com.hivemq.mqtt.message.pubrec.PUBREC;

import java.util.function.Supplier;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class PubrecOutboundOutputImpl extends AbstractSimpleAsyncOutput<PubrecOutboundOutput>
        implements PubrecOutboundOutput, Supplier<PubrecOutboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private @NotNull ModifiablePubrecPacketImpl pubrecPacket;

    public PubrecOutboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull PUBREC pubrec) {

        super(asyncer);
        this.configurationService = configurationService;
        pubrecPacket = new ModifiablePubrecPacketImpl(configurationService, pubrec);
    }

    @Override
    public @NotNull ModifiablePubrecPacketImpl getPubrecPacket() {
        return pubrecPacket;
    }

    @Override
    public @NotNull PubrecOutboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull PubrecPacket pubrecPacket) {
        this.pubrecPacket = new ModifiablePubrecPacketImpl(configurationService, pubrecPacket);
    }
}
