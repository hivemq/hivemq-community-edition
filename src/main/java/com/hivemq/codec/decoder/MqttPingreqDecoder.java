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

import com.google.inject.Inject;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hivemq.util.ChannelAttributes.MQTT_VERSION;
import static com.hivemq.util.ChannelUtils.getChannelIP;

/**
 * @author Florian Limpöck
 */
@LazySingleton
public class MqttPingreqDecoder extends MqttDecoder<PINGREQ> {

    private static final Logger log = LoggerFactory.getLogger(MqttPingreqDecoder.class);

    private final @NotNull EventLog eventLog;

    @Inject
    public MqttPingreqDecoder(final @NotNull EventLog eventLog) {
        this.eventLog = eventLog;
    }

    @Override
    public PINGREQ decode(@NotNull final Channel channel, @NotNull final ByteBuf buf, final byte header) {

        final ProtocolVersion protocolVersion = channel.attr(MQTT_VERSION).get();

        //Pingreq of MQTTv5 is equal to MQTTv3_1_1
        if (ProtocolVersion.MQTTv5 == protocolVersion || ProtocolVersion.MQTTv3_1_1 == protocolVersion) {
            if (!validateHeader(header)) {
                if (log.isDebugEnabled()) {
                    log.debug("A client (IP: {}) sent a ping with an invalid fixed header. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
                }
                eventLog.clientWasDisconnected(channel, "Invalid PINGREQ fixed header");
                channel.close();
                buf.clear();
                return null;
            }
        }

        return PINGREQ.INSTANCE;
    }
}
