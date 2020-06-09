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
package com.hivemq.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * @author Florian Limpöck
 * @since 4.1.0
 */
public class MQTTWebsocketEncoderTest {

    @Test
    public void test_encode() throws Exception {

        final EmbeddedChannel channel = new EmbeddedChannel();
        final MQTTWebsocketEncoder mqttWebsocketEncoder = new MQTTWebsocketEncoder();
        channel.pipeline().addLast(mqttWebsocketEncoder);
        final ChannelHandlerContext channelHandlerContext = channel.pipeline().context(mqttWebsocketEncoder);

        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeBytes(new byte[]{1, 2, 3, 4, 5});

        final ArrayList<Object> out = new ArrayList<>();
        mqttWebsocketEncoder.encode(channelHandlerContext, buffer, out);

        assertEquals(1, out.size());


    }
}