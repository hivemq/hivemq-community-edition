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
package com.hivemq.extensions.packets.connack;

import com.google.common.base.Preconditions;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.packets.connack.ModifiableConnackPacket;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.connect.Mqtt5CONNECT;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Lukas Brandl
 * @author Silvio Giebl
 */
@ThreadSafe
public class ModifiableConnackPacketImpl implements ModifiableConnackPacket {

    private @NotNull ConnackReasonCode reasonCode;
    private final boolean sessionPresent;
    private final long sessionExpiryInterval;
    private final int serverKeepAlive;
    private final @Nullable String assignedClientId;

    private final @Nullable String authenticationMethod;
    private final @Nullable ByteBuffer authenticationData;

    private final int receiveMaximum;
    private final int maximumPacketSize;
    private final int topicAliasMaximum;
    private final @Nullable Qos maximumQos;
    private final boolean retainAvailable;
    private final boolean wildCardSubscriptionAvailable;
    private final boolean sharedSubscriptionsAvailable;
    private final boolean subscriptionIdentifiersAvailable;

    private @Nullable String responseInformation;
    private @Nullable String serverReference;
    private @Nullable String reasonString;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;

    private final @NotNull FullConfigurationService configurationService;
    private final boolean requestResponseInformation;
    private boolean modified = false;

    public ModifiableConnackPacketImpl(
            final @NotNull ConnackPacketImpl packet,
            final @NotNull FullConfigurationService configurationService,
            final boolean requestResponseInformation) {

        reasonCode = packet.reasonCode;
        sessionPresent = packet.sessionPresent;
        sessionExpiryInterval = packet.sessionExpiryInterval;
        serverKeepAlive = packet.serverKeepAlive;
        assignedClientId = packet.assignedClientId;

        authenticationMethod = packet.authenticationMethod;
        authenticationData = packet.authenticationData;

        receiveMaximum = packet.receiveMaximum;
        maximumPacketSize = packet.maximumPacketSize;
        topicAliasMaximum = packet.topicAliasMaximum;
        maximumQos = packet.maximumQos;
        retainAvailable = packet.retainAvailable;
        wildCardSubscriptionAvailable = packet.wildCardSubscriptionAvailable;
        sharedSubscriptionsAvailable = packet.sharedSubscriptionsAvailable;
        subscriptionIdentifiersAvailable = packet.subscriptionIdentifiersAvailable;

        responseInformation = packet.responseInformation;
        serverReference = packet.serverReference;
        reasonString = packet.reasonString;
        userProperties = new ModifiableUserPropertiesImpl(
                packet.userProperties.asInternalList(), configurationService.securityConfiguration().validateUTF8());

        this.configurationService = configurationService;
        this.requestResponseInformation = requestResponseInformation;
    }

    @Override
    public @NotNull ConnackReasonCode getReasonCode() {
        return reasonCode;
    }

    @Override
    public void setReasonCode(final @NotNull ConnackReasonCode reasonCode) {
        Preconditions.checkNotNull(reasonCode, "Reason code must never be null");
        final boolean switched = (reasonCode == ConnackReasonCode.SUCCESS && this.reasonCode != ConnackReasonCode.SUCCESS) ||
                (reasonCode != ConnackReasonCode.SUCCESS && this.reasonCode == ConnackReasonCode.SUCCESS);
        Preconditions.checkState(!switched, "Reason code must not switch from successful to unsuccessful or vice versa");
        if (this.reasonCode == reasonCode) {
            return;
        }
        this.reasonCode = reasonCode;
        modified = true;
    }

    @Override
    public boolean getSessionPresent() {
        return sessionPresent;
    }

    @Override
    public @NotNull Optional<Long> getSessionExpiryInterval() {
        if (sessionExpiryInterval == Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET) {
            return Optional.empty();
        }
        return Optional.of(sessionExpiryInterval);
    }

    @Override
    public @NotNull Optional<Integer> getServerKeepAlive() {
        if (serverKeepAlive == Mqtt5CONNECT.KEEP_ALIVE_NOT_SET) {
            return Optional.empty();
        }
        return Optional.of(serverKeepAlive);
    }

    @Override
    public @NotNull Optional<String> getAssignedClientIdentifier() {
        return Optional.ofNullable(assignedClientId);
    }

    @Override
    public @NotNull Optional<String> getAuthenticationMethod() {
        return Optional.ofNullable(authenticationMethod);
    }

    @Override
    public @NotNull Optional<ByteBuffer> getAuthenticationData() {
        return (authenticationData == null) ? Optional.empty() : Optional.of(authenticationData.asReadOnlyBuffer());
    }

    @Override
    public int getReceiveMaximum() {
        return receiveMaximum;
    }

    @Override
    public int getMaximumPacketSize() {
        return maximumPacketSize;
    }

    @Override
    public int getTopicAliasMaximum() {
        return topicAliasMaximum;
    }

    @Override
    public @NotNull Optional<Qos> getMaximumQoS() {
        return Optional.ofNullable(maximumQos);
    }

    @Override
    public boolean getRetainAvailable() {
        return retainAvailable;
    }

    @Override
    public boolean getWildCardSubscriptionAvailable() {
        return wildCardSubscriptionAvailable;
    }

    @Override
    public boolean getSharedSubscriptionsAvailable() {
        return sharedSubscriptionsAvailable;
    }

    @Override
    public boolean getSubscriptionIdentifiersAvailable() {
        return subscriptionIdentifiersAvailable;
    }

    @Override
    public @NotNull Optional<String> getResponseInformation() {
        return Optional.ofNullable(responseInformation);
    }

    @Override
    public void setResponseInformation(final @Nullable String responseInformation) {
        PluginBuilderUtil.checkResponseInformation(
                responseInformation, requestResponseInformation,
                configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.responseInformation, responseInformation)) {
            return;
        }
        this.responseInformation = responseInformation;
        modified = true;
    }

    @Override
    public @NotNull Optional<String> getServerReference() {
        return Optional.ofNullable(serverReference);
    }

    @Override
    public void setServerReference(final @Nullable String serverReference) {
        PluginBuilderUtil.checkServerReference(
                serverReference, configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.serverReference, serverReference)) {
            return;
        }
        this.serverReference = serverReference;
        modified = true;
    }

    @Override
    public @NotNull Optional<String> getReasonString() {
        return Optional.ofNullable(reasonString);
    }

    @Override
    public void setReasonString(final @Nullable String reasonString) {
        if (reasonString != null) {
            Preconditions.checkState(
                    reasonCode != ConnackReasonCode.SUCCESS,
                    "Reason string must not be set when reason code is successful");
        }
        PluginBuilderUtil.checkReasonString(reasonString, configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.reasonString, reasonString)) {
            return;
        }
        this.reasonString = reasonString;
        modified = true;
    }

    @Override
    public @NotNull ModifiableUserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    public boolean isModified() {
        return modified || userProperties.isModified();
    }

    public @NotNull ConnackPacketImpl copy() {
        return new ConnackPacketImpl(reasonCode, sessionPresent, sessionExpiryInterval, serverKeepAlive,
                assignedClientId, authenticationMethod, authenticationData, receiveMaximum, maximumPacketSize,
                topicAliasMaximum, maximumQos, retainAvailable, wildCardSubscriptionAvailable,
                sharedSubscriptionsAvailable, subscriptionIdentifiersAvailable, responseInformation, serverReference,
                reasonString, userProperties.copy());
    }

    public @NotNull ModifiableConnackPacketImpl update(final @NotNull ConnackPacketImpl packet) {
        return new ModifiableConnackPacketImpl(packet, configurationService, requestResponseInformation);
    }
}
