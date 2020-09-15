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
package com.hivemq.extensions.events.client.parameters;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.events.client.parameters.ClientInitiatedDisconnectInput;
import com.hivemq.extension.sdk.api.events.client.parameters.ConnectionLostInput;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extensions.ExtensionInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import io.netty.channel.Channel;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ClientInitiatedDisconnectInputImpl
        implements ClientInitiatedDisconnectInput, ConnectionLostInput, PluginTaskInput,
        Supplier<ClientInitiatedDisconnectInputImpl> {

    private final @Nullable DisconnectedReasonCode reasonCode;
    private final @Nullable String reasonString;
    private final @Nullable UserProperties userProperties;
    private final boolean graceful;
    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;

    public ClientInitiatedDisconnectInputImpl(
            final @NotNull String clientId,
            final @NotNull Channel channel,
            final @Nullable DisconnectedReasonCode reasonCode,
            final @Nullable String reasonString,
            final @Nullable UserProperties userProperties,
            final boolean graceful) {

        Preconditions.checkNotNull(clientId, "client id must never be null");
        Preconditions.checkNotNull(channel, "channel must never be null");
        this.reasonCode = reasonCode;
        this.reasonString = reasonString;
        this.userProperties = userProperties;
        this.graceful = graceful;
        this.connectionInformation = ExtensionInformationUtil.getAndSetConnectionInformation(channel);
        this.clientInformation = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
    }

    @Override
    public @NotNull ConnectionInformation getConnectionInformation() {
        return connectionInformation;
    }

    @Override
    public @NotNull ClientInformation getClientInformation() {
        return clientInformation;
    }

    @Override
    public @NotNull Optional<DisconnectedReasonCode> getReasonCode() {
        return Optional.ofNullable(reasonCode);
    }

    @Override
    public @NotNull Optional<String> getReasonString() {
        return Optional.ofNullable(reasonString);
    }

    @Override
    public @NotNull Optional<UserProperties> getUserProperties() {
        return Optional.ofNullable(userProperties);
    }

    public boolean isGraceful() {
        return graceful;
    }

    @Override
    public @NotNull ClientInitiatedDisconnectInputImpl get() {
        return this;
    }
}
