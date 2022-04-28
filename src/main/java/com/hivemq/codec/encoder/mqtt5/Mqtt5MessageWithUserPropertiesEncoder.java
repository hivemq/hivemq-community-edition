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

import com.google.common.base.Preconditions;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.codec.encoder.MqttEncoder;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.event.PublishDroppedEvent;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connect.Mqtt5CONNECT;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttMessageWithUserProperties;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.reason.Mqtt5ReasonCode;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static com.hivemq.codec.encoder.mqtt5.Mqtt5MessageEncoderUtil.encodeNullableProperty;
import static com.hivemq.codec.encoder.mqtt5.Mqtt5MessageEncoderUtil.nullablePropertyEncodedLength;
import static com.hivemq.codec.encoder.mqtt5.MqttMessageEncoderUtil.encodedLengthWithHeader;
import static com.hivemq.codec.encoder.mqtt5.MqttMessageEncoderUtil.encodedPacketLength;
import static com.hivemq.codec.encoder.mqtt5.MqttVariableByteInteger.MAXIMUM_PACKET_SIZE_LIMIT;
import static com.hivemq.mqtt.message.mqtt5.MessageProperties.REASON_STRING;

/**
 * Base class for encoders of MQTT messages with omissible User Properties.
 *
 * @author Silvio Giebl
 */
abstract class Mqtt5MessageWithUserPropertiesEncoder<T extends Message> implements MqttEncoder<T> {

    private static final Logger log = LoggerFactory.getLogger(Mqtt5MessageWithUserPropertiesEncoder.class);

    private final @NotNull MessageDroppedService messageDroppedService;
    // Need the security service here for enabling / disabling configuration on runtime.
    private final @NotNull SecurityConfigurationService securityConfigurationService;

    Mqtt5MessageWithUserPropertiesEncoder(
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull SecurityConfigurationService securityConfigurationService) {
        this.messageDroppedService = messageDroppedService;
        this.securityConfigurationService = securityConfigurationService;
    }

    @Override
    public void encode(
            final @NotNull ClientConnection clientConnection, final @NotNull T msg, final @NotNull ByteBuf out) {

        Preconditions.checkNotNull(clientConnection, "ClientConnection must never be null");
        Preconditions.checkNotNull(msg, "Message must never be null");
        Preconditions.checkNotNull(out, "ByteBuf must never be null");

        if (msg.getOmittedProperties() > 0) {

            final String clientIdFromChannel = clientConnection.getClientId();
            final String clientId = clientIdFromChannel != null ? clientIdFromChannel : "UNKNOWN";

            final long maximumPacketSize = calculateMaxMessageSize(clientConnection);

            //PUBLISH must not omit any properties
            if (msg instanceof PUBLISH) {
                // The maximal packet size exceeds the clients accepted packet size
                clientConnection.getChannel().pipeline().fireUserEventTriggered(new PublishDroppedEvent((PUBLISH) msg));
                messageDroppedService.publishMaxPacketSizeExceeded(clientId, ((PUBLISH) msg).getTopic(), ((PUBLISH) msg).getQoS().getQosNumber(), maximumPacketSize, msg.getEncodedLength());
                if (log.isTraceEnabled()) {
                    log.trace("Could not encode publish message for client ({}): Maximum packet size limit exceeded", clientId);
                }
                return;
            }

            if (msg.getPropertyLength() < 0 && msg.getEncodedLength() > maximumPacketSize) {
                messageDroppedService.messageMaxPacketSizeExceeded(clientId, msg.getType().name(), maximumPacketSize, msg.getEncodedLength());
                if (log.isTraceEnabled()) {
                    log.trace("Could not encode message of type {} for client {}: Packet too large", msg.getType(), clientId);
                }
                throw new EncoderException("Maximum packet size exceeded");
            }
        }

        encode(msg, out);
    }

