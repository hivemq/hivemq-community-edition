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
package com.hivemq.codec.decoder;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hivemq.util.ChannelUtils.getChannelIP;

/**
 * @author Dominik Obermaier
 */
public abstract class MqttDecoder<T extends Message> {

    private static final Logger log = LoggerFactory.getLogger(MqttDecoder.class);

    public abstract @Nullable T decode(final @NotNull  Channel channel, final @NotNull  ByteBuf buf, final byte header);

    /**
     * Checks if the last 4 bits are actually zeroed out
     *
     * @param header the header byte
     * @return <code>true</code> if the last four header bytes are actually zeroed out, false otherwise.
     */
    protected boolean validateHeader(final byte header) {

        //This checks if the last 4 bits are actually zeroed

        return (header & 0b0000_1111) == 0;
    }

    /**
     * Checks if the topic is not empty and doesn't contain null characters.
     *
     * @param topic the topic string
     * @return <code>true</code> if the topic is valid.
     */
    protected boolean isInvalidTopic(final @NotNull Channel channel, final @Nullable String topic) {
        if (topic == null || topic.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("A client (IP: {}) sent an empty topic. This is not allowed. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
            }
            return true;
        }

        if (topic.contains("\u0000")) {
            if (log.isDebugEnabled()) {
                log.debug("A client (IP: {}) sent a topic which contained the Unicode null character (U+0000). This is not allowed. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
            }
            return true;
        }
        return false;
    }

}
