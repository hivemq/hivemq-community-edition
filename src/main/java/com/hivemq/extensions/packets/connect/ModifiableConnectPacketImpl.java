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
import com.hivemq.codec.encoder.mqtt5.UnsignedDataTypes;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.extension.sdk.api.packets.publish.ModifiableConnectPacket;
import com.hivemq.extension.sdk.api.packets.publish.ModifiableWillPublish;
import com.hivemq.extensions.packets.general.InternalUserProperties;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.packets.general.MqttVersionUtil;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.util.Utf8Utils;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Lukas Brandl
 */
@ThreadSafe
public class ModifiableConnectPacketImpl implements ModifiableConnectPacket {

    private final @NotNull FullConfigurationService configurationService;
    private final ProtocolVersion protocolVersion;
    private boolean modified = false;

    private @NotNull String clientId;
    private boolean cleanStart;
    private @Nullable ModifiableWillPublishImpl modifiableWillPublish;
    private long sessionExpiryInterval;
    private int keepAlive;
    private int receiveMaximum;
    private long maximumPacketSize;
    private int topicAliasMaximum;
    private boolean requestResponseInformation;
    private boolean requestProblemInformation;
    private @Nullable String authMethod;
    private @Nullable ByteBuffer authData;
    private @NotNull
    final ModifiableUserPropertiesImpl userProperties;
    private @Nullable String userName;
    private @Nullable ByteBuffer password;

    public ModifiableConnectPacketImpl(@NotNull final FullConfigurationService configurationService, @NotNull final CONNECT originalConnect) {
        this.configurationService = configurationService;
        this.protocolVersion = originalConnect.getProtocolVersion();

        this.clientId = originalConnect.getClientIdentifier();
        this.cleanStart = originalConnect.isCleanStart();
        this.sessionExpiryInterval = originalConnect.getSessionExpiryInterval();
        this.keepAlive = originalConnect.getKeepAlive();
        this.receiveMaximum = originalConnect.getReceiveMaximum();
        this.maximumPacketSize = originalConnect.getMaximumPacketSize();
        this.topicAliasMaximum = originalConnect.getTopicAliasMaximum();
        this.requestResponseInformation = originalConnect.isResponseInformationRequested();
        this.requestProblemInformation = originalConnect.isProblemInformationRequested();
        this.authMethod = originalConnect.getAuthMethod();
        this.authData = originalConnect.getAuthData() == null ? null : ByteBuffer.wrap(originalConnect.getAuthData());
        this.userProperties = new ModifiableUserPropertiesImpl(originalConnect.getUserProperties().getPluginUserProperties(),
                configurationService.securityConfiguration().validateUTF8());
        this.userName = originalConnect.getUsername();
        this.password = originalConnect.getPassword() == null ? null : ByteBuffer.wrap(originalConnect.getPassword());
        if (originalConnect.isWill() && originalConnect.getWillPublish() != null) {
            this.modifiableWillPublish = new ModifiableWillPublishImpl(configurationService,
                    WillPublishPacketImpl.fromMqttWillPublish(originalConnect.getWillPublish()));
        }
    }

    public ModifiableConnectPacketImpl(@NotNull final FullConfigurationService configurationService, @NotNull final ConnectPacket originalConnect) {
        this.configurationService = configurationService;
        this.protocolVersion = MqttVersionUtil.toProtocolVersion(originalConnect.getMqttVersion());

        this.clientId = originalConnect.getClientId();
        this.cleanStart = originalConnect.getCleanStart();
        this.sessionExpiryInterval = originalConnect.getSessionExpiryInterval();
        this.keepAlive = originalConnect.getKeepAlive();
        this.receiveMaximum = originalConnect.getReceiveMaximum();
        this.maximumPacketSize = originalConnect.getMaximumPacketSize();
        this.topicAliasMaximum = originalConnect.getTopicAliasMaximum();
        this.requestResponseInformation = originalConnect.getRequestResponseInformation();
        this.requestProblemInformation = originalConnect.getRequestProblemInformation();
        this.authMethod = originalConnect.getAuthenticationMethod().orElse(null);
        this.authData = originalConnect.getAuthenticationData().orElse(null);
        this.userProperties = new ModifiableUserPropertiesImpl((InternalUserProperties) originalConnect.getUserProperties(),
                configurationService.securityConfiguration().validateUTF8());
        this.userName = originalConnect.getUserName().orElse(null);
        this.password = originalConnect.getPassword().orElse(null);
        if (originalConnect.getWillPublish().isPresent()) {
            this.modifiableWillPublish = new ModifiableWillPublishImpl(configurationService, originalConnect.getWillPublish().get());
        }
    }

