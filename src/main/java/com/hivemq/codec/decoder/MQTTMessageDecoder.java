/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
import com.hivemq.extensions.events.OnServerDisconnectEvent;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.hivemq.mqtt.message.MessageType.CONNECT;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET;
import static com.hivemq.util.ChannelUtils.getChannelIP;

/**
 * @author Dominik Obermaier
 */
public class MQTTMessageDecoder extends ByteToMessageDecoder {

    private static final Logger log = LoggerFactory.getLogger(MQTTMessageDecoder.class);

    private static final int MAX_REMAINING_LENGTH_MULTIPLIER = 0x80 * 0x80 * 0x80;
    private static final int NOT_ENOUGH_BYTES_READABLE = -2;
    private static final int MALFORMED_REMAINING_LENGTH = -1;
    private static final int MIN_FIXED_HEADER_LENGTH = 2;

    private final MqttConnectDecoder connectDecoder;

    private final MqttConfigurationService mqttConfig;
    private final EventLog eventLog;
    private final MqttDecoders mqttDecoders;

    private final boolean strict;

    public MQTTMessageDecoder(final MqttConnectDecoder connectDecoder,
                              final boolean strict,
                              final MqttConfigurationService mqttConfig,
                              final EventLog eventLog,
                              final MqttDecoders mqttDecoders) {
        this.connectDecoder = connectDecoder;
        this.mqttConfig = mqttConfig;

        this.strict = strict;
        this.eventLog = eventLog;
        this.mqttDecoders = mqttDecoders;
    }

