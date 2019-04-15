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

package com.hivemq.mqtt.handler.connect;

import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.CONNECT;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DisconnectClientOnConnectMessageHandlerTest {

    private EmbeddedChannel embeddedChannel;

    @Before
    public void setUp() throws Exception {
        embeddedChannel = new EmbeddedChannel(new DisconnectClientOnConnectMessageHandler(new EventLog()));
    }

    @Test
    public void test_disconnect_on_connect_message() throws Exception {
        assertEquals(true, embeddedChannel.isOpen());
        embeddedChannel.writeInbound(new CONNECT.Mqtt3Builder().withProtocolVersion(ProtocolVersion.MQTTv3_1_1).withClientIdentifier("clientID").build());

        //verify that the client was disconnected
        assertEquals(false, embeddedChannel.isOpen());
    }

}