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

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;
import util.TestMessageUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ConnectionStartInputImplTest {

    @Test(expected = NullPointerException.class)
    public void test_construction_client_null() {
        new ConnectionStartInputImpl(null, new EmbeddedChannel());
    }

    @Test
    public void test_construction_values() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        ClientConnection clientConnection = new ClientConnection();
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        final ConnectionStartInputImpl input = new ConnectionStartInputImpl(TestMessageUtil.createFullMqtt5Connect(), channel);
        assertEquals(input, input.get());
        assertNotNull(input.getClientInformation());
        assertNotNull(input.getConnectionInformation());
        assertNotNull(input.getConnectPacket());
    }
}