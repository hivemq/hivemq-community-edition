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
package com.hivemq.extensions.packets.connect;

import com.hivemq.codec.encoder.mqtt5.UnsignedDataTypes;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.extension.sdk.api.packets.publish.ModifiableConnectPacket;
import com.hivemq.extension.sdk.api.packets.publish.ModifiableWillPublish;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.packets.publish.ModifiableWillPublishImpl;
import com.hivemq.extensions.packets.publish.WillPublishPacketImpl;
import com.hivemq.util.Utf8Utils;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Lukas Brandl
 * @author Silvio Giebl
 */
@ThreadSafe
public class ModifiableConnectPacketImpl implements ModifiableConnectPacket {

    private final @NotNull MqttVersion mqttVersion;
    private @NotNull String clientId;
    private boolean cleanStart;
    private long sessionExpiryInterval;
    private int keepAlive;

    private int receiveMaximum;
    private long maximumPacketSize;
    private int topicAliasMaximum;
    private boolean requestProblemInformation;
    private boolean requestResponseInformation;

    private @Nullable String userName;
    private @Nullable ByteBuffer password;
    private @Nullable String authenticationMethod;
    private @Nullable ByteBuffer authenticationData;

    private @Nullable ModifiableWillPublishImpl willPublish;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;

    private final @NotNull FullConfigurationService configurationService;
    private boolean modified = false;

    public ModifiableConnectPacketImpl(
            final @NotNull ConnectPacketImpl packet, final @NotNull FullConfigurationService configurationService) {

        mqttVersion = packet.mqttVersion;
        clientId = packet.clientId;
        cleanStart = packet.cleanStart;
        sessionExpiryInterval = packet.sessionExpiryInterval;
        keepAlive = packet.keepAlive;

        receiveMaximum = packet.receiveMaximum;
        maximumPacketSize = packet.maximumPacketSize;
        topicAliasMaximum = packet.topicAliasMaximum;
        requestProblemInformation = packet.requestProblemInformation;
        requestResponseInformation = packet.requestResponseInformation;

        userName = packet.userName;
        password = packet.password;
        authenticationMethod = packet.authenticationMethod;
        authenticationData = packet.authenticationData;

        willPublish = (packet.willPublish == null) ? null :
                new ModifiableWillPublishImpl(packet.willPublish, configurationService);
        userProperties = new ModifiableUserPropertiesImpl(
                packet.userProperties.asInternalList(), configurationService.securityConfiguration().validateUTF8());

        this.configurationService = configurationService;
    }

    @Override
    public @NotNull MqttVersion getMqttVersion() {
        return mqttVersion;
    }

    @Override
    public @NotNull String getClientId() {
        return clientId;
    }

    @Override
    public void setClientId(final @NotNull String clientId) {
        final int clientIdLength = configurationService.restrictionsConfiguration().maxClientIdLength();
        checkArgument(!Utf8Utils.containsMustNotCharacters(clientId), clientId + " is not a valid client id");
        checkArgument(!Utf8Utils.hasControlOrNonCharacter(clientId), clientId + " is not a valid client id");
        checkArgument(clientId.length() < clientIdLength, "client ID exceeds the maximum client ID length");
        checkArgument(!clientId.isEmpty(), "client ID must not be empty");
        if (this.clientId.equals(clientId)) {
            return;
        }
        this.clientId = clientId;
        modified = true;
    }

    @Override
    public boolean getCleanStart() {
        return cleanStart;
    }

    @Override
    public void setCleanStart(final boolean cleanStart) {
        if (this.cleanStart == cleanStart) {
            return;
        }
        this.cleanStart = cleanStart;
        modified = true;
    }

    @Override
    public long getSessionExpiryInterval() {
        return sessionExpiryInterval;
    }

    @Override
    public void setSessionExpiryInterval(final long sessionExpiryInterval) {
        final long configuredMaximum = configurationService.mqttConfiguration().maxSessionExpiryInterval();
        checkArgument(sessionExpiryInterval >= 0, "Session expiry interval must NOT be less than 0");
        checkArgument(
                sessionExpiryInterval < configuredMaximum,
                "Expiry interval must be less than the configured maximum of" + configuredMaximum);
        if (this.sessionExpiryInterval == sessionExpiryInterval) {
            return;
        }
        this.sessionExpiryInterval = sessionExpiryInterval;
        modified = true;
    }

    @Override
    public int getKeepAlive() {
        return keepAlive;
    }

    @Override
    public void setKeepAlive(final int keepAlive) {
        final int configuredMaximum = configurationService.mqttConfiguration().keepAliveMax();
        checkArgument(keepAlive >= 0, "Keep alive must NOT be less than 0");
        checkArgument(
                keepAlive < configuredMaximum,
                "Keep alive must be less than the configured maximum of " + configuredMaximum);
        if (this.keepAlive == keepAlive) {
            return;
        }
        this.keepAlive = keepAlive;
        modified = true;
    }

    @Override
    public int getReceiveMaximum() {
        return receiveMaximum;
    }

    @Override
    public void setReceiveMaximum(final int receiveMaximum) {
        checkArgument(receiveMaximum > 0, "Receive maximum must be bigger than 0");
        checkArgument(
                receiveMaximum < UnsignedDataTypes.UNSIGNED_SHORT_MAX_VALUE, "Receive maximum must be less than 65535");
        if (this.receiveMaximum == receiveMaximum) {
            return;
        }
        this.receiveMaximum = receiveMaximum;
        modified = true;
    }

    @Override
    public long getMaximumPacketSize() {
        return maximumPacketSize;
    }

