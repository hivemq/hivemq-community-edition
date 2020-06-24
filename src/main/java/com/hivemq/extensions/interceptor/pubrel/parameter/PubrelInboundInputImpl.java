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

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelInboundInput;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.pubrel.PubrelPacketImpl;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
@Immutable
public class PubrelInboundInputImpl implements PubrelInboundInput, PluginTaskInput {

    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;
    private final @NotNull PubrelPacketImpl pubrelPacket;

    public PubrelInboundInputImpl(
            final @NotNull ClientInformation clientInformation,
            final @NotNull ConnectionInformation connectionInformation,
            final @NotNull PubrelPacketImpl pubrelPacket) {

        this.clientInformation = clientInformation;
        this.connectionInformation = connectionInformation;
        this.pubrelPacket = pubrelPacket;
    }

    @Override
    public @NotNull ClientInformation getClientInformation() {
        return clientInformation;
    }

    @Override
    public @NotNull ConnectionInformation getConnectionInformation() {
        return connectionInformation;
    }

    @Override
    public @NotNull PubrelPacketImpl getPubrelPacket() {
        return pubrelPacket;
    }

    public @NotNull PubrelInboundInputImpl update(final @NotNull PubrelInboundOutputImpl output) {
        return new PubrelInboundInputImpl(clientInformation, connectionInformation, output.getPubrelPacket().copy());
    }
}
