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
package com.hivemq.extensions.events.client.parameters;

import com.google.common.collect.ImmutableList;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class AuthenticationFailedInputImplTest {

    @Test
    public void test_construction_null_values() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        final AuthenticationFailedInputImpl input = new AuthenticationFailedInputImpl(channel, "client", null, null, null);
        assertEquals(input, input.get());
        assertEquals("client", input.getClientInformation().getClientId());
        assertEquals(Optional.empty(), input.getReasonCode());
        assertEquals(Optional.empty(), input.getReasonString());
        assertEquals(Optional.empty(), input.getUserProperties());
        assertNotNull(input.getConnectionInformation());
    }

    @Test
    public void test_construction_with_values() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        final AuthenticationFailedInputImpl input =
                new AuthenticationFailedInputImpl(channel, "client", DisconnectedReasonCode.BAD_AUTHENTICATION_METHOD,
                        "reason", UserPropertiesImpl.of(ImmutableList.of(new MqttUserProperty("key", "value"))));

        assertEquals(input, input.get());
        assertEquals("client", input.getClientInformation().getClientId());
        assertEquals(Optional.of(DisconnectedReasonCode.BAD_AUTHENTICATION_METHOD), input.getReasonCode());
        assertEquals(Optional.of("reason"), input.getReasonString());
        final Optional<UserProperties> userProperties = input.getUserProperties();
        assertTrue(userProperties.isPresent());
        assertEquals(1, userProperties.get().asList().size());
        assertNotNull(input.getConnectionInformation());
    }

    @Test(expected = NullPointerException.class)
    public void test_construction_client_id_null() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        new AuthenticationFailedInputImpl(channel, null, null, null, null);
    }
}