    @Override
    public synchronized void setClientId(@NotNull final String clientId) {
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
    public synchronized void setCleanStart(final boolean cleanStart) {
        if (this.cleanStart == cleanStart) {
            return;
        }
        this.cleanStart = cleanStart;
        modified = true;
    }

    @Override
    public synchronized void setWillPublish(@Nullable final WillPublishPacket willPublish) {
        if (willPublish == null) {
            modifiableWillPublish = null;
        } else {
            modifiableWillPublish = new ModifiableWillPublishImpl(configurationService, willPublish);
        }
        modified = true;
    }

    @Override
    public synchronized void setSessionExpiryInterval(final long sessionExpiryInterval) {
        final long configuredMaximum = configurationService.mqttConfiguration().maxSessionExpiryInterval();

        checkArgument(sessionExpiryInterval >= 0, "Session expiry interval must NOT be less than 0");
        checkArgument(sessionExpiryInterval < configuredMaximum, "Expiry interval must be less than the configured maximum of" + configuredMaximum);
        if (this.sessionExpiryInterval == sessionExpiryInterval) {
            return;
        }
        this.sessionExpiryInterval = sessionExpiryInterval;
        modified = true;
    }

    @Override
    public synchronized void setKeepAlive(final int keepAlive) {
        final int configuredMaximum = configurationService.mqttConfiguration().keepAliveMax();
        checkArgument(keepAlive >= 0, "Keep alive must NOT be less than 0");
        checkArgument(keepAlive < configuredMaximum, "Keep alive must be less than the configured maximum of " + configuredMaximum);
        if (this.keepAlive == keepAlive) {
            return;
        }
        this.keepAlive = keepAlive;
        modified = true;
    }

    @Override
    public synchronized void setReceiveMaximum(final int receiveMaximum) {
        checkArgument(receiveMaximum > 0, "Receive maximum must be bigger than 0");
        checkArgument(receiveMaximum < UnsignedDataTypes.UNSIGNED_SHORT_MAX_VALUE, "Receive maximum must be less than 65535");
        if (this.receiveMaximum == receiveMaximum) {
            return;
        }
        this.receiveMaximum = receiveMaximum;
        modified = true;
    }

    @Override
    public synchronized void setMaximumPacketSize(final int maximumPacketSize) {
        final int configuredMaximum = configurationService.mqttConfiguration().maxPacketSize();
        checkArgument(maximumPacketSize > 0, "Maximum packet size must be bigger than 0");
        checkArgument(maximumPacketSize < configuredMaximum, "Maximum packet must be less than the configured maximum of " + configuredMaximum);
        if (this.maximumPacketSize == maximumPacketSize) {
            return;
        }
        this.maximumPacketSize = maximumPacketSize;
        modified = true;
    }

    @Override
    public synchronized void setTopicAliasMaximum(final int topicAliasMaximum) {
        checkArgument(topicAliasMaximum >= 0, "Topic alias must NOT be less than 0");
        checkArgument(topicAliasMaximum < UnsignedDataTypes.UNSIGNED_SHORT_MAX_VALUE, "Maximum packet must be less than 65535");
        if (getMqttVersion() != MqttVersion.V_5) {
            return;
        }
        if (this.topicAliasMaximum == topicAliasMaximum) {
            return;
        }
        this.topicAliasMaximum = topicAliasMaximum;
        modified = true;
    }

    @Override
    public synchronized void setRequestResponseInformation(final boolean requestResponseInformation) {
        if (getMqttVersion() != MqttVersion.V_5) {
            return;
        }
        if (this.requestResponseInformation == requestResponseInformation) {
            return;
        }
        this.requestResponseInformation = requestResponseInformation;
        modified = true;
    }

    @Override
    public synchronized void setRequestProblemInformation(final boolean requestProblemInformation) {
        if (getMqttVersion() != MqttVersion.V_5) {
            return;
        }
        if (this.requestProblemInformation == requestProblemInformation) {
            return;
        }
        this.requestProblemInformation = requestProblemInformation;
        modified = true;
    }

    @Override
    public synchronized void setAuthenticationMethod(final @Nullable String authenticationMethod) {
        if (authenticationMethod == null) {
            authMethod = null;
            modified = true;
            return;
        }
        checkArgument(!Utf8Utils.containsMustNotCharacters(authenticationMethod), authenticationMethod + " is not a valid authentication method");
        checkArgument(!Utf8Utils.hasControlOrNonCharacter(authenticationMethod), authenticationMethod + " is not a valid authentication method");

        if (Objects.equals(this.authMethod, authenticationMethod)) {
            return;
        }
        this.authMethod = authenticationMethod;
        modified = true;
    }

    @Override
    public synchronized void setAuthenticationData(final @Nullable ByteBuffer authenticationData) {
        if (authenticationData != null && authenticationData.equals(this.authData)) {
            return;
        }
        if (authenticationData == null && this.authData == null) {
            return;
        }
        this.authData = authenticationData;
        modified = true;
    }

    @Override
    public synchronized void setUserName(final @Nullable String userName) {
        if (Objects.equals(this.userName, userName)) {
            return;
        }
        this.userName = userName;
        modified = true;
    }

    @Override
    public synchronized void setPassword(final @Nullable ByteBuffer password) {
        if (password != null && password.equals(this.password)) {
            return;
        }
        if (password == null && this.password == null) {
            return;
        }
        this.password = password;
        modified = true;
    }

    public boolean isModified() {
        final boolean willModified;
        if (modifiableWillPublish == null) {
            willModified = false;
        } else {
            willModified = modifiableWillPublish.isModified();
        }
        return modified || userProperties.isModified() || willModified;
    }

    @Override
    public @NotNull MqttVersion getMqttVersion() {
        return MqttVersionUtil.toMqttVersion(protocolVersion);
    }

    @Override
    public @NotNull String getClientId() {
        return clientId;
    }

    @Override
    public boolean getCleanStart() {
        return cleanStart;
    }

    @Override
    public @NotNull Optional<ModifiableWillPublish> getModifiableWillPublish() {
        return Optional.ofNullable(modifiableWillPublish);
    }

    @Override
    public @NotNull Optional<WillPublishPacket> getWillPublish() {
        return Optional.ofNullable(modifiableWillPublish);
    }

    @Override
    public long getSessionExpiryInterval() {
        return sessionExpiryInterval;
    }

    @Override
    public int getKeepAlive() {
        return keepAlive;
    }

    @Override
    public int getReceiveMaximum() {
        return receiveMaximum;
    }

    @Override
    public long getMaximumPacketSize() {
        return maximumPacketSize;
    }

    @Override
    public int getTopicAliasMaximum() {
        return topicAliasMaximum;
    }

    @Override
    public boolean getRequestResponseInformation() {
        return requestResponseInformation;
    }

    @Override
    public boolean getRequestProblemInformation() {
        return requestProblemInformation;
    }

    @Override
    public @NotNull Optional<String> getAuthenticationMethod() {
        return Optional.ofNullable(authMethod);
    }

    @Override
    public @NotNull Optional<ByteBuffer> getAuthenticationData() {
        if (authData == null) {
            return Optional.empty();
        }
        return Optional.of(authData.asReadOnlyBuffer());
    }

    @Override
    public @NotNull ModifiableUserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    @Override
    public @NotNull Optional<String> getUserName() {
        return Optional.ofNullable(userName);
    }

    @Override
    public @NotNull Optional<ByteBuffer> getPassword() {
        if (password == null) {
            return Optional.empty();
        }
        return Optional.of(password.asReadOnlyBuffer());
    }
}
