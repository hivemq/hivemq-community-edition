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
package com.hivemq.codec.decoder.mqtt3;

import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.codec.decoder.AbstractMqttConnectDecoder;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import static com.hivemq.util.Bytes.isBitSet;

/**
 * This is the MQTT CONNECT decoder for MQTT 3.1.1 messages. The general strategy of the decoder is
 * to disconnect an invalid client as fast as possible. That means, the actual object creation of the CONNECT
 * message is deferred until all protocol checks have passed successfully in order to be as efficient as possibles
 *
 * @author Dominik Obermaier
 */
@LazySingleton
public class Mqtt311ConnectDecoder extends AbstractMqttConnectDecoder {

    public static final String PROTOCOL_NAME = "MQTT";
    private final @NotNull HivemqId hiveMQId;

    public Mqtt311ConnectDecoder(final @NotNull MqttConnacker connacker,
                                 final @NotNull ClientIds clientIds,
                                 final @NotNull FullConfigurationService fullConfigurationService,
                                 final @NotNull HivemqId hiveMQId) {
        super(connacker, fullConfigurationService, clientIds);
        this.hiveMQId = hiveMQId;
    }

    @Nullable
    @Override
    public CONNECT decode(final @NotNull Channel channel, final @NotNull ByteBuf buf, final byte header) {


        if (!validateHeader(header)) {
            disconnectByInvalidFixedHeader(channel);
            return null;
        }

        final ByteBuf connectHeader = decodeFixedVariableHeaderConnect(channel, buf);
        if (connectHeader == null) {
            return null;
        }

        if (!validateProtocolName(connectHeader, channel, PROTOCOL_NAME)) {
            return null;
        }

        //We don't need to validate the protocol version byte since we already know it's valid, otherwise
        //we wouldn't be in this protocol-version dependant decoder
        connectHeader.readByte();

        final byte connectFlagsByte = connectHeader.readByte();

        if (!validateConnectFlagByte(connectFlagsByte, channel)) {
            return null;
        }

        final boolean isCleanSessionFlag = isBitSet(connectFlagsByte, 1);
        final boolean isWillFlag = isBitSet(connectFlagsByte, 2);
        final boolean isWillRetain = isBitSet(connectFlagsByte, 5);
        final boolean isPasswordFlag = isBitSet(connectFlagsByte, 6);
        final boolean isUsernameFlag = isBitSet(connectFlagsByte, 7);

        final int willQoS = (connectFlagsByte & 0b0001_1000) >> 3;

        if (!validateWill(isWillFlag, isWillRetain, willQoS, channel)) {
            return null;
        }

        if (!validateUsernamePassword(isUsernameFlag, isPasswordFlag)) {
            mqttConnacker.connackError(channel,
                    "A client (IP: {}) connected with an invalid username/password combination. The password flag was set but the username flag was not set. Disconnecting client.",
                    "Sent a CONNECT with invalid username/password combination",
                    Mqtt5ConnAckReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.CONNACK_PROTOCOL_ERROR_INVALID_USER_PASS_COMB_MQTT3);
            return null;
        }

        final int keepAlive = connectHeader.readUnsignedShort();

        final int utf8StringLength;

        if (buf.readableBytes() < 2 || (buf.readableBytes() < (utf8StringLength = buf.readUnsignedShort()) && utf8StringLength > 0)) {
            mqttConnacker.connackError(channel,
                    "A client (IP: {}) sent a CONNECT message with an incorrect client id length. Disconnecting client.",
                    "Sent CONNECT with incorrect client id length",
                    Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                    ReasonStrings.CONNACK_CLIENT_IDENTIFIER_NOT_VALID);
            return null;
        }

        final String clientId;

        if (validateUTF8 && utf8StringLength > 0) {
            clientId = Strings.getValidatedPrefixedString(buf, utf8StringLength, true);
            if (clientId == null) {
                mqttConnacker.connackError(channel,
                        "The client id of the client (IP: {}) is not well formed. This is not allowed. Disconnecting client.",
                        "Sent CONNECT with malformed client id",
                        Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                        ReasonStrings.CONNACK_CLIENT_IDENTIFIER_NOT_VALID);
                return null;
            }
        } else {
            if (utf8StringLength == 0) {
                if (!isCleanSessionFlag) {
                    mqttConnacker.connackError(channel,
                            "A client (IP: {}) connected with a persistent session and NO clientID. Using an empty client ID is only allowed when using a cleanSession. Disconnecting client.",
                            "Sent CONNECT with a persistent session and NO clientID",
                            Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID,
                            ReasonStrings.CONNACK_CLIENT_IDENTIFIER_NOT_VALID);
                    return null;
                }
                if (!allowAssignedClientId) {
                    mqttConnacker.connackError(channel,
                            "The client id of the client (IP: {}) is empty. This is not allowed. Disconnecting client.",
                            "Sent CONNECT with empty client id",
                            Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID,
                            ReasonStrings.CONNACK_CLIENT_IDENTIFIER_NOT_VALID);
                    return null;
                }

                clientId = clientIds.generateNext();
                channel.attr(ChannelAttributes.CLIENT_ID_ASSIGNED).set(true);
            } else {
                clientId = Strings.getPrefixedString(buf, utf8StringLength);
            }
        }

        channel.attr(ChannelAttributes.CLIENT_ID).set(clientId);

        final MqttWillPublish willPublish;

        if (isWillFlag) {
            willPublish = readMqtt3WillPublish(channel, buf, willQoS, isWillRetain, hiveMQId);
            //channel already closed.
            if (willPublish == null) {
                return null;
            }
        } else {
            willPublish = null;
        }

        final String userName;

        if (isUsernameFlag) {
            userName = Strings.getPrefixedString(buf);
            if (userName == null) {
                mqttConnacker.connackError(channel,
                        "A client (IP: {}) sent a CONNECT with an incorrect username length. Disconnecting client.",
                        "Sent a CONNECT with an incorrect username length",
                        Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                        ReasonStrings.CONNACK_MALFORMED_PACKET_USERNAME);
                return null;
            }
            channel.attr(ChannelAttributes.AUTH_USERNAME).set(userName);
        } else {
            userName = null;
        }

        final byte[] password;

        if (isPasswordFlag) {
            password = Bytes.getPrefixedBytes(buf);
            if (password == null) {
                mqttConnacker.connackError(channel,
                        "A client (IP: {}) sent a CONNECT with an incorrect password length. Disconnecting client.",
                        "Sent a CONNECT with an incorrect password length",
                        Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                        ReasonStrings.CONNACK_MALFORMED_PACKET_PASSWORD);
                return null;
            }
            channel.attr(ChannelAttributes.AUTH_PASSWORD).set(password);
        } else {
            password = null;
        }

        channel.attr(ChannelAttributes.CONNECT_KEEP_ALIVE).set(keepAlive);
        channel.attr(ChannelAttributes.CLEAN_START).set(isCleanSessionFlag);

        return new CONNECT.Mqtt3Builder().withProtocolVersion(ProtocolVersion.MQTTv3_1_1)
                .withClientIdentifier(clientId)
                .withUsername(userName)
                .withPassword(password)
                .withCleanStart(isCleanSessionFlag)
                .withSessionExpiryInterval(isCleanSessionFlag ? 0 : maxSessionExpiryInterval)
                .withKeepAlive(keepAlive)
                .withWillPublish(willPublish).build();
    }

}
