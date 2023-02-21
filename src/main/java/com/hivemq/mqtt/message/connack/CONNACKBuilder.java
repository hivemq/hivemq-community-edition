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
package com.hivemq.mqtt.message.connack;

import com.hivemq.codec.encoder.mqtt5.UnsignedDataTypes;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.Mqtt5CONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.mqtt.message.connack.CONNACK.KEEP_ALIVE_NOT_SET;
import static com.hivemq.mqtt.message.connack.CONNACK.SESSION_EXPIRY_NOT_SET;
import static com.hivemq.mqtt.message.connack.Mqtt5CONNACK.*;

public class CONNACKBuilder {

    private @Nullable Mqtt5ConnAckReasonCode mqtt5ConnAckReasonCode;
    private @Nullable Mqtt3ConnAckReturnCode mqtt3ConnAckReturnCode;
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

        checkArgument(receiveMaximum != 0, "Receive maximum must never be zero");

        if (assignedClientIdentifier != null) {
            checkArgument(UnsignedDataTypes.isUnsignedShort(assignedClientIdentifier.getBytes(StandardCharsets.UTF_8).length),
                    "A client Id must never exceed 65.535 bytes");
        }
        if (authMethod != null) {
            checkArgument(UnsignedDataTypes.isUnsignedShort(authMethod.getBytes(StandardCharsets.UTF_8).length),
                    "An auth method must never exceed 65.535 bytes");
        }
        if (authData != null) {
            checkNotNull(authMethod, "Auth method must be set if auth data is set");
            checkArgument(UnsignedDataTypes.isUnsignedShort(authData.length),
                    "An auth data must never exceed 65.535 bytes");
        }
        if (responseInformation != null) {
            checkArgument(UnsignedDataTypes.isUnsignedShort(responseInformation.getBytes(StandardCharsets.UTF_8).length),
                    "A response information must never exceed 65.535 bytes");
        }
        if (serverReference != null) {
            checkArgument(UnsignedDataTypes.isUnsignedShort(serverReference.getBytes(StandardCharsets.UTF_8).length),
                    "A server reference must never exceed 65.535 bytes");
        }
        if (sessionExpiryInterval != SESSION_EXPIRY_NOT_SET) {
            checkArgument(UnsignedDataTypes.isUnsignedInt(sessionExpiryInterval),
                    "A session expiry interval must never be larger than 4.294.967.296");
        }

        checkArgument(maximumPacketSize <= Mqtt5CONNECT.DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT,
                "A maximum packet size must never be larger than 268.435.460");

        checkArgument(UnsignedDataTypes.isUnsignedShort(topicAliasMaximum),
                "A topic alias maximum must never be larger than 65.535");

        if (serverKeepAlive != KEEP_ALIVE_NOT_SET) {
            checkArgument(UnsignedDataTypes.isUnsignedShort(serverKeepAlive),
                    "A server keep alive must never be larger than 65.535");
        }

        if (mqtt3ConnAckReturnCode != null) {
            if (mqtt3ConnAckReturnCode != Mqtt3ConnAckReturnCode.ACCEPTED && sessionPresent) {

                throw new IllegalArgumentException("The sessionPresent flag is only allowed for return code " +
                        Mqtt3ConnAckReturnCode.ACCEPTED);
            }
            mqtt5ConnAckReasonCode = Mqtt5ConnAckReasonCode.fromReturnCode(mqtt3ConnAckReturnCode);
        }

        checkNotNull(mqtt5ConnAckReasonCode);

        return new CONNACK(mqtt5ConnAckReasonCode,
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

    public @NotNull CONNACKBuilder withReasonCode(final @Nullable Mqtt5ConnAckReasonCode mqtt5ConnAckReasonCode) {
        this.mqtt5ConnAckReasonCode = mqtt5ConnAckReasonCode;
        return self();
    }

    public @NotNull CONNACKBuilder withMqtt3ReturnCode(final @Nullable Mqtt3ConnAckReturnCode mqtt3ConnAckReturnCode) {
        this.mqtt3ConnAckReturnCode = mqtt3ConnAckReturnCode;
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
