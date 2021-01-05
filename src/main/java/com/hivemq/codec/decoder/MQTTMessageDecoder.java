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

import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.events.OnServerDisconnectEvent;
import com.hivemq.metrics.handler.GlobalMQTTMessageCounter;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ReasonStrings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import static com.hivemq.mqtt.message.MessageType.CONNECT;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET;

/**
 * @author Dominik Obermaier
 */
public class MQTTMessageDecoder extends ByteToMessageDecoder {

    private static final int MAX_REMAINING_LENGTH_MULTIPLIER = 0x80 * 0x80 * 0x80;
    private static final int NOT_ENOUGH_BYTES_READABLE = -2;
    private static final int MALFORMED_REMAINING_LENGTH = -1;
    private static final int MIN_FIXED_HEADER_LENGTH = 2;

    private final @NotNull MqttConnectDecoder connectDecoder;
    private final @NotNull MqttConfigurationService mqttConfig;
    private final @NotNull MqttDecoders mqttDecoders;
    private final @NotNull MqttServerDisconnector mqttServerDisconnector;
    private final @NotNull GlobalMQTTMessageCounter globalMQTTMessageCounter;

    public MQTTMessageDecoder(final @NotNull MqttConnectDecoder connectDecoder,
                              final @NotNull MqttConfigurationService mqttConfig,
                              final @NotNull MqttDecoders mqttDecoders,
                              final @NotNull MqttServerDisconnector mqttServerDisconnector,
                              final @NotNull GlobalMQTTMessageCounter globalMQTTMessageCounter) {
        this.connectDecoder = connectDecoder;
        this.mqttConfig = mqttConfig;
        this.mqttDecoders = mqttDecoders;
        this.mqttServerDisconnector = mqttServerDisconnector;
        this.globalMQTTMessageCounter = globalMQTTMessageCounter;
    }

    public MQTTMessageDecoder(final ChannelDependencies channelDependencies) {
        this(channelDependencies.getMqttConnectDecoder(),
                channelDependencies.getConfigurationService().mqttConfiguration(),
                channelDependencies.getMqttDecoders(),
                channelDependencies.getMqttServerDisconnector(),
                channelDependencies.getGlobalMQTTMessageCounter());
    }

