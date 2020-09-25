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

import com.google.inject.Inject;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.codec.decoder.mqtt3.Mqtt311ConnectDecoder;
import com.hivemq.codec.decoder.mqtt3.Mqtt31ConnectDecoder;
import com.hivemq.codec.decoder.mqtt5.Mqtt5ConnectDecoder;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ClientIds;
import com.hivemq.util.ReasonStrings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

/**
 * The MQTT 'parent' decoder which decides to which actual MQTT decoder the message is delegated to
 *
 * @author Dominik Obermaier
 */
@LazySingleton
public class MqttConnectDecoder {

    private final @NotNull Mqtt5ConnectDecoder mqtt5ConnectDecoder;
    private final @NotNull Mqtt311ConnectDecoder mqtt311ConnectDecoder;
    private final @NotNull Mqtt31ConnectDecoder mqtt31ConnectDecoder;

    private final @NotNull MqttConnacker mqttConnacker;

    @Inject
    public MqttConnectDecoder(final @NotNull MqttConnacker mqttConnacker,
                              final @NotNull FullConfigurationService fullConfigurationService,
                              final @NotNull HivemqId hiveMQId,
                              final @NotNull ClientIds clientIds) {
        this.mqttConnacker = mqttConnacker;
        this.mqtt5ConnectDecoder = new Mqtt5ConnectDecoder(mqttConnacker, hiveMQId, clientIds, fullConfigurationService);
        this.mqtt311ConnectDecoder = new Mqtt311ConnectDecoder(mqttConnacker, clientIds, fullConfigurationService, hiveMQId);
        this.mqtt31ConnectDecoder = new Mqtt31ConnectDecoder(mqttConnacker, clientIds, fullConfigurationService, hiveMQId);
    }

    public @Nullable CONNECT decode(final @NotNull Channel channel, final @NotNull ByteBuf buf, final byte fixedHeader) {

        /*
         * It is sufficient to look at the second byte of the variable header (Length LSB) This byte
         * indicates how long the following protocol name is going to be. In case of the
         * MQTT 3.1 specification the name is 'MQIsdp' which is 6 bytes long.
         *
         * The MQTT 3.1.1 spec has a defined protocol name of "MQTT" which is 4 bytes long.
         *
         * For future protocol versions we definitely need to inspect the protocol version additionally
         * decide since the protocol name is not going to change.
         */

        //The reader index is now at the beginning of the variable MQTT header field. We're only
        // interested in the Length LSB byte
        if (buf.readableBytes() < 2) {
            mqttConnacker.connackError(channel,
                    "A client (IP: {}) connected with a packet without protocol version.",
                    "Sent CONNECT without protocol version",
                    Mqtt5ConnAckReasonCode.UNSUPPORTED_PROTOCOL_VERSION,
                    ReasonStrings.CONNACK_UNSUPPORTED_PROTOCOL_VERSION);
            return null;
        }

        final ByteBuf lengthLSBBuf = buf.slice(buf.readerIndex() + 1, 1);

        final int lengthLSB = lengthLSBBuf.readByte();

        final ProtocolVersion protocolVersion;
        switch (lengthLSB) {
            case 4:
                if (buf.readableBytes() < 7) {
                    connackInvalidProtocolVersion(channel);
                    return null;
                }
                final ByteBuf protocolVersionBuf = buf.slice(buf.readerIndex() + 6, 1);
                final byte versionByte = protocolVersionBuf.readByte();
                if (versionByte == 5) {
                    protocolVersion = ProtocolVersion.MQTTv5;
                } else if (versionByte == 4) {
                    protocolVersion = ProtocolVersion.MQTTv3_1_1;
                } else {
                    connackInvalidProtocolVersion(channel);
                    return null;
                }
                break;
            case 6:
                protocolVersion = ProtocolVersion.MQTTv3_1;
                break;
            default:
                connackInvalidProtocolVersion(channel);
                return null;
        }

        channel.attr(ChannelAttributes.MQTT_VERSION).set(protocolVersion);
        channel.attr(ChannelAttributes.CONNECT_RECEIVED_TIMESTAMP).set(System.currentTimeMillis());

        if (protocolVersion == ProtocolVersion.MQTTv5) {
            return mqtt5ConnectDecoder.decode(channel, buf, fixedHeader);
        } else if (protocolVersion == ProtocolVersion.MQTTv3_1_1) {
            return mqtt311ConnectDecoder.decode(channel, buf, fixedHeader);
        } else {
            return mqtt31ConnectDecoder.decode(channel, buf, fixedHeader);
        }

    }

    private void connackInvalidProtocolVersion(final @NotNull Channel channel) {
        mqttConnacker.connackError(channel,
                "A client (IP: {}) connected with an invalid protocol version.",
                "Sent CONNECT with an invalid protocol version",
                Mqtt5ConnAckReasonCode.UNSUPPORTED_PROTOCOL_VERSION,
                ReasonStrings.CONNACK_UNSUPPORTED_PROTOCOL_VERSION);
    }
}
