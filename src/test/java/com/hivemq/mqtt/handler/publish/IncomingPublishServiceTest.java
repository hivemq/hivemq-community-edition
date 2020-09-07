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

import com.google.common.util.concurrent.Futures;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.impl.MqttConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.RestrictionsConfigurationServiceImpl;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extensions.handler.tasks.PublishAuthorizerResult;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.extensions.services.builder.TopicPermissionBuilderImpl;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5PubAckReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
import com.hivemq.mqtt.services.InternalPublishService;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import util.CheckUserEventTriggeredOnSuper;
import util.TestConfigurationBootstrap;
import util.TestException;
import util.TestMessageUtil;

import java.util.concurrent.ExecutorService;

import static com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties.NO_USER_PROPERTIES;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@SuppressWarnings("NullabilityAnnotations")
public class IncomingPublishServiceTest {

    @Rule
    public ErrorCollector errorCollector = new ErrorCollector();

    private static final long CURRENT_MILLIS_FIXED = 1430756192355L;

    @Mock
    private InternalPublishService publishService;

    private MqttConfigurationService mqttConfigurationService;

    private RestrictionsConfigurationService restrictionsConfigurationService;

    @Mock
    private EventLog eventLog;

    private EmbeddedChannel embeddedChannel;
    private ChannelHandlerContext ctx;

    private IncomingPublishService incomingPublishService;

    @Mock
    private MqttServerDisconnectorImpl mqttServerDisconnector;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mqttConfigurationService = Mockito.spy(new MqttConfigurationServiceImpl());
        restrictionsConfigurationService = Mockito.spy(new RestrictionsConfigurationServiceImpl());
        when(publishService.publish(
                any(PUBLISH.class),
                any(ExecutorService.class),
                anyString())).thenReturn(Futures.immediateFuture(PublishReturnCode.DELIVERED));

        setupHandlerAndChannel();

