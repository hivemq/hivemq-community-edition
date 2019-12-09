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

import com.hivemq.extension.sdk.api.packets.auth.AuthPacket;
import com.hivemq.extension.sdk.api.packets.auth.AuthReasonCode;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import com.hivemq.util.Bytes;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import util.DummyHandler;

import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class AuthTaskInputTest {

    private final String method = "test";
    private final byte[] authData = "test".getBytes();
    private final Mqtt5AuthReasonCode reasonCode = Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION;
    private final Mqtt5UserProperties userProperties = Mqtt5UserProperties.builder().add(new MqttUserProperty("test", "1")).build();
    private final String reasonString = "testString";
    private AUTH auth;
    private AuthTaskInput authTaskInput;

    @Before
    public void setUp() {

        final EmbeddedChannel embeddedChannel = new EmbeddedChannel(new DummyHandler());
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        final ChannelHandlerContext ctx = embeddedChannel.pipeline().context(DummyHandler.class);

        auth = new AUTH(method, authData, reasonCode, userProperties, reasonString);

        authTaskInput = new AuthTaskInput(auth, "client", false, ctx);
    }

    @Test(timeout = 5000)
    public void test_connect_packet_contains_auth_information() {
        final AuthPacket authPacket = authTaskInput.getAuthPacket();
        assertEquals(method, authPacket.getAuthenticationMethod());
        assertArrayEquals(authData, Bytes.getBytesFromReadOnlyBuffer(authPacket.getAuthenticationData()));
        assertEquals(AuthReasonCode.CONTINUE_AUTHENTICATION, authPacket.getReasonCode());
        assertEquals(reasonString, authPacket.getReasonString().get());
        assertEquals("1", authPacket.getUserProperties().getFirst("test").get());

        assertNotNull(authTaskInput.getClientInformation());
        assertNotNull(authTaskInput.getConnectionInformation());
        assertFalse(authTaskInput.isReAuthentication());
        assertEquals(authTaskInput, authTaskInput.get());
    }

}