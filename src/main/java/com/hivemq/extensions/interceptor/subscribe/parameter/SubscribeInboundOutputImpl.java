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
package com.hivemq.extensions.interceptor.subscribe.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.packets.subscribe.ModifiableSubscribePacketImpl;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 * @since 4.2.0
 */
public class SubscribeInboundOutputImpl extends AbstractAsyncOutput<SubscribeInboundOutput>
        implements SubscribeInboundOutput {

    private final @NotNull ModifiableSubscribePacketImpl subscribePacket;
    private final @NotNull AtomicBoolean preventDelivery = new AtomicBoolean(false);

    public SubscribeInboundOutputImpl(
            final @NotNull PluginOutPutAsyncer asyncer, final @NotNull ModifiableSubscribePacketImpl subscribePacket) {

        super(asyncer);
        this.subscribePacket = subscribePacket;
    }

    @Override
    public @NotNull ModifiableSubscribePacketImpl getSubscribePacket() {
        return subscribePacket;
    }

    public void forciblyPreventSubscribeDelivery() {
        preventDelivery.set(true);
    }

    public boolean isPreventDelivery() {
        return preventDelivery.get();
    }

    public @NotNull SubscribeInboundOutputImpl update(final @NotNull SubscribeInboundInputImpl output) {
        return new SubscribeInboundOutputImpl(asyncer, subscribePacket.update(output.getSubscribePacket()));
    }
}
