/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.extensions.interceptor.unsuback.parameter;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.unsuback.parameter.UnsubackOutboundInput;
import com.hivemq.extension.sdk.api.packets.unsuback.UnsubackPacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.unsuback.UnsubackPacketImpl;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class UnsubackOutboundInputImpl
        implements Supplier<UnsubackOutboundInputImpl>, UnsubackOutboundInput, PluginTaskInput {

    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;
    private @NotNull UnsubackPacketImpl unsubackPacket;

    public UnsubackOutboundInputImpl(
            final @NotNull String clientId,
            final @NotNull Channel channel,
            final @NotNull UNSUBACK unsuback) {

        clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
        connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
        unsubackPacket = new UnsubackPacketImpl(unsuback);
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
    public @Immutable @NotNull UnsubackPacket getUnsubackPacket() {
        return unsubackPacket;
    }

    @Override
    public @NotNull UnsubackOutboundInputImpl get() {
        return this;
    }

    public void update(final @NotNull UnsubackPacket unsubackPacket) {
        this.unsubackPacket = new UnsubackPacketImpl(unsubackPacket);
    }
}
