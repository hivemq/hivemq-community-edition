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
package com.hivemq.codec.encoder.mqtt3;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode.*;

public class Mqtt3SubackEncoder extends AbstractVariableHeaderLengthEncoder<SUBACK> {

    private static final Logger log = LoggerFactory.getLogger(Mqtt3SubackEncoder.class);

    public static final int VARIABLE_HEADER_SIZE = 2;
    private static final byte SUBACK_FIXED_HEADER = (byte) 0b1001_0000;

    private final @NotNull MqttServerDisconnector mqttServerDisconnector;

    public Mqtt3SubackEncoder(final @NotNull MqttServerDisconnector mqttServerDisconnector) {
        this.mqttServerDisconnector = mqttServerDisconnector;
    }

    @Override
    public void encode(
            final @NotNull ClientConnection clientConnection,
            final @NotNull SUBACK msg,
            final @NotNull ByteBuf out) {

        if (closedIfNotAllowed(clientConnection, msg)) {
            return;
        }

        out.writeByte(SUBACK_FIXED_HEADER);

        final int remainingLength = msg.getRemainingLength();

        createRemainingLength(remainingLength, out);
        out.writeShort(msg.getPacketIdentifier());

        for (final Mqtt5SubAckReasonCode granted : msg.getReasonCodes()) {
            if (granted.getCode() >= 128) {
                out.writeByte(UNSPECIFIED_ERROR.getCode());
            } else {
                out.writeByte(granted.getCode());
            }
        }
    }

    private boolean closedIfNotAllowed(final @NotNull ClientConnection clientConnection, final @NotNull SUBACK msg) {
        final ProtocolVersion protocolVersion = clientConnection.getProtocolVersion();
        final List<Mqtt5SubAckReasonCode> grantedQos = msg.getReasonCodes();

        if (grantedQos.isEmpty()) {
            log.error("Tried to write a SUBACK with empty payload to a client. Disconnecting client (IP: {}).",
                    clientConnection.getChannelIP().orElse("UNKNOWN"));
            mqttServerDisconnector.disconnect(clientConnection.getChannel(),
                    null, //already logged
                    "Tried to write a SUBACK with empty payload to a client.",
                    Mqtt5DisconnectReasonCode.IMPLEMENTATION_SPECIFIC_ERROR,
                    null,
                    Mqtt5UserProperties.NO_USER_PROPERTIES,
                    false,
                    true);
            return true;
        }

        for (final Mqtt5SubAckReasonCode granted : grantedQos) {
            if ((granted.getCode() >= 128) && (protocolVersion == ProtocolVersion.MQTTv3_1)) {
                log.error("Tried to write a failure code (0x80) to a MQTT 3.1 subscriber. Disconnecting client (IP: {}).",
                        clientConnection.getChannelIP().orElse("UNKNOWN"));
                mqttServerDisconnector.disconnect(clientConnection.getChannel(),
                        null, //already logged
                        "Tried to write a failure code (0x80) to a MQTT 3.1 subscriber.",
                        Mqtt5DisconnectReasonCode.IMPLEMENTATION_SPECIFIC_ERROR,
                        null,
                        Mqtt5UserProperties.NO_USER_PROPERTIES,
                        false,
                        true);
                return true;
            } else if (granted != GRANTED_QOS_0 &&
                    granted != GRANTED_QOS_1 &&
                    granted != GRANTED_QOS_2 &&
                    granted.getCode() < 128) {
                log.error("Tried to write an invalid SUBACK return code to a subscriber. Disconnecting client (IP: {}).",
                        clientConnection.getChannelIP().orElse("UNKNOWN"));
                mqttServerDisconnector.disconnect(clientConnection.getChannel(),
                        null, //already logged
                        "Tried to write an invalid SUBACK return code to a subscriber.",
                        Mqtt5DisconnectReasonCode.IMPLEMENTATION_SPECIFIC_ERROR,
                        null,
                        Mqtt5UserProperties.NO_USER_PROPERTIES,
                        false,
                        true);
                return true;
            }
        }
        return false;
    }

    @Override
    protected int remainingLength(final @NotNull SUBACK msg) {
        return msg.getReasonCodes().size() + VARIABLE_HEADER_SIZE;
    }
}
