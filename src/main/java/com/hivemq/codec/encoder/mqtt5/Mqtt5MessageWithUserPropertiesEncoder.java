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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.codec.encoder.FixedSizeMessageEncoder;
import com.hivemq.configuration.service.SecurityConfigurationService;
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
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
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
abstract class Mqtt5MessageWithUserPropertiesEncoder<T extends Message> extends FixedSizeMessageEncoder<T> {

    private static final Logger log = LoggerFactory.getLogger(Mqtt5MessageWithUserPropertiesEncoder.class);
    private final @NotNull MessageDroppedService messageDroppedService;

    // Need the security service here for enabling / disabling configuration on runtime.
    private final @NotNull SecurityConfigurationService securityConfigurationService;

    public Mqtt5MessageWithUserPropertiesEncoder(final @NotNull MessageDroppedService messageDroppedService, final @NotNull SecurityConfigurationService securityConfigurationService) {
        this.messageDroppedService = messageDroppedService;
        this.securityConfigurationService = securityConfigurationService;
    }

    @Override
    public void encode(@NotNull final ChannelHandlerContext ctx, @NotNull final T message, @NotNull final ByteBuf out) {

        Preconditions.checkNotNull(ctx, "ChannelHandlerContext must never be null");
        Preconditions.checkNotNull(message, "Message must never be null");
        Preconditions.checkNotNull(out, "ByteBuf must never be null");

        if (message.getOmittedProperties() > 0) {

            final String clientIdFromChannel = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();
            final String clientId = clientIdFromChannel != null ? clientIdFromChannel : "UNKNOWN";

            final long maximumPacketSize = calculateMaxMessageSize(ctx.channel());

            //PUBLISH must not omit any properties
            if (message instanceof PUBLISH) {
                // The maximal packet size exceeds the clients accepted packet size
                ctx.fireUserEventTriggered(new PublishDroppedEvent((PUBLISH) message));
                messageDroppedService.publishMaxPacketSizeExceeded(clientId, ((PUBLISH) message).getTopic(), ((PUBLISH) message).getQoS().getQosNumber(), maximumPacketSize, message.getEncodedLength());
                log.trace("Could not encode publish message for client ({}): Maximum packet size limit exceeded", clientId);
                return;
            }

            if (message.getPropertyLength() < 0 && message.getEncodedLength() > maximumPacketSize) {
                messageDroppedService.messageMaxPacketSizeExceeded(clientId, message.getType().name(), maximumPacketSize, message.getEncodedLength());
                log.trace("Could not encode message of type {} for client {}: Packet to large", message.getType(), clientId);
                throw new EncoderException("Maximum packet size exceeded");
            }
        }

        encode(message, out);
    }

    @Override
    public int bufferSize(@NotNull final ChannelHandlerContext ctx, final @NotNull T message) {

        int omittedProperties = 0;
        int propertyLength = calculatePropertyLength(message);

        if (!securityConfigurationService.allowRequestProblemInformation() || !Objects.requireNonNullElse(ctx.channel().attr(ChannelAttributes.REQUEST_PROBLEM_INFORMATION).get(), Mqtt5CONNECT.DEFAULT_PROBLEM_INFORMATION_REQUESTED)) {

            //Must omit user properties and reason string for any other packet than PUBLISH, CONNACK, DISCONNECT
            //if no problem information requested.
            final boolean mustOmit = !(message instanceof PUBLISH || message instanceof CONNACK || message instanceof DISCONNECT);

            if (mustOmit) {
                // propertyLength - reason string
                propertyLength = propertyLength(message, propertyLength, ++omittedProperties);
                // propertyLength - user properties
                propertyLength = propertyLength(message, propertyLength, ++omittedProperties);
            }
        }

        final long maximumPacketSize = calculateMaxMessageSize(ctx.channel());
        final int remainingLengthWithoutProperties = calculateRemainingLengthWithoutProperties(message);
        int remainingLength = remainingLength(message, remainingLengthWithoutProperties, propertyLength);
        int encodedLength = encodedPacketLength(remainingLength);
        while (encodedLength > maximumPacketSize) {

            omittedProperties++;
            propertyLength = propertyLength(message, propertyLength, omittedProperties);

            if (propertyLength == -1) {
                break;
            }

            remainingLength = remainingLength(message, remainingLengthWithoutProperties, propertyLength);
            encodedLength = encodedPacketLength(remainingLength);
        }

        message.setEncodedLength(encodedLength);
        message.setOmittedProperties(omittedProperties);
        message.setRemainingLength(remainingLength);
        message.setPropertyLength(propertyLength);

        return encodedLength;
    }

    private @NotNull Long calculateMaxMessageSize(final @NotNull Channel channel) {
        Preconditions.checkNotNull(channel, "A Channel must never be null");
        final Long maxMessageSize = channel.attr(ChannelAttributes.MAX_PACKET_SIZE_SEND).get();
        return Objects.requireNonNullElse(maxMessageSize, (long) MAXIMUM_PACKET_SIZE_LIMIT);
    }

    abstract void encode(@NotNull final T message, @NotNull final ByteBuf out);

    public int remainingLength(final @NotNull T message, final int remainingLengthWithoutProperties, final int propertyLength) {

        return remainingLengthWithoutProperties + encodedPropertyLengthWithHeader(message, propertyLength);
    }

    /**
     * Calculates the remaining length byte count without the properties of the MQTT message.
     *
     * @return the remaining length without the properties of the MQTT message.
     */
    abstract int calculateRemainingLengthWithoutProperties(@NotNull final T message);

