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

package com.hivemq.extensions.packets.connect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extensions.packets.general.MqttVersionUtil;
import com.hivemq.mqtt.message.connect.CONNECT;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * @author Georg Held
 */
public class ConnectPacketImpl implements ConnectPacket {
    @NotNull
    private final CONNECT connect;

    @Nullable
    private final WillPublishPacket willPublishPacket;

    public ConnectPacketImpl(@NotNull final CONNECT connect) {
        this.connect = connect;
        this.willPublishPacket = WillPublishPacketImpl.fromMqttWillPublish(connect.getWillPublish());
    }

    @NotNull
    @Override
    public MqttVersion getMqttVersion() {
        return MqttVersionUtil.toMqttVersion(connect.getProtocolVersion());
    }

    @NotNull
    @Override
    public String getClientId() {
        return connect.getClientIdentifier();
    }

    @Override
    public boolean getCleanStart() {
        return connect.isCleanStart();
    }

    @NotNull
    @Override
    public Optional<WillPublishPacket> getWillPublish() {
        return Optional.ofNullable(willPublishPacket);
    }

    @Override
    public long getSessionExpiryInterval() {
        return connect.getSessionExpiryInterval();
    }

    @Override
    public int getKeepAlive() {
        return connect.getKeepAlive();
    }

    @Override
    public int getReceiveMaximum() {
        return connect.getReceiveMaximum();
    }

    @Override
    public long getMaximumPacketSize() {
        return connect.getMaximumPacketSize();
    }

    @Override
    public int getTopicAliasMaximum() {
        return connect.getTopicAliasMaximum();
    }

    @Override
    public boolean getRequestResponseInformation() {
        return connect.isResponseInformationRequested();
    }

    @Override
    public boolean getRequestProblemInformation() {
        return connect.isProblemInformationRequested();
    }

    @NotNull
    @Override
    public Optional<String> getAuthenticationMethod() {
        return Optional.ofNullable(connect.getAuthMethod());
    }

    @NotNull
    @Override
    public Optional<ByteBuffer> getAuthenticationData() {
        final byte[] authData = connect.getAuthData();
        if (authData == null) {
            return Optional.empty();
        }
        return Optional.of(ByteBuffer.wrap(authData));
    }

    @NotNull
    @Override
    public UserProperties getUserProperties() {
        return connect.getUserProperties().getPluginUserProperties();
    }

    @NotNull
    @Override
    public Optional<String> getUserName() {
        return Optional.ofNullable(connect.getUsername());
    }

    @NotNull
    @Override
    public Optional<ByteBuffer> getPassword() {
        final byte[] password = connect.getPassword();
        if (password == null) {
            return Optional.empty();
        }
        return Optional.of(ByteBuffer.wrap(password));
    }
}
