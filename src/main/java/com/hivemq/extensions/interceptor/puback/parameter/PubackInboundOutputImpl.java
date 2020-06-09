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
package com.hivemq.extensions.interceptor.puback.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.puback.ModifiablePubackPacketImpl;

/**
 * @author Yannick Weber
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class PubackInboundOutputImpl extends AbstractSimpleAsyncOutput<PubackInboundOutput>
        implements PubackInboundOutput {

    private final @NotNull ModifiablePubackPacketImpl pubackPacket;
    private boolean failed = false;

    public PubackInboundOutputImpl(
            final @NotNull PluginOutPutAsyncer asyncer, final @NotNull ModifiablePubackPacketImpl pubackPacket) {

        super(asyncer);
        this.pubackPacket = pubackPacket;
    }

    @Override
    public @NotNull ModifiablePubackPacketImpl getPubackPacket() {
        return pubackPacket;
    }

    public boolean isFailed() {
        return failed;
    }

    public void markAsFailed() {
        failed = true;
    }

    public @NotNull PubackInboundOutputImpl update(final @NotNull PubackInboundInputImpl input) {
        return new PubackInboundOutputImpl(asyncer, pubackPacket.update(input.getPubackPacket()));
    }
}
