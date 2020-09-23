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
package com.hivemq.codec.decoder;

import com.google.common.collect.ImmutableList;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.codec.encoder.mqtt5.MqttBinaryData;
import com.hivemq.codec.encoder.mqtt5.MqttVariableByteInteger;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.reason.MqttCommonReasonCode;
import com.hivemq.util.ReasonStrings;
import com.hivemq.util.Strings;
import com.hivemq.util.Topics;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET;
import static com.hivemq.mqtt.message.publish.PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET;

/**
 * Abstract Base Class for all Mqtt Decoders
 * <p>
 * T = {@link Message}
 *
 * @author Florian Limpöck
 */
public abstract class AbstractMqttDecoder<T extends Message> extends MqttDecoder<T> {

    protected static final int DISCONNECTED = -1;

    protected final @NotNull FullConfigurationService configurationService;
    protected final @NotNull MqttServerDisconnector disconnector;

    protected final boolean validateUTF8;
    protected final long maxMessageExpiryInterval;
    protected final long maxUserPropertiesLength;
    protected final boolean subscriptionIdentifiersAvailable;

    protected AbstractMqttDecoder(final @NotNull MqttServerDisconnector disconnector, final @NotNull FullConfigurationService configurationService) {
        this.configurationService = configurationService;
        this.disconnector = disconnector;
        this.validateUTF8 = configurationService.securityConfiguration().validateUTF8();
        this.maxMessageExpiryInterval = configurationService.mqttConfiguration().maxMessageExpiryInterval();
        this.maxUserPropertiesLength = InternalConfigurations.USER_PROPERTIES_MAX_SIZE;
        this.subscriptionIdentifiersAvailable = configurationService.mqttConfiguration().subscriptionIdentifierEnabled();
    }

