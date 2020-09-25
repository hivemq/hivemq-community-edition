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
package com.hivemq.mqtt.handler.disconnect;

import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extensions.events.OnAuthFailedEvent;
import com.hivemq.extensions.events.OnServerDisconnectEvent;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("ALL")
public class MqttServerDisconnectorTest {

    private MqttServerDisconnector mqttServerDisconnector;

    @Mock
    EventLog eventLog;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mqttServerDisconnector = new MqttServerDisconnectorImpl(eventLog, new HivemqId());
    }

    @After
    public void tearDown() throws Exception {
        InternalConfigurations.DISCONNECT_WITH_REASON_CODE.set(true);
        InternalConfigurations.DISCONNECT_WITH_REASON_STRING.set(true);
    }

    @Test
    public void test_log_and_close_with_event() throws InterruptedException {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.EXTENSION_CONNECT_EVENT_SENT).set(true);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final CountDownLatch eventLatch = new CountDownLatch(1);
        final CountDownLatch authLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new TestDisconnectEventHandler(eventLatch, authLatch));
        assertTrue(channel.isActive());

        mqttServerDisconnector.logAndClose(channel, "log", "event");

        assertFalse(channel.isActive());
        assertTrue(eventLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void test_log_and_close_without_event() throws InterruptedException {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final CountDownLatch eventLatch = new CountDownLatch(1);
        final CountDownLatch authLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new TestDisconnectEventHandler(eventLatch, authLatch));
        assertTrue(channel.isActive());

        mqttServerDisconnector.logAndClose(channel, "log", "event");

        assertFalse(channel.isActive());
        assertFalse(eventLatch.await(100, TimeUnit.MILLISECONDS));
        assertFalse(authLatch.await(0, TimeUnit.SECONDS));
    }

    @Test
    public void test_disconnect_channel_without_reason_code_and_reason_string() throws InterruptedException {

        InternalConfigurations.DISCONNECT_WITH_REASON_CODE.set(false);
        InternalConfigurations.DISCONNECT_WITH_REASON_STRING.set(false);

        mqttServerDisconnector = new MqttServerDisconnectorImpl(eventLog, new HivemqId());

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.EXTENSION_CONNECT_EVENT_SENT).set(true);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final CountDownLatch eventLatch = new CountDownLatch(1);
        final CountDownLatch authLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new TestDisconnectEventHandler(eventLatch, authLatch));
        assertTrue(channel.isActive());

        mqttServerDisconnector.disconnect(channel,
                "log",
                "eventlog", Mqtt5DisconnectReasonCode.SERVER_BUSY,
                "reasonstring",
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                false,
                false);

        final DISCONNECT disconnect = channel.readOutbound();

        assertNull(disconnect);
        assertFalse(channel.isActive());
        assertTrue(eventLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void test_disconnect_channel_with_reason_code_and_reason_string() throws InterruptedException {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.EXTENSION_CONNECT_EVENT_SENT).set(true);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final CountDownLatch eventLatch = new CountDownLatch(1);
        final CountDownLatch authLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new TestDisconnectEventHandler(eventLatch, authLatch));
        assertTrue(channel.isActive());

        mqttServerDisconnector.disconnect(channel, "log", "eventlog", Mqtt5DisconnectReasonCode.SERVER_BUSY, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES, false, false);

        final DISCONNECT disconnect = channel.readOutbound();

        assertNotNull(disconnect);

        assertEquals(Mqtt5DisconnectReasonCode.SERVER_BUSY, disconnect.getReasonCode());
        assertEquals("reason", disconnect.getReasonString());

        assertFalse(channel.isActive());
        assertTrue(eventLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void test_disconnect_channel_with_reason_code_and_reason_string_not_wanted() throws InterruptedException {

        InternalConfigurations.DISCONNECT_WITH_REASON_STRING.set(false);

        mqttServerDisconnector = new MqttServerDisconnectorImpl(eventLog, new HivemqId());

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.EXTENSION_CONNECT_EVENT_SENT).set(true);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final CountDownLatch eventLatch = new CountDownLatch(1);
        final CountDownLatch authLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new TestDisconnectEventHandler(eventLatch, authLatch));
        assertTrue(channel.isActive());

        mqttServerDisconnector.disconnect(channel, "log", "eventlog", Mqtt5DisconnectReasonCode.SERVER_BUSY, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES, false, false);

        final DISCONNECT disconnect = channel.readOutbound();

        assertNotNull(disconnect);

        assertEquals(Mqtt5DisconnectReasonCode.SERVER_BUSY, disconnect.getReasonCode());
        assertEquals(null, disconnect.getReasonString());

        assertFalse(channel.isActive());
        assertTrue(eventLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void test_disconnect_channel_with_client_id() throws InterruptedException {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.EXTENSION_CONNECT_EVENT_SENT).set(true);
        channel.attr(ChannelAttributes.CLIENT_ID).set("client");

        final CountDownLatch eventLatch = new CountDownLatch(1);
        final CountDownLatch authLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new TestDisconnectEventHandler(eventLatch, authLatch));
        assertTrue(channel.isActive());

        mqttServerDisconnector.disconnect(channel, "log", "eventlog", Mqtt5DisconnectReasonCode.SERVER_BUSY, null, Mqtt5UserProperties.NO_USER_PROPERTIES, false, false);

        assertFalse(channel.isActive());
        assertTrue(eventLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void test_disconnect_channel_with_reason_code() throws InterruptedException {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.EXTENSION_CONNECT_EVENT_SENT).set(true);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final CountDownLatch eventLatch = new CountDownLatch(1);
        final CountDownLatch authLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new TestDisconnectEventHandler(eventLatch, authLatch));
        assertTrue(channel.isActive());

        mqttServerDisconnector.disconnect(channel, "log", "eventlog", Mqtt5DisconnectReasonCode.MALFORMED_PACKET, null, Mqtt5UserProperties.NO_USER_PROPERTIES, false, false);

        final DISCONNECT disconnect = channel.readOutbound();

        assertNotNull(disconnect);

        assertEquals(Mqtt5DisconnectReasonCode.MALFORMED_PACKET, disconnect.getReasonCode());
        assertEquals(null, disconnect.getReasonString());

        assertFalse(channel.isActive());
        assertTrue(eventLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void test_disconnect_channel_with_reason_code_and_reason_string_at_auth() throws InterruptedException {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.EXTENSION_CONNECT_EVENT_SENT).set(true);

        final CountDownLatch eventLatch = new CountDownLatch(1);
        final CountDownLatch authLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new TestDisconnectEventHandler(eventLatch, authLatch));
        assertTrue(channel.isActive());

        mqttServerDisconnector.disconnect(channel, "log", "eventlog", Mqtt5DisconnectReasonCode.MALFORMED_PACKET, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES, true, false);

        assertFalse(channel.isActive());
        assertTrue(authLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void test_disconnect_channel_with_reason_code_and_reason_string_at_auth_mqtt3_1_1() throws InterruptedException {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.EXTENSION_CONNECT_EVENT_SENT).set(true);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        final CountDownLatch eventLatch = new CountDownLatch(1);
        final CountDownLatch authLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new TestDisconnectEventHandler(eventLatch, authLatch));
        assertTrue(channel.isActive());

        mqttServerDisconnector.disconnect(channel, "log", "eventlog", Mqtt5DisconnectReasonCode.MALFORMED_PACKET, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES, true, false);

        assertFalse(channel.isActive());
        assertTrue(authLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void test_disconnect_channel_with_reason_code_and_reason_string_at_auth_mqtt_3_1() throws InterruptedException {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.EXTENSION_CONNECT_EVENT_SENT).set(true);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        final CountDownLatch eventLatch = new CountDownLatch(1);
        final CountDownLatch authLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new TestDisconnectEventHandler(eventLatch, authLatch));
        assertTrue(channel.isActive());

        mqttServerDisconnector.disconnect(channel, "log", "eventlog", Mqtt5DisconnectReasonCode.MALFORMED_PACKET, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES, true, false);

        assertFalse(channel.isActive());
        assertTrue(authLatch.await(10, TimeUnit.SECONDS));
    }

    @Test(expected = NullPointerException.class)
    public void test_disconnect_channel_with_reason_code_null() throws InterruptedException {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        mqttServerDisconnector.disconnect(channel, "log", "eventlog", null, null, Mqtt5UserProperties.NO_USER_PROPERTIES, false, false);
    }

    @Test
    public void test_disconnect_channel_with_reason_code_null_mqtt_3() throws InterruptedException {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
        mqttServerDisconnector.disconnect(channel, "log", "eventlog", null, null, Mqtt5UserProperties.NO_USER_PROPERTIES, false, false);
        assertFalse(channel.isActive());
    }

    private static class TestDisconnectEventHandler extends SimpleChannelInboundHandler<CONNECT> {
        private final CountDownLatch eventLatch;
        private final CountDownLatch authLatch;

        public TestDisconnectEventHandler(CountDownLatch eventLatch, CountDownLatch authLatch) {
            this.eventLatch = eventLatch;
            this.authLatch = authLatch;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, CONNECT msg) throws Exception {
            ctx.fireChannelRead(msg);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof OnServerDisconnectEvent) {
                eventLatch.countDown();
            }
            if (evt instanceof OnAuthFailedEvent) {
                authLatch.countDown();
            }
        }
    }
}