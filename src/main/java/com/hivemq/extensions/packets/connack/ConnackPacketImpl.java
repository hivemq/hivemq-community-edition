package com.hivemq.extensions.packets.connack;

import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.connack.ConnackPacket;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.mqtt.message.connack.CONNACK;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class ConnackPacketImpl implements ConnackPacket {

    private final long sessionExpiryInterval;
    private final int serverKeepAlive;
    private final int receiveMaximum;
    private final int maximumPacketSize;
    private final int topicAliasMaximum;
    private final @Nullable Qos maximumQos;
    private final @Nullable String authenticationMethod;
    private final @Nullable ByteBuffer authenticationData;
    private final @NotNull UserProperties userProperties;
    private final @NotNull ConnackReasonCode connackReasonCode;
    private final boolean sessionPresent;
    private final boolean retainAvailable;
    private final @Nullable String assignedClientId;
    private final @Nullable String reasonString;
    private final boolean wildCardSubscriptionAvailable;
    private final boolean subscriptionIdentifiersAvailable;
    private final boolean sharedSubscriptionsAvailable;
    private final @Nullable String responseInformation;
    private final @Nullable String serverReference;

    public ConnackPacketImpl(@NotNull final CONNACK connack) {
        this.sessionExpiryInterval = connack.getSessionExpiryInterval();
        this.serverKeepAlive = connack.getServerKeepAlive();
        this.receiveMaximum = connack.getReceiveMaximum();
        this.maximumPacketSize = connack.getMaximumPacketSize();
        this.topicAliasMaximum = connack.getTopicAliasMaximum();
        if (connack.getMaximumQoS() == null) {
            maximumQos = null;
        } else {
            maximumQos = Qos.valueOf(connack.getMaximumQoS().getQosNumber());
        }
        this.authenticationMethod = connack.getAuthMethod();
        final byte[] authData = connack.getAuthData();
        if (authData == null) {
            authenticationData = null;
        } else {
            authenticationData = ByteBuffer.wrap(authData);
        }
        this.userProperties = connack.getUserProperties().getPluginUserProperties();
        this.connackReasonCode = ConnackReasonCode.valueOf(connack.getReasonCode().name());
        this.sessionPresent = connack.isSessionPresent();
        this.retainAvailable = connack.isRetainAvailable();
        this.assignedClientId = connack.getAssignedClientIdentifier();
        this.reasonString = connack.getReasonString();
        this.wildCardSubscriptionAvailable = connack.isWildcardSubscriptionAvailable();
        this.subscriptionIdentifiersAvailable = connack.isSubscriptionIdentifierAvailable();
        this.sharedSubscriptionsAvailable = connack.isSharedSubscriptionAvailable();
        this.responseInformation = connack.getResponseInformation();
        this.serverReference = connack.getServerReference();
    }

    public ConnackPacketImpl(@NotNull final ConnackPacket connackPacket) {
        this.sessionExpiryInterval = connackPacket.getSessionExpiryInterval();
        this.serverKeepAlive = connackPacket.getServerKeepAlive();
        this.receiveMaximum = connackPacket.getReceiveMaximum();
        this.maximumPacketSize = connackPacket.getMaximumPacketSize();
        this.topicAliasMaximum = connackPacket.getTopicAliasMaximum();
        this.maximumQos = connackPacket.getMaximumQoS().orElse(null);
        this.authenticationMethod = connackPacket.getAuthenticationMethod().orElse(null);
        this.authenticationData = connackPacket.getAuthenticationData().orElse(null);
        this.userProperties = connackPacket.getUserProperties();
        this.connackReasonCode = connackPacket.getReasonCode();
        this.sessionPresent = connackPacket.getSessionPresent();
        this.retainAvailable = connackPacket.getRetainAvailable();
        this.assignedClientId = connackPacket.getAssignedClientIdentifier().orElse(null);
        this.reasonString = connackPacket.getReasonString().orElse(null);
        this.wildCardSubscriptionAvailable = connackPacket.getWildCardSubscriptionAvailable();
        this.subscriptionIdentifiersAvailable = connackPacket.getSubscriptionIdentifiersAvailable();
        this.sharedSubscriptionsAvailable = connackPacket.getSharedSubscriptionsAvailable();
        this.responseInformation = connackPacket.getResponseInformation().orElse(null);
        this.serverReference = connackPacket.getServerReference().orElse(null);
    }

    @Override
    public long getSessionExpiryInterval() {
        return sessionExpiryInterval;
    }

    @Override
    public int getServerKeepAlive() {
        return serverKeepAlive;
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

    @NotNull
    @Override
    public Optional<String> getAuthenticationMethod() {
        return Optional.ofNullable(authenticationMethod);
    }

    @NotNull
    @Override
    public Optional<ByteBuffer> getAuthenticationData() {
        return Optional.ofNullable(authenticationData);
    }

    @NotNull
    @Override
    public UserProperties getUserProperties() {
        return userProperties;
    }

    @Override
    public @NotNull ConnackReasonCode getReasonCode() {
        return connackReasonCode;
    }

    @Override
    public boolean getSessionPresent() {
        return sessionPresent;
    }

    @Override
    public boolean getRetainAvailable() {
        return retainAvailable;
    }

    @Override
    public @NotNull Optional<String> getAssignedClientIdentifier() {
        return Optional.ofNullable(assignedClientId);
    }

    @Override
    public @NotNull Optional<String> getReasonString() {
        return Optional.ofNullable(reasonString);
    }

    @Override
    public boolean getWildCardSubscriptionAvailable() {
        return wildCardSubscriptionAvailable;
    }

    @Override
    public boolean getSubscriptionIdentifiersAvailable() {
        return subscriptionIdentifiersAvailable;
    }

    @Override
    public boolean getSharedSubscriptionsAvailable() {
        return sharedSubscriptionsAvailable;
    }

    @Override
    public @NotNull Optional<String> getResponseInformation() {
        return Optional.ofNullable(responseInformation);
    }

    @Override
    public @NotNull Optional<String> getServerReference() {
        return Optional.ofNullable(serverReference);
    }

}
