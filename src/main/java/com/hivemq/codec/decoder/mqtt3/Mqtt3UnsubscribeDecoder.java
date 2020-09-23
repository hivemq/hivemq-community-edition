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
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.util.ReasonStrings;
import com.hivemq.util.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;

import static com.hivemq.util.ChannelAttributes.MQTT_VERSION;

/**
 * @author Dominik Obermaier
 */
@LazySingleton
public class Mqtt3UnsubscribeDecoder extends AbstractMqttDecoder<UNSUBSCRIBE> {

    @Inject
    public Mqtt3UnsubscribeDecoder(final @NotNull MqttServerDisconnector disconnector,
                                   final @NotNull FullConfigurationService fullConfigurationService) {
        super(disconnector, fullConfigurationService);
    }

    @Nullable
    @Override
    public UNSUBSCRIBE decode(final @NotNull Channel channel, final @NotNull ByteBuf buf, final byte header) {

        if (ProtocolVersion.MQTTv3_1_1 == channel.attr(MQTT_VERSION).get()) {
            //Must match 0b0000_0010
            if ((header & 0b0000_1111) != 2) {
                disconnectByInvalidFixedHeader(channel, MessageType.UNSUBSCRIBE);
                buf.clear();
                return null;
            }
        } else if (ProtocolVersion.MQTTv3_1 == channel.attr(MQTT_VERSION).get()) {
            //Must match 0b0000_0010 or 0b0000_0011
            if ((header & 0b0000_1111) > 3) {
                disconnectByInvalidFixedHeader(channel, MessageType.UNSUBSCRIBE);
                buf.clear();
                return null;
            }
        }

        if (buf.readableBytes() < 2) {
            disconnectByNoMessageId(channel, MessageType.UNSUBSCRIBE);
            buf.clear();
            return null;
        }
        final int messageId = buf.readUnsignedShort();
        final List<String> topics = new ArrayList<>();

        while (buf.isReadable()) {
            final String topic = Strings.getPrefixedString(buf);
            if (isInvalidTopic(channel, topic)) {
                disconnector.disconnect(channel,
                        "A client (IP: {}) sent an UNSUBSCRIBE with an empty topic. This is not allowed. Disconnecting client.",
                        "Sent UNSUBSCRIBE with an empty topic",
                        Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                        ReasonStrings.DISCONNECT_MALFORMED_EMPTY_UNSUB_TOPIC);
                buf.clear();
                return null;
            }
            topics.add(topic);
        }

        if (topics.isEmpty()) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent an UNSUBSCRIBE without topic filters. This is not allowed. Disconnecting client.",
                    "Sent UNSUBSCRIBE without topic filters",
                    Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.DISCONNECT_PROTOCOL_ERROR_UNSUBSCRIBE_NO_TOPIC_FILTERS);
            buf.clear();
            return null;
        }

        return new UNSUBSCRIBE(topics, messageId);
    }
}
