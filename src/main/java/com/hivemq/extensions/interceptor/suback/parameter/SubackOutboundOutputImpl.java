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
package com.hivemq.extensions.interceptor.suback.parameter;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.suback.parameter.SubackOutboundOutput;
import com.hivemq.extension.sdk.api.packets.suback.SubackPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.suback.ModifiableSubackPacketImpl;
import com.hivemq.mqtt.message.suback.SUBACK;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class SubackOutboundOutputImpl extends AbstractSimpleAsyncOutput<SubackOutboundOutput>
        implements SubackOutboundOutput, Supplier<SubackOutboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private @NotNull ModifiableSubackPacketImpl subAckPacket;

    public SubackOutboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull SUBACK suback) {

        super(asyncer);
        this.configurationService = configurationService;
        this.subAckPacket = new ModifiableSubackPacketImpl(configurationService, suback);
    }

    @Override
    public @NotNull ModifiableSubackPacketImpl getSubackPacket() {
        return this.subAckPacket;
    }

    @Override
    public @NotNull SubackOutboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull SubackPacket subackPacket) {
        this.subAckPacket = new ModifiableSubackPacketImpl(configurationService, subackPacket);
    }
}