    /**
     * Calculates the property length byte count of the MQTT message.
     *
     * @return the property length of the MQTT message.
     */
    abstract int calculatePropertyLength(@NotNull final T message);

    int propertyLength(@NotNull final T message, final int propertyLength, final int omittedProperties) {
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
    int omissiblePropertiesLength(@NotNull final T message) {
        return getUserProperties(message).encodedLength();
    }

    /**
     * Encodes the omissible properties of the MQTT message if they must not be omitted due to the given maximum packet
     * size.
     *
     * @param out the byte buffer to encode to.
     */
    void encodeOmissibleProperties(@NotNull final T message, @NotNull final ByteBuf out) {
        if (message.getOmittedProperties() == 0) {
            getUserProperties(message).encode(out);
        }
    }

    abstract @NotNull Mqtt5UserProperties getUserProperties(@NotNull final T message);


    /**
     * Base class for encoders of MQTT messages with an omissible Reason String and omissible User Properties.
     */
    static abstract class Mqtt5MessageWithReasonStringEncoder<M extends MqttMessageWithUserProperties.MqttMessageWithReasonString>
            extends Mqtt5MessageWithUserPropertiesEncoder<M> {

        Mqtt5MessageWithReasonStringEncoder(final @NotNull MessageDroppedService messageDroppedService, final @NotNull SecurityConfigurationService securityConfigurationService) {
            super(messageDroppedService, securityConfigurationService);
        }

        @Override
        int propertyLength(@NotNull final M message, final int propertyLength, final int omittedProperties) {
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

        final int omissiblePropertiesLength(@NotNull final M message) {
            return reasonStringLength(message) + getUserProperties(message).encodedLength();
        }

        @Override
        void encodeOmissibleProperties(@NotNull final M message, @NotNull final ByteBuf out) {

            if (message.getOmittedProperties() == 0) {
                encodeNullableProperty(REASON_STRING, message.getReasonString(), out);
            }
            if (message.getOmittedProperties() <= 1) {
                getUserProperties(message).encode(out);
            }
        }

        private int reasonStringLength(@NotNull final M message) {
            return nullablePropertyEncodedLength(message.getReasonString());
        }

        @Override
        @NotNull Mqtt5UserProperties getUserProperties(@NotNull final M message) {
            return message.getUserProperties();
        }

    }


    /**
     * Base class for encoders of MQTT messages with an omissible Reason Code, an omissible Reason String and omissible
     * User Properties. The Reason Code is omitted if it is the default and the property length is 0.
     */
    static abstract class Mqtt5MessageWithOmissibleReasonCodeEncoder<M extends MqttMessageWithUserProperties.MqttMessageWithReasonCode<R>, R extends Mqtt5ReasonCode>
            extends Mqtt5MessageWithReasonStringEncoder<M> {

        Mqtt5MessageWithOmissibleReasonCodeEncoder(final @NotNull MessageDroppedService messageDroppedService,
                                                   final @NotNull SecurityConfigurationService securityConfigurationService) {
            super(messageDroppedService, securityConfigurationService);
        }

        abstract int getFixedHeader();

        abstract @NotNull R getDefaultReasonCode();

        @Override
        final int calculateRemainingLengthWithoutProperties(@NotNull final M message) {
            return 1 + additionalRemainingLength(message); // reason code (1)
        }

        int additionalRemainingLength(@NotNull final M message) {
            return 0;
        }

        @Override
        final int calculatePropertyLength(@NotNull final M message) {
            return omissiblePropertiesLength(message) + additionalPropertyLength(message);
        }

        int additionalPropertyLength(@NotNull final M message) {
            return 0;
        }

        @Override
        protected void encode(
                @NotNull final M message, @NotNull final ByteBuf out) {

            encodeFixedHeader(out, message.getRemainingLength());
            encodeVariableHeader(message, out);
        }

        private void encodeFixedHeader(@NotNull final ByteBuf out, final int remainingLength) {
            out.writeByte(getFixedHeader());
            MqttVariableByteInteger.encode(remainingLength, out);
        }

        private void encodeVariableHeader(
                @NotNull final M message, @NotNull final ByteBuf out) {

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

        void encodeAdditionalVariableHeader(@NotNull final M message, @NotNull final ByteBuf out) {
        }

        void encodeAdditionalProperties(@NotNull final M message, @NotNull final ByteBuf out) {
        }

        @Override
        final int encodedPropertyLengthWithHeader(@NotNull final M message, final int propertyLength) {
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
    static abstract class Mqtt5MessageWithIdAndOmissibleReasonCodeEncoder<M extends MqttMessageWithUserProperties.MqttMessageWithIdAndReasonCode<R>, R extends Mqtt5ReasonCode>
            extends Mqtt5MessageWithOmissibleReasonCodeEncoder<M, R> {

        Mqtt5MessageWithIdAndOmissibleReasonCodeEncoder(final @NotNull MessageDroppedService messageDroppedService, final @NotNull SecurityConfigurationService securityConfigurationService) {
            super(messageDroppedService, securityConfigurationService);
        }

        @Override
        int additionalRemainingLength(@NotNull final M message) {
            return 2; // packet identifier (2)
        }

        @Override
        void encodeAdditionalVariableHeader(@NotNull final M message, @NotNull final ByteBuf out) {
            out.writeShort(message.getPacketIdentifier());
        }

    }

}
