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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * The Encoder is used to encode mqtt messages.
 *
 * @author Waldemar Ruck
 * @since 4.0
 */
public interface MqttEncoder<T extends Message> {

    /**
     * Encode a mqtt message into a {@link ByteBuf}. This method will be called for each written message that can be
     * handled by this encoder.
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link MessageToByteEncoder} belongs to
     * @param msg the message to encode
     * @param out the {@link ByteBuf} into which the encoded message will be written
     */
    void encode(@NotNull ChannelHandlerContext ctx, @NotNull T msg, @NotNull ByteBuf out);

    /**
     * @param ctx the channel handler context of the clients {@link Channel}
     * @param msg the message to get the buffer size for
     * @return the buffer size a {@link Message} needs.
     */
    int bufferSize(@NotNull ChannelHandlerContext ctx, @NotNull T msg);
}
