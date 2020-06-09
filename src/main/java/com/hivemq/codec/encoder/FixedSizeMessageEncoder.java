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
package com.hivemq.codec.encoder;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author Lukas Brandl
 */
public abstract class FixedSizeMessageEncoder<T extends Message> extends MessageToByteEncoder<T> {

    @Override
    protected @NotNull ByteBuf allocateBuffer(final ChannelHandlerContext ctx, final T msg, final boolean preferDirect) {
        final int bufferSize = bufferSize(ctx, msg);
        if (preferDirect) {
            return ctx.alloc().ioBuffer(bufferSize);
        } else {
            return ctx.alloc().heapBuffer(bufferSize);
        }
    }

    public abstract int bufferSize(@NotNull ChannelHandlerContext ctx, @NotNull T msg);
}