    @Override
    protected void decode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf buf, final @NotNull List<Object> out) {
        final int readableBytes = buf.readableBytes();
        if (readableBytes < MIN_FIXED_HEADER_LENGTH) {
            return;
        }

        buf.markReaderIndex();

        final byte fixedHeader = buf.readByte();

        final int remainingLength = calculateRemainingLength(buf);

        if (remainingLength == NOT_ENOUGH_BYTES_READABLE) {
            buf.resetReaderIndex();
            return;
        }

        final Channel channel = ctx.channel();
        if (remainingLength == MALFORMED_REMAINING_LENGTH) {
            mqttServerDisconnector.disconnect(channel,
                    "A client (IP: {}) sent a message but the remaining length was malformed. Disconnecting client.",
                    "Sent a message with invalid remaining length",
                    Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                    ReasonStrings.CLOSE_MALFORMED_REMAINING_LENGTH,
                    Mqtt5UserProperties.NO_USER_PROPERTIES,
                    false,
                    true);
            buf.clear();
            return;
        }

        if (buf.readableBytes() < remainingLength) {
            buf.resetReaderIndex();
            return;
        }

        final int fixedHeaderSize = getFixedHeaderSize(remainingLength);

        final ProtocolVersion protocolVersion = channel.attr(ChannelAttributes.MQTT_VERSION).get();
        //this is the message size HiveMQ allows for incoming messages
        if (remainingLength + fixedHeaderSize > mqttConfig.maxPacketSize()) {

            //force channel close for Mqtt3.1, Mqtt3.1.1 and null (before connect)
            final boolean forceClose = protocolVersion != ProtocolVersion.MQTTv5;
            mqttServerDisconnector.disconnect(channel,
                    "A client (IP: {}) sent a message, that was bigger than the maximum message size. Disconnecting client.",
                    "Sent a message that was bigger than the maximum size",
                    Mqtt5DisconnectReasonCode.PACKET_TOO_LARGE,
                    ReasonStrings.DISCONNECT_PACKET_TOO_LARGE_MESSAGE,
                    Mqtt5UserProperties.NO_USER_PROPERTIES,
                    false,
                    forceClose);
            buf.clear();
            if (protocolVersion == ProtocolVersion.MQTTv5) {
                final DISCONNECT disconnect = new DISCONNECT(Mqtt5DisconnectReasonCode.PACKET_TOO_LARGE, null, Mqtt5UserProperties.NO_USER_PROPERTIES,
                        null, SESSION_EXPIRY_NOT_SET);
                if (ctx.channel().attr(ChannelAttributes.EXTENSION_DISCONNECT_EVENT_SENT).getAndSet(true) == null) {
                    ctx.channel().pipeline().fireUserEventTriggered(new OnServerDisconnectEvent(disconnect));
                }
                ctx.channel().writeAndFlush(disconnect)
                        .addListener(ChannelFutureListener.CLOSE);
            } else {
                ctx.close();
            }

            return;
        }

        final Message message;
        //We're slicing the buffer to the exact MQTT message size so we don't have to pass the actual length around
        final ByteBuf messageBuffer = buf.readSlice(remainingLength);
        //We mark the end of the message
        buf.markReaderIndex();

        final MessageType messageType = getMessageType(fixedHeader);

        if (protocolVersion == null && messageType != CONNECT) {
            mqttServerDisconnector.logAndClose(channel,
                    "A client (IP: {}) sent other message before CONNECT. Disconnecting client.",
                    "Sent other message before CONNECT");
            buf.clear();
            return;
        }

        if (protocolVersion != null && messageType == CONNECT) {
            mqttServerDisconnector.logAndClose(channel,
                    "A client (IP: {}) sent second CONNECT message. This is not allowed. Disconnecting client.",
                    "Sent second CONNECT message");
            buf.clear();
            return;
        }

        globalMQTTMessageCounter.countInboundTraffic(readableBytes);

        if (messageType == CONNECT) {
            message = connectDecoder.decode(channel, messageBuffer, fixedHeader);
        } else {
            final MqttDecoder<?> decoder = mqttDecoders.decoder(messageType, protocolVersion);

            if (decoder != null) {
                message = decoder.decode(channel, messageBuffer, fixedHeader);
            } else {
                switch (messageType) {
                    case RESERVED_ZERO:
                        mqttServerDisconnector.disconnect(channel,
                                "A client (IP: {}) sent a message with an invalid message type '0'. This message type is reserved. Disconnecting client.",
                                "Sent a message with message type '0'",
                                Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                                ReasonStrings.DISCONNECT_MESSAGE_TYPE_ZERO);
                        buf.clear();
                        return;
                    case CONNACK:
                        mqttServerDisconnector.disconnect(channel,
                                "A client (IP: {}) sent a CONNACK message. This is invalid because clients are not allowed to send CONNACKs. Disconnecting client.",
                                "Sent a CONNACK message",
                                Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                                ReasonStrings.DISCONNECT_CONNACK_RECEIVED);
                        buf.clear();
                        return;
                    case SUBACK:
                        mqttServerDisconnector.disconnect(channel,
                                "A client (IP: {}) sent a SUBACK message. This is invalid because clients are not allowed to send SUBACKs. Disconnecting client.",
                                "Sent a SUBACK message",
                                Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                                ReasonStrings.DISCONNECT_SUBACK_RECEIVED);
                        buf.clear();
                        return;
                    case UNSUBACK:
                        mqttServerDisconnector.disconnect(channel,
                                "A client (IP: {}) sent a UNSUBACK message. This is invalid because clients are not allowed to send UNSUBACKs. Disconnecting client.",
                                "Sent a UNSUBACK message",
                                Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                                ReasonStrings.DISCONNECT_UNSUBACK_RECEIVED);
                        buf.clear();
                        return;
                    case PINGRESP:
                        mqttServerDisconnector.disconnect(channel,
                                "A client (IP: {}) sent a PINGRESP message. This is invalid because clients are not allowed to send PINGRESPs. Disconnecting client.",
                                "Sent a PINGRESP message",
                                Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                                ReasonStrings.DISCONNECT_PINGRESP_RECEIVED);
                        buf.clear();
                        return;
                    case AUTH:
                        mqttServerDisconnector.disconnect(channel,
                                "A client (IP: {}) sent a message with an invalid message type '15'. This message type is reserved. Disconnecting client.",
                                "Sent a message with message type '15'",
                                Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                                ReasonStrings.DISCONNECT_MESSAGE_TYPE_FIFTEEN);
                        buf.clear();
                        return;
                    default:
                        mqttServerDisconnector.disconnect(channel,
                                "A client (IP: {}) connected but the message type could not get determined. Disconnecting client.",
                                "Sent a message with invalid message type",
                                Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                                ReasonStrings.DISCONNECT_MESSAGE_TYPE_INVALID);
                        buf.clear();
                        return;
                }
            }
        }

        if (message == null) {
            buf.clear();
            return;
        }

        globalMQTTMessageCounter.countInbound(message);
        out.add(message);

    }

    private int getFixedHeaderSize(final int remainingLength) {

        // 2 = 1 byte fixed header + 1 byte first byte of remaining length
        int remainingLengthSize = 2;

        if (remainingLength > 127) {
            remainingLengthSize++;
        }
        if (remainingLength > 16383) {
            remainingLengthSize++;
        }
        if (remainingLength > 2097151) {
            remainingLengthSize++;
        }
        return remainingLengthSize;
    }

    /**
     * Calculates the remaining length according to the MQTT spec.
     *
     * @param buf the message buffer
     * @return the remaining length, -1 if the remaining length is malformed or -2 if not enough bytes are available
     */
    private int calculateRemainingLength(final @NotNull ByteBuf buf) {

        int remainingLength = 0;
        int multiplier = 1;
        byte encodedByte;

        do {
            if (multiplier > MAX_REMAINING_LENGTH_MULTIPLIER) {
                buf.skipBytes(buf.readableBytes());
                //This means the remaining length is malformed!
                return MALFORMED_REMAINING_LENGTH;
            }

            if (!buf.isReadable()) {
                return NOT_ENOUGH_BYTES_READABLE;
            }

            encodedByte = buf.readByte();

            remainingLength += ((encodedByte & (byte) 0x7f) * multiplier);
            multiplier *= 0x80;

        } while ((encodedByte & 0x80) != 0);

        return remainingLength;
    }

    private MessageType getMessageType(final byte fixedHeader) {
        return MessageType.valueOf((fixedHeader & 0b1111_0000) >> 4);
    }
}
