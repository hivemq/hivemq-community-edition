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

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecOutboundInput;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.pubrec.PubrecPacketImpl;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
@Immutable
public class PubrecOutboundInputImpl implements PubrecOutboundInput, PluginTaskInput {

    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;
    private final @NotNull PubrecPacketImpl pubrecPacket;

    public PubrecOutboundInputImpl(
            final @NotNull ClientInformation clientInformation,
            final @NotNull ConnectionInformation connectionInformation,
            final @NotNull PubrecPacketImpl pubrecPacket) {

        this.clientInformation = clientInformation;
        this.connectionInformation = connectionInformation;
        this.pubrecPacket = pubrecPacket;
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
    public @NotNull PubrecPacketImpl getPubrecPacket() {
        return pubrecPacket;
    }

    public @NotNull PubrecOutboundInputImpl update(final @NotNull PubrecOutboundOutputImpl output) {
        return new PubrecOutboundInputImpl(clientInformation, connectionInformation, output.getPubrecPacket().copy());
    }
}
