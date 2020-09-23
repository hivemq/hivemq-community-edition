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
package com.hivemq.codec.decoder.mqtt5;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.codec.decoder.AbstractMqttConnectDecoder;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.codec.encoder.mqtt5.MqttBinaryData;
import com.hivemq.codec.encoder.mqtt5.MqttVariableByteInteger;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.mqtt.message.reason.MqttCommonReasonCode;
import com.hivemq.util.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import static com.hivemq.mqtt.message.connect.CONNECT.*;
import static com.hivemq.mqtt.message.connect.MqttWillPublish.WILL_DELAY_INTERVAL_NOT_SET;
import static com.hivemq.mqtt.message.mqtt5.MessageProperties.*;
import static com.hivemq.mqtt.message.publish.PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET;
import static com.hivemq.util.Bytes.isBitSet;

/**
 * @author Florian Limpöck
 */
@LazySingleton
public class Mqtt5ConnectDecoder extends AbstractMqttConnectDecoder {

    private static final String PROTOCOL_NAME = "MQTT";
    private static final byte VARIABLE_HEADER_LENGTH = 10;
    private final @NotNull HivemqId hiveMQId;

    public Mqtt5ConnectDecoder(final @NotNull MqttConnacker mqttConnacker,
                               final @NotNull HivemqId hiveMQId,
                               final @NotNull ClientIds clientIds,
                               final @NotNull FullConfigurationService fullMqttConfigurationService) {
        super(mqttConnacker, fullMqttConfigurationService, clientIds);
        this.hiveMQId = hiveMQId;
    }

    @Override
    public CONNECT decode(@NotNull final Channel channel, @NotNull final ByteBuf buf, final byte header) {
        Preconditions.checkNotNull(channel, "A channel must never be null");
        Preconditions.checkNotNull(buf, "A byte buffer must never be null");

        if (!validateHeader(header)) {
            disconnectByInvalidFixedHeader(channel);
            return null;
        }

        final ByteBuf fixedVariableHeader = decodeFixedVariableHeaderConnect(channel, buf);
        if (fixedVariableHeader == null) {
            return null;
        }

        //bytes 1-6
        if (!validateProtocolName(fixedVariableHeader, channel, PROTOCOL_NAME)) {
            return null;
        }

        //byte 7
        //We don't need to validate the protocol version byte since we already know it's valid, otherwise
        //we wouldn't be in this protocol-version dependant decoder
        //we skip this byte
        fixedVariableHeader.skipBytes(1);

        //byte 8
        final byte connectFlagsByte = fixedVariableHeader.readByte();

        //flag bit 0
        if (!validateConnectFlagByte(connectFlagsByte, channel)) {
            return null;
        }

        return decodeConnect(channel, buf, connectFlagsByte, fixedVariableHeader);

    }

    private CONNECT decodeConnect(final @NotNull Channel channel, final @NotNull ByteBuf buf, final byte connectFlagsByte, final @NotNull ByteBuf fixedVariableHeader) {


        final boolean cleanStart = isBitSet(connectFlagsByte, 1);
        final boolean will = isBitSet(connectFlagsByte, 2);
        final int willQos = (connectFlagsByte & 0b0001_1000) >> 3; // flag bit 3 & 4
        final boolean willRetain = isBitSet(connectFlagsByte, 5);
        final boolean passwordRequired = isBitSet(connectFlagsByte, 6);
        final boolean usernameRequired = isBitSet(connectFlagsByte, 7);

        if (!validateWill(will, willRetain, willQos, channel)) {
            return null;
        }

        //bytes 9 & 10
        final int keepAlive = fixedVariableHeader.readUnsignedShort();

        final Mqtt5Builder connectBuilder = new Mqtt5Builder();

        if (!readConnectProperties(channel, buf, connectBuilder)) {
            return null;
        }

        String clientId = MqttBinaryData.decodeString(buf, validateUTF8);
        if (clientId == null) {
            mqttConnacker.connackError(channel,
                    "The client id of the client (IP: {}) is not well formed. This is not allowed.",
                    "Sent CONNECT with malformed client id",
                    Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                    ReasonStrings.CONNACK_CLIENT_IDENTIFIER_NOT_VALID);
            return null;
        }

        if (clientId.isEmpty() && allowAssignedClientId) {
            clientId = clientIds.generateNext();
            channel.attr(ChannelAttributes.CLIENT_ID_ASSIGNED).set(true);
        } else if (clientId.isEmpty()) {
            mqttConnacker.connackError(channel,
                    "The client id of the client (IP: {}) is empty. This is not allowed.",
                    "Sent CONNECT with empty client id",
                    Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID,
                    ReasonStrings.CONNACK_CLIENT_IDENTIFIER_EMPTY);
            return null;
        } else {
            channel.attr(ChannelAttributes.CLIENT_ID_ASSIGNED).set(false);
        }

        channel.attr(ChannelAttributes.CLIENT_ID).set(clientId);

        final MqttWillPublish mqttWillPublish;
        if (will) {
            mqttWillPublish = decodeAndValidateWill(channel, buf, willQos, willRetain);
            if (mqttWillPublish == null) {
                return null;
            }
        } else {
            mqttWillPublish = null;
        }

        if (!decodeAndValidateUsername(channel, buf, connectBuilder, usernameRequired)) {
            return null;
        }

        if (!decodeAndValidatePassword(channel, buf, connectBuilder, passwordRequired)) {
            return null;
        }

        channel.attr(ChannelAttributes.CLEAN_START).set(cleanStart);

        return connectBuilder
                .withClientIdentifier(clientId)
                .withCleanStart(cleanStart)
                .withKeepAlive(keepAlive)
                .withWillPublish(mqttWillPublish)
                .build();
    }

