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

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WebSocketTextFrameHandlerTest {

    private EmbeddedChannel channel;

    @Before
    public void setUp() throws Exception {
        final WebSocketTextFrameHandler webSocketTextFrameHandler = new WebSocketTextFrameHandler();
        channel = new EmbeddedChannel(webSocketTextFrameHandler);
    }

    @Test
    public void test_disconnect_client() throws Exception {

        final TextWebSocketFrame frame = new TextWebSocketFrame();
        channel.writeInbound(frame);
        assertEquals(false, channel.isOpen());
    }
}