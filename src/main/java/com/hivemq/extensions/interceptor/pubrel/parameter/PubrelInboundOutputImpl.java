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
package com.hivemq.extensions.interceptor.pubrel.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.pubrel.ModifiablePubrelPacketImpl;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class PubrelInboundOutputImpl extends AbstractSimpleAsyncOutput<PubrelInboundOutput>
        implements PubrelInboundOutput {

    private final @NotNull ModifiablePubrelPacketImpl pubrelPacket;
    private boolean failed = false;

    public PubrelInboundOutputImpl(
            final @NotNull PluginOutPutAsyncer asyncer, final @NotNull ModifiablePubrelPacketImpl pubrelPacket) {

        super(asyncer);
        this.pubrelPacket = pubrelPacket;
    }

    @Override
    public @NotNull ModifiablePubrelPacketImpl getPubrelPacket() {
        return pubrelPacket;
    }

    public boolean isFailed() {
        return failed;
    }

    public void markAsFailed() {
        failed = true;
    }

    public @NotNull PubrelInboundOutputImpl update(final @NotNull PubrelInboundInputImpl input) {
        return new PubrelInboundOutputImpl(asyncer, pubrelPacket.update(input.getPubrelPacket()));
    }
}
