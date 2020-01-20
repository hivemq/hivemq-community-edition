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

import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.Bytes;
import com.hivemq.util.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

/**
 * An Abstract Class for all Mqtt CONNECT Decoders
 *
 * @author Florian Limpöck
 */
public abstract class AbstractMqttConnectDecoder extends AbstractMqttDecoder<CONNECT> {

    protected static final int DISCONNECTED = -1;

    protected final long maxSessionExpiryInterval;

    protected final MqttConnacker mqttConnacker;

    public AbstractMqttConnectDecoder(final MqttConnacker mqttConnacker, final MqttServerDisconnector disconnector, final FullConfigurationService fullMqttConfigurationService) {
        super(disconnector, fullMqttConfigurationService);
        this.mqttConnacker = mqttConnacker;
        this.maxSessionExpiryInterval = fullMqttConfigurationService.mqttConfiguration().maxSessionExpiryInterval();
    }

    /**
     * Decodes and validates a Mqtt3 LWT (Will)
     * <p>
     * Client will be disconnected by:
     * <p>
     * - Bad will topic length
     * <p>
     * - topic with bad UTF-8 Character
     * <p>
     * - topic is empty
     * <p>
     * - topic contains null character
     *
     * @param channel      the channel of the mqtt client
     * @param buf          the ByteBuf of the encoded will message
     * @param willQoS      the quality of service of the will message
     * @param isWillRetain the retain flag of the will message
     * @param log          the static logger
     * @param eventLog     the event log
     * @return a {@link MqttWillPublish} if valid, else {@code null}.
     */
    protected MqttWillPublish readMqtt3WillPublish(final Channel channel, final ByteBuf buf, final int willQoS, final boolean isWillRetain,
                                                   final EventLog eventLog, final HivemqId hiveMQId) {

        final MqttWillPublish.Mqtt3Builder willBuilder = new MqttWillPublish.Mqtt3Builder();
        willBuilder.withQos(QoS.valueOf(willQoS));
        willBuilder.withRetain(isWillRetain);

        final String willTopic;
        final int utf8StringLengthWill;

        if (buf.readableBytes() < 2 || buf.readableBytes() < (utf8StringLengthWill = buf.readUnsignedShort())) {
            mqttConnacker.connackError(
                    channel,
                    "A client (IP: {}) sent a CONNECT with an incorrect will-topic length. Disconnecting client.",
                    "Incorrect CONNECT will-topic length",
                    Mqtt5ConnAckReasonCode.NOT_AUTHORIZED,
                    "Incorrect CONNECT will-topic length");
            return null;
        }

        if (validateUTF8) {
            willTopic = Strings.getValidatedPrefixedString(buf, utf8StringLengthWill, true);
            if (willTopic == null) {
                mqttConnacker.connackError(
                        channel,
                        "The will-topic of the client (IP: {}) is not well formed. This is not allowed. Disconnecting client.",
                        "Sent CONNECT with bad UTF-8 character",
                        Mqtt5ConnAckReasonCode.NOT_AUTHORIZED,
                        "Sent CONNECT with bad UTF-8 character");
                return null;
            }
        } else {
            willTopic = Strings.getPrefixedString(buf, utf8StringLengthWill);
        }

        if (isInvalidTopic(channel, willTopic)) {
            eventLog.clientWasDisconnected(channel, "Invalid CONNECT will-topic");
            channel.close();
            return null;
        }

        final byte[] prefixedBytes = Bytes.getPrefixedBytes(buf);
        final byte[] willMessage = prefixedBytes != null ? prefixedBytes : new byte[0];

        return willBuilder.withPayload(willMessage).withTopic(willTopic).withHivemqId(hiveMQId.get()).build();

    }

}
