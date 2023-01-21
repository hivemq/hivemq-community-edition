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
public class ClientInitiatedDisconnectInputImplTest {

    @Test(expected = NullPointerException.class)
    public void test_construction_client_null() {
        new ClientInitiatedDisconnectInputImpl(null, new EmbeddedChannel(), null, null, null, false);
    }

    @Test
    public void test_construction_values_null() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        final ClientInitiatedDisconnectInputImpl disconnectInput = new ClientInitiatedDisconnectInputImpl("client", channel, null, null, null, false);
        assertEquals(Optional.empty(), disconnectInput.getReasonCode());
        assertEquals(Optional.empty(), disconnectInput.getReasonString());
        assertEquals(Optional.empty(), disconnectInput.getUserProperties());
        assertEquals(disconnectInput, disconnectInput.get());
        assertEquals("client", disconnectInput.getClientInformation().getClientId());
        assertNotNull(disconnectInput.getConnectionInformation());
        assertEquals(false, disconnectInput.isGraceful());
    }

    @Test
    public void test_construction_values_set() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        final ClientInitiatedDisconnectInputImpl disconnectInput =
                new ClientInitiatedDisconnectInputImpl("client", channel, DisconnectedReasonCode.NORMAL_DISCONNECTION,
                        "reason", UserPropertiesImpl.of(ImmutableList.of(new MqttUserProperty("key", "val"))), true);
        assertEquals(Optional.of(DisconnectedReasonCode.NORMAL_DISCONNECTION), disconnectInput.getReasonCode());
        assertEquals(Optional.of("reason"), disconnectInput.getReasonString());
        assertTrue(disconnectInput.getUserProperties().isPresent());
        assertEquals(disconnectInput, disconnectInput.get());
        assertEquals("client", disconnectInput.getClientInformation().getClientId());
        assertNotNull(disconnectInput.getConnectionInformation());
        assertEquals(true, disconnectInput.isGraceful());
    }
}