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
package com.hivemq.extensions.interceptor.unsubscribe.parameter;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundOutput;
import com.hivemq.extension.sdk.api.packets.unsubscribe.UnsubscribePacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.extensions.packets.unsubscribe.ModifiableUnsubscribePacketImpl;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class UnsubscribeInboundOutputImpl extends AbstractAsyncOutput<UnsubscribeInboundOutput>
        implements UnsubscribeInboundOutput, PluginTaskOutput, Supplier<UnsubscribeInboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private @NotNull ModifiableUnsubscribePacketImpl unsubscribePacket;

    private boolean preventDelivery = false;

    public UnsubscribeInboundOutputImpl(
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull FullConfigurationService configurationService,
            final @NotNull UNSUBSCRIBE unsubscribe) {

        super(asyncer);
        this.configurationService = configurationService;
        this.unsubscribePacket = new ModifiableUnsubscribePacketImpl(configurationService, unsubscribe);
    }

    @Override
    public @NotNull ModifiableUnsubscribePacketImpl getUnsubscribePacket() {
        return unsubscribePacket;
    }

    @Override
    public @NotNull UnsubscribeInboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull UnsubscribePacket unsubscribePacket) {
        this.unsubscribePacket = new ModifiableUnsubscribePacketImpl(configurationService, unsubscribePacket);
    }

    public void preventDelivery() {
        preventDelivery = true;
    }

    public boolean isPreventDelivery() {
        return preventDelivery;
    }
}