    @Override
    public int bufferSize(final @NotNull ClientConnection clientConnection, final @NotNull T msg) {

        int omittedProperties = 0;
        int propertyLength = calculatePropertyLength(msg);

        if (!securityConfigurationService.allowRequestProblemInformation()
                || !Objects.requireNonNullElse(clientConnection.getRequestProblemInformation(), Mqtt5CONNECT.DEFAULT_PROBLEM_INFORMATION_REQUESTED)) {

            //Must omit user properties and reason string for any other packet than PUBLISH, CONNACK, DISCONNECT
            //if no problem information requested.
            final boolean mustOmit = !(msg instanceof PUBLISH || msg instanceof CONNACK || msg instanceof DISCONNECT);

            if (mustOmit) {
                // propertyLength - reason string
                propertyLength = propertyLength(msg, propertyLength, ++omittedProperties);
                // propertyLength - user properties
                propertyLength = propertyLength(msg, propertyLength, ++omittedProperties);
            }
        }

        final long maximumPacketSize = calculateMaxMessageSize(clientConnection);
        final int remainingLengthWithoutProperties = calculateRemainingLengthWithoutProperties(msg);
        int remainingLength = remainingLength(msg, remainingLengthWithoutProperties, propertyLength);
        int encodedLength = encodedPacketLength(remainingLength);
        while (encodedLength > maximumPacketSize) {

            omittedProperties++;
            propertyLength = propertyLength(msg, propertyLength, omittedProperties);

            if (propertyLength == -1) {
                break;
            }

            remainingLength = remainingLength(msg, remainingLengthWithoutProperties, propertyLength);
            encodedLength = encodedPacketLength(remainingLength);
        }

        msg.setEncodedLength(encodedLength);
        msg.setOmittedProperties(omittedProperties);
        msg.setRemainingLength(remainingLength);
        msg.setPropertyLength(propertyLength);

        return encodedLength;
    }

    private static long calculateMaxMessageSize(final @NotNull ClientConnection clientConnection) {
        Preconditions.checkNotNull(clientConnection, "A ClientConnection must never be null");
        final Long maxMessageSize = clientConnection.getMaxPacketSizeSend();
        return Objects.requireNonNullElse(maxMessageSize, (long) MAXIMUM_PACKET_SIZE_LIMIT);
    }

    abstract void encode(@NotNull T message, @NotNull ByteBuf out);

    public int remainingLength(final @NotNull T message, final int remainingLengthWithoutProperties, final int propertyLength) {
        return remainingLengthWithoutProperties + encodedPropertyLengthWithHeader(message, propertyLength);
    }

    /**
     * Calculates the remaining length byte count without the properties of the MQTT message.
     *
     * @return the remaining length without the properties of the MQTT message.
     */
    abstract int calculateRemainingLengthWithoutProperties(@NotNull T message);

    /**
     * Calculates the property length byte count of the MQTT message.
     *
     * @return the property length of the MQTT message.
     */
    abstract int calculatePropertyLength(@NotNull T message);

    int propertyLength(final @NotNull T message, final int propertyLength, final int omittedProperties) {
        switch (omittedProperties) {
            case 0:
                return propertyLength;
            case 1:
                return propertyLength - getUserProperties(message).encodedLength();
            default:
                return -1;
        }
    }

    /**
     * Calculates the encoded property length with a prefixed header.
     *
     * @param propertyLength the encoded property length.
     * @return the encoded property length with a prefixed header.
     */
    int encodedPropertyLengthWithHeader(final @NotNull T message, final int propertyLength) {
        return encodedLengthWithHeader(propertyLength);
    }

    /**
     * @return the length of the omissible properties of the MQTT message.
     */
    int omissiblePropertiesLength(final @NotNull T message) {
        return getUserProperties(message).encodedLength();
    }

    /**
     * Encodes the omissible properties of the MQTT message if they must not be omitted due to the given maximum packet
     * size.
     *
     * @param out the byte buffer to encode to.
     */
    void encodeOmissibleProperties(final @NotNull T message, final @NotNull ByteBuf out) {
        if (message.getOmittedProperties() == 0) {
            getUserProperties(message).encode(out);
        }
    }

    abstract @NotNull Mqtt5UserProperties getUserProperties(@NotNull T message);

    /**
     * Base class for encoders of MQTT messages with an omissible Reason String and omissible User Properties.
     */
    abstract static class Mqtt5MessageWithReasonStringEncoder<M extends MqttMessageWithUserProperties.MqttMessageWithReasonString>
            extends Mqtt5MessageWithUserPropertiesEncoder<M> {

        Mqtt5MessageWithReasonStringEncoder(
                final @NotNull MessageDroppedService messageDroppedService,
                final @NotNull SecurityConfigurationService securityConfigurationService) {
            super(messageDroppedService, securityConfigurationService);
        }

        @Override
        int propertyLength(final @NotNull M message, final int propertyLength, final int omittedProperties) {
            switch (omittedProperties) {
                case 0:
                    return propertyLength;
                case 1:
                    return propertyLength - reasonStringLength(message);
                case 2:
                    return propertyLength - getUserProperties(message).encodedLength();
                default:
                    return -1;
            }
        }

        final int omissiblePropertiesLength(final @NotNull M message) {
            return reasonStringLength(message) + getUserProperties(message).encodedLength();
        }

        @Override
        void encodeOmissibleProperties(final @NotNull M message, final @NotNull ByteBuf out) {

            if (message.getOmittedProperties() == 0) {
                encodeNullableProperty(REASON_STRING, message.getReasonString(), out);
            }
            if (message.getOmittedProperties() <= 1) {
                getUserProperties(message).encode(out);
            }
        }

        private int reasonStringLength(final @NotNull M message) {
            return nullablePropertyEncodedLength(message.getReasonString());
        }

        @Override
        @NotNull Mqtt5UserProperties getUserProperties(final @NotNull M message) {
            return message.getUserProperties();
        }
    }

