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

package com.hivemq.extensions.interceptor.connack;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.connack.parameter.ConnackOutboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.extensions.packets.connack.ModifiableConnackPacketImpl;
import com.hivemq.mqtt.message.connack.CONNACK;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class ConnackOutboundOutputImpl extends AbstractAsyncOutput<ConnackOutboundOutput> implements ConnackOutboundOutput, PluginTaskOutput, Supplier<ConnackOutboundOutputImpl> {

    private final @NotNull ModifiableConnackPacketImpl connackPacket;
    private final @NotNull AtomicBoolean connackPrevented = new AtomicBoolean(false);

    public ConnackOutboundOutputImpl(final @NotNull FullConfigurationService configurationService, final @NotNull PluginOutPutAsyncer asyncer, final @NotNull CONNACK connack, final boolean requestResponseInformation) {
        super(asyncer);
        this.connackPacket = new ModifiableConnackPacketImpl(configurationService, connack, requestResponseInformation);
    }

    @Override
    public @NotNull ModifiableConnackPacketImpl getConnackPacket() {
        return connackPacket;
    }

    @Override
    public @NotNull ConnackOutboundOutputImpl get() {
        return this;
    }

    public void forciblyDisconnect() {
        this.connackPrevented.set(true);
    }

    public boolean connackPrevented() {
        return connackPrevented.get();
    }
}
