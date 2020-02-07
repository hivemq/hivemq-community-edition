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

package com.hivemq.codec.encoder;

import com.google.inject.Inject;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author Dominik Obermaier
 */
@ChannelHandler.Sharable
public class MQTTMessageEncoder extends MessageToByteEncoder<Message> {

    private final @NotNull EncoderFactory encoderFactory;

    @Inject
    public MQTTMessageEncoder(final @NotNull EncoderFactory encoderFactory) {
        this.encoderFactory = encoderFactory;
    }

    @Override
    protected void encode(final @NotNull ChannelHandlerContext ctx, final @NotNull Message msg, final @NotNull ByteBuf out) {
        encoderFactory.encode(ctx, msg, out);
    }

    @Override
    protected @NotNull ByteBuf allocateBuffer(final @NotNull ChannelHandlerContext ctx, final @NotNull Message msg, final boolean preferDirect) {
        return encoderFactory.allocateBuffer(ctx, msg, preferDirect);
    }
}
