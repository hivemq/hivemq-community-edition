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
package com.hivemq.mqtt.handler.publish;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.configuration.service.impl.MqttConfigurationServiceImpl;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestMqttDecoder;

import java.nio.channels.ClosedChannelException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class FlowControlHandlerTest {

    private FlowControlHandler flowControlHandler;

    private MqttConfigurationServiceImpl mqttConfigurationService;

    @Mock
    EventLog eventLog;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        mqttConfigurationService = new MqttConfigurationServiceImpl();
        mqttConfigurationService.setServerReceiveMaximum(10);

        final MqttServerDisconnector serverDisconnector = new MqttServerDisconnectorImpl(eventLog);

        flowControlHandler = new FlowControlHandler(mqttConfigurationService, serverDisconnector);

    }

    @Test
    public void test_disconnect_after_receiving_to_many_qos_1_publishes() {

        final EmbeddedChannel channel = new EmbeddedChannel(TestMqttDecoder.create(), flowControlHandler);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        final PUBLISHFactory.Mqtt5Builder builder = new PUBLISHFactory.Mqtt5Builder();
        builder.withTopic("topic").withQoS(QoS.AT_LEAST_ONCE).withOnwardQos(QoS.AT_LEAST_ONCE).withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withPayload(new byte[0]).withHivemqId("hivemqId1");

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientReceiveMaximum(10);

        for (int i = 0; i < 11; i++) {
            channel.writeInbound(builder.build());
        }

        verify(eventLog).clientWasDisconnected(channel, "Sent too many concurrent PUBLISH messages");
        assertFalse(channel.isOpen());
        assertFalse(channel.isActive());

    }

    @Test(expected = ClosedChannelException.class)
    public void test_sending_publish_after_disconnect() {

        final EmbeddedChannel channel = new EmbeddedChannel(TestMqttDecoder.create(), flowControlHandler);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));

        final PUBLISHFactory.Mqtt5Builder builder = new PUBLISHFactory.Mqtt5Builder();
        builder.withTopic("topic").withQoS(QoS.AT_LEAST_ONCE).withOnwardQos(QoS.AT_LEAST_ONCE).withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withPayload(new byte[0]).withHivemqId("hivemqId1");

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientReceiveMaximum(10);

        for (int i = 0; i < 12; i++) {
            channel.writeInbound(builder.build());
        }

    }

    @Test
    public void test_no_disconnect_after_receiving_to_9_publishes_than_sending_9_pubacks() {

        final EmbeddedChannel channel = new EmbeddedChannel(TestMqttDecoder.create(), flowControlHandler);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));

        final PUBLISHFactory.Mqtt5Builder builder = new PUBLISHFactory.Mqtt5Builder();
        builder.withTopic("topic").withQoS(QoS.AT_LEAST_ONCE).withOnwardQos(QoS.AT_LEAST_ONCE).withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withPayload(new byte[0]).withHivemqId("hivemqId1");

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientReceiveMaximum(10);

        for (int i = 0; i < 10; i++) {
            channel.writeInbound(builder.build());
        }

        for (int i = 0; i < 10; i++) {
            channel.writeOutbound(new PUBACK(i));
        }

        for (int i = 0; i < 10; i++) {
            channel.writeInbound(builder.build());
        }

        verify(eventLog, never()).clientWasDisconnected(channel, "Sent too many concurrent PUBLISH messages");
        assertTrue(channel.isOpen());
        assertTrue(channel.isActive());

    }

    @Test
    public void test_quota_not_exceeding_max_value() {

        final EmbeddedChannel channel = new EmbeddedChannel(TestMqttDecoder.create(), flowControlHandler);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));

        final PUBLISHFactory.Mqtt5Builder builder = new PUBLISHFactory.Mqtt5Builder();
        builder.withTopic("topic").withQoS(QoS.AT_LEAST_ONCE).withOnwardQos(QoS.AT_LEAST_ONCE).withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withPayload(new byte[0]).withHivemqId("hivemqId1");

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientReceiveMaximum(10);

        for (int i = 0; i < 10; i++) {
            channel.writeInbound(builder.build());
        }

        for (int i = 0; i < 100; i++) {
            channel.writeOutbound(new PUBACK(i));
        }

        verify(eventLog, never()).clientWasDisconnected(channel, "Sent too many concurrent PUBLISH messages");
        assertTrue(channel.isOpen());
        assertTrue(channel.isActive());
        assertEquals(10, flowControlHandler.getServerSendQuota());

    }

    @Test
    public void test_no_disconnect_after_receiving_to_10_publishes_than_sending_10_pubcomps() {

        final EmbeddedChannel channel = new EmbeddedChannel(TestMqttDecoder.create(), flowControlHandler);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));

        final PUBLISHFactory.Mqtt5Builder builder = new PUBLISHFactory.Mqtt5Builder();
        builder.withTopic("topic").withQoS(QoS.EXACTLY_ONCE).withOnwardQos(QoS.EXACTLY_ONCE).withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withPayload(new byte[0]).withHivemqId("hivemqId1");

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientReceiveMaximum(10);

        for (int i = 0; i < 10; i++) {
            channel.writeInbound(builder.build());
        }

        for (int i = 0; i < 10; i++) {
            channel.writeOutbound(new PUBCOMP(i));
        }

        for (int i = 0; i < 10; i++) {
            channel.writeInbound(builder.build());
        }

        verify(eventLog, never()).clientWasDisconnected(channel, "Sent too many concurrent PUBLISH messages");
        assertTrue(channel.isOpen());
        assertTrue(channel.isActive());

    }

    @Test
    public void test_no_disconnect_after_receiving_to_10_publishes_than_sending_10_pubrecs_with_failure_code() {

        final EmbeddedChannel channel = new EmbeddedChannel(TestMqttDecoder.create(), flowControlHandler);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));

        final PUBLISHFactory.Mqtt5Builder builder = new PUBLISHFactory.Mqtt5Builder();
        builder.withTopic("topic").withQoS(QoS.EXACTLY_ONCE).withOnwardQos(QoS.EXACTLY_ONCE).withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withPayload(new byte[0]).withHivemqId("hivemqId1");

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientReceiveMaximum(10);

        for (int i = 0; i < 10; i++) {
            channel.writeInbound(builder.build());
        }

        for (int i = 0; i < 10; i++) {
            channel.writeOutbound(new PUBREC(i, Mqtt5PubRecReasonCode.QUOTA_EXCEEDED, null, Mqtt5UserProperties.NO_USER_PROPERTIES));
        }

        for (int i = 0; i < 10; i++) {
            channel.writeInbound(builder.build());
        }

        verify(eventLog, never()).clientWasDisconnected(channel, "Sent too many concurrent PUBLISH messages");
        assertTrue(channel.isOpen());
        assertTrue(channel.isActive());

    }

    @Test
    public void test_disconnect_after_receiving_to_10_publishes_than_sending_10_pubrecs_with_success_pub() {

        final EmbeddedChannel channel = new EmbeddedChannel(TestMqttDecoder.create(), flowControlHandler);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));

        final PUBLISHFactory.Mqtt5Builder builder = new PUBLISHFactory.Mqtt5Builder();
        builder.withTopic("topic").withQoS(QoS.EXACTLY_ONCE).withOnwardQos(QoS.EXACTLY_ONCE).withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withPayload(new byte[0]).withHivemqId("hivemqId1");

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientReceiveMaximum(10);

        for (int i = 0; i < 10; i++) {
            channel.writeInbound(builder.build());
        }

        for (int i = 0; i < 10; i++) {
            channel.writeOutbound(new PUBREC(i, Mqtt5PubRecReasonCode.SUCCESS, null, Mqtt5UserProperties.NO_USER_PROPERTIES));
        }

        channel.writeInbound(builder.build());

        verify(eventLog).clientWasDisconnected(channel, "Sent too many concurrent PUBLISH messages");
        assertFalse(channel.isOpen());
        assertFalse(channel.isActive());

    }
}