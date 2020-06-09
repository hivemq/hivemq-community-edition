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

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackInboundInput;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.puback.PubackPacketImpl;

/**
 * @author Yannick Weber
 * @author Robin Atherton
 * @author Silvio Giebl
 */
@Immutable
public class PubackInboundInputImpl implements PubackInboundInput, PluginTaskInput {

    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;
    private final @NotNull PubackPacketImpl pubackPacket;

    public PubackInboundInputImpl(
            final @NotNull ClientInformation clientInformation,
            final @NotNull ConnectionInformation connectionInformation,
            final @NotNull PubackPacketImpl pubackPacket) {

        this.clientInformation = clientInformation;
        this.connectionInformation = connectionInformation;
        this.pubackPacket = pubackPacket;
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
    public @NotNull PubackPacketImpl getPubackPacket() {
        return pubackPacket;
    }

    public @NotNull PubackInboundInputImpl update(final @NotNull PubackInboundOutputImpl output) {
        return new PubackInboundInputImpl(clientInformation, connectionInformation, output.getPubackPacket().copy());
    }
}
