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
package com.hivemq.extensions.interceptor.pubcomp.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter.PubcompOutboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.pubcomp.ModifiablePubcompPacketImpl;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class PubcompOutboundOutputImpl extends AbstractSimpleAsyncOutput<PubcompOutboundOutput>
        implements PubcompOutboundOutput {

    private final @NotNull ModifiablePubcompPacketImpl pubcompPacket;
    private boolean failed = false;

    public PubcompOutboundOutputImpl(
            final @NotNull PluginOutPutAsyncer asyncer, final @NotNull ModifiablePubcompPacketImpl pubcompPacket) {

        super(asyncer);
        this.pubcompPacket = pubcompPacket;
    }

    @Override
    public @NotNull ModifiablePubcompPacketImpl getPubcompPacket() {
        return pubcompPacket;
    }

    public boolean isFailed() {
        return failed;
    }

    public void markAsFailed() {
        failed = true;
    }

    public @NotNull PubcompOutboundOutputImpl update(final @NotNull PubcompOutboundInputImpl input) {
        return new PubcompOutboundOutputImpl(asyncer, pubcompPacket.update(input.getPubcompPacket()));
    }
}