    private ImmutableList.Builder<MqttUserProperty> readUserProperty(final @NotNull Channel channel, final @NotNull ByteBuf buf, ImmutableList.@Nullable Builder<MqttUserProperty> userPropertiesBuilder) {
        final MqttUserProperty userProperty = MqttUserProperty.decode(buf, validateUTF8);
        if (userProperty == null) {
            mqttConnacker.connackError(channel,
                    "A client (IP: {}) sent a CONNECT with a malformed user property. This is not allowed.",
                    "Sent a CONNECT with a malformed user property",
                    Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                    ReasonStrings.CONNACK_MALFORMED_PACKET_USER_PROPERTY);
            return null;
        }
        if (userPropertiesBuilder == null) {
            userPropertiesBuilder = ImmutableList.builder();
        }
        userPropertiesBuilder.add(userProperty);
        return userPropertiesBuilder;
    }


    private boolean decodeAndValidateUsername(final @NotNull Channel channel, final @NotNull ByteBuf buf, final @NotNull Mqtt5Builder connectBuilder, final boolean usernameRequired) {
        if (usernameRequired) {
            final String username = MqttBinaryData.decodeString(buf, validateUTF8);
            if (username == null) {
                mqttConnacker.connackError(channel,
                        "A client (IP: {}) sent a CONNECT with a malformed UTF-8 string for username. This is not allowed.",
                        "Sent a CONNECT with a malformed UTF-8 string for username",
                        Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                        ReasonStrings.CONNACK_MALFORMED_PACKET_USERNAME);

                return false;
            }
            channel.attr(ChannelAttributes.AUTH_USERNAME).set(username);
            connectBuilder.withUsername(username);
        }
        return true;
    }

    private boolean decodeAndValidatePassword(final @NotNull Channel channel, final @NotNull ByteBuf buf, final @NotNull Mqtt5Builder connectBuilder, final boolean passwordRequired) {
        if (passwordRequired) {
            final byte[] password = MqttBinaryData.decode(buf);
            if (password == null) {
                mqttConnacker.connackError(channel,
                        "A client (IP: {}) sent a CONNECT with malformed password. This is not allowed.",
                        "Sent a CONNECT with malformed password",
                        Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                        ReasonStrings.CONNACK_MALFORMED_PACKET_PASSWORD);
                return false;
            }
            channel.attr(ChannelAttributes.AUTH_PASSWORD).set(password);
            connectBuilder.withPassword(password);
        }
        return true;
    }

