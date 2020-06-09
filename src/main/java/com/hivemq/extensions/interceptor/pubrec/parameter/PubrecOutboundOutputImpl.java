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
package com.hivemq.extensions.interceptor.pubrec.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecOutboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.pubrec.ModifiablePubrecPacketImpl;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class PubrecOutboundOutputImpl extends AbstractSimpleAsyncOutput<PubrecOutboundOutput>
        implements PubrecOutboundOutput {

    private final @NotNull ModifiablePubrecPacketImpl pubrecPacket;
    private boolean failed = false;

    public PubrecOutboundOutputImpl(
            final @NotNull PluginOutPutAsyncer asyncer, final @NotNull ModifiablePubrecPacketImpl pubrecPacket) {

        super(asyncer);
        this.pubrecPacket = pubrecPacket;
    }

    @Override
    public @NotNull ModifiablePubrecPacketImpl getPubrecPacket() {
        return pubrecPacket;
    }

    public boolean isFailed() {
        return failed;
    }

    public void markAsFailed() {
        failed = true;
    }

    public @NotNull PubrecOutboundOutputImpl update(final @NotNull PubrecOutboundInputImpl input) {
        return new PubrecOutboundOutputImpl(asyncer, pubrecPacket.update(input.getPubrecPacket()));
    }
}