    /**
     * Base class for encoders of MQTT messages with an omissible Reason Code, an omissible Reason String and omissible
     * User Properties. The Reason Code is omitted if it is the default and the property length is 0.
     */
    abstract static class Mqtt5MessageWithOmissibleReasonCodeEncoder<M extends MqttMessageWithUserProperties.MqttMessageWithReasonCode<R>, R extends Mqtt5ReasonCode>
            extends Mqtt5MessageWithReasonStringEncoder<M> {

        Mqtt5MessageWithOmissibleReasonCodeEncoder(
                final @NotNull MessageDroppedService messageDroppedService,
                final @NotNull SecurityConfigurationService securityConfigurationService) {
            super(messageDroppedService, securityConfigurationService);
        }

        abstract int getFixedHeader();

        abstract @NotNull R getDefaultReasonCode();

        @Override
        final int calculateRemainingLengthWithoutProperties(final @NotNull M message) {
            return 1 + additionalRemainingLength(message); // reason code (1)
        }

        int additionalRemainingLength(final @NotNull M message) {
            return 0;
        }

        @Override
        final int calculatePropertyLength(final @NotNull M message) {
            return omissiblePropertiesLength(message) + additionalPropertyLength(message);
        }

        int additionalPropertyLength(final @NotNull M message) {
            return 0;
        }

        @Override
        void encode(final @NotNull M message, final @NotNull ByteBuf out) {
            encodeFixedHeader(out, message.getRemainingLength());
            encodeVariableHeader(message, out);
        }

        private void encodeFixedHeader(final @NotNull ByteBuf out, final int remainingLength) {
            out.writeByte(getFixedHeader());
            MqttVariableByteInteger.encode(remainingLength, out);
        }

        private void encodeVariableHeader(final @NotNull M message, final @NotNull ByteBuf out) {
            encodeAdditionalVariableHeader(message, out);
            final R reasonCode = message.getReasonCode();
            if (message.getPropertyLength() == 0) {
                if (reasonCode != getDefaultReasonCode()) {
                    out.writeByte(reasonCode.getCode());
                }
            } else {
                out.writeByte(reasonCode.getCode());
                MqttVariableByteInteger.encode(message.getPropertyLength(), out);
                encodeAdditionalProperties(message, out);
                encodeOmissibleProperties(message, out);
            }
        }

        void encodeAdditionalVariableHeader(final @NotNull M message, final @NotNull ByteBuf out) {
        }

        void encodeAdditionalProperties(final @NotNull M message, final @NotNull ByteBuf out) {
        }

        @Override
        final int encodedPropertyLengthWithHeader(final @NotNull M message, final int propertyLength) {
            if (propertyLength == 0) {
                return (message.getReasonCode() == getDefaultReasonCode()) ? -1 : 0;
            }
            return super.encodedPropertyLengthWithHeader(message, propertyLength);
        }
    }

    /**
     * Base class for encoders of MQTT messages with an Packet Identifier, an omissible Reason Code, an omissible Reason
     * String and omissible User Properties. The Reason Code is omitted if it is the default and the property length is
     * 0.
     */
    abstract static class Mqtt5MessageWithIdAndOmissibleReasonCodeEncoder<M extends MqttMessageWithUserProperties.MqttMessageWithIdAndReasonCode<R>, R extends Mqtt5ReasonCode>
            extends Mqtt5MessageWithOmissibleReasonCodeEncoder<M, R> {

        Mqtt5MessageWithIdAndOmissibleReasonCodeEncoder(
                final @NotNull MessageDroppedService messageDroppedService,
                final @NotNull SecurityConfigurationService securityConfigurationService) {
            super(messageDroppedService, securityConfigurationService);
        }

        @Override
        int additionalRemainingLength(final @NotNull M message) {
            return 2; // packet identifier (2)
        }

        @Override
        void encodeAdditionalVariableHeader(final @NotNull M message, final @NotNull ByteBuf out) {
            out.writeShort(message.getPacketIdentifier());
        }
    }
}
