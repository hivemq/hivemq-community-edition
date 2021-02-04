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

package com.hivemq.mqtt.handler.connack;

import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extensions.events.OnAuthFailedEvent;
import com.hivemq.extensions.events.OnServerDisconnectEvent;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import util.DummyHandler;
import util.LogbackCapturingAppender;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Florian Limpöck
 */
@SuppressWarnings("NullabilityAnnotations")
public class MqttConnackerTest {

    @Mock
    private EventLog eventLog;

    private MqttConnacker mqttConnacker;
    private EmbeddedChannel channel;
    private LogbackCapturingAppender logbackCapturingAppender;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mqttConnacker = new MqttConnackerImpl(eventLog);
        channel = new EmbeddedChannel();
        channel.pipeline().addLast(new DummyHandler());
        logbackCapturingAppender = LogbackCapturingAppender.Factory.weaveInto(LoggerFactory.getLogger(MqttConnackerImpl.class));
    }

    @After
    public void tearDown() throws Exception {
        InternalConfigurations.CONNACK_WITH_REASON_CODE.set(true);
        InternalConfigurations.CONNACK_WITH_REASON_STRING.set(true);
    }

    @Test(expected = NullPointerException.class)
    public void test_connackError_channel_null() {
        mqttConnacker.connackError(null, "log", "eventlog", null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_connackError_success_code() {
        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.SUCCESS, null);
    }

    @Test
    public void test_connackError_no_protocol_version() {
        assertTrue(channel.isActive());
        mqttConnacker.connackError(channel, "log", "eventlog", null, null);
        assertFalse(channel.isActive());
    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_3_no_logs_no_reason() {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, null, null, null, null);

        assertFalse(channel.isActive());
        verify(eventLog, never()).clientDisconnected(any(), any());
    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_3_no_logs_no_reason_with_client_id() {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, null, null, null, null);

        assertFalse(channel.isActive());
        verify(eventLog, never()).clientDisconnected(any(), any());
    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_3_with_logs_with_reason_with_client_id() {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID, "packettoolarge");

        assertEquals(Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID, ((CONNACK) channel.readOutbound()).getReasonCode());
        assertFalse(channel.isActive());
        verify(eventLog, times(1)).clientWasDisconnected(any(), any());
        assertEquals("log", logbackCapturingAppender.getLastCapturedLog().getFormattedMessage());
    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_3_incompatible_reason_code_UNSPECIFIED_ERROR() {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR, "packettoolarge");

        assertNull(channel.readOutbound());
        assertFalse(channel.isActive());

    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_3_incompatible_reason_code_MALFORMED_PACKET() {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.MALFORMED_PACKET, "packettoolarge");

        assertNull(channel.readOutbound());
        assertFalse(channel.isActive());

    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_3_incompatible_reason_code_PROTOCOL_ERROR() {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.PROTOCOL_ERROR, "packettoolarge");

        assertNull(channel.readOutbound());
        assertFalse(channel.isActive());

    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_3_incompatible_reason_code_IMPLEMENTATION_SPECIFIC_ERROR() {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, "packettoolarge");

        assertNull(channel.readOutbound());
        assertFalse(channel.isActive());

    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_3_1_with_logs_with_reason_with_client_id() {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID, "packettoolarge");

        assertEquals(Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID, ((CONNACK) channel.readOutbound()).getReasonCode());
        assertFalse(channel.isActive());
        verify(eventLog, times(1)).clientWasDisconnected(any(), any());
        assertEquals("log", logbackCapturingAppender.getLastCapturedLog().getFormattedMessage());
    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_3_1_incompatible_reason_code_UNSPECIFIED_ERROR() {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR, "unspecified_error");

        assertNull(channel.readOutbound());
        assertFalse(channel.isActive());

    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_3_1_incompatible_reason_code_MALFORMED_PACKET() {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.MALFORMED_PACKET, "malformed_packet");

        assertNull(channel.readOutbound());
        assertFalse(channel.isActive());

    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_3_1_incompatible_reason_code_PROTOCOL_ERROR() {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.PROTOCOL_ERROR, "protocol_error");

        assertNull(channel.readOutbound());
        assertFalse(channel.isActive());

    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_3_1_incompatible_reason_code_IMPLEMENTATION_SPECIFIC_ERROR() {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, "implementation_specific_error");

        assertNull(channel.readOutbound());
        assertFalse(channel.isActive());

    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_3_1_without_reason_code() {
        InternalConfigurations.CONNACK_WITH_REASON_CODE.set(false);
        mqttConnacker = new MqttConnackerImpl(eventLog);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID, "packettoolarge");

        assertNull(channel.readOutbound());
        assertFalse(channel.isActive());

    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_3_send_extension_server_disc_event() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                if(evt instanceof OnServerDisconnectEvent){
                    latch.countDown();
                }
            }
        });
        channel.attr(ChannelAttributes.EXTENSION_CONNECT_EVENT_SENT).set(true);
        channel.attr(ChannelAttributes.EXTENSION_DISCONNECT_EVENT_SENT).set(null);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID, "packettoolarge");

        assertEquals(Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID, ((CONNACK) channel.readOutbound()).getReasonCode());
        assertFalse(channel.isActive());
        assertTrue(latch.await(10, TimeUnit.SECONDS));

    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_3_send_extension_auth_failed_event() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                if(evt instanceof OnAuthFailedEvent){
                    latch.countDown();
                }
            }
        });
        channel.attr(ChannelAttributes.EXTENSION_CONNECT_EVENT_SENT).set(true);
        channel.attr(ChannelAttributes.EXTENSION_DISCONNECT_EVENT_SENT).set(null);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID, "packettoolarge", Mqtt5UserProperties.NO_USER_PROPERTIES, true);

        assertEquals(Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID, ((CONNACK) channel.readOutbound()).getReasonCode());
        assertFalse(channel.isActive());
        assertTrue(latch.await(10, TimeUnit.SECONDS));

    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_5_send_extension_server_disc_event() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                if(evt instanceof OnServerDisconnectEvent){
                    latch.countDown();
                }
            }
        });
        channel.attr(ChannelAttributes.EXTENSION_CONNECT_EVENT_SENT).set(true);
        channel.attr(ChannelAttributes.EXTENSION_DISCONNECT_EVENT_SENT).set(null);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID, "packettoolarge");

        assertEquals(Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID, ((CONNACK) channel.readOutbound()).getReasonCode());
        assertFalse(channel.isActive());
        assertTrue(latch.await(10, TimeUnit.SECONDS));

    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_5_send_extension_auth_failed_event() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                if(evt instanceof OnAuthFailedEvent){
                    latch.countDown();
                }
            }
        });
        channel.attr(ChannelAttributes.EXTENSION_CONNECT_EVENT_SENT).set(true);
        channel.attr(ChannelAttributes.EXTENSION_DISCONNECT_EVENT_SENT).set(null);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID, "packettoolarge", Mqtt5UserProperties.NO_USER_PROPERTIES, true);

        final CONNACK connack = channel.readOutbound();
        assertEquals(Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID, connack.getReasonCode());
        assertEquals("packettoolarge", connack.getReasonString());
        assertFalse(channel.isActive());
        assertTrue(latch.await(10, TimeUnit.SECONDS));

    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_5_without_reason_string() throws InterruptedException {
        InternalConfigurations.CONNACK_WITH_REASON_STRING.set(false);
        mqttConnacker = new MqttConnackerImpl(eventLog);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID, "packettoolarge", Mqtt5UserProperties.NO_USER_PROPERTIES, true);

        final CONNACK connack = channel.readOutbound();
        assertEquals(Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID, connack.getReasonCode());
        assertNull(connack.getReasonString());
        assertFalse(channel.isActive());

    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_5_without_reason_code() {
        InternalConfigurations.CONNACK_WITH_REASON_CODE.set(false);
        mqttConnacker = new MqttConnackerImpl(eventLog);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID, "packettoolarge");

        assertNull(channel.readOutbound());
        assertFalse(channel.isActive());

    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_5_with_auth_data_and_method() {

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        channel.attr(ChannelAttributes.AUTH_DATA).set(ByteBuffer.wrap("decent_guy".getBytes()));
        channel.attr(ChannelAttributes.AUTH_METHOD).set("face_check");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.NOT_AUTHORIZED, "dont like him");

        final CONNACK connack = channel.readOutbound();
        assertEquals(Mqtt5ConnAckReasonCode.NOT_AUTHORIZED, connack.getReasonCode());
        assertNotNull(connack.getReasonString());
        assertNotNull(connack.getAuthData());
        assertNotNull(connack.getAuthMethod());
        assertFalse(channel.isActive());

    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_5_with_auth_data_but_no_method() {

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        channel.attr(ChannelAttributes.AUTH_DATA).set(ByteBuffer.wrap("decent_guy".getBytes()));
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.NOT_AUTHORIZED, "dont like him");

        final CONNACK connack = channel.readOutbound();
        assertEquals(Mqtt5ConnAckReasonCode.NOT_AUTHORIZED, connack.getReasonCode());
        assertNotNull(connack.getReasonString());
        assertNull(connack.getAuthData());
        assertNull(connack.getAuthMethod());
        assertFalse(channel.isActive());

    }

    @Test(timeout = 20000)
    public void test_connackError_mqtt_5_with_no_auth_data_but_method() {

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.attr(ChannelAttributes.CLIENT_ID).set("luke_skywalker");
        channel.attr(ChannelAttributes.AUTH_METHOD).set("face_check");
        assertTrue(channel.isActive());

        mqttConnacker.connackError(channel, "log", "eventlog", Mqtt5ConnAckReasonCode.NOT_AUTHORIZED, "dont like him");

        final CONNACK connack = channel.readOutbound();
        assertEquals(Mqtt5ConnAckReasonCode.NOT_AUTHORIZED, connack.getReasonCode());
        assertNotNull(connack.getReasonString());
        assertNotNull(connack.getAuthMethod());
        assertNull(connack.getAuthData());
        assertFalse(channel.isActive());

    }

    @Test(expected = NullPointerException.class)
    public void test_connackSuccess_ctx_null() {
        mqttConnacker.connackSuccess(null, new CONNACK(Mqtt5ConnAckReasonCode.SUCCESS, null));
    }

    @Test(expected = NullPointerException.class)
    public void test_connackSuccess_connack_null() {
        mqttConnacker.connackSuccess(channel.pipeline().firstContext(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_connackSuccess_connack_error_reason_code() {
        mqttConnacker.connackSuccess(channel.pipeline().firstContext(), new CONNACK(Mqtt5ConnAckReasonCode.NOT_AUTHORIZED, null));
    }

    @Test
    public void test_connackSuccess() {
        mqttConnacker.connackSuccess(channel.pipeline().firstContext(), new CONNACK(Mqtt5ConnAckReasonCode.SUCCESS, null));

        final CONNACK connack = channel.readOutbound();
        assertEquals(Mqtt5ConnAckReasonCode.SUCCESS, connack.getReasonCode());
        assertTrue(channel.isActive());
        assertTrue(channel.attr(ChannelAttributes.CONNACK_SENT).get());
    }
}