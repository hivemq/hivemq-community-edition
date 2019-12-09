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

package com.hivemq.extensions.services.auth;

import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import util.DummyHandler;

import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class ConnectEnhancedAuthTaskInputTest {

    private CONNECT connect;
    private ConnectEnhancedAuthTaskInput taskInput;

    @Before
    public void setUp() {

        final EmbeddedChannel embeddedChannel = new EmbeddedChannel(new DummyHandler());
        final ChannelHandlerContext ctx = embeddedChannel.pipeline().context(DummyHandler.class);
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        connect = new CONNECT.Mqtt5Builder().withClientIdentifier("client").withUsername("user").withPassword("password".getBytes(Charset.defaultCharset())).withAuthMethod("method").withAuthData(new byte[]{'a', 'b', 'c'}).build();
        taskInput = new ConnectEnhancedAuthTaskInput(connect, ctx);
    }

    @Test(timeout = 5000)
    public void test_connect_packet_contains_auth_information() {

        final ConnectPacket connectPacket = taskInput.getConnectPacket();

        assertEquals("method", connectPacket.getAuthenticationMethod().get());
        assertEquals("user", connectPacket.getUserName().get());
        assertEquals("password", new String(connectPacket.getPassword().get().array()));
        assertEquals("abc", new String(connectPacket.getAuthenticationData().get().array()));
        assertEquals(taskInput, taskInput.get());
    }
}