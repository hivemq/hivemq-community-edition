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
package com.hivemq.codec.encoder.mqtt5;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.codec.encoder.MqttEncoder;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import io.netty.buffer.ByteBuf;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.codec.encoder.mqtt5.Mqtt5MessageEncoderUtil.*;
import static com.hivemq.mqtt.message.connack.CONNACK.*;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.KEEP_ALIVE_NOT_SET;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET;
import static com.hivemq.mqtt.message.mqtt5.MessageProperties.*;

/**
 * @author Florian Limpöck
 */
public class Mqtt5ConnackEncoder extends Mqtt5MessageWithUserPropertiesEncoder.Mqtt5MessageWithReasonStringEncoder<CONNACK> implements MqttEncoder<CONNACK> {

    private static final int CONNACK_FIXED_HEADER = MessageType.CONNACK.ordinal() << 4;

    public Mqtt5ConnackEncoder(final @NotNull MessageDroppedService messageDroppedService, final @NotNull SecurityConfigurationService securityConfigurationService) {
        super(messageDroppedService, securityConfigurationService);
    }

    @Override
    void encode(@NotNull final CONNACK connack,
                @NotNull final ByteBuf out) {
        checkNotNull(connack, "Connack must not be null.");
        checkNotNull(out, "ByteBuf must not be null.");

        encodeFixedHeader(out, connack.getRemainingLength());
        encodeVariableHeader(connack, out);
    }

    @Override
    int calculateRemainingLengthWithoutProperties(@NotNull final CONNACK connack) {
        checkNotNull(connack, "Connack must not be null.");

        return 2; // ConnectFlags Byte + ReasonCode Byte
    }

    @Override
    int calculatePropertyLength(@NotNull final CONNACK connack) {
        checkNotNull(connack, "Connack must not be null.");

        int propertyLength = 0;

        propertyLength += fixedPropertyLength(connack);

        //omissible
        propertyLength += nullablePropertyEncodedLength(connack.getReasonString());
        propertyLength += connack.getUserProperties().encodedLength();

        return propertyLength;
    }

    private int fixedPropertyLength(@NotNull final CONNACK connack) {
        checkNotNull(connack, "Connack must not be null.");

        int propertyLength = 0;

        propertyLength += intPropertyEncodedLength(connack.getSessionExpiryInterval(), SESSION_EXPIRY_NOT_SET);
        propertyLength += shortPropertyEncodedLength(connack.getReceiveMaximum(), DEFAULT_RECEIVE_MAXIMUM);
        propertyLength += nullablePropertyEncodedLength(connack.getMaximumQoS());
        propertyLength += booleanPropertyEncodedLength(connack.isRetainAvailable(), DEFAULT_RETAIN_AVAILABLE);
        propertyLength += intPropertyEncodedLength(connack.getMaximumPacketSize(), DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT);
        propertyLength += nullablePropertyEncodedLength(connack.getAssignedClientIdentifier());
        propertyLength += shortPropertyEncodedLength(connack.getTopicAliasMaximum(), DEFAULT_TOPIC_ALIAS_MAXIMUM);
        propertyLength += booleanPropertyEncodedLength(connack.isWildcardSubscriptionAvailable(), DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE);
        propertyLength += booleanPropertyEncodedLength(connack.isSubscriptionIdentifierAvailable(), DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE);
        propertyLength += booleanPropertyEncodedLength(connack.isSharedSubscriptionAvailable(), DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE);

        propertyLength += shortPropertyEncodedLength(connack.getServerKeepAlive(), KEEP_ALIVE_NOT_SET);

        propertyLength += nullablePropertyEncodedLength(connack.getResponseInformation());
        propertyLength += nullablePropertyEncodedLength(connack.getServerReference());
        propertyLength += nullablePropertyEncodedLength(connack.getAuthMethod());
        propertyLength += nullablePropertyEncodedLength(connack.getAuthData());

        return propertyLength;
    }

    private void encodeFixedHeader(@NotNull final ByteBuf out, final int remainingLength) {
        checkNotNull(out, "ByteBuf must not be null.");
        out.writeByte(CONNACK_FIXED_HEADER);
        MqttVariableByteInteger.encode(remainingLength, out);
    }

    private void encodeVariableHeader(@NotNull final CONNACK connack,
                                      @NotNull final ByteBuf out) {
        checkNotNull(connack, "Connack must not be null.");
        checkNotNull(out, "ByteBuf must not be null.");

        //Connect Acknowledge Flags
        out.writeByte(connack.isSessionPresent() ? 0x01 : 0x00);

        //Reason Code
        final Mqtt5ConnAckReasonCode reasonCode = connack.getReasonCode();
        out.writeByte(reasonCode.getCode());

        encodeProperties(connack, out);
    }

    private void encodeProperties(@NotNull final CONNACK connack, @NotNull final ByteBuf out) {
        checkNotNull(connack, "Connack must not be null.");
        checkNotNull(out, "ByteBuf must not be null.");

        MqttVariableByteInteger.encode(connack.getPropertyLength(), out);

        encodeFixedProperties(connack, out);
        encodeOmissibleProperties(connack, out);

    }

    private void encodeFixedProperties(@NotNull final CONNACK connack, @NotNull final ByteBuf out) {
        checkNotNull(connack, "Connack must not be null.");
        checkNotNull(out, "ByteBuf must not be null.");

        encodeIntProperty(SESSION_EXPIRY_INTERVAL, connack.getSessionExpiryInterval(), SESSION_EXPIRY_NOT_SET, out);
        encodeShortProperty(RECEIVE_MAXIMUM, connack.getReceiveMaximum(), DEFAULT_RECEIVE_MAXIMUM, out);
        encodeNullableProperty(MAXIMUM_QOS, connack.getMaximumQoS(), out);
        encodeBooleanProperty(RETAIN_AVAILABLE, connack.isRetainAvailable(), DEFAULT_RETAIN_AVAILABLE, out);
        encodeIntProperty(MAXIMUM_PACKET_SIZE, connack.getMaximumPacketSize(), DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT, out);
        encodeNullableProperty(ASSIGNED_CLIENT_IDENTIFIER, connack.getAssignedClientIdentifier(), out);
        encodeShortProperty(TOPIC_ALIAS_MAXIMUM, connack.getTopicAliasMaximum(), DEFAULT_TOPIC_ALIAS_MAXIMUM, out);
        encodeBooleanProperty(WILDCARD_SUBSCRIPTION_AVAILABLE, connack.isWildcardSubscriptionAvailable(), DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE, out);
        encodeBooleanProperty(SUBSCRIPTION_IDENTIFIER_AVAILABLE, connack.isSubscriptionIdentifierAvailable(), DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE, out);
        encodeBooleanProperty(SHARED_SUBSCRIPTION_AVAILABLE, connack.isSharedSubscriptionAvailable(), DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE, out);

        encodeShortProperty(SERVER_KEEP_ALIVE, connack.getServerKeepAlive(), KEEP_ALIVE_NOT_SET, out);

        encodeNullableProperty(RESPONSE_INFORMATION, connack.getResponseInformation(), out);
        encodeNullableProperty(SERVER_REFERENCE, connack.getServerReference(), out);
        encodeNullableProperty(AUTHENTICATION_METHOD, connack.getAuthMethod(), out);
        encodeNullableProperty(AUTHENTICATION_DATA, connack.getAuthData(), out);

    }
}