    public MQTTMessageDecoder(final ChannelDependencies channelDependencies) {
        this(channelDependencies.getMqttConnectDecoder(),
                true,
                channelDependencies.getConfigurationService().mqttConfiguration(),
                channelDependencies.getEventLog(),
                channelDependencies.getMqttDecoders());
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf buf, final List<Object> out) {
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

        if (remainingLength == MALFORMED_REMAINING_LENGTH) {
            if (log.isDebugEnabled()) {
                log.debug("A client (IP: {}) sent a message but the remaining length was malformed. Disconnecting client", getChannelIP(ctx.channel()).or("UNKNOWN"));
            }
            eventLog.clientWasDisconnected(ctx.channel(), "Sent a message with invalid remaining length");
            ctx.close();
            buf.clear();
            return;
        }

        if (buf.readableBytes() < remainingLength) {
            buf.resetReaderIndex();
            return;
        }

        final int fixedHeaderSize = getFixedHeaderSize(remainingLength);

        final ProtocolVersion protocolVersion = ctx.channel().attr(ChannelAttributes.MQTT_VERSION).get();
        //this is the message size HiveMQ allows for incoming messages
        if (remainingLength + fixedHeaderSize > mqttConfig.maxPacketSize()) {
            if (log.isDebugEnabled()) {
                log.debug("A client (IP: {}) sent a message, that was bigger than the maximum message size. Disconnecting client", getChannelIP(ctx.channel()).or("UNKNOWN"));
            }
            eventLog.clientWasDisconnected(ctx.channel(), "Sent a message that was bigger than the maximum size");
            buf.clear();
            if (protocolVersion == ProtocolVersion.MQTTv5) {
                final DISCONNECT disconnect = new DISCONNECT(Mqtt5DisconnectReasonCode.PACKET_TOO_LARGE, null, Mqtt5UserProperties.NO_USER_PROPERTIES,
                        null, SESSION_EXPIRY_NOT_SET);
                if (ctx.channel().attr(ChannelAttributes.PLUGIN_DISCONNECT_EVENT_SENT).getAndSet(true) == null) {
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

        if (strict && protocolVersion == null && messageType != CONNECT) {
            if (log.isDebugEnabled()) {
                log.debug("A client (IP: {}) sent other message before CONNECT. Disconnecting client", getChannelIP(ctx.channel()).or("UNKNOWN"));
            }
            eventLog.clientWasDisconnected(ctx.channel(), "Sent other message before CONNECT");
            ctx.close();
            buf.clear();
            return;
        }

        switch (messageType) {
            case RESERVED_ZERO:
                if (log.isDebugEnabled()) {
                    log.debug("A client (IP: {}) sent a message with an invalid message type '0'. This message type is reserved. Disconnecting client", getChannelIP(ctx.channel()).or("UNKNOWN"));
                }
                eventLog.clientWasDisconnected(ctx.channel(), "Sent a message with message type '0'");
                ctx.close();
                buf.clear();
                return;
            case CONNECT:
                message = connectDecoder.decode(ctx.channel(), messageBuffer, fixedHeader);
                break;
            case CONNACK:
                if (strict) {
                    if (log.isDebugEnabled()) {
                        log.debug("A client (IP: {}) sent a CONNACK message. This is invalid because clients are not allowed to send CONNACKs. Disconnecting clients", getChannelIP(ctx.channel()).or("UNKNOWN"));
                    }
                    eventLog.clientWasDisconnected(ctx.channel(), "Sent a CONNACK message");
                    ctx.close();
                    buf.clear();
                    return;
                } else {
                    message = mqttDecoders.decoder(CONNACK.class, protocolVersion).decode(ctx.channel(), messageBuffer, fixedHeader);
                    break;
                }
            case PUBLISH:
                message = mqttDecoders.decoder(PUBLISH.class, protocolVersion).decode(ctx.channel(), messageBuffer, fixedHeader);
                break;
            case PUBACK:
                message = mqttDecoders.decoder(PUBACK.class, protocolVersion).decode(ctx.channel(), messageBuffer, fixedHeader);
                break;
            case PUBREC:
                message = mqttDecoders.decoder(PUBREC.class, protocolVersion).decode(ctx.channel(), messageBuffer, fixedHeader);
                break;
            case PUBREL:
                message = mqttDecoders.decoder(PUBREL.class, protocolVersion).decode(ctx.channel(), messageBuffer, fixedHeader);
                break;
            case PUBCOMP:
                message = mqttDecoders.decoder(PUBCOMP.class, protocolVersion).decode(ctx.channel(), messageBuffer, fixedHeader);
                break;
            case SUBSCRIBE:
                message = mqttDecoders.decoder(SUBSCRIBE.class, protocolVersion).decode(ctx.channel(), messageBuffer, fixedHeader);
                break;
            case SUBACK:
                if (strict) {
                    if (log.isDebugEnabled()) {
                        log.debug("A client (IP: {}) sent a SUBACK message. This is invalid because clients are not allowed to send SUBACKs. Disconnecting client", getChannelIP(ctx.channel()).or("UNKNOWN"));
                    }
                    eventLog.clientWasDisconnected(ctx.channel(), "Sent a SUBACK message");
                    ctx.close();
                    buf.clear();
                    return;
                } else {
                    message = mqttDecoders.decoder(SUBACK.class, protocolVersion).decode(ctx.channel(), messageBuffer, fixedHeader);
                    break;
                }
            case UNSUBSCRIBE:
                message = mqttDecoders.decoder(UNSUBSCRIBE.class, protocolVersion).decode(ctx.channel(), messageBuffer, fixedHeader);
                break;
            case UNSUBACK:
                if (strict) {
                    if (log.isDebugEnabled()) {
                        log.debug("A client (IP: {}) sent a UNSUBACK message. This is invalid because clients are not allowed to send UNSUBACKs. Disconnecting client", getChannelIP(ctx.channel()).or("UNKNOWN"));
                    }
                    eventLog.clientWasDisconnected(ctx.channel(), "Sent a UNSUBACK message");
                    ctx.close();
                    buf.clear();
                    return;
                } else {
                    message = mqttDecoders.decoder(UNSUBACK.class, protocolVersion).decode(ctx.channel(), messageBuffer, fixedHeader);
                    break;
                }
            case PINGREQ:
                message = mqttDecoders.decoder(PINGREQ.class, protocolVersion).decode(ctx.channel(), messageBuffer, fixedHeader);
                break;
            case PINGRESP:
                if (log.isDebugEnabled()) {
                    log.debug("A client (IP: {}) sent a PINGRESP message. This is invalid because clients are not allowed to send PINGRESPs. Disconnecting client", getChannelIP(ctx.channel()).or("UNKNOWN"));
                }
                eventLog.clientWasDisconnected(ctx.channel(), "Sent a PINGRESP message");
                ctx.close();
                buf.clear();
                return;
            case DISCONNECT:
                message = mqttDecoders.decoder(DISCONNECT.class, protocolVersion).decode(ctx.channel(), messageBuffer, fixedHeader);
                break;
            case AUTH:
                if (ProtocolVersion.MQTTv5 == protocolVersion) {
                    message = mqttDecoders.decoder(AUTH.class, protocolVersion).decode(ctx.channel(), messageBuffer, fixedHeader);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("A client (IP: {}) connected with an invalid message type '15'. This message type is reserved. Disconnecting client", getChannelIP(ctx.channel()).or("UNKNOWN"));
                    }
                    eventLog.clientWasDisconnected(ctx.channel(), "Sent a message with reserved type '15'");
                    ctx.close();
                    buf.clear();
                    return;
                }
                break;
            default:
                if (log.isDebugEnabled()) {
                    log.debug("A client (IP: {}) connected but the message type could not get determined. Disconnecting client", getChannelIP(ctx.channel()).or("UNKNOWN"));
                }
                eventLog.clientWasDisconnected(ctx.channel(), "Sent a message with invalid message type");
                ctx.close();
                buf.clear();
                return;
        }

        if (message == null) {
            buf.clear();
            return;
        }

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
    private int calculateRemainingLength(final ByteBuf buf) {

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