    private boolean readConnectProperties(final @NotNull Channel channel, final @NotNull ByteBuf buf, final @NotNull Mqtt5Builder connectBuilder) {
        final int propertiesLength = MqttVariableByteInteger.decode(buf);

        if (propertiesLengthInvalid(channel, buf, propertiesLength)) {
            return false;
        }

        long sessionExpiryInterval = SESSION_EXPIRY_NOT_SET;
        int receiveMaximum = RECEIVE_MAXIMUM_NOT_SET;
        long maximumPacketSize = MAXIMUM_PACKET_SIZE_NOT_SET;
        int topicAliasMaximum = TOPIC_ALIAS_MAXIMUM_NOT_SET;
        Boolean requestResponseInformation = null;
        Boolean requestProblemInformation = null;
        ImmutableList.Builder<MqttUserProperty> userPropertiesBuilder = null;
        String authMethod = null;
        byte[] authData = null;

        final int propertiesStartIndex = buf.readerIndex();
        int readPropertyLength;
        while ((readPropertyLength = buf.readerIndex() - propertiesStartIndex) < propertiesLength) {

            final int propertyIdentifier = buf.readByte();

            switch (propertyIdentifier) {
                case SESSION_EXPIRY_INTERVAL:
                    sessionExpiryInterval = decodeSessionExpiryInterval(channel, buf, sessionExpiryInterval);
                    if (sessionExpiryInterval == DISCONNECTED) {
                        return false;
                    }
                    break;

                case RECEIVE_MAXIMUM:
                    receiveMaximum = readReceiveMaximum(channel, buf, receiveMaximum);
                    if (receiveMaximum == DISCONNECTED) {
                        return false;
                    }
                    break;

                case MAXIMUM_PACKET_SIZE:
                    maximumPacketSize = readMaximumPacketSize(channel, buf, maximumPacketSize);
                    if (maximumPacketSize == DISCONNECTED) {
                        return false;
                    }
                    break;

                case TOPIC_ALIAS_MAXIMUM:
                    topicAliasMaximum = readTopicAliasMaximum(channel, buf, topicAliasMaximum);
                    if (topicAliasMaximum == DISCONNECTED) {
                        return false;
                    }
                    break;

                case REQUEST_RESPONSE_INFORMATION:
                    requestResponseInformation = readBoolean(channel, buf, requestResponseInformation, "request response information");
                    if (requestResponseInformation == null) {
                        return false;
                    }
                    break;

                case REQUEST_PROBLEM_INFORMATION:
                    requestProblemInformation = readBoolean(channel, buf, requestProblemInformation, "request problem information");
                    if (requestProblemInformation == null) {
                        return false;
                    }
                    break;

                case USER_PROPERTY:
                    userPropertiesBuilder = readUserProperty(channel, buf, userPropertiesBuilder);
                    if (userPropertiesBuilder == null) {
                        return false;
                    }
                    break;

                case AUTHENTICATION_METHOD:
                    authMethod = readAuthMethod(channel, buf, authMethod);
                    if (authMethod == null) {
                        return false;
                    }
                    break;

                case AUTHENTICATION_DATA:
                    authData = readAuthData(channel, buf, authData);
                    if (authData == null) {
                        return false;
                    }
                    break;

                default:
                    connackByInvalidPropertyIdentifier(channel, propertyIdentifier);
                    return false;
            }
        }

        if (readPropertyLength != propertiesLength) {
            connackByMalformedPropertyLength(channel);
            return false;
        }

        if (authMethod == null && authData != null) {
            mqttConnacker.connackError(channel,
                    "A client (IP: {}) sent a CONNECT with a auth data but without auth method. This is not allowed.",
                    "Sent a CONNECT with a auth data but without auth method",
                    Mqtt5ConnAckReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.CONNACK_PROTOCOL_ERROR_NO_AUTH);
            return false;
        }

        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.build(userPropertiesBuilder);
        if (invalidUserPropertiesLength(channel, userProperties)) {
            return false;
        }

        channel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).set(sessionExpiryInterval);
        connectBuilder.withAuthMethod(authMethod)
                .withAuthData(authData)
                .withSessionExpiryInterval(sessionExpiryInterval)
                .withReceiveMaximum(receiveMaximum)
                .withMaximumPacketSize(maximumPacketSize)
                .withTopicAliasMaximum(topicAliasMaximum)
                .withResponseInformationRequested(
                        requestResponseInformation == null ? DEFAULT_RESPONSE_INFORMATION_REQUESTED :
                                requestResponseInformation)
                .withProblemInformationRequested(
                        requestProblemInformation == null ? DEFAULT_PROBLEM_INFORMATION_REQUESTED :
                                requestProblemInformation)
                .withUserProperties(userProperties);