    @Override
    public void setMaximumPacketSize(final int maximumPacketSize) {
        final int configuredMaximum = configurationService.mqttConfiguration().maxPacketSize();
        checkArgument(maximumPacketSize > 0, "Maximum packet size must be bigger than 0");
        checkArgument(
                maximumPacketSize < configuredMaximum,
                "Maximum packet must be less than the configured maximum of " + configuredMaximum);
        if (this.maximumPacketSize == maximumPacketSize) {
            return;
        }
        this.maximumPacketSize = maximumPacketSize;
        modified = true;
    }

    @Override
    public int getTopicAliasMaximum() {
        return topicAliasMaximum;
    }

    @Override
    public void setTopicAliasMaximum(final int topicAliasMaximum) {
        checkArgument(topicAliasMaximum >= 0, "Topic alias must NOT be less than 0");
        checkArgument(
                topicAliasMaximum < UnsignedDataTypes.UNSIGNED_SHORT_MAX_VALUE,
                "Maximum packet must be less than 65535");
        if (mqttVersion != MqttVersion.V_5) {
            return;
        }
        if (this.topicAliasMaximum == topicAliasMaximum) {
            return;
        }
        this.topicAliasMaximum = topicAliasMaximum;
        modified = true;
    }

    @Override
    public boolean getRequestProblemInformation() {
        return requestProblemInformation;
    }

    @Override
    public void setRequestProblemInformation(final boolean requestProblemInformation) {
        if (mqttVersion != MqttVersion.V_5) {
            return;
        }
        if (this.requestProblemInformation == requestProblemInformation) {
            return;
        }
        this.requestProblemInformation = requestProblemInformation;
        modified = true;
    }

    @Override
    public boolean getRequestResponseInformation() {
        return requestResponseInformation;
    }

    @Override
    public void setRequestResponseInformation(final boolean requestResponseInformation) {
        if (mqttVersion != MqttVersion.V_5) {
            return;
        }
        if (this.requestResponseInformation == requestResponseInformation) {
            return;
        }
        this.requestResponseInformation = requestResponseInformation;
        modified = true;
    }

    @Override
    public @NotNull Optional<String> getUserName() {
        return Optional.ofNullable(userName);
    }

    @Override
    public void setUserName(final @Nullable String userName) {
        if (Objects.equals(this.userName, userName)) {
            return;
        }
        this.userName = userName;
        modified = true;
    }

    @Override
    public @NotNull Optional<ByteBuffer> getPassword() {
        return (password == null) ? Optional.empty() : Optional.of(password.asReadOnlyBuffer());
    }

    @Override
    public void setPassword(final @Nullable ByteBuffer password) {
        if (Objects.equals(this.password, password)) {
            return;
        }
        this.password = password;
        modified = true;
    }

    @Override
    public @NotNull Optional<String> getAuthenticationMethod() {
        return Optional.ofNullable(authenticationMethod);
    }

    @Override
    public void setAuthenticationMethod(final @Nullable String authenticationMethod) {
        if (authenticationMethod != null) {
            checkArgument(
                    !Utf8Utils.containsMustNotCharacters(authenticationMethod),
                    authenticationMethod + " is not a valid authentication method");
            checkArgument(
                    !Utf8Utils.hasControlOrNonCharacter(authenticationMethod),
                    authenticationMethod + " is not a valid authentication method");
        }
        if (Objects.equals(this.authenticationMethod, authenticationMethod)) {
            return;
        }
        this.authenticationMethod = authenticationMethod;
        modified = true;
    }

    @Override
    public @NotNull Optional<ByteBuffer> getAuthenticationData() {
        return (authenticationData == null) ? Optional.empty() : Optional.of(authenticationData.asReadOnlyBuffer());
    }

    @Override
    public void setAuthenticationData(final @Nullable ByteBuffer authenticationData) {
        if (Objects.equals(this.authenticationData, authenticationData)) {
            return;
        }
        this.authenticationData = authenticationData;
        modified = true;
    }

    @Override
    public @NotNull Optional<WillPublishPacket> getWillPublish() {
        return Optional.ofNullable(willPublish);
    }

    @Override
    public @NotNull Optional<ModifiableWillPublish> getModifiableWillPublish() {
        return Optional.ofNullable(willPublish);
    }

    @Override
    public void setWillPublish(final @Nullable WillPublishPacket willPublish) {
        final ModifiableWillPublishImpl modifiableWillPublish;
        if (willPublish == null) {
            modifiableWillPublish = null;
        } else if (willPublish instanceof WillPublishPacketImpl) {
            modifiableWillPublish =
                    new ModifiableWillPublishImpl((WillPublishPacketImpl) willPublish, configurationService);
        } else if (willPublish instanceof ModifiableWillPublishImpl) {
            modifiableWillPublish = (ModifiableWillPublishImpl) willPublish;
        } else {
            throw new DoNotImplementException(WillPublishPacket.class.getSimpleName());
        }
        if (Objects.equals(this.willPublish, modifiableWillPublish)) {
            return;
        }
        this.willPublish = modifiableWillPublish;
        modified = true;
    }

    @Override
    public @NotNull ModifiableUserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    public boolean isModified() {
        return modified || userProperties.isModified() || ((willPublish != null) && willPublish.isModified());
    }

    public @NotNull ConnectPacketImpl copy() {
        return new ConnectPacketImpl(mqttVersion, clientId, cleanStart, sessionExpiryInterval, keepAlive,
                receiveMaximum, maximumPacketSize, topicAliasMaximum, requestProblemInformation,
                requestResponseInformation, userName, password, authenticationMethod, authenticationData,
                (willPublish == null) ? null : willPublish.copy(), userProperties.copy());
    }

    public @NotNull ModifiableConnectPacketImpl update(final @NotNull ConnectPacketImpl packet) {
        return new ModifiableConnectPacketImpl(packet, configurationService);
    }
}