        ctx = embeddedChannel.pipeline().context(CheckUserEventTriggeredOnSuper.class);
        embeddedChannel.attr(ChannelAttributes.AUTH_PERMISSIONS).set(new ModifiableDefaultPermissionsImpl());

    }

    private void setupHandlerAndChannel() {

        incomingPublishService = new IncomingPublishService(publishService,
                mqttConfigurationService,
                restrictionsConfigurationService,
                mqttServerDisconnector);

        final CheckUserEventTriggeredOnSuper triggeredUserEvents = new CheckUserEventTriggeredOnSuper();

        embeddedChannel = new EmbeddedChannel(triggeredUserEvents);

        embeddedChannel.attr(ChannelAttributes.CLIENT_ID).set("clientid");
        embeddedChannel.attr(ChannelAttributes.MAX_PACKET_SIZE_SEND).set(1000L);
    }

    @Test
    public void test_publishes_skipped() {
        embeddedChannel.attr(ChannelAttributes.INCOMING_PUBLISHES_SKIP_REST).set(true);
        incomingPublishService.processPublish(ctx, TestMessageUtil.createMqtt5Publish(), null);

        verify(mqttServerDisconnector, never()).disconnect(
                any(Channel.class),
                anyString(),
                anyString(),
                any(Mqtt5DisconnectReasonCode.class),
                anyString());
    }

    @Test
    public void test_publish_size_too_big() {

        embeddedChannel.attr(ChannelAttributes.MAX_PACKET_SIZE_SEND).set(5L);
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        final PUBLISH publish =
                TestMessageUtil.createMqtt3Publish("testtopic", "123456790".getBytes(), QoS.AT_MOST_ONCE);

        incomingPublishService.processPublish(ctx, publish, null);

        verify(mqttServerDisconnector).disconnect(any(), any(), any(), eq(Mqtt5DisconnectReasonCode.PACKET_TOO_LARGE), any());
    }

    @Test
    public void test_publish_size_ok() {

        embeddedChannel.attr(ChannelAttributes.MAX_PACKET_SIZE_SEND).set(5L);

        final PUBLISH publish = TestMessageUtil.createMqtt3Publish("testtopic", "1234".getBytes(), QoS.AT_MOST_ONCE);

        incomingPublishService.processPublish(ctx, publish, null);

        assertEquals(true, embeddedChannel.isActive());
    }

    @Test
    public void test_publish_valid_qos0_with_ordering() throws InterruptedException {

        final PUBLISH publish = TestMessageUtil.createMqtt3Publish("testtopic", "1234".getBytes(), QoS.AT_MOST_ONCE);
        incomingPublishService.processPublish(ctx, publish, null);

        assertEquals(true, embeddedChannel.isActive());

        assertEquals(0, embeddedChannel.outboundMessages().size());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());
    }

    @Test
    public void test_publish_valid_qos0_failed_return_code() throws InterruptedException {

        when(publishService.publish(
                any(PUBLISH.class),
                any(ExecutorService.class),
                anyString())).thenReturn(Futures.immediateFailedFuture(TestException.INSTANCE));

        final PUBLISH publish = TestMessageUtil.createMqtt3Publish("testtopic", "1234".getBytes(), QoS.AT_MOST_ONCE);
        incomingPublishService.processPublish(ctx, publish, null);

        assertEquals(true, embeddedChannel.isActive());

        assertEquals(0, embeddedChannel.outboundMessages().size());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());
    }

    @Test
    public void test_publish_valid_qos1() throws InterruptedException {

        final PUBLISH publish = TestMessageUtil.createMqtt3Publish("testtopic", "1234".getBytes(), QoS.AT_LEAST_ONCE);
        incomingPublishService.processPublish(ctx, publish, null);

        assertEquals(true, embeddedChannel.isActive());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());
    }

    @Test
    public void test_publish_mqtt5_valid_qos1_authorized() throws InterruptedException {
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic1", QoS.AT_LEAST_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter(
                "#").build());

        embeddedChannel.attr(ChannelAttributes.AUTH_PERMISSIONS).set(permissions);

        incomingPublishService.processPublish(ctx, publish, null);

        assertEquals(true, embeddedChannel.isActive());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());

        while (embeddedChannel.outboundMessages().size() == 0) {
            embeddedChannel.runScheduledPendingTasks();
            embeddedChannel.runPendingTasks();
            Thread.sleep(10);
        }

        final PUBACK puback = (PUBACK) embeddedChannel.outboundMessages().poll();
        assertEquals(Mqtt5PubAckReasonCode.SUCCESS, puback.getReasonCode());
    }

    @Test
    public void test_publish_mqtt5_valid_qos1_authorizer_allow() throws InterruptedException {
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic1", QoS.AT_LEAST_ONCE);

        final PublishAuthorizerResult authorizerResult = new PublishAuthorizerResult(AckReasonCode.SUCCESS, null, true);

        incomingPublishService.processPublish(ctx, publish, authorizerResult);
        assertEquals(true, embeddedChannel.isActive());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());

        while (embeddedChannel.outboundMessages().size() == 0) {
            embeddedChannel.runScheduledPendingTasks();
            embeddedChannel.runPendingTasks();
            Thread.sleep(10);
        }

        final PUBACK puback = (PUBACK) embeddedChannel.outboundMessages().poll();
        assertEquals(Mqtt5PubAckReasonCode.SUCCESS, puback.getReasonCode());
    }

    @Test
    public void test_publish_mqtt5_valid_qos1_authorizer_undecided() {
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic1", QoS.AT_LEAST_ONCE);

        final PublishAuthorizerResult authorizerResult = new PublishAuthorizerResult(null, null, true);

        incomingPublishService.processPublish(ctx, publish, authorizerResult);

        verify(mqttServerDisconnector).disconnect(
                eq(embeddedChannel),
                anyString(),
                anyString(),
                eq(Mqtt5DisconnectReasonCode.NOT_AUTHORIZED),
                anyString());
        verify(publishService, never()).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());

        final PUBACK puback = (PUBACK) embeddedChannel.outboundMessages().poll();
        assertEquals(Mqtt5PubAckReasonCode.NOT_AUTHORIZED, puback.getReasonCode());

    }

    @Test
    public void test_publish_mqtt5_valid_qos0_authorizer_allow() throws InterruptedException {
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic1", QoS.AT_MOST_ONCE);

        final PublishAuthorizerResult authorizerResult = new PublishAuthorizerResult(AckReasonCode.SUCCESS, null, true);

        incomingPublishService.processPublish(ctx, publish, authorizerResult);
        assertEquals(true, embeddedChannel.isActive());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());
    }

    @Test
    public void test_publish_mqtt5_valid_qos1_authorizer_failed() {
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic1", QoS.AT_LEAST_ONCE);

        final PublishAuthorizerResult authorizerResult =
                new PublishAuthorizerResult(AckReasonCode.PACKET_IDENTIFIER_IN_USE, "abc", true);

        incomingPublishService.processPublish(ctx, publish, authorizerResult);

        verify(mqttServerDisconnector).disconnect(
                eq(embeddedChannel),
                anyString(),
                anyString(),
                eq(Mqtt5DisconnectReasonCode.NOT_AUTHORIZED),
                anyString());
        verify(publishService, never()).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());

        final PUBACK puback = (PUBACK) embeddedChannel.outboundMessages().poll();
        assertEquals(Mqtt5PubAckReasonCode.PACKET_IDENTIFIER_IN_USE, puback.getReasonCode());
        assertEquals("abc", puback.getReasonString());

    }

    @Test
    public void test_publish_mqtt5_valid_qos0_authorizer_failed() {
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic1", QoS.AT_MOST_ONCE);

        final PublishAuthorizerResult authorizerResult =
                new PublishAuthorizerResult(AckReasonCode.PACKET_IDENTIFIER_IN_USE, "abc", true);

        incomingPublishService.processPublish(ctx, publish, authorizerResult);

        verify(mqttServerDisconnector).disconnect(
                eq(embeddedChannel),
                anyString(),
                anyString(),
                eq(Mqtt5DisconnectReasonCode.NOT_AUTHORIZED),
                anyString());
        verify(publishService, never()).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());

    }

    @Test
    public void test_publish_mqtt5_valid_qos2_authorizer_allow() throws InterruptedException {
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic1", QoS.EXACTLY_ONCE);

        final PublishAuthorizerResult authorizerResult = new PublishAuthorizerResult(AckReasonCode.SUCCESS, null, true);

        incomingPublishService.processPublish(ctx, publish, authorizerResult);
        assertEquals(true, embeddedChannel.isActive());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());

        while (embeddedChannel.outboundMessages().size() == 0) {
            embeddedChannel.runScheduledPendingTasks();
            embeddedChannel.runPendingTasks();
            Thread.sleep(10);
        }

        final PUBREC puback = (PUBREC) embeddedChannel.outboundMessages().poll();
        assertEquals(Mqtt5PubRecReasonCode.SUCCESS, puback.getReasonCode());
    }

    @Test
    public void test_publish_mqtt5_valid_qos2_authorizer_failed() {
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic1", QoS.EXACTLY_ONCE);

        final PublishAuthorizerResult authorizerResult =
                new PublishAuthorizerResult(AckReasonCode.PACKET_IDENTIFIER_IN_USE, "abc", true);

        incomingPublishService.processPublish(ctx, publish, authorizerResult);

        verify(mqttServerDisconnector).disconnect(
                eq(embeddedChannel),
                anyString(),
                anyString(),
                eq(Mqtt5DisconnectReasonCode.NOT_AUTHORIZED),
                anyString());
        verify(publishService, never()).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());

        final PUBREC puback = (PUBREC) embeddedChannel.outboundMessages().poll();
        assertEquals(Mqtt5PubRecReasonCode.PACKET_IDENTIFIER_IN_USE, puback.getReasonCode());
        assertEquals("abc", puback.getReasonString());

    }

    @Test
    public void test_publish_mqtt3_valid_qos1_authorized() throws InterruptedException {
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic1", QoS.AT_LEAST_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter(
                "#").build());

        embeddedChannel.attr(ChannelAttributes.AUTH_PERMISSIONS).set(permissions);

        incomingPublishService.processPublish(ctx, publish, null);

        assertEquals(true, embeddedChannel.isActive());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());
    }

    @Test
    public void test_publish_mqtt5_valid_qos1_not_authorized() throws InterruptedException {

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic1", QoS.AT_LEAST_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter(
                "#").type(TopicPermission.PermissionType.DENY).build());

        embeddedChannel.attr(ChannelAttributes.AUTH_PERMISSIONS).set(permissions);

        incomingPublishService.processPublish(ctx, publish, null);

        verify(mqttServerDisconnector).disconnect(
                eq(embeddedChannel),
                anyString(),
                anyString(),
                eq(Mqtt5DisconnectReasonCode.NOT_AUTHORIZED),
                anyString());

        verify(publishService, never()).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());

        final PUBACK puback = embeddedChannel.readOutbound();

        assertEquals(Mqtt5PubAckReasonCode.NOT_AUTHORIZED, puback.getReasonCode());
        assertEquals(
                "Not authorized to publish on topic 'topic1' with QoS '1' and retain 'false'",
                puback.getReasonString());

    }

    @Test
    public void test_publish_mqtt3_valid_qos1_not_authorized() {

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic1", QoS.AT_LEAST_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter(
                "#").type(TopicPermission.PermissionType.DENY).build());

        embeddedChannel.attr(ChannelAttributes.AUTH_PERMISSIONS).set(permissions);

        incomingPublishService.processPublish(ctx, publish, null);

        verify(mqttServerDisconnector).disconnect(any(), any(), any(), eq(Mqtt5DisconnectReasonCode.NOT_AUTHORIZED), any());

        verify(publishService, never()).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());
    }

    @Test
    public void test_publish_mqtt3_valid_qos2_not_authorized() {

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic1", QoS.EXACTLY_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter(
                "#").type(TopicPermission.PermissionType.DENY).build());

        embeddedChannel.attr(ChannelAttributes.AUTH_PERMISSIONS).set(permissions);

        incomingPublishService.processPublish(ctx, publish, null);

        verify(mqttServerDisconnector).disconnect(any(), any(), any(), eq(Mqtt5DisconnectReasonCode.NOT_AUTHORIZED), any());

        verify(publishService, never()).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());
    }

    @Test
    public void test_publish_mqtt5_valid_qos0_authorized() throws InterruptedException {
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic1", QoS.AT_MOST_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter(
                "#").build());

        embeddedChannel.attr(ChannelAttributes.AUTH_PERMISSIONS).set(permissions);

        incomingPublishService.processPublish(ctx, publish, null);

        assertEquals(true, embeddedChannel.isActive());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());
    }

    @Test
    public void test_publish_mqtt5_valid_qos0_not_authorized() {

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic1", QoS.AT_MOST_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter(
                "#").type(TopicPermission.PermissionType.DENY).build());

        embeddedChannel.attr(ChannelAttributes.AUTH_PERMISSIONS).set(permissions);

        incomingPublishService.processPublish(ctx, publish, null);

        verify(mqttServerDisconnector).disconnect(
                eq(embeddedChannel),
                anyString(),
                anyString(),
                eq(Mqtt5DisconnectReasonCode.NOT_AUTHORIZED),
                anyString());

        verify(publishService, never()).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());

    }

    @Test
    public void test_publish_mqtt5_valid_qos2_authorized() throws InterruptedException {
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic1", QoS.EXACTLY_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter(
                "#").build());

        embeddedChannel.attr(ChannelAttributes.AUTH_PERMISSIONS).set(permissions);

        incomingPublishService.processPublish(ctx, publish, null);

        assertEquals(true, embeddedChannel.isActive());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());
    }

    @Test
    public void test_publish_mqtt5_valid_qos2_not_authorized() {

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic1", QoS.EXACTLY_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter(
                "#").type(TopicPermission.PermissionType.DENY).build());

        embeddedChannel.attr(ChannelAttributes.AUTH_PERMISSIONS).set(permissions);

        incomingPublishService.processPublish(ctx, publish, null);

        verify(mqttServerDisconnector).disconnect(
                eq(embeddedChannel),
                anyString(),
                anyString(),
                eq(Mqtt5DisconnectReasonCode.NOT_AUTHORIZED),
                anyString());

        verify(publishService, never()).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());

        final PUBREC pubrec = embeddedChannel.readOutbound();

        assertEquals(Mqtt5PubRecReasonCode.NOT_AUTHORIZED, pubrec.getReasonCode());
        assertEquals(
                "Not authorized to publish on topic 'topic1' with QoS '2' and retain 'false'",
                pubrec.getReasonString());

    }

    @Test
    public void test_publish_valid_qos1_no_matching_subs() throws InterruptedException {

        when(publishService.publish(
                any(PUBLISH.class),
                any(ExecutorService.class),
                anyString())).thenReturn(Futures.immediateFuture(PublishReturnCode.NO_MATCHING_SUBSCRIBERS));

        final PUBLISH publish = TestMessageUtil.createMqtt3Publish("testtopic", "1234".getBytes(), QoS.AT_LEAST_ONCE);
        incomingPublishService.processPublish(ctx, publish, null);

        assertEquals(true, embeddedChannel.isActive());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());

        while (embeddedChannel.outboundMessages().size() == 0) {
            embeddedChannel.runScheduledPendingTasks();
            embeddedChannel.runPendingTasks();
            Thread.sleep(10);
        }

        final PUBACK puback = embeddedChannel.readOutbound();

        assertEquals(Mqtt5PubAckReasonCode.NO_MATCHING_SUBSCRIBERS, puback.getReasonCode());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());
    }

    @Test
    public void test_publish_valid_qos1_failed_publish() throws InterruptedException {

        when(publishService.publish(
                any(PUBLISH.class),
                any(ExecutorService.class),
                anyString())).thenReturn(Futures.immediateFailedFuture(TestException.INSTANCE));

        final PUBLISH publish = TestMessageUtil.createMqtt3Publish("testtopic", "1234".getBytes(), QoS.AT_LEAST_ONCE);
        incomingPublishService.processPublish(ctx, publish, null);

        assertEquals(true, embeddedChannel.isActive());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());

        while (embeddedChannel.outboundMessages().size() == 0) {
            embeddedChannel.runScheduledPendingTasks();
            embeddedChannel.runPendingTasks();
            Thread.sleep(10);
        }

        final PUBACK puback = embeddedChannel.readOutbound();

        assertEquals(Mqtt5PubAckReasonCode.SUCCESS, puback.getReasonCode());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());
    }

    @Test
    public void test_publish_valid_qos2() throws InterruptedException {

        final PUBLISH publish = TestMessageUtil.createMqtt3Publish("testtopic", "1234".getBytes(), QoS.EXACTLY_ONCE);
        incomingPublishService.processPublish(ctx, publish, null);

        assertEquals(true, embeddedChannel.isActive());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());

        while (embeddedChannel.outboundMessages().size() == 0) {
            embeddedChannel.runScheduledPendingTasks();
            embeddedChannel.runPendingTasks();
            Thread.sleep(10);
        }

        assertEquals(1, embeddedChannel.outboundMessages().size());

        final PUBREC pubrec = embeddedChannel.readOutbound();

        assertEquals(Mqtt5PubRecReasonCode.SUCCESS, pubrec.getReasonCode());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());
    }

    @Test
    public void test_publish_valid_qos2_no_matching_subs() throws InterruptedException {

        when(publishService.publish(
                any(PUBLISH.class),
                any(ExecutorService.class),
                anyString())).thenReturn(Futures.immediateFuture(PublishReturnCode.NO_MATCHING_SUBSCRIBERS));

        final PUBLISH publish = TestMessageUtil.createMqtt3Publish("testtopic", "1234".getBytes(), QoS.EXACTLY_ONCE);
        incomingPublishService.processPublish(ctx, publish, null);

        assertEquals(true, embeddedChannel.isActive());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());

        while (embeddedChannel.outboundMessages().size() == 0) {
            embeddedChannel.runScheduledPendingTasks();
            embeddedChannel.runPendingTasks();
            Thread.sleep(10);
        }

        final PUBREC pubrec = embeddedChannel.readOutbound();

        assertEquals(Mqtt5PubRecReasonCode.NO_MATCHING_SUBSCRIBERS, pubrec.getReasonCode());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());
    }

    @Test
    public void test_publish_valid_qos2_failed_publish() throws InterruptedException {

        when(publishService.publish(
                any(PUBLISH.class),
                any(ExecutorService.class),
                anyString())).thenReturn(Futures.immediateFailedFuture(TestException.INSTANCE));

        final PUBLISH publish = TestMessageUtil.createMqtt3Publish("testtopic", "1234".getBytes(), QoS.EXACTLY_ONCE);
        incomingPublishService.processPublish(ctx, publish, null);

        assertEquals(true, embeddedChannel.isActive());
        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());

        while (embeddedChannel.outboundMessages().size() == 0) {
            embeddedChannel.runScheduledPendingTasks();
            embeddedChannel.runPendingTasks();
            Thread.sleep(10);
        }

        final PUBREC pubrec = embeddedChannel.readOutbound();

        assertEquals(Mqtt5PubRecReasonCode.SUCCESS, pubrec.getReasonCode());
        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());
    }

    @Test
    public void test_publish_no_callback() {

        final PUBLISH publish = TestMessageUtil.createMqtt3Publish("testtopic", "1234".getBytes(), QoS.AT_MOST_ONCE);
        incomingPublishService.processPublish(ctx, publish, null);

        assertEquals(true, embeddedChannel.isActive());

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());
    }

    @Test
    public void test_qos_exceeded_disconnect() {
        when(mqttConfigurationService.maximumQos()).thenReturn(QoS.AT_MOST_ONCE);
        setupHandlerAndChannel();
        ctx.channel().attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("none",
                "topic",
                new byte[0],
                QoS.EXACTLY_ONCE,
                false,
                MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT,
                Mqtt5PayloadFormatIndicator.UTF_8,
                null,
                "responseTopic",
                null,
                NO_USER_PROPERTIES,
                15,
                false,
                true,
                null);

        incomingPublishService.processPublish(ctx, publish, null);

        verify(mqttServerDisconnector).disconnect(
                eq(ctx.channel()),
                anyString(),
                anyString(),
                eq(Mqtt5DisconnectReasonCode.QOS_NOT_SUPPORTED),
                anyString());

        // Verify PUBLISH not processed
        verify(publishService, never()).publish(any(), any(), anyString());
    }

    @Test
    public void test_qos_exceeded_mqtt3_disconnect() {
        when(mqttConfigurationService.maximumQos()).thenReturn(QoS.AT_MOST_ONCE);
        setupHandlerAndChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        final PUBLISH publish =
                TestMessageUtil.createMqtt3Publish("none", "topic", QoS.EXACTLY_ONCE, new byte[0], false);

        incomingPublishService.processPublish(ctx, publish, null);

        verify(mqttServerDisconnector).disconnect(any(), any(), any(), eq(Mqtt5DisconnectReasonCode.QOS_NOT_SUPPORTED), any());

        // Verify PUBLISH not processed
        verify(publishService, never()).publish(any(), any(), anyString());
    }

    @Test
    public void test_default_not_authorized() {

        embeddedChannel.attr(ChannelAttributes.INCOMING_PUBLISHES_DEFAULT_FAILED_SKIP_REST).set(true);

        final PUBLISH publish = TestMessageUtil.createMqtt3Publish();
        incomingPublishService.processPublish(
                ctx,
                publish,
                new PublishAuthorizerResult(AckReasonCode.SUCCESS, null, true));

        verify(mqttServerDisconnector).disconnect(any(), any(), any(), eq(Mqtt5DisconnectReasonCode.NOT_AUTHORIZED), any());

    }

    @Test(timeout = 20000)
    public void test_topic_length_exceeded_mqtt3() {
        when(restrictionsConfigurationService.maxTopicLength()).thenReturn(3);
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        final PUBLISH publish = TestMessageUtil.createMqtt3Publish();
        incomingPublishService.processPublish(ctx, publish, new PublishAuthorizerResult(AckReasonCode.SUCCESS, null, true));

        verify(mqttServerDisconnector).disconnect(any(), any(), any(), eq(Mqtt5DisconnectReasonCode.TOPIC_NAME_INVALID), any());
    }

    @Test(timeout = 20000)
    public void test_topic_length_exceeded_mqtt5() {
        when(restrictionsConfigurationService.maxTopicLength()).thenReturn(3);
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.AT_LEAST_ONCE);
        incomingPublishService.processPublish(ctx, publish, new PublishAuthorizerResult(AckReasonCode.SUCCESS, null, true));

        verify(mqttServerDisconnector).disconnect(any(), anyString(), anyString(), eq(Mqtt5DisconnectReasonCode.TOPIC_NAME_INVALID), anyString());
    }

}