        return true;

    }

    private int readReceiveMaximum(final @NotNull Channel channel, final @NotNull ByteBuf buf, int receiveMaximum) {
        if (receiveMaximum != Integer.MAX_VALUE) {
            connackByMoreThanOnce(channel, "receive maximum");
            return DISCONNECTED;
        }
        if (buf.readableBytes() < 2) {
            connackByRemainingLengthToShort(channel);
            return DISCONNECTED;
        }
        receiveMaximum = buf.readUnsignedShort();
        if (receiveMaximum == 0) {
            mqttConnacker.connackError(channel,
                    "A client (IP: {}) sent a CONNECT with receive maximum = '0'. This is not allowed.",
                    "Sent a CONNECT with receive maximum = '0'",
                    Mqtt5ConnAckReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.CONNACK_PROTOCOL_RECEIVE_MAXIMUM);
            return DISCONNECTED;
        }
        return receiveMaximum;
    }

    private long readMaximumPacketSize(final @NotNull Channel channel, final @NotNull ByteBuf buf, long maximumPacketSize) {
        if (maximumPacketSize != Long.MAX_VALUE) {
            connackByMoreThanOnce(channel, "maximum packet size");
            return DISCONNECTED;
        }
        if (buf.readableBytes() < 4) {
            connackByRemainingLengthToShort(channel);
            return DISCONNECTED;
        }
        maximumPacketSize = buf.readUnsignedInt();
        if (maximumPacketSize == 0) {
            mqttConnacker.connackError(channel,
                    "A client (IP: {}) sent a CONNECT with maximum packet size = '0'. This is not allowed.",
                    "Sent a CONNECT with maximum packet size = '0'",
                    Mqtt5ConnAckReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.CONNACK_PROTOCOL_PACKET_SIZE);
            return DISCONNECTED;
        }
        return maximumPacketSize;
    }

    private int readTopicAliasMaximum(final @NotNull Channel channel, final @NotNull ByteBuf buf, final int topicAliasMaximum) {
        if (topicAliasMaximum != Integer.MAX_VALUE) {
            connackByMoreThanOnce(channel, "topic alias maximum");
            return DISCONNECTED;
        }
        if (buf.readableBytes() < 2) {
            connackByRemainingLengthToShort(channel);
            return DISCONNECTED;
        }
        return buf.readUnsignedShort();
    }

    private Boolean readBoolean(final @NotNull Channel channel, final @NotNull ByteBuf buf, final @Nullable Boolean current, final @NotNull String key) {
        if (current != null) {
            connackByMoreThanOnce(channel, key);
            return null;
        }
        if (buf.readableBytes() < 1) {
            connackByRemainingLengthToShort(channel);
            return null;
        }
        final byte read = buf.readByte();
        if (read == 0) {
            return false;
        }
        if (read == 1) {
            return true;
        }
        mqttConnacker.connackError(channel,
                "A client (IP: {}) sent a CONNECT with a malformed boolean for " + key + ". This is not allowed.",
                "Sent a CONNECT with a malformed boolean for " + key,
                Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                ReasonStrings.CONNACK_MALFORMED_BOOLEAN);
        return null;
    }

    @Nullable
    private String readAuthMethod(final @NotNull Channel channel, final @NotNull ByteBuf buf, @Nullable String authMethod) {
        if (authMethod != null) {
            connackByMoreThanOnce(channel, "auth method");
            return null;
        }
        authMethod = MqttBinaryData.decodeString(buf, validateUTF8);
        if (authMethod == null) {
            mqttConnacker.connackError(channel,
                    "A client (IP: {}) sent a CONNECT with a malformed UTF-8 string for auth method. This is not allowed.",
                    "Sent a CONNECT with a malformed UTF-8 string for auth method",
                    Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                    ReasonStrings.CONNACK_MALFORMED_AUTH_METHOD);
            return null;
        }
        return authMethod;
    }

    private byte @Nullable [] readAuthData(final @NotNull Channel channel, final @NotNull ByteBuf buf, byte @Nullable [] authData) {
        if (authData != null) {
            connackByMoreThanOnce(channel, "auth data");
            return null;
        }
        authData = MqttBinaryData.decode(buf);
        if (authData == null) {
            mqttConnacker.connackError(channel,
                    "A client (IP: {}) sent a CONNECT with a malformed auth data. This is not allowed.",
                    "Sent a CONNECT with a malformed auth data",
                    Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                    ReasonStrings.CONNACK_MALFORMED_AUTH_DATA);
            return null;
        }
        return authData;
    }

    private MqttWillPublish decodeAndValidateWill(final @NotNull Channel channel, final @NotNull ByteBuf buf, final int willQos, final boolean willRetain) {

        final MqttWillPublish.Mqtt5Builder mqtt5Builder = new MqttWillPublish.Mqtt5Builder();

        mqtt5Builder.withHivemqId(hiveMQId.get()).withQos(QoS.valueOf(willQos)).withRetain(willRetain);

        final int willPropertiesLength = MqttVariableByteInteger.decode(buf);
        if (propertiesLengthInvalid(channel, buf, willPropertiesLength)) {
            return null;
        }

        long willDelayInterval = WILL_DELAY_INTERVAL_NOT_SET;
        long messageExpiryInterval = MESSAGE_EXPIRY_INTERVAL_NOT_SET;
        Mqtt5PayloadFormatIndicator payloadFormatIndicator = null;
        String contentType = null;
        String responseTopic = null;
        byte[] correlationData = null;
        ImmutableList.Builder<MqttUserProperty> willUserPropertiesBuilder = null;

        final int willPropertiesStartIndex = buf.readerIndex();
        int willReadPropertyLength;
        while ((willReadPropertyLength = buf.readerIndex() - willPropertiesStartIndex) < willPropertiesLength) {

            final int propertyIdentifier = buf.readByte();

            switch (propertyIdentifier) {
                case WILL_DELAY_INTERVAL:
                    if (willDelayIntervalInvalid(channel, buf, willDelayInterval)) {
                        return null;
                    }
                    willDelayInterval = buf.readUnsignedInt();
                    break;

                case PAYLOAD_FORMAT_INDICATOR:
                    payloadFormatIndicator = readPayloadFormatIndicator(channel, buf, payloadFormatIndicator);
                    if (payloadFormatIndicator == null) {
                        return null;
                    }
                    mqtt5Builder.withPayloadFormatIndicator(payloadFormatIndicator);
                    break;

                case MESSAGE_EXPIRY_INTERVAL:
                    if (messageExpiryIntervalInvalid(channel, buf, messageExpiryInterval)) {
                        return null;
                    }
                    messageExpiryInterval = buf.readUnsignedInt();
                    break;

                case CONTENT_TYPE:
                    contentType = readContentType(channel, buf, contentType);
                    if (contentType == null) {
                        return null;
                    }
                    mqtt5Builder.withContentType(contentType);
                    break;

                case RESPONSE_TOPIC:
                    responseTopic = readResponseTopic(channel, buf, responseTopic);
                    if (responseTopic == null) {
                        return null;
                    }
                    mqtt5Builder.withResponseTopic(responseTopic);
                    break;

                case CORRELATION_DATA:
                    correlationData = readCorrelationData(channel, buf, correlationData);
                    if (correlationData == null) {
                        return null;
                    }
                    mqtt5Builder.withCorrelationData(correlationData);
                    break;

                case USER_PROPERTY:
                    willUserPropertiesBuilder = readUserProperty(channel, buf, willUserPropertiesBuilder);
                    if (willUserPropertiesBuilder == null) {
                        return null;
                    }
                    break;

                default:
                    connackByInvalidPropertyIdentifier(channel, propertyIdentifier);
                    return null;
            }
        }

        if (willReadPropertyLength != willPropertiesLength) {
            connackByMalformedPropertyLength(channel);
            return null;
        }

        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.build(willUserPropertiesBuilder);
        if (invalidUserPropertiesLength(channel, userProperties)) {
            return null;
        }

        if (!readAndValidateTopic(channel, buf, mqtt5Builder)) {
            return null;
        }

        if (!readAndValidatePayload(channel, buf, mqtt5Builder)) {
            return null;
        }

        mqtt5Builder.withUserProperties(userProperties);
        mqtt5Builder.withDelayInterval(willDelayInterval);
        mqtt5Builder.withMessageExpiryInterval(messageExpiryInterval);

        return mqtt5Builder.build();

    }

    private boolean willDelayIntervalInvalid(final @NotNull Channel channel, final @NotNull ByteBuf buf, final long willDelayInterval) {
        if (willDelayInterval != WILL_DELAY_INTERVAL_NOT_SET) {
            connackByMoreThanOnce(channel, "will delay interval");
            return true;
        }
        if (buf.readableBytes() < 4) {
            connackByRemainingLengthToShort(channel);
            return true;
        }
        return false;
    }

    private boolean readAndValidatePayload(final @NotNull Channel channel, final @NotNull ByteBuf buf, final MqttWillPublish.@NotNull Mqtt5Builder mqtt5Builder) {
        final byte[] payload = MqttBinaryData.decode(buf);
        if (payload == null) {
            mqttConnacker.connackError(channel,
                    "A client (IP: {}) sent a CONNECT with malformed will payload. This is not allowed.",
                    "Sent a CONNECT with malformed will payload",
                    Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                    ReasonStrings.CONNACK_MALFORMED_WILL_PAYLOAD);
            return false;
        }
        mqtt5Builder.withPayload(payload);
        return true;
    }

    /**
     * decodes and validates a topic UTF-8 String length
     * <p>
     * Results in {@link Mqtt5ConnAckReasonCode#TOPIC_NAME_INVALID} with CONNACK by:
     * <p>
     * - readable bytes less than 2 - readable bytes less than indicated string length
     *
     * @param channel the channel of the mqtt client
     * @param buf     the encoded ByteBuf of the message
     * @return the length of the string or -1 for malformed packet
     */
    protected int decodeUTF8StringLength(final @NotNull Channel channel, final ByteBuf buf) {

        final int utf8StringLength;

        if (buf.readableBytes() < 2 || (buf.readableBytes() < (utf8StringLength = buf.readUnsignedShort()))) {
            mqttConnacker.connackError(channel,
                    "A client (IP: {}) sent a CONNECT with an incorrect UTF-8 string length for 'will topic'.",
                    "Incorrect CONNECT UTF-8 string length for 'will topic'",
                    Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                    ReasonStrings.CONNACK_TOPIC_NAME_INVALID_WILL_LENGTH);

            return DISCONNECTED;
        }

        return utf8StringLength;
    }


    /**
     * Decodes a topic without knowing the length of it
     *
     * @param channel the channel of the mqtt client
     * @param buf     the encoded ByteBuf of the message
     * @return the topic as String or {@code null} if failed
     */
    @Nullable
    protected String decodeUTF8Topic(final @NotNull Channel channel, final @NotNull ByteBuf buf) {

        final int utf8StringLength = decodeUTF8StringLength(channel, buf);
        if (utf8StringLength == DISCONNECTED) {
            return null;
        }

        return decodeUTF8Topic(channel, buf, utf8StringLength);
    }

    /**
     * Decodes and validates a topic with a given length
     * <p>
     * Results in {@link Mqtt5ConnAckReasonCode#TOPIC_NAME_INVALID} with CONNACK by:
     * <p>
     * - topic not UTF-8 well formed
     * <p>
     * - readable bytes less than 2
     * <p>
     * - readable bytes less than indicated string length
     *
     * @param channel          the channel of the mqtt client
     * @param buf              the encoded ByteBuf of the message
     * @param utf8StringLength length of the topic
     * @return the topic as String or {@code null} if failed
     */
    @Nullable
    protected String decodeUTF8Topic(final @NotNull Channel channel, final @NotNull ByteBuf buf, final int utf8StringLength) {

        final String utf8String = Strings.getValidatedPrefixedString(buf, utf8StringLength, validateUTF8);
        if (utf8String == null) {
            mqttConnacker.connackError(
                    channel,
                    "A client (IP: {}) sent a CONNECT with a malformed 'will topic'. This is not allowed.",
                    "Sent CONNECT with malformed UTF-8 String for 'will topic'",
                    Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                    ReasonStrings.CONNACK_TOPIC_NAME_INVALID_WILL_MALFORMED);

        }
        return utf8String;
    }


    private boolean readAndValidateTopic(final @NotNull Channel channel, final @NotNull ByteBuf buf, final MqttWillPublish.@NotNull Mqtt5Builder mqtt5Builder) {
        final String willTopic = decodeUTF8Topic(channel, buf);
        if (willTopic == null) {
            return false;
        }

        if (willTopic.isEmpty()) {
            mqttConnacker.connackError(channel,
                    "A client (IP: {}) sent a CONNECT with empty will topic. This is not allowed.",
                    "Sent a CONNECT with empty will topic",
                    Mqtt5ConnAckReasonCode.TOPIC_NAME_INVALID,
                    ReasonStrings.CONNACK_TOPIC_NAME_INVALID_WILL_EMPTY);
            return false;
        } else {
            if (topicInvalid(channel, willTopic, "will topic")) {
                return false;
            }
        }
        mqtt5Builder.withTopic(willTopic);
        return true;
    }

    private boolean propertiesLengthInvalid(final @NotNull Channel channel, final @NotNull ByteBuf buf, final int propertyLength) {
        if (propertyLength < 0) {
            mqttConnacker.connackError(channel,
                    "A client (IP: {}) sent a CONNECT with malformed properties length.",
                    "Sent CONNECT with malformed properties length",
                    Mqtt5ConnAckReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.CONNACK_MALFORMED_PROPERTIES_LENGTH);
            return true;
        }
        if (buf.readableBytes() < propertyLength) {
            connackByRemainingLengthToShort(channel);
            return true;
        }
        return false;
    }

    /**
     * Check if the encoded size of the user properties exceed the internal limit Results in {@link
     * com.hivemq.mqtt.message.reason.MqttCommonReasonCode#PACKET_TOO_LARGE} with DISCONNECT
     *
     * @param userProperties the properties to validate
     */
    private boolean invalidUserPropertiesLength(final @NotNull Channel channel, final @NotNull Mqtt5UserProperties userProperties) {
        if (userProperties.encodedLength() > maxUserPropertiesLength) {
            mqttConnacker.connackError(channel,
                    "A client (IP: {}) sent a CONNECT with user properties that are too large. Disconnecting client.",
                    "Sent a CONNECT with too large user properties",
                    Mqtt5ConnAckReasonCode.PACKET_TOO_LARGE,
                    ReasonStrings.CONNACK_PACKET_TOO_LARGE_USER_PROPERTIES);
            return true;
        }
        return false;
    }

    /**
     * Decodes and validates a response topic property
     * <p>
     * Results in {@link Mqtt5ConnAckReasonCode#PROTOCOL_ERROR} with CONNACK by:
     * <p>
     * - assigning it more than once
     * <p>
     * Results in {@link Mqtt5ConnAckReasonCode#MALFORMED_PACKET} with CONNACK by:
     * <p>
     * - response topic not UTF-8 well formed
     * - readable bytes less than 2
     * - readable bytes less than indicated binary length
     * <p>
     * Results in {@link Mqtt5ConnAckReasonCode#TOPIC_NAME_INVALID} with CONNACK by:
     * <p>
     * - topic contains '+' character
     * - topic contains '#' character
     *
     * @param channel       the channel of the mqtt client
     * @param buf           the encoded ByteBuf of the message
     * @param responseTopic the initial response topic (must be null)
     * @return a response topic,
     * or {@code null} when failed.
     */
    @Nullable
    private String readResponseTopic(final @NotNull Channel channel, final @NotNull ByteBuf buf, @Nullable String responseTopic) {
        if (responseTopic != null) {
            connackByMoreThanOnce(channel, "response topic");
            return null;
        }
        responseTopic = MqttBinaryData.decodeString(buf, validateUTF8);
        if (responseTopic == null) {

            mqttConnacker.connackError(
                    channel,
                    "A client (IP: {}) sent a CONNECT with a malformed UTF-8 string for response topic. This is not allowed.",
                    "Sent a CONNECT with a malformed UTF-8 string for response topic",
                    Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                    ReasonStrings.CONNACK_MALFORMED_RESPONSE_TOPIC);

            return null;
        }
        if (topicInvalid(channel, responseTopic, "response topic")) {
            return null;
        }
        return responseTopic;
    }


    /**
     * Decodes and validates a correlation data property
     * <p>
     * Results in {@link Mqtt5ConnAckReasonCode#PROTOCOL_ERROR} with DISCONNECT by:
     * <p>
     * - assigning it more than once
     * <p>
     * Results in Results in {@link Mqtt5ConnAckReasonCode#MALFORMED_PACKET} with DISCONNECT by:
     * <p>
     * - readable bytes less than 2
     * - readable bytes less than indicated binary length
     *
     * @param channel         the channel of the mqtt client
     * @param buf             the encoded ByteBuf of the message
     * @param correlationData the initial correlation data (must be null)
     * @return a byte[] containing decoded correlation data,
     * or {@code null} when failed.
     */
    private byte @Nullable [] readCorrelationData(final @NotNull Channel channel, final @NotNull ByteBuf buf, byte @Nullable [] correlationData) {
        if (correlationData != null) {
            connackByMoreThanOnce(channel, "correlation data");
            return null;
        }
        correlationData = MqttBinaryData.decode(buf);
        if (correlationData == null) {
            mqttConnacker.connackError(channel,
                    "A client (IP: {}) sent a CONNECT with a malformed correlation data. This is not allowed.",
                    "Sent a CONNECT with a malformed correlation data",
                    Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                    ReasonStrings.CONNACK_MALFORMED_CORRELATION_DATA);

            return null;
        }
        return correlationData;
    }

    /**
     * Decodes and validates a content type property
     * <p>
     * Results in {@link Mqtt5ConnAckReasonCode#PROTOCOL_ERROR} with CONNACK by:
     * <p>
     * - assigning it more than once
     * <p>
     * Results in {@link Mqtt5ConnAckReasonCode#MALFORMED_PACKET} with CONNACK by:
     * <p>
     * - content type not UTF-8 well formed
     * - readable bytes less than 2
     * - readable bytes less than indicated binary length
     *
     * @param channel     the channel of the mqtt client
     * @param buf         the encoded ByteBuf of the message
     * @param contentType the initial content type (must be null)
     * @return a content type,
     * or {@code null} when failed.
     */
    @Nullable
    private String readContentType(final @NotNull Channel channel, final @NotNull ByteBuf buf, @Nullable String contentType) {
        if (contentType != null) {
            connackByMoreThanOnce(channel, "content type");
            return null;
        }
        contentType = MqttBinaryData.decodeString(buf, validateUTF8);
        if (contentType == null) {
            mqttConnacker.connackError(channel,
                    "A client (IP: {}) sent a CONNECT with a malformed UTF-8 string for content type. This is not allowed.",
                    "Sent a CONNECT with a malformed UTF-8 string for content type",
                    Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                    ReasonStrings.CONNACK_MALFORMED_CONTENT_TYPE);

            return null;
        }
        return contentType;
    }

    /**
     * Decodes and validates a payload format indicator property
     * <p>
     * Results in {@link Mqtt5ConnAckReasonCode#PROTOCOL_ERROR} with CONNACK by:
     * <p>
     * - assigning it more than once
     * <p>
     * Results in {@link Mqtt5ConnAckReasonCode#MALFORMED_PACKET} with CONNACK by:
     * <p>
     * - payload format indicator not UTF-8 well formed
     * - readable bytes less than 1
     * - readUnsignedByte() > 1  (only 0 and 1 are allowed)
     *
     * @param channel                the channel of the mqtt client
     * @param buf                    the encoded ByteBuf of the message
     * @param payloadFormatIndicator the initial payload format indicator (must be null)
     * @return a {@link Mqtt5PayloadFormatIndicator},
     * or {@code null} when failed.
     */
    @Nullable
    private Mqtt5PayloadFormatIndicator readPayloadFormatIndicator(final @NotNull Channel channel,
                                                                   final @NotNull ByteBuf buf,
                                                                   @Nullable Mqtt5PayloadFormatIndicator payloadFormatIndicator) {
        if (payloadFormatIndicator != null) {
            connackByMoreThanOnce(channel, "payload format indicator");
            return null;
        }
        if (buf.readableBytes() < 1) {
            connackByRemainingLengthToShort(channel);
            return null;
        }
        final short payloadFormatIndicatorByte = buf.readUnsignedByte();
        payloadFormatIndicator = Mqtt5PayloadFormatIndicator.fromCode(payloadFormatIndicatorByte);
        if (payloadFormatIndicator == null) {
            //NOT IN MQTT 5 SPEC
            mqttConnacker.connackError(channel,
                    "A client (IP: {}) sent a CONNECT with a wrong payload format indicator. This is not allowed.",
                    "Sent a CONNECT with a wrong payload format indicator",
                    Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                    ReasonStrings.CONNACK_MALFORMED_PFI);

            return null;
        }
        return payloadFormatIndicator;
    }

    /**
     * Decodes and validates a session expiry interval property
     * <p>
     * Results in {@link MqttCommonReasonCode#PROTOCOL_ERROR} with CONNACK by:
     * <p>
     * - assigning it more than once
     * <p>
     * Results in {@link MqttCommonReasonCode#MALFORMED_PACKET} with CONNACK by:
     * <p>
     * - readable bytes less than 4
     *
     * @param channel               the channel of the mqtt client
     * @param buf                   the encoded ByteBuf of the message
     * @param sessionExpiryInterval the initial session expiry interval (must be equal to {@link
     *                              com.hivemq.mqtt.message.connect.Mqtt5CONNECT#SESSION_EXPIRY_NOT_SET})
     * @return the session expiry interval, or -1 when decoding failed.
     */
    private long decodeSessionExpiryInterval(final @NotNull Channel channel, final @NotNull ByteBuf buf, final long sessionExpiryInterval) {
        if (sessionExpiryInterval != SESSION_EXPIRY_NOT_SET) {
            connackByMoreThanOnce(channel, "session expiry interval");
            return DISCONNECTED;
        }
        if (buf.readableBytes() < 4) {
            connackByRemainingLengthToShort(channel);
            return DISCONNECTED;
        }
        return buf.readUnsignedInt();
    }

    /**
     * validates a message expiry interval property
     * <p>
     * Results in {@link Mqtt5ConnAckReasonCode#PROTOCOL_ERROR} with CONNACK by:
     * <p>
     * - assigning it more than once
     * <p>
     * Results in {@link Mqtt5ConnAckReasonCode#MALFORMED_PACKET} with CONNACK by:
     * <p>
     * - readable bytes less than 4
     *
     * @param channel               the channel of the mqtt client
     * @param buf                   the encoded ByteBuf of the message
     * @param messageExpiryInterval the initial message expiry interval
     *                              (must be {@link PUBLISH#MESSAGE_EXPIRY_INTERVAL_NOT_SET})
     * @return true if invalid, false if valid
     */
    private boolean messageExpiryIntervalInvalid(final @NotNull Channel channel, final @NotNull ByteBuf buf, final long messageExpiryInterval) {
        if (messageExpiryInterval != MESSAGE_EXPIRY_INTERVAL_NOT_SET) {
            connackByMoreThanOnce(channel, "message expiry interval");
            return true;
        }
        if (buf.readableBytes() < 4) {
            connackByRemainingLengthToShort(channel);
            return true;
        }
        return false;
    }

    /**
     * Validates a given topic
     * <p>
     * Results in {@link Mqtt5ConnAckReasonCode#TOPIC_NAME_INVALID} Malformed Packet with CONNACK by:
     * <p>
     * - topic contains '+' character - topic contains '#' character
     *
     * @param channel   the channel of the mqtt client
     * @param topicName the topic
     * @return true if invalid, false if valid
     */
    private boolean topicInvalid(final @NotNull Channel channel, final @NotNull String topicName, final @NotNull String location) {

        if (Topics.containsWildcard(topicName)) {
            mqttConnacker.connackError(
                    channel,
                    "A client (IP: {}) sent a CONNECT with a wildcard character (# or +) in the " + location + ". This is not allowed.",
                    "Sent CONNECT with wildcard character (#/+) in the " + location,
                    Mqtt5ConnAckReasonCode.TOPIC_NAME_INVALID,
                    String.format(ReasonStrings.CONNACK_TOPIC_NAME_INVALID_WILL_WILDCARD, location));

            return true;
        }
        return false;
    }

    private void connackByMoreThanOnce(final @NotNull Channel channel, final @NotNull String key) {
        mqttConnacker.connackError(channel,
                "A client (IP: {}) sent a CONNECT with '" + key + "' included more than once. This is not allowed.",
                "Sent a CONNECT with '" + key + "' included more than once",
                Mqtt5ConnAckReasonCode.PROTOCOL_ERROR,
                String.format(ReasonStrings.CONNACK_PROTOCOL_MULTIPLE_KEY, key));

    }

    private void connackByRemainingLengthToShort(final @NotNull Channel channel) {
        mqttConnacker.connackError(channel,
                "A client (IP: {}) sent a CONNECT with remaining length too short. This is not allowed.",
                "Sent a CONNECT with remaining length too short",
                Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                ReasonStrings.CONNACK_MALFORMED_REMAINING);
    }

    private void connackByMalformedPropertyLength(final @NotNull Channel channel) {
        mqttConnacker.connackError(channel,
                "A client (IP: {}) sent a CONNECT with a malformed properties length. This is not allowed. Disconnecting client.",
                "Sent a CONNECT with a malformed properties length",
                Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                ReasonStrings.CONNACK_MALFORMED_PROPERTIES_LENGTH);

    }

    private void connackByInvalidPropertyIdentifier(final @NotNull Channel channel, final int propertyIdentifier) {
        mqttConnacker.connackError(channel,
                "A client (IP: {}) sent a CONNECT with a invalid property identifier '" + propertyIdentifier + "'. This is not allowed. Disconnecting client.",
                "Sent CONNECT with invalid property identifier",
                Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                ReasonStrings.CONNACK_MALFORMED_PROPERTIES_INVALID);
    }
}
