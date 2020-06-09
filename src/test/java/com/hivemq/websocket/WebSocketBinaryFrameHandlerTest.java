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
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WebSocketBinaryFrameHandlerTest {

    private EmbeddedChannel channel;

    @Before
    public void setUp() throws Exception {
        final WebSocketBinaryFrameHandler webSocketBinaryFrameHandler = new WebSocketBinaryFrameHandler();
        channel = new EmbeddedChannel(webSocketBinaryFrameHandler);
    }

    @Test
    public void test_unwrap_byte_buffer() throws Exception {

        final ByteBuf expected = Unpooled.buffer();
        final BinaryWebSocketFrame frame = new BinaryWebSocketFrame(expected);
        channel.writeInbound(frame);

        final Object object = channel.readInbound();
        final ByteBuf result = (ByteBuf) object;

        assertEquals(expected, result);
        assertEquals(1, result.refCnt());
    }
}