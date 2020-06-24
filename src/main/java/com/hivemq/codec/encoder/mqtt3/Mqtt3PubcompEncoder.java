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
package com.hivemq.codec.encoder.mqtt3;

import com.hivemq.codec.encoder.FixedSizeMessageEncoder;
import com.hivemq.codec.encoder.MqttEncoder;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Dominik Obermaier
 */
public class Mqtt3PubcompEncoder extends FixedSizeMessageEncoder<PUBCOMP> implements MqttEncoder<PUBCOMP> {

    private static final byte PUBCOMP_FIXED_HEADER = 0b0111_0000;
    private static final byte PUBCOMP_REMAINING_LENGTH = 0b0000_0010;
    public static final int ENCODED_PUBCOMP_SIZE = 4;

    @Override
    public void encode(final ChannelHandlerContext ctx, final PUBCOMP msg, final ByteBuf out) {

        if (msg.getPacketIdentifier() == 0) {
            throw new IllegalArgumentException("Message ID must not be null");
        }

        out.writeByte(PUBCOMP_FIXED_HEADER);
        //The remaining length is always static for PUBCOMPs
        out.writeByte(PUBCOMP_REMAINING_LENGTH);

        out.writeShort(msg.getPacketIdentifier());
    }


    @Override
    public int bufferSize(final ChannelHandlerContext ctx, final PUBCOMP msg) {
        return ENCODED_PUBCOMP_SIZE;
    }
}
