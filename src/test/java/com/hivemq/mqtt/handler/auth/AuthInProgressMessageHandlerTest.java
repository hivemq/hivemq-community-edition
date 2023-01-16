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
package com.hivemq.mqtt.handler.auth;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.connack.MqttConnackerImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.hivemq.mqtt.message.disconnect.DISCONNECT.SESSION_EXPIRY_NOT_SET;
import static org.junit.Assert.*;

/**
 * @author Georg Held
 */
@SuppressWarnings("NullabilityAnnotations")
public class AuthInProgressMessageHandlerTest {

    @Mock
    private EventLog eventLog;

    private MqttConnacker connacker;
    private EmbeddedChannel channel;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        connacker = new MqttConnackerImpl(eventLog);

        channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        channel.pipeline().addFirst(new AuthInProgressMessageHandler(connacker));
    }

    @Test(timeout = 5000)
    public void test_handler_allows_AUTH_messages() {
        final AUTH successAUTH = AUTH.getSuccessAUTH();
        channel.writeInbound(successAUTH);

        assertSame(successAUTH, channel.readInbound());
        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_handler_allows_DISCONNECT_messages() {
        final DISCONNECT disconnect = new DISCONNECT(Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION,
                null,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                null,
                SESSION_EXPIRY_NOT_SET);

        channel.writeInbound(disconnect);

        assertSame(disconnect, channel.readInbound());
        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_handler_disallows_publish() {
        final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder().withTopic("topic").withQoS(QoS.AT_LEAST_ONCE).withOnwardQos(QoS.AT_LEAST_ONCE).withPayload("payload".getBytes()).withHivemqId("hivemqId").build();

        channel.writeInbound(publish);
        final CONNACK connack = channel.readOutbound();

        assertNull(channel.readInbound());
        assertEquals(Mqtt5ConnAckReasonCode.PROTOCOL_ERROR, connack.getReasonCode());
        assertEquals("Client must not send a message other than AUTH or DISCONNECT during enhanced authentication", connack.getReasonString());
    }
}