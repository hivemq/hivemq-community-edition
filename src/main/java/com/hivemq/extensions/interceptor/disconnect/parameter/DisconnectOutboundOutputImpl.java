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
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.disconnect.ModifiableOutboundDisconnectPacketImpl;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class DisconnectOutboundOutputImpl extends AbstractSimpleAsyncOutput<DisconnectOutboundOutput>
        implements DisconnectOutboundOutput {

    private final @NotNull ModifiableOutboundDisconnectPacketImpl disconnectPacket;
    private boolean failed = false;

    public DisconnectOutboundOutputImpl(
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull ModifiableOutboundDisconnectPacketImpl disconnectPacket) {

        super(asyncer);
        this.disconnectPacket = disconnectPacket;
    }

    @Override
    public @NotNull ModifiableOutboundDisconnectPacketImpl getDisconnectPacket() {
        return disconnectPacket;
    }

    public boolean isFailed() {
        return failed;
    }

    public void markAsFailed() {
        failed = true;
    }

    public @NotNull DisconnectOutboundOutputImpl update(final @NotNull DisconnectOutboundInputImpl input) {
        return new DisconnectOutboundOutputImpl(asyncer, disconnectPacket.update(input.getDisconnectPacket()));
    }
}
