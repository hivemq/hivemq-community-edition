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

package com.hivemq.codec.decoder.mqtt3;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.codec.decoder.MqttDecoder;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.util.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hivemq.util.ChannelAttributes.MQTT_VERSION;
import static com.hivemq.util.ChannelUtils.getChannelIP;

/**
 * @author Dominik Obermaier
 */
@LazySingleton
public class Mqtt3SubscribeDecoder extends MqttDecoder<SUBSCRIBE> {

    private static final Logger log = LoggerFactory.getLogger(Mqtt3SubscribeDecoder.class);

    private final EventLog eventLog;

    @Inject
    public Mqtt3SubscribeDecoder(final EventLog eventLog) {
        this.eventLog = eventLog;
    }

    @Override
    public SUBSCRIBE decode(final Channel channel, final ByteBuf buf, final byte header) {

        if (ProtocolVersion.MQTTv3_1_1 == channel.attr(MQTT_VERSION).get()) {
            //Must match 0b0000_0010
            if ((header & 0b0000_1111) != 2) {
                if (log.isDebugEnabled()) {
                    log.error("A client (IP: {}) sent a subscribe message with an invalid fixed header. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
                }
                eventLog.clientWasDisconnected(channel, "Invalid SUBSCRIBE fixed header");
                channel.close();
                buf.clear();
                return null;
            }
        } else if (ProtocolVersion.MQTTv3_1 == channel.attr(MQTT_VERSION).get()) {
            //Must match 0b0000_0010 or 0b0000_0011
            if ((header & 0b0000_1111) > 3) {
                if (log.isDebugEnabled()) {
                    log.debug("A client (IP: {}) sent a subscribe message with an invalid fixed header. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
                }
                eventLog.clientWasDisconnected(channel, "Invalid SUBSCRIBE fixed header");
                channel.close();
                buf.clear();
                return null;
            }
        }

        final int messageId;
        if (buf.readableBytes() >= 2) {
            messageId = buf.readUnsignedShort();
        } else {
            if (log.isDebugEnabled()) {
                log.debug("A client (IP: {}) sent a subscribe message with an invalid message id. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
            }
            eventLog.clientWasDisconnected(channel, "Invalid SUBSCRIBE message id");
            channel.close();
            buf.clear();
            return null;
        }

        if (messageId < 1) {
            if (log.isDebugEnabled()) {
                log.debug("A client (IP: {}) sent a subscribe message with invalid message id '{}'. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"), messageId);
            }
            eventLog.clientWasDisconnected(channel, "Invalid SUBSCRIBE message id");
            channel.close();
            buf.clear();
            return null;
        }

        final ImmutableList.Builder<Topic> topics = new ImmutableList.Builder<>();

        if(!buf.isReadable()){
            if (log.isDebugEnabled()) {
                log.debug("A client (IP: {}) sent a subscribe message which didn't contain any subscription. This is not allowed. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
            }
            eventLog.clientWasDisconnected(channel, "Sent SUBSCRIBE without any subscriptions");
            channel.close();
            buf.clear();
            return null;
        }

        while (buf.isReadable()) {
            final String topic = Strings.getPrefixedString(buf);

            if (isInvalidTopic(channel, topic)) {
                channel.close();
                buf.clear();
                return null;
            }

            if (buf.readableBytes() == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("A client (IP: {}) sent a subscribe message which contained no QoS. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
                }
                eventLog.clientWasDisconnected(channel, "Sent SUBSCRIBE without QoS");
                channel.close();
                buf.clear();
                return null;
            }
            final int qos = buf.readByte();
            if (qos < 0 || qos > 2) {
                if (log.isDebugEnabled()) {
                    log.debug("A client (IP: {}) sent a subscribe message which contained an invalid QoS subscription. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
                }
                eventLog.clientWasDisconnected(channel, "Invalid SUBSCRIBE QoS");
                channel.close();
                buf.clear();
                return null;
            }
            topics.add(new Topic(topic, QoS.valueOf(qos)));
        }

        return new SUBSCRIBE(topics.build(), messageId);
    }
}
