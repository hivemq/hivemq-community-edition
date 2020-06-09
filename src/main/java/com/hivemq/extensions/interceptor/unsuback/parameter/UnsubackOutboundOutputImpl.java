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
package com.hivemq.extensions.interceptor.unsuback.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.unsuback.parameter.UnsubackOutboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.unsuback.ModifiableUnsubackPacketImpl;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class UnsubackOutboundOutputImpl extends AbstractSimpleAsyncOutput<UnsubackOutboundOutput>
        implements UnsubackOutboundOutput {

    private final @NotNull ModifiableUnsubackPacketImpl unsubackPacket;
    private boolean failed = false;

    public UnsubackOutboundOutputImpl(
            final @NotNull PluginOutPutAsyncer asyncer, final @NotNull ModifiableUnsubackPacketImpl unsubackPacket) {

        super(asyncer);
        this.unsubackPacket = unsubackPacket;
    }

    @Override
    public @NotNull ModifiableUnsubackPacketImpl getUnsubackPacket() {
        return unsubackPacket;
    }

    public boolean isFailed() {
        return failed;
    }

    public void markAsFailed() {
        failed = true;
    }

    public @NotNull UnsubackOutboundOutputImpl update(final @NotNull UnsubackOutboundInputImpl input) {
        return new UnsubackOutboundOutputImpl(asyncer, unsubackPacket.update(input.getUnsubackPacket()));
    }
}
