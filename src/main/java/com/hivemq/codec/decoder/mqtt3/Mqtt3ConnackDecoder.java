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
import com.hivemq.codec.decoder.MqttDecoder;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
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
public class Mqtt3ConnackDecoder extends MqttDecoder<CONNACK> {

    private static final Logger log = LoggerFactory.getLogger(Mqtt3ConnackDecoder.class);
    private static final int SESSION_PRESENT_BITMASK = 0b0000_0001;

    private final @NotNull EventLog eventLog;

    @Inject
    public Mqtt3ConnackDecoder(final @NotNull EventLog eventLog) {
        this.eventLog = eventLog;
    }

    @Override
    public CONNACK decode(final @NotNull Channel channel, final @NotNull ByteBuf buf, final byte header) {
        final boolean isMqtt311 = ProtocolVersion.MQTTv3_1_1 == channel.attr(MQTT_VERSION).get();
        final byte connectAcknowledgeFlags = buf.readByte();
        if (isMqtt311) {
            if (!validateHeader(header)) {
                log.debug("A client (IP: {}) sent a Connack with an invalid fixed header. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
                eventLog.clientWasDisconnected(channel, "Invalid CONNACK fixed header");
                channel.close();
                buf.clear();
                return null;
            }
            if (connectAcknowledgeFlags != 0 && connectAcknowledgeFlags != 1) {
                log.debug("A client (IP: {}) sent a Connack with an invalid variable header. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
                eventLog.clientWasDisconnected(channel, "Invalid CONNACK variable header");
                channel.close();
                buf.clear();
                return null;
            }
        }


        boolean sessionPresent = false;
        if (isMqtt311) {
            sessionPresent = (connectAcknowledgeFlags & SESSION_PRESENT_BITMASK) == 0b0000_0001;
        }
        final byte returnCode = buf.readByte();

        return new CONNACK(Mqtt3ConnAckReturnCode.fromCode(returnCode), sessionPresent);
    }
}
