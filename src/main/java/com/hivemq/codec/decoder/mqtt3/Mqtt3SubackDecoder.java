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

import com.google.inject.Inject;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.codec.decoder.AbstractMqttDecoder;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;

import static com.hivemq.util.ChannelAttributes.MQTT_VERSION;

/**
 * @author Lukas Brandl
 */
@LazySingleton
public class Mqtt3SubackDecoder extends AbstractMqttDecoder<SUBACK> {

    @Inject
    public Mqtt3SubackDecoder(final @NotNull MqttServerDisconnector disconnector,
                              final @NotNull FullConfigurationService fullConfigurationService) {
        super(disconnector, fullConfigurationService);
    }

    @Override
    public SUBACK decode(final @NotNull Channel channel, final @NotNull ByteBuf buf, final byte header) {
        if (ProtocolVersion.MQTTv3_1_1 == channel.attr(MQTT_VERSION).get()) {
            if (!validateHeader(header)) {
                disconnectByInvalidFixedHeader(channel, MessageType.SUBACK);
                buf.clear();
                return null;
            }
        }

        final int messageId = buf.readUnsignedShort();
        final List<Mqtt5SubAckReasonCode> returnCodes = new ArrayList<>();
        while (buf.isReadable()) {
            final byte qos = buf.readByte();
            if (qos < 0 || qos > 2) {
                disconnector.disconnect(channel,
                        "A client (IP: {}) sent a SUBACK with an invalid QoS. Disconnecting client.",
                        "Sent SUBACK with invalid QoS",
                        Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                        null); // hivemq never sends SUBACKs reason string
                buf.clear();
                return null;
            }
            returnCodes.add(Mqtt5SubAckReasonCode.fromCode(qos));
        }
        return new SUBACK(messageId, returnCodes);
    }
}
