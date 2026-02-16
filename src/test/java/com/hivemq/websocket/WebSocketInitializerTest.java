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

import com.google.common.collect.Lists;
import com.hivemq.configuration.service.entity.WebsocketListener;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import util.DummyHandler;

import java.util.ArrayList;
import java.util.List;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.HTTP_OBJECT_AGGREGATOR;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.HTTP_SERVER_CODEC;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_WEBSOCKET_ENCODER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.WEBSOCKET_BINARY_FRAME_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.WEBSOCKET_SERVER_PROTOCOL_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.WEBSOCKET_TEXT_FRAME_HANDLER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WebSocketInitializerTest {

    private final EmbeddedChannel channel = new EmbeddedChannel();

    @Before
    public void setUp() throws Exception {
        channel.pipeline().addLast("dummy", new DummyHandler());
    }

    @Test
    public void test_handler_in_pipeline() {
        final WebsocketListener websocketListener =
                new WebsocketListener.Builder().port(8000).bindAddress("0.0.0.0").build();
        final WebSocketInitializer webSocketInitializer = new WebSocketInitializer(websocketListener);

        webSocketInitializer.addHandlers(channel, "dummy");

        final List<String> handlerNames = channel.pipeline().names();
        assertTrue(handlerNames.contains(HTTP_SERVER_CODEC));
        assertTrue(handlerNames.contains(HTTP_OBJECT_AGGREGATOR));
        assertTrue(handlerNames.contains(WEBSOCKET_SERVER_PROTOCOL_HANDLER));
        assertTrue(handlerNames.contains(WEBSOCKET_BINARY_FRAME_HANDLER));
        assertTrue(handlerNames.contains(WEBSOCKET_TEXT_FRAME_HANDLER));
        assertTrue(handlerNames.contains(MQTT_WEBSOCKET_ENCODER));
    }

    @Test
    public void test_handler_order() {
        final WebsocketListener websocketListener =
                new WebsocketListener.Builder().port(8000).bindAddress("0.0.0.0").build();
        final WebSocketInitializer webSocketInitializer = new WebSocketInitializer(websocketListener);
        webSocketInitializer.addHandlers(channel, "dummy");

        final List<String> handlerNames = channel.pipeline().names();
        final int hscIdx = handlerNames.indexOf(HTTP_SERVER_CODEC);
        final int hoaIdx = handlerNames.indexOf(HTTP_OBJECT_AGGREGATOR);
        final int wsphIdx = handlerNames.indexOf(WEBSOCKET_SERVER_PROTOCOL_HANDLER);
        final int wbfhIdx = handlerNames.indexOf(WEBSOCKET_BINARY_FRAME_HANDLER);
        final int wtfhIdx = handlerNames.indexOf(WEBSOCKET_TEXT_FRAME_HANDLER);
        final int mweIdx = handlerNames.indexOf(MQTT_WEBSOCKET_ENCODER);
        assertTrue(hscIdx < hoaIdx);
        assertTrue(hoaIdx < wsphIdx);
        assertTrue(wsphIdx < wbfhIdx);
        assertTrue(wbfhIdx < wtfhIdx);
        assertTrue(wtfhIdx < mweIdx);
    }

    @Test
    public void test_no_subprotocols() {
        final WebsocketListener websocketListener = new WebsocketListener.Builder().port(8000)
                .bindAddress("0.0.0.0")
                .subprotocols(new ArrayList<>())
                .build();
        final WebSocketInitializer webSocketInitializer = new WebSocketInitializer(websocketListener);
        final String subprotocolString = webSocketInitializer.getSubprotocolString();
        assertEquals("", subprotocolString);
    }

    @Test
    public void test_one_subprotocol() {
        final WebsocketListener websocketListener = new WebsocketListener.Builder().port(8000)
                .bindAddress("0.0.0.0")
                .subprotocols(Lists.newArrayList("mqttv3.1"))
                .build();

        final WebSocketInitializer webSocketInitializer = new WebSocketInitializer(websocketListener);
        final String subprotocolString = webSocketInitializer.getSubprotocolString();
        assertEquals("mqttv3.1", subprotocolString);
    }

    @Test
    public void test_multiple_subprotocols() {
        final WebsocketListener websocketListener = new WebsocketListener.Builder().port(8000)
                .bindAddress("0.0.0.0")
                .subprotocols(Lists.newArrayList("mqttv3.1", "mqtt"))
                .build();

        final WebSocketInitializer webSocketInitializer = new WebSocketInitializer(websocketListener);
        final String subprotocolString = webSocketInitializer.getSubprotocolString();
        assertEquals("mqttv3.1,mqtt", subprotocolString);
    }
}
