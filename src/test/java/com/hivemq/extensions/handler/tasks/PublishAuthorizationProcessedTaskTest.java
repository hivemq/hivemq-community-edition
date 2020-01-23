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

package com.hivemq.extensions.handler.tasks;

import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extensions.auth.parameter.PublishAuthorizerOutputImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.mqtt.handler.disconnect.Mqtt3ServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.Mqtt5ServerDisconnector;
import com.hivemq.mqtt.handler.publish.IncomingPublishService;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.DummyHandler;
import util.TestMessageUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("NullabilityAnnotations")
public class PublishAuthorizationProcessedTaskTest {

    @Mock
    Mqtt5ServerDisconnector mqtt5ServerDisconnector;

    @Mock
    Mqtt3ServerDisconnector mqtt3ServerDisconnector;

    @Mock
    IncomingPublishService incomingPublishService;

    private PublishAuthorizationProcessedTask task;
    private EmbeddedChannel channel;
    private ChannelHandlerContext ctx;

    private PublishAuthorizerOutputImpl output;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        channel = new EmbeddedChannel(new DummyHandler());
        ctx = channel.pipeline().context(DummyHandler.class);
        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.AT_LEAST_ONCE);
        task = new PublishAuthorizationProcessedTask(publish, ctx, mqtt5ServerDisconnector, mqtt3ServerDisconnector, incomingPublishService);

        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(mock(ShutdownHooks.class));
        output = new PublishAuthorizerOutputImpl(asyncer);
    }


    @Test
    public void test_mqtt5_disconnect() {

        ctx.channel().attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        output.disconnectClient();
        task.onSuccess(output);

        verify(mqtt5ServerDisconnector).disconnect(any(), anyString(), anyString(),
                eq(Mqtt5DisconnectReasonCode.NOT_AUTHORIZED), eq(null));
    }

    @Test
    public void test_mqtt5_disconnect_code() {

        ctx.channel().attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        output.disconnectClient(DisconnectReasonCode.QUOTA_EXCEEDED);
        task.onSuccess(output);

        verify(mqtt5ServerDisconnector).disconnect(any(), anyString(), anyString(),
                eq(Mqtt5DisconnectReasonCode.QUOTA_EXCEEDED), eq(null));
    }

    @Test
    public void test_mqtt5_disconnect_code_string() {

        ctx.channel().attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        output.disconnectClient(DisconnectReasonCode.QUOTA_EXCEEDED, "test-string");
        task.onSuccess(output);

        verify(mqtt5ServerDisconnector).disconnect(any(), anyString(), anyString(),
                eq(Mqtt5DisconnectReasonCode.QUOTA_EXCEEDED), eq("test-string"));
    }

    @Test
    public void test_mqtt3_disconnect() {

        ctx.channel().attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        output.disconnectClient();
        task.onSuccess(output);

        verify(mqtt3ServerDisconnector).disconnect(any(), anyString(), anyString(), eq(null), eq(null));
    }

    @Test
    public void test_mqtt3_1_disconnect() {

        ctx.channel().attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        output.disconnectClient();
        task.onSuccess(output);

        verify(mqtt3ServerDisconnector).disconnect(any(), anyString(), anyString(), eq(null), eq(null));
    }

    @Test
    public void test_mqtt5_fail() {

        final ArgumentCaptor<PublishAuthorizerResult> captor = ArgumentCaptor.forClass(PublishAuthorizerResult.class);

        ctx.channel().attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        output.failAuthorization();
        task.onSuccess(output);

        channel.runPendingTasks();

        verify(incomingPublishService).processPublish(any(), any(), captor.capture());

        final PublishAuthorizerResult result = captor.getValue();
        assertEquals(AckReasonCode.NOT_AUTHORIZED, result.getAckReasonCode());
        assertEquals("Not authorized to publish on topic 'topic' with QoS '1' and retain 'false'", result.getReasonString());
    }

    @Test
    public void test_mqtt5_fail_code() {

        final ArgumentCaptor<PublishAuthorizerResult> captor = ArgumentCaptor.forClass(PublishAuthorizerResult.class);

        ctx.channel().attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        output.failAuthorization(AckReasonCode.TOPIC_NAME_INVALID);
        task.onSuccess(output);

        channel.runPendingTasks();

        verify(incomingPublishService).processPublish(any(), any(), captor.capture());

        final PublishAuthorizerResult result = captor.getValue();
        assertEquals(AckReasonCode.TOPIC_NAME_INVALID, result.getAckReasonCode());
        assertEquals("Not authorized to publish on topic 'topic' with QoS '1' and retain 'false'", result.getReasonString());
    }

    @Test
    public void test_mqtt5_fail_code_string() {

        final ArgumentCaptor<PublishAuthorizerResult> captor = ArgumentCaptor.forClass(PublishAuthorizerResult.class);

        ctx.channel().attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        output.failAuthorization(AckReasonCode.TOPIC_NAME_INVALID, "test-string");
        task.onSuccess(output);

        channel.runPendingTasks();

        verify(incomingPublishService).processPublish(any(), any(), captor.capture());

        final PublishAuthorizerResult result = captor.getValue();
        assertEquals(AckReasonCode.TOPIC_NAME_INVALID, result.getAckReasonCode());
        assertEquals("test-string", result.getReasonString());
    }

    @Test
    public void test_mqtt3_fail() {

        final ArgumentCaptor<PublishAuthorizerResult> captor = ArgumentCaptor.forClass(PublishAuthorizerResult.class);

        ctx.channel().attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        output.failAuthorization();
        task.onSuccess(output);

        channel.runPendingTasks();

        verify(incomingPublishService).processPublish(any(), any(), captor.capture());

        final PublishAuthorizerResult result = captor.getValue();
        assertEquals(AckReasonCode.NOT_AUTHORIZED, result.getAckReasonCode());
        assertEquals("Not authorized to publish on topic 'topic' with QoS '1' and retain 'false'", result.getReasonString());
    }

    @Test
    public void test_mqtt3_1_fail() {

        final ArgumentCaptor<PublishAuthorizerResult> captor = ArgumentCaptor.forClass(PublishAuthorizerResult.class);

        ctx.channel().attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        output.failAuthorization();
        task.onSuccess(output);

        channel.runPendingTasks();

        verify(incomingPublishService).processPublish(any(), any(), captor.capture());

        final PublishAuthorizerResult result = captor.getValue();
        assertEquals(AckReasonCode.NOT_AUTHORIZED, result.getAckReasonCode());
        assertEquals("Not authorized to publish on topic 'topic' with QoS '1' and retain 'false'", result.getReasonString());
    }


    @Test
    public void test_success() {
        final ArgumentCaptor<PublishAuthorizerResult> captor = ArgumentCaptor.forClass(PublishAuthorizerResult.class);

        output.authorizeSuccessfully();
        task.onSuccess(output);

        channel.runPendingTasks();

        verify(incomingPublishService).processPublish(any(), any(), captor.capture());

        final PublishAuthorizerResult result = captor.getValue();
        assertEquals(AckReasonCode.SUCCESS, result.getAckReasonCode());
        assertNull(result.getReasonString());
    }

    @Test
    public void test_next() {
        final ArgumentCaptor<PublishAuthorizerResult> captor = ArgumentCaptor.forClass(PublishAuthorizerResult.class);

        output.nextExtensionOrDefault();
        task.onSuccess(output);

        channel.runPendingTasks();

        verify(incomingPublishService).processPublish(any(), any(), captor.capture());

        final PublishAuthorizerResult result = captor.getValue();
        assertNull(result.getAckReasonCode());
        assertNull(result.getReasonString());
    }

    @Test
    public void test_undecided() {
        final ArgumentCaptor<PublishAuthorizerResult> captor = ArgumentCaptor.forClass(PublishAuthorizerResult.class);

        task.onSuccess(output);

        channel.runPendingTasks();

        verify(incomingPublishService).processPublish(any(), any(), captor.capture());

        final PublishAuthorizerResult result = captor.getValue();
        assertNull(result.getAckReasonCode());
        assertNull(result.getReasonString());
    }


    @Test
    public void test_undecided_authorizers_present() {
        final ArgumentCaptor<PublishAuthorizerResult> captor = ArgumentCaptor.forClass(PublishAuthorizerResult.class);

        output.authorizerPresent();
        task.onSuccess(output);

        channel.runPendingTasks();

        verify(incomingPublishService).processPublish(any(), any(), captor.capture());

        final PublishAuthorizerResult result = captor.getValue();
        assertEquals(AckReasonCode.NOT_AUTHORIZED, result.getAckReasonCode());
        assertEquals("Not authorized to publish on topic 'topic' with QoS '1' and retain 'false'", result.getReasonString());
    }

    @Test
    public void test_failure() {
        ctx.channel().attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        task.onFailure(new RuntimeException("test"));

        verify(mqtt5ServerDisconnector).disconnect(any(), anyString(), anyString(),
                eq(Mqtt5DisconnectReasonCode.NOT_AUTHORIZED), eq(null));
    }
}