    /**
     * decodes and validates a {@link MqttUserProperty}
     * <p>
     * Results in Results in {@link MqttCommonReasonCode#MALFORMED_PACKET} with DISCONNECT by:
     * <p>
     * - name or value are not UTF-8 well formed
     * <p>
     * - readable bytes less than 2
     * <p>
     * - readable bytes less than indicated string length
     * <p>
     * Valid user properties will be added to the UserProperty-Builder
     *
     * @param channel               the channel of the mqtt client
     * @param buf                   the encoded ByteBuf of the message
     * @param userPropertiesBuilder the builder of the user properties
     * @param messageType           the type of the Message
     * @return the UserProperty-Builder
     */
    @Nullable
    protected ImmutableList.Builder<MqttUserProperty> readUserProperty(final @NotNull Channel channel,
                                                                       final @NotNull ByteBuf buf,
                                                                       @Nullable ImmutableList.Builder<MqttUserProperty> userPropertiesBuilder,
                                                                       final @NotNull MessageType messageType) {
        final MqttUserProperty userProperty = MqttUserProperty.decode(buf, validateUTF8);
        if (userProperty == null) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a " + messageType.name() + " with a malformed user property. This is not allowed. Disconnecting client.",
                    "Sent a " + messageType.name() + " with a malformed user property",
                    Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                    String.format(ReasonStrings.DISCONNECT_MALFORMED_USER_PROPERTY,messageType.name()));
            return null;
        }
        if (userPropertiesBuilder == null) {
            userPropertiesBuilder = ImmutableList.builder();
        }
        userPropertiesBuilder.add(userProperty);
        return userPropertiesBuilder;
    }

    /**
     * decodes and validates a topic UTF-8 String length
     * <p>
     * Results in Results in {@link MqttCommonReasonCode#MALFORMED_PACKET} with DISCONNECT by:
     * <p>
     * - readable bytes less than 2
     * <p>
     * - readable bytes less than indicated string length
     *
     * @param channel     the channel of the mqtt client
     * @param buf         the encoded ByteBuf of the message
     * @param key         the name of the property, eg. 'will topic' or 'topic'
     * @param messageType the type of the message
     * @return the length of the string or -1 for malformed packet
     */
    protected int decodeUTF8StringLength(final @NotNull Channel channel, final @NotNull ByteBuf buf, final @NotNull String key, final @NotNull MessageType messageType) {

        final int utf8StringLength;

        if (buf.readableBytes() < 2 || (buf.readableBytes() < (utf8StringLength = buf.readUnsignedShort()))) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a " + messageType.name() + " with an incorrect UTF-8 string length for '" + key + "'. Disconnecting client.",
                    "Incorrect " + messageType.name() + " UTF-8 string length for '" + key + "'",
                    Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                    String.format(ReasonStrings.DISCONNECT_MALFORMED_UTF8_LENGTH,messageType.name(),key));
            return DISCONNECTED;

        }

        return utf8StringLength;

    }

    /**
     * Decodes a topic without knowing the length of it
     *
     * @param channel     the channel of the mqtt client
     * @param buf         the encoded ByteBuf of the message
     * @param key         the name of the property
     * @param messageType the type of the message
     * @return the topic as String or {@code null} if failed
     */
    @Nullable
    protected String decodeUTF8Topic(final @NotNull Channel channel, final @NotNull ByteBuf buf, final @NotNull String key, final @NotNull MessageType messageType) {

        final int utf8StringLength = decodeUTF8StringLength(channel, buf, key, messageType);
        if (utf8StringLength == DISCONNECTED) {
            return null;
        }

        return decodeUTF8Topic(channel, buf, utf8StringLength, key, messageType);
    }

    /**
     * Decodes and validates a topic with a given length
     * <p>
     * Results in Results in {@link MqttCommonReasonCode#MALFORMED_PACKET} with DISCONNECT by:
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
     * @param key              name of the property
     * @param messageType      the type of the message
     * @return the topic as String or {@code null} if failed
     */
    @Nullable
    protected String decodeUTF8Topic(final @NotNull Channel channel, final @NotNull ByteBuf buf, final int utf8StringLength, final @NotNull String key, final @NotNull MessageType messageType) {

        final String utf8String = Strings.getValidatedPrefixedString(buf, utf8StringLength, validateUTF8);
        if (utf8String == null) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a " + messageType.name() + " with a malformed '" + key + "'. This is not allowed. Disconnecting client.",
                    "Sent a " + messageType.name() + " with malformed UTF-8 String for '" + key + "'",
                    Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                    String.format(ReasonStrings.DISCONNECT_MALFORMED_UTF8_STRING,messageType.name(),key));

        }
        return utf8String;
    }

    /**
     * Decodes and validates the authentication method.
     *
     * @param channel              the {@link Channel} of the MQTT Client
     * @param buf                  the {@link ByteBuf} to decode
     * @param authenticationMethod the {@link String} name of the authentication method
     * @param messageType          the {@link MessageType} type of the message
     * @return decoded authentication method as {@link String}
     */
    @Nullable
    protected String decodeAuthenticationMethod(final @NotNull Channel channel, final @NotNull ByteBuf buf, @Nullable String authenticationMethod, final @NotNull MessageType messageType) {
        if (authenticationMethod != null) {
            disconnectByMoreThanOnce(channel, "auth method", messageType);
            return null;
        }
        authenticationMethod = MqttBinaryData.decodeString(buf, validateUTF8);
        if (authenticationMethod == null) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a " + messageType.name() + " with a malformed auth method. This is not allowed. Disconnecting client.",
                    "Sent a " + messageType.name() + " with malformed UTF-8 String for auth method",
                    Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                    String.format(ReasonStrings.DISCONNECT_MALFORMED_AUTH_METHOD,messageType.name()));
            return null;
        }
        return authenticationMethod;
    }

    /**
     * Validates a given topic
     * <p>
     * Results in {@link MqttCommonReasonCode#TOPIC_NAME_INVALID} Malformed Packet with DISCONNECT by:
     * <p>
     * - topic contains '+' character
     * <p>
     * - topic contains '#' character
     *
     * @param channel     the channel of the mqtt client
     * @param topicName   the topic
     * @param messageType the type of the message
     * @return true if invalid, false if valid
     */
    protected boolean topicInvalid(final @NotNull Channel channel, final @NotNull String topicName, final @NotNull MessageType messageType) {
        if (Topics.containsWildcard(topicName)) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a " + messageType.name() + " with a wildcard character (# or +). This is not allowed. Disconnecting client.",
                    "Sent a " + messageType.name() + " with wildcard character (#/+) in topic: " + topicName,
                    Mqtt5DisconnectReasonCode.TOPIC_NAME_INVALID,
                    String.format(ReasonStrings.DISCONNECT_MALFORMED_WILDCARD,messageType.name()));

            return true;
        }
        return false;
    }

    /**
     * Decodes and validates a correlation data property
     * <p>
     * Results in {@link MqttCommonReasonCode#PROTOCOL_ERROR} with DISCONNECT by:
     * <p>
     * - assigning it more than once
     * <p>
     * Results in Results in {@link MqttCommonReasonCode#MALFORMED_PACKET} with DISCONNECT by:
     * <p>
     * - readable bytes less than 2
     * <p>
     * - readable bytes less than indicated binary length
     *
     * @param channel         the channel of the mqtt client
     * @param buf             the encoded ByteBuf of the message
     * @param correlationData the initial correlation data (must be null)
     * @param messageType     the type of the message
     * @return a byte[] containing decoded correlation data, or {@code null} when failed.
     */
    protected byte @Nullable[] readCorrelationData(final @NotNull Channel channel, final @NotNull ByteBuf buf, byte @Nullable[] correlationData, final @NotNull MessageType messageType) {
        if (correlationData != null) {
            disconnectByMoreThanOnce(channel, "correlation data", messageType);
            return null;
        }
        correlationData = MqttBinaryData.decode(buf);
        if (correlationData == null) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a " + messageType.name() + " with a malformed correlation data. This is not allowed. Disconnecting client.",
                    "Sent a " + messageType.name() + " with a malformed correlation data",
                    Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                    String.format(ReasonStrings.DISCONNECT_MALFORMED_CORRELATION_DATA,messageType.name()));
        }
        return correlationData;
    }

    /**
     * Decodes and validates a response topic property
     * <p>
     * Results in {@link MqttCommonReasonCode#PROTOCOL_ERROR} with DISCONNECT by:
     * <p>
     * - assigning it more than once
     * <p>
     * Results in {@link MqttCommonReasonCode#MALFORMED_PACKET} with DISCONNECT by:
     * <p>
     * - response topic not UTF-8 well formed
     * <p>
     * - readable bytes less than 2
     * <p>
     * - readable bytes less than indicated binary length
     * <p>
     * Results in {@link MqttCommonReasonCode#TOPIC_NAME_INVALID} with DISCONNECT by:
     * <p>
     * - topic contains '+' character
     * <p>
     * - topic contains '#' character
     *
     * @param channel       the channel of the mqtt client
     * @param buf           the encoded ByteBuf of the message
     * @param responseTopic the initial response topic (must be null)
     * @param messageType   the type of the message
     * @return a response topic, or {@code null} when failed.
     */
    @Nullable
    protected String readResponseTopic(final @NotNull Channel channel, final @NotNull ByteBuf buf, @Nullable String responseTopic, final @NotNull MessageType messageType) {
        if (responseTopic != null) {
            disconnectByMoreThanOnce(channel, "response topic", messageType);
            return null;
        }
        responseTopic = MqttBinaryData.decodeString(buf, validateUTF8);
        if (responseTopic == null) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a " + messageType.name() + " with a malformed UTF-8 string for response topic. This is not allowed. Disconnecting client.",
                    "Sent a " + messageType.name() + " with a malformed UTF-8 string for response topic",
                    Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                    String.format(ReasonStrings.DISCONNECT_MALFORMED_RESPONSE_TOPIC,messageType.name()));

            return null;
        }
        if (topicInvalid(channel, responseTopic, messageType)) {
            return null;
        }
        return responseTopic;
    }

    /**
     * Decodes and validates a content type property
     * <p>
     * Results in {@link MqttCommonReasonCode#PROTOCOL_ERROR} with DISCONNECT by:
     * <p>
     * - assigning it more than once
     * <p>
     * Results in {@link MqttCommonReasonCode#MALFORMED_PACKET} with DISCONNECT by:
     * <p>
     * - content type not UTF-8 well formed
     * <p>
     * - readable bytes less than 2
     * <p>
     * - readable bytes less than indicated binary length
     *
     * @param channel     the channel of the mqtt client
     * @param buf         the encoded ByteBuf of the message
     * @param contentType the initial content type (must be null)
     * @param messageType the type of the message
     * @return a content type, or {@code null} when failed.
     */
    @Nullable
    protected String readContentType(final @NotNull Channel channel, final @NotNull ByteBuf buf, @Nullable String contentType, final @NotNull MessageType messageType) {
        if (contentType != null) {
            disconnectByMoreThanOnce(channel, "content type", messageType);
            return null;
        }
        contentType = MqttBinaryData.decodeString(buf, validateUTF8);
        if (contentType == null) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a " + messageType.name() + " with a malformed UTF-8 string for content type. This is not allowed. Disconnecting client.",
                    "Sent a " + messageType.name() + " with a malformed UTF-8 string for content type",
                    Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                    String.format(ReasonStrings.DISCONNECT_MALFORMED_CONTENT_TYPE,messageType.name()));

            return null;
        }
        return contentType;
    }

    /**
     * Decodes and validates a payload format indicator property
     * <p>
     * Results in {@link MqttCommonReasonCode#PROTOCOL_ERROR} with DISCONNECT by:
     * <p>
     * - assigning it more than once
     * <p>
     * Results in {@link MqttCommonReasonCode#MALFORMED_PACKET} with DISCONNECT by:
     * <p>
     * - payload format indicator not UTF-8 well formed
     * <p>
     * - readable bytes less than 1
     * <p>
     * - readUnsignedByte() > 1  (only 0 and 1 are allowed)
     *
     * @param channel                the channel of the mqtt client
     * @param buf                    the encoded ByteBuf of the message
     * @param payloadFormatIndicator the initial payload format indicator (must be null)
     * @param messageType            the type of the message
     * @return a {@link Mqtt5PayloadFormatIndicator}, or {@code null} when failed.
     */
    @Nullable
    protected Mqtt5PayloadFormatIndicator readPayloadFormatIndicator(final @NotNull Channel channel,
                                                                     final @NotNull ByteBuf buf,
                                                                     @Nullable Mqtt5PayloadFormatIndicator payloadFormatIndicator,
                                                                     final @NotNull MessageType messageType) {
        if (payloadFormatIndicator != null) {
            disconnectByMoreThanOnce(channel, "payload format indicator", messageType);
            return null;
        }
        if (buf.readableBytes() < 1) {
            disconnectByRemainingLengthToShort(channel, messageType);
            return null;
        }
        final short payloadFormatIndicatorByte = buf.readUnsignedByte();
        payloadFormatIndicator = Mqtt5PayloadFormatIndicator.fromCode(payloadFormatIndicatorByte);
        if (payloadFormatIndicator == null) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a " + messageType.name() + " with a wrong payload format indicator. This is not allowed. Disconnecting client.",
                    "Sent a " + messageType.name() + " with a wrong payload format indicator",
                    Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                    String.format(ReasonStrings.DISCONNECT_MALFORMED_PFI,messageType.name()));

            return null;
        }
        return payloadFormatIndicator;
    }

    /**
     * validates a message expiry interval property
     * <p>
     * Results in {@link MqttCommonReasonCode#PROTOCOL_ERROR} with DISCONNECT by:
     * <p>
     * - assigning it more than once
     * <p>
     * Results in {@link MqttCommonReasonCode#MALFORMED_PACKET} with DISCONNECT by:
     * <p>
     * - readable bytes less than 4
     *
     * @param channel               the channel of the mqtt client
     * @param buf                   the encoded ByteBuf of the message
     * @param messageExpiryInterval the initial message expiry interval (must be {@link PUBLISH#MESSAGE_EXPIRY_INTERVAL_NOT_SET})
     * @param messageType           the type of the message
     * @return true if invalid, false if valid
     */
    protected boolean messageExpiryIntervalInvalid(final @NotNull Channel channel, final @NotNull ByteBuf buf, final long messageExpiryInterval, final @NotNull MessageType messageType) {
        if (messageExpiryInterval != MESSAGE_EXPIRY_INTERVAL_NOT_SET) {
            disconnectByMoreThanOnce(channel, "message expiry interval", messageType);
            return true;
        }
        if (buf.readableBytes() < 4) {
            disconnectByRemainingLengthToShort(channel, messageType);
            return true;
        }
        return false;
    }

    /**
     * Decodes and validates a variable byte integer properties length of a message without payload
     * <p>
     * Results in {@link MqttCommonReasonCode#MALFORMED_PACKET} with DISCONNECT by:
     * <p>
     * - not enough bytes to read
     * <p>
     * - more than 4 bytes
     * <p>
     * - not even minimum bytes
     * <p>
     * - readable bytes > decoded properties length => payload not allowed
     * <p>
     * - readable bytes < decoded properties length
     *
     * @param channel     the channel of the mqtt client
     * @param buf         the encoded ByteBuf of the message
     * @param messageType the type of the message
     * @return the properties length, or -1 when failed.
     */
    protected int decodePropertiesLengthNoPayload(@NotNull final ByteBuf buf, @NotNull final Channel channel, @NotNull final MessageType messageType) {
        final int propertyLength = MqttVariableByteInteger.decode(buf);
        if (propertyLength < 0) {
            disconnectByMalformedPropertyLength(channel, messageType);
            return DISCONNECTED;
        }
        if (buf.readableBytes() != propertyLength) {
            if (buf.readableBytes() < propertyLength) {
                disconnectByRemainingLengthToShort(channel, messageType);
            } else {
                disconnector.disconnect(channel,
                        "A client (IP: {}) sent a " + messageType.name() + " with payload. This is not allowed. Disconnecting client.",
                        "Sent a " + messageType.name() + " with payload",
                        Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                        String.format(ReasonStrings.DISCONNECT_MALFORMED_PAYLOAD,messageType.name()));
            }
            return DISCONNECTED;
        }
        return propertyLength;
    }

    /**
     * Decodes and validates a session expiry interval property
     * <p>
     * Results in {@link MqttCommonReasonCode#PROTOCOL_ERROR} with DISCONNECT by:
     * <p>
     * - assigning it more than once
     * <p>
     * Results in {@link MqttCommonReasonCode#MALFORMED_PACKET} with DISCONNECT by:
     * <p>
     * - readable bytes less than 4
     *
     * @param channel               the channel of the mqtt client
     * @param buf                   the encoded ByteBuf of the message
     * @param sessionExpiryInterval the initial session expiry interval (must be equal to {@link
     *                              com.hivemq.mqtt.message.connect.Mqtt5CONNECT#SESSION_EXPIRY_NOT_SET})
     * @param messageType           the type of the message
     * @return the session expiry interval, or -1 when decoding failed.
     */
    protected long decodeSessionExpiryInterval(final @NotNull Channel channel, final @NotNull ByteBuf buf, final long sessionExpiryInterval, final @NotNull MessageType messageType) {
        if (sessionExpiryInterval != SESSION_EXPIRY_NOT_SET) {
            disconnectByMoreThanOnce(channel, "session expiry interval", messageType);
            return DISCONNECTED;
        }
        if (buf.readableBytes() < 4) {
            disconnectByRemainingLengthToShort(channel, messageType);
            return DISCONNECTED;
        }
        return buf.readUnsignedInt();
    }

    /**
     * Decodes and validates a server reference property
     * <p>
     * Results in {@link MqttCommonReasonCode#PROTOCOL_ERROR} with DISCONNECT by:
     * <p>
     * - assigning it more than once
     * <p>
     * Results in {@link MqttCommonReasonCode#MALFORMED_PACKET} with DISCONNECT by:
     * <p>
     * - server reference not UTF-8 well formed - readable bytes less than 2 - readable bytes less than indicated string
     * length
     *
     * @param channel         the channel of the mqtt client
     * @param buf             the encoded ByteBuf of the message
     * @param serverReference the initial server reference (must be null)
     * @param messageType     the type of the message
     * @return a server reference, or {@code null} when failed.
     */
    @Nullable
    protected String decodeServerReference(final @NotNull Channel channel, final @NotNull ByteBuf buf, @Nullable String serverReference, final @NotNull MessageType messageType) {
        if (serverReference != null) {
            disconnectByMoreThanOnce(channel, "server reference", messageType);
            return null;
        }
        serverReference = MqttBinaryData.decodeString(buf, validateUTF8);
        if (serverReference == null) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a " + messageType.name() + " with a malformed UTF-8 string for server reference. This is not allowed. Disconnecting client.",
                    "Sent a " + messageType.name() + " with a malformed UTF-8 string for server reference",
                    Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                    String.format(ReasonStrings.DISCONNECT_MALFORMED_SERVER_REFERENCE,messageType.name()));
            return null;
        }
        return serverReference;
    }

    /**
     * Decodes and validates a reason string property
     * <p>
     * Results in {@link MqttCommonReasonCode#PROTOCOL_ERROR} with DISCONNECT by:
     * <p>
     * - assigning it more than once
     * <p>
     * Results in {@link MqttCommonReasonCode#MALFORMED_PACKET} with DISCONNECT by:
     * <p>
     * - reason string not UTF-8 well formed - readable bytes less than 2 - readable bytes less than indicated string
     * length
     *
     * @param channel      the channel of the mqtt client
     * @param buf          the encoded ByteBuf of the message
     * @param reasonString the initial reason string (must be null)
     * @param messageType  the type of the message
     * @return a reason string, or {@code null} when failed.
     */
    @Nullable
    protected String decodeReasonString(final @NotNull Channel channel, final @NotNull ByteBuf buf, @Nullable String reasonString, final @NotNull MessageType messageType) {
        if (reasonString != null) {
            disconnectByMoreThanOnce(channel, "reason string", messageType);
            return null;
        }
        reasonString = MqttBinaryData.decodeString(buf, validateUTF8);
        if (reasonString == null) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a " + messageType.name() + " with a malformed UTF-8 string for reason string. This is not allowed. Disconnecting client.",
                    "Sent a " + messageType.name() + " with a malformed UTF-8 string for reason string",
                    Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                    String.format(ReasonStrings.DISCONNECT_MALFORMED_REASON_STRING,messageType.name()));
            return null;
        }
        return reasonString;
    }

    /**
     * Sends a DISCONNECT to a client because of sending an invalid reason code.
     *
     * @param channel     the channel of the mqtt client
     * @param messageType the type of the message
     */
    protected void disconnectByInvalidReasonCode(final @NotNull Channel channel, final @NotNull MessageType messageType) {
        disconnector.disconnect(channel,
                "A client (IP: {}) sent a " + messageType.name() + " with invalid reason code. Disconnecting client.",
                "Sent a " + messageType.name() + " with invalid reason code",
                Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_REASON_CODE,messageType.name()));

    }

    /**
     * Sends a DISCONNECT to a client because of sending an invalid authentication method.
     *
     * @param channel     the channel of the mqtt client
     * @param messageType the type of the message
     */
    protected void disconnectByInvalidAuthMethod(final @NotNull Channel channel, final @NotNull MessageType messageType) {
        disconnector.disconnect(channel,
                "A client (IP: {}) sent a " + messageType.name() + " with invalid authentication method. Disconnecting client.",
                "Sent a " + messageType.name() + " with invalid authentication method",
                Mqtt5DisconnectReasonCode.BAD_AUTHENTICATION_METHOD,
                String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_AUTH_METHOD,messageType.name()));
    }

    /**
     * Sends a DISCONNECT to a client because of sending a property more than once.
     *
     * @param channel     the channel of the mqtt client
     * @param messageType the type of the message
     * @param key         the name of the property
     */
    protected void disconnectByMoreThanOnce(final @NotNull Channel channel, final @NotNull String key, final @NotNull MessageType messageType) {
        disconnector.disconnect(channel,
                "A client (IP: {}) sent a " + messageType.name() + " with '" + key + "' included more than once. This is not allowed. Disconnecting client.",
                "Sent a " + messageType.name() + " with '" + key + "' included more than once",
                Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_MULTI_KEY,messageType.name(),key));

    }

    /**
     * Sends a DISCONNECT to a client because of remaining length to short.
     *
     * @param channel     the channel of the mqtt client
     * @param messageType the type of the message
     */
    protected void disconnectByRemainingLengthToShort(final @NotNull Channel channel, final @NotNull MessageType messageType) {
        disconnector.disconnect(channel,
                "A client (IP: {}) sent a " + messageType.name() + " with remaining length too short. This is not allowed. Disconnecting client.",
                "Sent a " + messageType.name() + " with remaining length too short",
                Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                String.format(ReasonStrings.DISCONNECT_MALFORMED_REMAINING_LENGTH,messageType.name()));

    }

    /**
     * Sends a DISCONNECT to a client because of sending a malformed properties length
     *
     * @param channel     the channel of the mqtt client
     * @param messageType the type of the message
     */
    protected void disconnectByMalformedPropertyLength(final @NotNull Channel channel, final @NotNull MessageType messageType) {
        disconnector.disconnect(channel,
                "A client (IP: {}) sent a " + messageType.name() + " with a malformed properties length. This is not allowed. Disconnecting client.",
                "Sent a " + messageType.name() + " with a malformed properties length",
                Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                String.format(ReasonStrings.DISCONNECT_MALFORMED_PROPERTIES_LENGTH,messageType.name()));

    }

    /**
     * Sends a DISCONNECT to a client because of sending an invalid property identifier
     *
     * @param channel            the channel of the mqtt client
     * @param messageType        the type of the message
     * @param propertyIdentifier the invalid property identifier as int
     */
    protected void disconnectByInvalidPropertyIdentifier(final @NotNull Channel channel, final int propertyIdentifier, final @NotNull MessageType messageType) {

        disconnector.disconnect(channel,
                "A client (IP: {}) sent a " + messageType.name() + " with an invalid property identifier '" + propertyIdentifier + "'. This is not allowed. Disconnecting client.",
                "Sent a " + messageType.name() + " with invalid property identifier",
                Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                String.format(ReasonStrings.DISCONNECT_MALFORMED_PROPERTY_IDENTIFIER,messageType.name()));

    }

    /**
     * Sends a DISCONNECT to a client because of sending an invalid fixed header
     * The Server MUST treat this as malformed and close the Network Connection
     *
     * @param channel            the channel of the mqtt client
     * @param messageType        the type of the message
     */
    protected void disconnectByInvalidFixedHeader(final @NotNull Channel channel, final @NotNull MessageType messageType) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a " + messageType.name() + " with an invalid fixed header. Disconnecting client.",
                    "Sent a " + messageType.name() + " with invalid fixed header",
                    Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                    String.format(ReasonStrings.DISCONNECT_MALFORMED_FIXED_HEADER, messageType.name()));
    }

    /**
     * Closes the connection of a client because of sending a message without identifier
     * <p>
     * MQTT 3 only. adapted from {@link this#decodePacketIdentifier(Channel, ByteBuf, MessageType)}
     * <p>
     *
     * @param channel            the channel of the mqtt client
     * @param messageType        the type of the message
     */
    protected void disconnectByNoMessageId(final @NotNull Channel channel, final @NotNull MessageType messageType) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a " + messageType.name() + " without a message id. Disconnecting client.",
                    "Sent a " + messageType.name() + " without message id",
                    Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_MESSAGE_ID, messageType.name()));
    }

    /**
     * Decodes and validates the packet identifier of the message
     * <p>
     * Results in {@link com.hivemq.mqtt.message.reason.MqttCommonReasonCode#PROTOCOL_ERROR} with DISCONNECT by:
     * <p>
     * - packet identifier == 0
     *
     * @param channel     the channel of the mqtt client
     * @param buf         the encoded ByteBuf of the message
     * @param messageType the type of the message
     * @return the packet identifier
     */
    protected int decodePacketIdentifier(final @NotNull Channel channel, final @NotNull ByteBuf buf, final @NotNull MessageType messageType) {

        final int packetIdentifier = buf.readUnsignedShort();
        if (packetIdentifier == 0) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a " + messageType.name() + " with message ID 0. Disconnecting client.",
                    "Sent a " + messageType.name() + " with message id 0",
                    Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_ID_ZERO,messageType.name()));
        }

        return packetIdentifier;
    }

    /**
     * Check if the encoded size of the user properties exceed the internal limit Results in {@link
     * com.hivemq.mqtt.message.reason.MqttCommonReasonCode#PACKET_TOO_LARGE} with DISCONNECT
     *
     * @param userProperties the properties to validate
     */
    protected boolean invalidUserPropertiesLength(final @NotNull Channel channel, final @NotNull MessageType messageType, final @NotNull Mqtt5UserProperties userProperties) {
        if (userProperties.encodedLength() > maxUserPropertiesLength) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a " + messageType.name() + " with user properties that are too large. Disconnecting client.",
                    "Sent a " + messageType.name() + " with too large user properties",
                    Mqtt5DisconnectReasonCode.PACKET_TOO_LARGE,
                    String.format(ReasonStrings.DISCONNECT_PACKET_TOO_LARGE_USER_PROPERTIES,messageType.name()));
            return true;
        }
        return false;
    }

}
