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
package com.hivemq.extensions.auth;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.CONNECT;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

/**
 * @author Georg Held
 */
@SuppressWarnings("NullabilityAnnotations")
public class AuthConnectInputTest {

    private CONNECT connect;
    private AuthConnectInput taskInput;

    @Before
    public void setUp() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setConnectReceivedTimestamp(12345L);

        connect = new CONNECT.Mqtt5Builder()
                .withClientIdentifier("client")
                .withUsername("user")
                .withPassword("password".getBytes(Charset.defaultCharset()))
                .withAuthMethod("method")
                .withAuthData(new byte[]{'a', 'b', 'c'})
                .build();
        taskInput = new AuthConnectInput(connect, channel);
    }

    @Test(timeout = 5000)
    public void test_connect_packet_contains_auth_information() {

        final ConnectPacket connectPacket = taskInput.getConnectPacket();

        assertEquals("method", connectPacket.getAuthenticationMethod().get());
        assertEquals("user", connectPacket.getUserName().get());
        assertEquals(ByteBuffer.wrap("password".getBytes()), connectPacket.getPassword().get());
        assertEquals(ByteBuffer.wrap("abc".getBytes()), connectPacket.getAuthenticationData().get());
        assertEquals(taskInput, taskInput.get());
    }
}