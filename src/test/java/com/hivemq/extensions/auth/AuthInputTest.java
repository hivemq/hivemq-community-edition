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
import com.hivemq.extension.sdk.api.packets.auth.AuthPacket;
import com.hivemq.extension.sdk.api.packets.auth.AuthReasonCode;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import com.hivemq.util.Bytes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class AuthInputTest {

    private final String method = "test";
    private final byte[] authData = "test".getBytes();
    private final Mqtt5AuthReasonCode reasonCode = Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION;
    private final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(new MqttUserProperty("test", "1"));
    private final String reasonString = "testString";
    private AUTH auth;
    private AuthInput authInput;
    private ClientConnection clientConnection;

    @Before
    public void setUp() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        auth = new AUTH(method, authData, reasonCode, userProperties, reasonString);

        authInput = new AuthInput("client", channel, auth, false);
    }

    @Test(timeout = 5000)
    public void test_connect_packet_contains_auth_information() {
        final AuthPacket authPacket = authInput.getAuthPacket();
        assertEquals(method, authPacket.getAuthenticationMethod());
        assertArrayEquals(authData, Bytes.getBytesFromReadOnlyBuffer(authPacket.getAuthenticationData()));
        assertEquals(AuthReasonCode.CONTINUE_AUTHENTICATION, authPacket.getReasonCode());
        assertEquals(reasonString, authPacket.getReasonString().get());
        assertEquals("1", authPacket.getUserProperties().getFirst("test").get());

        assertNotNull(authInput.getClientInformation());
        assertNotNull(authInput.getConnectionInformation());
        assertFalse(authInput.isReAuthentication());
        assertEquals(authInput, authInput.get());
    }

}