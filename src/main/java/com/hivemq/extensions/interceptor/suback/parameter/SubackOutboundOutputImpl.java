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
package com.hivemq.extensions.interceptor.suback.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.suback.parameter.SubackOutboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.suback.ModifiableSubackPacketImpl;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class SubackOutboundOutputImpl extends AbstractSimpleAsyncOutput<SubackOutboundOutput>
        implements SubackOutboundOutput {

    private final @NotNull ModifiableSubackPacketImpl subAckPacket;
    private boolean failed;

    public SubackOutboundOutputImpl(
            final @NotNull PluginOutPutAsyncer asyncer, final @NotNull ModifiableSubackPacketImpl subAckPacket) {

        super(asyncer);
        this.subAckPacket = subAckPacket;
    }

    @Override
    public @NotNull ModifiableSubackPacketImpl getSubackPacket() {
        return subAckPacket;
    }

    public boolean isFailed() {
        return failed;
    }

    public void markAsFailed() {
        failed = true;
    }

    public @NotNull SubackOutboundOutputImpl update(final @NotNull SubackOutboundInputImpl input) {
        return new SubackOutboundOutputImpl(asyncer, subAckPacket.update(input.getSubackPacket()));
    }
}
