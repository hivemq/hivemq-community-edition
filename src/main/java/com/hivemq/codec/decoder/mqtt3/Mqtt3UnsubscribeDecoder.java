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

import com.google.inject.Inject;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.codec.decoder.MqttDecoder;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.util.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.hivemq.util.ChannelAttributes.MQTT_VERSION;
import static com.hivemq.util.ChannelUtils.getChannelIP;

/**
 * @author Dominik Obermaier
 */
@LazySingleton
public class Mqtt3UnsubscribeDecoder extends MqttDecoder<UNSUBSCRIBE> {

    private static final Logger log = LoggerFactory.getLogger(Mqtt3UnsubscribeDecoder.class);
    private final EventLog eventLog;

    @Inject
    public Mqtt3UnsubscribeDecoder(final EventLog eventLog) {
        this.eventLog = eventLog;
    }

    @Override
    public UNSUBSCRIBE decode(final Channel channel, final ByteBuf buf, final byte header) {

        if (ProtocolVersion.MQTTv3_1_1 == channel.attr(MQTT_VERSION).get()) {
            //Must match 0b0000_0010
            if ((header & 0b0000_1111) != 2) {
                if (log.isDebugEnabled()) {
                    log.debug("A client (IP: {}) sent a unsubscribe message with an invalid fixed header. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
                }
                eventLog.clientWasDisconnected(channel, "Invalid UNSUBSCRIBE fixed header");
                channel.close();
                buf.clear();
                return null;
            }
        } else if (ProtocolVersion.MQTTv3_1 == channel.attr(MQTT_VERSION).get()) {
            //Must match 0b0000_0010 or 0b0000_0011
            if ((header & 0b0000_1111) > 3) {
                if (log.isDebugEnabled()) {
                    log.debug("A client (IP: {}) sent a unsubscribe message with an invalid fixed header. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
                }
                eventLog.clientWasDisconnected(channel, "Invalid UNSUBSCRIBE fixed header");
                channel.close();
                buf.clear();
                return null;
            }
        }

        if (buf.readableBytes() < 2) {
            if (log.isDebugEnabled()) {
                log.debug("A client (IP: {}) sent a unsubscribe message with an invalid message id. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
            }
            eventLog.clientWasDisconnected(channel, "Invalid UNSUBSCRIBE message id");
            channel.close();
            buf.clear();
            return null;
        }
        final int messageId = buf.readUnsignedShort();
        final List<String> topics = new ArrayList<>();

        while (buf.isReadable()) {
            final String topic = Strings.getPrefixedString(buf);
            if (topic == null || topic.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("A client (IP: {}) sent a Unsubscribe message with an empty topic. Disconnecting client", getChannelIP(channel).or("UNKNOWN"));
                }
                eventLog.clientWasDisconnected(channel, "Sent UNSUBSCRIBE with empty topic");
                channel.close();
                buf.clear();
                return null;
            }
            topics.add(topic);
        }

        if (topics.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("A client (IP: {}) sent a Unsubscribe message with an empty payload. Disconnecting client", getChannelIP(channel).or("UNKNOWN"));
            }
            eventLog.clientWasDisconnected(channel, "Sent UNSUBSCRIBE with empty payload");
            channel.close();
            buf.clear();
            return null;
        }

        return new UNSUBSCRIBE(topics, messageId);
    }
}
