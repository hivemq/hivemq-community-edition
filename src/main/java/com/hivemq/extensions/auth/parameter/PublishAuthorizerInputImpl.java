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
package com.hivemq.extensions.auth.parameter;

import com.google.common.base.Preconditions;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.PublishAuthorizerInput;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extensions.ExtensionInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.publish.PublishPacketImpl;
import com.hivemq.extensions.packets.publish.WillPublishPacketImpl;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.publish.PUBLISH;
import io.netty.channel.Channel;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author Christoph Schäbel
 */
public class PublishAuthorizerInputImpl implements PublishAuthorizerInput, PluginTaskInput, Supplier<PublishAuthorizerInputImpl> {

    private final @NotNull PublishPacket publishPacket;
    private final @NotNull ConnectionInformation connectionInformation;
    private final @NotNull ClientInformation clientInformation;

    public PublishAuthorizerInputImpl(final @NotNull PUBLISH publish, final @NotNull Channel channel, final @NotNull String clientId) {
        Preconditions.checkNotNull(publish, "publish must never be null");
        Preconditions.checkNotNull(channel, "channel must never be null");
        Preconditions.checkNotNull(clientId, "clientId must never be null");

        this.publishPacket = new PublishPacketImpl(publish);
        this.clientInformation = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        this.connectionInformation = ExtensionInformationUtil.getAndSetConnectionInformation(channel);
    }

    public PublishAuthorizerInputImpl(final @NotNull MqttWillPublish publish, final @NotNull Channel channel, final @NotNull String clientId) {
        Preconditions.checkNotNull(publish, "publish must never be null");
        Preconditions.checkNotNull(channel, "channel must never be null");
        Preconditions.checkNotNull(clientId, "clientId must never be null");

        final Long timestamp = Objects.requireNonNullElse(channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().getConnectReceivedTimestamp(),
                System.currentTimeMillis());
        this.publishPacket = new WillPublishPacketImpl(publish, timestamp);
        this.clientInformation = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        this.connectionInformation = ExtensionInformationUtil.getAndSetConnectionInformation(channel);
    }

    @NotNull
    @Override
    public PublishPacket getPublishPacket() {
        return publishPacket;
    }

    @NotNull
    @Override
    public ConnectionInformation getConnectionInformation() {
        return connectionInformation;
    }

    @NotNull
    @Override
    public ClientInformation getClientInformation() {
        return clientInformation;
    }

    @NotNull
    @Override
    public PublishAuthorizerInputImpl get() {
        return this;
    }

}
