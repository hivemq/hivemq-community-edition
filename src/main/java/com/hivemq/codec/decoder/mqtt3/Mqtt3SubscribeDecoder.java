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

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.codec.decoder.AbstractMqttDecoder;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.util.ReasonStrings;
import com.hivemq.util.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import static com.hivemq.util.ChannelAttributes.MQTT_VERSION;

/**
 * @author Dominik Obermaier
 */
@LazySingleton
public class Mqtt3SubscribeDecoder extends AbstractMqttDecoder<SUBSCRIBE> {

    @Inject
    public Mqtt3SubscribeDecoder(final @NotNull MqttServerDisconnector disconnector,
                                 final @NotNull FullConfigurationService fullConfigurationService) {
        super(disconnector, fullConfigurationService);
    }

    @Nullable
    @Override
    public SUBSCRIBE decode(final @NotNull Channel channel, final @NotNull ByteBuf buf, final byte header) {

        if (ProtocolVersion.MQTTv3_1_1 == channel.attr(MQTT_VERSION).get()) {
            //Must match 0b0000_0010
            if ((header & 0b0000_1111) != 2) {
                disconnectByInvalidFixedHeader(channel, MessageType.SUBSCRIBE);
                buf.clear();
                return null;
            }
        } else if (ProtocolVersion.MQTTv3_1 == channel.attr(MQTT_VERSION).get()) {
            //Must match 0b0000_0010 or 0b0000_0011
            if ((header & 0b0000_1111) > 3) {
                disconnectByInvalidFixedHeader(channel, MessageType.SUBSCRIBE);
                buf.clear();
                return null;
            }
        }

        final int messageId;
        if (buf.readableBytes() >= 2) {
            messageId = buf.readUnsignedShort();
        } else {
            disconnectByNoMessageId(channel, MessageType.SUBSCRIBE);
            buf.clear();
            return null;
        }

        if (messageId < 1) {
            disconnectByNoMessageId(channel, MessageType.SUBSCRIBE);
            buf.clear();
            return null;
        }

        final ImmutableList.Builder<Topic> topics = new ImmutableList.Builder<>();

        if(!buf.isReadable()){
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a SUBSCRIBE which didn't contain any subscription. This is not allowed. Disconnecting client.",
                    "Sent SUBSCRIBE without any subscriptions",
                    Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.DISCONNECT_PROTOCOL_ERROR_NO_SUBSCRIPTIONS);
            buf.clear();
            return null;
        }

        while (buf.isReadable()) {
            final String topic = Strings.getPrefixedString(buf);
            if (isInvalidTopic(channel, topic)) {
                disconnector.disconnect(
                        channel,
                        null, //already logged
                        "Sent SUBSCRIBE with an invalid topic filter",
                        Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                        ReasonStrings.DISCONNECT_SUBSCRIBE_TOPIC_FILTER_INVALID);
                return null;
            }

            if (buf.readableBytes() == 0) {
                disconnector.disconnect(channel,
                        "A client (IP: {}) sent a SUBSCRIBE message without QoS. Disconnecting client.",
                        "Sent SUBSCRIBE without QoS",
                        Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                        ReasonStrings.DISCONNECT_PROTOCOL_ERROR_SUBSCRIBE_NO_QOS);
                buf.clear();
                return null;
            }
            final int qos = buf.readByte();
            if (qos < 0 || qos > 2) {
                disconnector.disconnect(channel,
                        "A client (IP: {}) sent a SUBSCRIBE with an invalid qos '3'. This is not allowed. Disconnecting client.",
                        "Invalid SUBSCRIBE with invalid qos '3'",
                        Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                        ReasonStrings.DISCONNECT_PROTOCOL_ERROR_SUBSCRIBE_QOS_3);
                buf.clear();
                return null;
            }
            topics.add(new Topic(topic, QoS.valueOf(qos)));
        }

        return new SUBSCRIBE(topics.build(), messageId);
    }
}
