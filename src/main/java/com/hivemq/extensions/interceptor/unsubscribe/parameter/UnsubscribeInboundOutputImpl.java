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
package com.hivemq.extensions.interceptor.unsubscribe.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.packets.unsubscribe.ModifiableUnsubscribePacketImpl;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class UnsubscribeInboundOutputImpl extends AbstractAsyncOutput<UnsubscribeInboundOutput>
        implements UnsubscribeInboundOutput {

    private final @NotNull ModifiableUnsubscribePacketImpl unsubscribePacket;
    private final @NotNull AtomicBoolean preventDelivery = new AtomicBoolean(false);

    public UnsubscribeInboundOutputImpl(
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull ModifiableUnsubscribePacketImpl unsubscribePacket) {

        super(asyncer);
        this.unsubscribePacket = unsubscribePacket;
    }

    @Override
    public @NotNull ModifiableUnsubscribePacketImpl getUnsubscribePacket() {
        return unsubscribePacket;
    }

    public void preventDelivery() {
        preventDelivery.set(true);
    }

    public boolean isPreventDelivery() {
        return preventDelivery.get();
    }

    public @NotNull UnsubscribeInboundOutputImpl update(final @NotNull UnsubscribeInboundInputImpl input) {
        return new UnsubscribeInboundOutputImpl(asyncer, unsubscribePacket.update(input.getUnsubscribePacket()));
    }
}
