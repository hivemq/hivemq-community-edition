/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.interceptor.connack.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.connack.parameter.ConnackOutboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.packets.connack.ModifiableConnackPacketImpl;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 * @since 4.2.0
 */
public class ConnackOutboundOutputImpl extends AbstractAsyncOutput<ConnackOutboundOutput>
        implements ConnackOutboundOutput {

    private final @NotNull ModifiableConnackPacketImpl connackPacket;
    private final @NotNull AtomicBoolean prevent = new AtomicBoolean(false);

    public ConnackOutboundOutputImpl(
            final @NotNull PluginOutPutAsyncer asyncer, final @NotNull ModifiableConnackPacketImpl connackPacket) {

        super(asyncer);
        this.connackPacket = connackPacket;
    }

    @Override
    public @NotNull ModifiableConnackPacketImpl getConnackPacket() {
        return connackPacket;
    }

    public void prevent() {
        prevent.set(true);
    }

    public boolean isPrevent() {
        return prevent.get();
    }

    public @NotNull ConnackOutboundOutputImpl update(final @NotNull ConnackOutboundInputImpl input) {
        return new ConnackOutboundOutputImpl(asyncer, connackPacket.update(input.getConnackPacket()));
    }
}
