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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.codec.encoder.FixedSizeMessageEncoder;
import com.hivemq.codec.encoder.MqttEncoder;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connack.Mqtt3CONNACK;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * @author Dominik Obermaier
 */
public class Mqtt3ConnackEncoder extends FixedSizeMessageEncoder<Mqtt3CONNACK> implements MqttEncoder<Mqtt3CONNACK> {

    private static final byte CONNACK_FIXED_HEADER = 0b0010_0000;
    private static final byte CONNACK_REMAINING_LENGTH = 0b0000_0010;
    private static final byte CONNACK_FLAGS_EMPTY = 0b0000_0000;
    private static final byte CONNACK_FLAGS_SP_SET = 0b0000_0001;
    private static final int ENCODED_CONNACK_SIZE = 4;

    @Override
    public void encode(final @NotNull ChannelHandlerContext ctx, final @NotNull Mqtt3CONNACK msg, final @NotNull ByteBuf out) {

        out.writeByte(CONNACK_FIXED_HEADER);
        //The remaining length is always static for CONNACKs
        out.writeByte(CONNACK_REMAINING_LENGTH);

        final Mqtt3ConnAckReturnCode returnCode = msg.getReturnCode();
        switch (ctx.channel().attr(ChannelAttributes.MQTT_VERSION).get()) {
            case MQTTv3_1:
                out.writeByte(CONNACK_FLAGS_EMPTY);
                break;
            case MQTTv3_1_1:

                if (returnCode == Mqtt3ConnAckReturnCode.ACCEPTED && msg.isSessionPresent()) {
                    out.writeByte(CONNACK_FLAGS_SP_SET);
                } else {
                    out.writeByte(CONNACK_FLAGS_EMPTY);
                }
                break;
        }
        out.writeByte(returnCode.getCode());
    }

    @Override
    public void write(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg, final @NotNull ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        if (msg instanceof CONNACK) {
            //We make sure we really disconnect the client when there's a wrong return code
            if (((CONNACK) msg).getReturnCode() != Mqtt3ConnAckReturnCode.ACCEPTED) {
                promise.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    @Override
    public int bufferSize(final @NotNull ChannelHandlerContext ctx, final @NotNull Mqtt3CONNACK connack) {
        return ENCODED_CONNACK_SIZE;
    }
}
