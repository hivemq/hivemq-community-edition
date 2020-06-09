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
package com.hivemq.extensions.interceptor.disconnect.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.disconnect.ModifiableInboundDisconnectPacketImpl;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class DisconnectInboundOutputImpl extends AbstractSimpleAsyncOutput<DisconnectInboundOutput>
        implements DisconnectInboundOutput {

    private final @NotNull ModifiableInboundDisconnectPacketImpl disconnectPacket;
    private boolean failed = false;

    public DisconnectInboundOutputImpl(
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull ModifiableInboundDisconnectPacketImpl disconnectPacket) {

        super(asyncer);
        this.disconnectPacket = disconnectPacket;
    }

    @Override
    public @NotNull ModifiableInboundDisconnectPacketImpl getDisconnectPacket() {
        return disconnectPacket;
    }

    public boolean isFailed() {
        return failed;
    }

    public void markAsFailed() {
        failed = true;
    }

    public @NotNull DisconnectInboundOutputImpl update(final @NotNull DisconnectInboundInputImpl input) {
        return new DisconnectInboundOutputImpl(asyncer, disconnectPacket.update(input.getDisconnectPacket()));
    }
}
