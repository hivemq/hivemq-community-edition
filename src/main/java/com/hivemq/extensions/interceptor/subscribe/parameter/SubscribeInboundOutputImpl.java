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

package com.hivemq.extensions.interceptor.subscribe.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.extensions.packets.subscribe.ModifiableSubscribePacketImpl;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class SubscribeInboundOutputImpl extends AbstractAsyncOutput<SubscribeInboundOutput> implements SubscribeInboundOutput, PluginTaskOutput, Supplier<SubscribeInboundOutputImpl> {

    private final @NotNull AtomicBoolean preventDelivery;

    private final @NotNull ModifiableSubscribePacketImpl subscribePacket;

    public SubscribeInboundOutputImpl(final @NotNull FullConfigurationService configurationService, final @NotNull PluginOutPutAsyncer asyncer, final @NotNull SUBSCRIBE subscribe) {
        super(asyncer);
        this.subscribePacket = new ModifiableSubscribePacketImpl(configurationService, subscribe);
        this.preventDelivery = new AtomicBoolean(false);
    }

    @Override
    public @NotNull ModifiableSubscribePacketImpl getSubscribePacket() {
        return subscribePacket;
    }

    /**
     * happens for async timeout and fallback behaviour {@link TimeoutFallback#FAILURE}.
     */
    public void forciblyPreventSubscribeDelivery() {
        this.preventDelivery.set(true);
    }

    @Override
    public @NotNull SubscribeInboundOutputImpl get() {
        return this;
    }

    public boolean deliveryPrevented() {
        return preventDelivery.get();
    }
}
