package com.hivemq.mqtt.message.connack;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;

import java.util.Objects;

import static com.hivemq.mqtt.message.connack.CONNACK.KEEP_ALIVE_NOT_SET;
import static com.hivemq.mqtt.message.connack.CONNACK.SESSION_EXPIRY_NOT_SET;
import static com.hivemq.mqtt.message.connack.Mqtt5CONNACK.*;

public class CONNACKBuilder {

    private @Nullable Mqtt5ConnAckReasonCode reasonCode;
    private @Nullable String reasonString;
    private @Nullable Mqtt5UserProperties userProperties;

    private boolean sessionPresent;

    //Mqtt 5
    private long sessionExpiryInterval = SESSION_EXPIRY_NOT_SET;
    private int serverKeepAlive = KEEP_ALIVE_NOT_SET;
    private @Nullable String assignedClientIdentifier;

    //Auth
    private @Nullable String authMethod;
    private byte @Nullable [] authData;

    //Restrictions from Server
    private int receiveMaximum = DEFAULT_RECEIVE_MAXIMUM;
    private int topicAliasMaximum = DEFAULT_TOPIC_ALIAS_MAXIMUM;
    private int maximumPacketSize = DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT;
    private @Nullable QoS maximumQoS;
    private boolean isRetainAvailable = DEFAULT_RETAIN_AVAILABLE;
    private boolean isWildcardSubscriptionAvailable = DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE;
    private boolean isSubscriptionIdentifierAvailable = DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE;
    private boolean isSharedSubscriptionAvailable = DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE;

    private @Nullable String responseInformation;
    private @Nullable String serverReference;

    CONNACKBuilder() {
    }

    public @NotNull CONNACK build() {
        return new CONNACK(reasonCode,
                reasonString,
                Objects.requireNonNullElse(userProperties, Mqtt5UserProperties.NO_USER_PROPERTIES),
                sessionPresent,
                sessionExpiryInterval,
                serverKeepAlive,
                assignedClientIdentifier,
                authMethod,
                authData,
                receiveMaximum,
                topicAliasMaximum,
                maximumPacketSize,
                maximumQoS,
                isRetainAvailable,
                isWildcardSubscriptionAvailable,
                isSubscriptionIdentifierAvailable,
                isSharedSubscriptionAvailable,
                responseInformation,
                serverReference);
    }

    private @NotNull CONNACKBuilder self() {
        //noinspection ReturnOfThis
        return this;
    }

    public @NotNull CONNACKBuilder withReasonCode(final @Nullable Mqtt5ConnAckReasonCode reasonCode) {
        this.reasonCode = reasonCode;
        return self();
    }

    public @NotNull CONNACKBuilder withReasonString(final @Nullable String reasonString) {
        this.reasonString = reasonString;
        return self();
    }

    public @NotNull CONNACKBuilder withUserProperties(final @Nullable Mqtt5UserProperties userProperties) {
        this.userProperties = userProperties;
        return self();
    }

    public @NotNull CONNACKBuilder withSessionPresent(final boolean sessionPresent) {
        this.sessionPresent = sessionPresent;
        return self();
    }

    public @NotNull CONNACKBuilder withSessionExpiryInterval(final long sessionExpiryInterval) {
        this.sessionExpiryInterval = sessionExpiryInterval;
        return self();
    }

    public @NotNull CONNACKBuilder withServerKeepAlive(final int serverKeepAlive) {
        this.serverKeepAlive = serverKeepAlive;
        return self();
    }

    public @NotNull CONNACKBuilder withAssignedClientIdentifier(final @Nullable String assignedClientIdentifier) {
        this.assignedClientIdentifier = assignedClientIdentifier;
        return self();
    }

    public @NotNull CONNACKBuilder withAuthMethod(final @Nullable String authMethod) {
        this.authMethod = authMethod;
        return self();
    }

    public @NotNull CONNACKBuilder withAuthData(final byte @Nullable [] authData) {
        this.authData = authData;
        return self();
    }

    public @NotNull CONNACKBuilder withReceiveMaximum(final int receiveMaximum) {
        this.receiveMaximum = receiveMaximum;
        return self();
    }

    public @NotNull CONNACKBuilder withTopicAliasMaximum(final int topicAliasMaximum) {
        this.topicAliasMaximum = topicAliasMaximum;
        return self();
    }

    public @NotNull CONNACKBuilder withMaximumPacketSize(final int maximumPacketSize) {
        this.maximumPacketSize = maximumPacketSize;
        return self();
    }

    public @NotNull CONNACKBuilder withMaximumQoS(final @Nullable QoS maximumQoS) {
        this.maximumQoS = maximumQoS;
        return self();
    }

    public @NotNull CONNACKBuilder withRetainAvailable(final boolean retainAvailable) {
        isRetainAvailable = retainAvailable;
        return self();
    }

    public @NotNull CONNACKBuilder withWildcardSubscriptionAvailable(final boolean wildcardSubscriptionAvailable) {
        isWildcardSubscriptionAvailable = wildcardSubscriptionAvailable;
        return self();
    }

    public @NotNull CONNACKBuilder withSubscriptionIdentifierAvailable(final boolean subscriptionIdentifierAvailable) {
        isSubscriptionIdentifierAvailable = subscriptionIdentifierAvailable;
        return self();
    }

    public @NotNull CONNACKBuilder withSharedSubscriptionAvailable(final boolean sharedSubscriptionAvailable) {
        isSharedSubscriptionAvailable = sharedSubscriptionAvailable;
        return self();
    }

    public @NotNull CONNACKBuilder withResponseInformation(final @Nullable String responseInformation) {
        this.responseInformation = responseInformation;
        return self();
    }

    public @NotNull CONNACKBuilder withServerReference(final @Nullable String serverReference) {
        this.serverReference = serverReference;
        return self();
    }
}
