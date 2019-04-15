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
import com.hivemq.mqtt.message.pubrel.PUBREL;
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
public class Mqtt3PubrelDecoder extends MqttDecoder<PUBREL> {

    private static final Logger log = LoggerFactory.getLogger(Mqtt3PubrelDecoder.class);

    private final EventLog eventLog;

    @Inject
    public Mqtt3PubrelDecoder(final EventLog eventLog) {
        this.eventLog = eventLog;
    }

    @Override
    public PUBREL decode(final Channel channel, final ByteBuf buf, final byte header) {

        if (ProtocolVersion.MQTTv3_1_1 == channel.attr(MQTT_VERSION).get()) {
            if (!validateHeader(header)) {
                if (log.isDebugEnabled()) {
                    log.debug("A client (IP: {}) sent a Pubrel with an invalid fixed header. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
                }
                eventLog.clientWasDisconnected(channel, "Invalid PUBREL fixed header");
                channel.close();
                buf.clear();
                return null;
            }
        }

        if (buf.readableBytes() < 2) {
            if (log.isDebugEnabled()) {
                log.debug("A client (IP: {}) sent a Pubrel without a message id. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
            }
            eventLog.clientWasDisconnected(channel, "Sent PINGREQ without message id");
            channel.close();
            buf.clear();
            return null;
        }
        final int messageId = buf.readUnsignedShort();


        return new PUBREL(messageId);
    }

    @Override
    protected boolean validateHeader(final byte header) {
        return (header & 0b0000_0010) == 2;
    }
}
