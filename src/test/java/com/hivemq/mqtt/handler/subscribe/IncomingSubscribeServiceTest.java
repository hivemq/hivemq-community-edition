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
package com.hivemq.mqtt.handler.subscribe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.packets.auth.DefaultAuthorizationBehaviour;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.extensions.services.builder.TopicPermissionBuilderImpl;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import com.hivemq.mqtt.handler.subscribe.retained.RetainedMessagesSender;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.clientsession.SharedSubscriptionService;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.InitFutureUtilsExecutorRule;
import util.TestConfigurationBootstrap;

import java.util.HashSet;
import java.util.Queue;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

@SuppressWarnings("ALL")
public class IncomingSubscribeServiceTest {

    @Rule
    public InitFutureUtilsExecutorRule initFutureUtilsExecutorRule = new InitFutureUtilsExecutorRule();

    @Mock
    private ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence;

    @Mock
    private RetainedMessagePersistence retainedMessagePersistence;

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private ChannelFuture channelFuture;

    @Mock
    private EventLog eventLog;

    @Mock
    private RetainedMessagesSender retainedMessagesSender;

    @Mock
    private SharedSubscriptionService sharedSubscriptionService;

    @Mock
    private MqttConfigurationService mqttConfigurationService;

    @Mock
    private RestrictionsConfigurationService restrictionsConfigurationService;

    private EmbeddedChannel channel;
    private IncomingSubscribeService incomingSubscribeService;
    private HivemqId hivemqId;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        hivemqId = new HivemqId();
        incomingSubscribeService = new IncomingSubscribeService(clientSessionSubscriptionPersistence, retainedMessagePersistence, sharedSubscriptionService, retainedMessagesSender, mqttConfigurationService, restrictionsConfigurationService, new MqttServerDisconnectorImpl(eventLog, hivemqId));

        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_ID).set("client");

        when(clientSessionSubscriptionPersistence.addSubscription(anyString(), any(Topic.class))).thenReturn(Futures.immediateFuture(null));
        when(clientSessionSubscriptionPersistence.addSubscriptions(anyString(), any(ImmutableSet.class))).thenReturn(Futures.<Void>immediateFuture(null));
        when(ctx.channel()).thenReturn(channel);
        when(ctx.writeAndFlush(anyObject())).thenReturn(channelFuture);
        when(ctx.executor()).thenReturn(ImmediateEventExecutor.INSTANCE);
        when(restrictionsConfigurationService.maxTopicLength()).thenReturn(65535);
    }


    @Test
    public void test_subscribe_single_and_acknowledge() throws Exception {

        final Topic topic = new Topic("test", QoS.AT_LEAST_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);
//        channel.writeInbound(subscribe);

        final Queue<Object> objects = channel.outboundMessages();

        assertEquals(1, objects.size());

        final SUBACK response = (SUBACK) objects.element();
        assertEquals(1, response.getReasonCodes().size());
        assertEquals((byte) QoS.AT_LEAST_ONCE.getQosNumber(), response.getReasonCodes().get(0).getCode());

        verify(clientSessionSubscriptionPersistence).addSubscription(eq("client"), same(topic));
    }

    @Test
    public void test_subscribe_three_and_acknowledge() throws Exception {


        final Topic topic1 = new Topic("test1", QoS.AT_LEAST_ONCE);
        final Topic topic2 = new Topic("test2", QoS.AT_MOST_ONCE);
        final Topic topic3 = new Topic("test3", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic1, topic2, topic3)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final Queue<Object> objects = channel.outboundMessages();

        assertEquals(1, objects.size());

        final SUBACK response = (SUBACK) objects.element();
        assertEquals(3, response.getReasonCodes().size());
        assertEquals((byte) QoS.AT_LEAST_ONCE.getQosNumber(), response.getReasonCodes().get(0).getCode());
        assertEquals((byte) QoS.AT_MOST_ONCE.getQosNumber(), response.getReasonCodes().get(1).getCode());
        assertEquals((byte) QoS.EXACTLY_ONCE.getQosNumber(), response.getReasonCodes().get(2).getCode());

        verify(clientSessionSubscriptionPersistence).addSubscriptions(eq("client"), any(ImmutableSet.class));
    }

    @Test
    public void test_subscribe_batched_and_acknowledge() throws Exception {

        final Topic topic1 = new Topic("test1", QoS.AT_LEAST_ONCE);
        final Topic topic2 = new Topic("test2", QoS.AT_MOST_ONCE);
        final Topic topic3 = new Topic("test3", QoS.EXACTLY_ONCE);
        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic1, topic2, topic3)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final Queue<Object> objects = channel.outboundMessages();

        assertEquals(1, objects.size());

        final SUBACK response = (SUBACK) objects.element();
        assertEquals(3, response.getReasonCodes().size());
        assertEquals((byte) QoS.AT_LEAST_ONCE.getQosNumber(), response.getReasonCodes().get(0).getCode());
        assertEquals((byte) QoS.AT_MOST_ONCE.getQosNumber(), response.getReasonCodes().get(1).getCode());
        assertEquals((byte) QoS.EXACTLY_ONCE.getQosNumber(), response.getReasonCodes().get(2).getCode());

        verify(clientSessionSubscriptionPersistence).addSubscriptions(eq("client"), any(ImmutableSet.class));
    }

    @Test
    public void test_subscribe_batched_to_non_batched_with_same_filter_and_acknowledge() throws Exception {


        final Topic topic1 = new Topic("test", QoS.EXACTLY_ONCE);
        final Topic topic2 = new Topic("test", QoS.AT_LEAST_ONCE);
        final Topic topic3 = new Topic("test", QoS.AT_MOST_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic1, topic2, topic3)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final Queue<Object> objects = channel.outboundMessages();

        assertEquals(1, objects.size());

        final SUBACK response = (SUBACK) objects.element();
        assertEquals(3, response.getReasonCodes().size());
        assertEquals((byte) QoS.EXACTLY_ONCE.getQosNumber(), response.getReasonCodes().get(0).getCode());
        assertEquals((byte) QoS.AT_LEAST_ONCE.getQosNumber(), response.getReasonCodes().get(1).getCode());
        assertEquals((byte) QoS.AT_MOST_ONCE.getQosNumber(), response.getReasonCodes().get(2).getCode());

        verify(clientSessionSubscriptionPersistence).addSubscription(eq("client"), same(topic3));
    }

    @Test
    public void test_subscribe_batched_to_batched_with_same_filter_and_acknowledge() throws Exception {


        final Topic topic1 = new Topic("test1", QoS.EXACTLY_ONCE);
        final Topic topic2 = new Topic("test2", QoS.AT_LEAST_ONCE);
        final Topic topic3 = new Topic("test2", QoS.AT_MOST_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic1, topic2, topic3)), 10);

        final ImmutableSet<Topic> persistedTopics = ImmutableSet.of(topic1, topic3);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final Queue<Object> objects = channel.outboundMessages();

        assertEquals(1, objects.size());

        final SUBACK response = (SUBACK) objects.element();
        assertEquals(3, response.getReasonCodes().size());
        assertEquals((byte) QoS.EXACTLY_ONCE.getQosNumber(), response.getReasonCodes().get(0).getCode());
        assertEquals((byte) QoS.AT_LEAST_ONCE.getQosNumber(), response.getReasonCodes().get(1).getCode());
        assertEquals((byte) QoS.AT_MOST_ONCE.getQosNumber(), response.getReasonCodes().get(2).getCode());

        verify(clientSessionSubscriptionPersistence).addSubscriptions(eq("client"), eq(persistedTopics));
    }

    @Test
    public void test_subscription_metric() throws Exception {

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(new Topic("t1", QoS.AT_LEAST_ONCE), new Topic("t2", QoS.AT_LEAST_ONCE))), 1);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);
    }

    @Test
    public void test_send_invalid_subscribe_message() throws Exception {

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(new Topic("not/#/allowed", QoS.AT_LEAST_ONCE))), 1);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        //We need to make sure we got disconnected
        assertEquals(false, channel.isActive());
        verify(eventLog).clientWasDisconnected(any(Channel.class), anyString());
    }

    @Test
    public void single_topic_dont_batch() throws Exception {

        final HashSet<Topic> topics = Sets.newHashSet(Topic.topicFromString("topic1"));
        assertFalse(incomingSubscribeService.batch(topics));
    }

    @Test
    public void test_batch() throws Exception {

        final HashSet<Topic> topics = Sets.newHashSet(Topic.topicFromString("topic1"), Topic.topicFromString("topic2"));
        assertTrue(incomingSubscribeService.batch(topics));
    }

    @Test
    public void test_subscribe_wildcard_disabled_mqtt5() {
        when(mqttConfigurationService.wildcardSubscriptionsEnabled()).thenReturn(false);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final Topic topic = new Topic("#", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        assertFalse(channel.isActive());
        
        verify(clientSessionSubscriptionPersistence, never()).addSubscriptions(any(), any());
    }

    @Test
    public void test_subscribe_wildcard_disabled_mqtt3_1_1() {
        when(mqttConfigurationService.wildcardSubscriptionsEnabled()).thenReturn(false);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        final Topic topic = new Topic("#", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        assertFalse(channel.isActive());
        
        verify(clientSessionSubscriptionPersistence, never()).addSubscriptions(any(), any());
    }

    @Test
    public void test_subscribe_wildcard_disabled_mqtt3_1() {
        when(mqttConfigurationService.wildcardSubscriptionsEnabled()).thenReturn(false);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        final Topic topic = new Topic("#", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        assertFalse(channel.isActive());
        verify(clientSessionSubscriptionPersistence, never()).addSubscriptions(any(), any());
    }

    @Test
    public void test_shared_subscription_disabled_mqtt5() {
        when(mqttConfigurationService.sharedSubscriptionsEnabled()).thenReturn(false);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final Topic topic = new Topic("$share/group1/topic1", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        assertFalse(channel.isActive());

        verify(clientSessionSubscriptionPersistence, never()).addSubscriptions(any(), any());
    }

    @Test
    public void test_shared_subscription_disabled_mqtt3_1_1() {
        when(mqttConfigurationService.sharedSubscriptionsEnabled()).thenReturn(false);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        final Topic topic = new Topic("$share/group1/topic1", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        assertFalse(channel.isActive());

        verify(clientSessionSubscriptionPersistence, never()).addSubscriptions(any(), any());
    }

    @Test
    public void test_shared_subscription_disabled_mqtt3_1() {
        when(mqttConfigurationService.sharedSubscriptionsEnabled()).thenReturn(false);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        final Topic topic = new Topic("$share/group1/topic1", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        assertFalse(channel.isActive());

        verify(clientSessionSubscriptionPersistence, never()).addSubscriptions(any(), any());
    }


    @Test
    public void test_subscribe_single_authorized() throws Exception {

        final Topic topic = new Topic("test", QoS.AT_LEAST_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic)), 10);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("#").type(TopicPermission.PermissionType.ALLOW).build());

        channel.attr(ChannelAttributes.AUTH_PERMISSIONS).set(permissions);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final SUBACK response = channel.readOutbound();

        assertEquals(1, response.getReasonCodes().size());
        assertEquals((byte) QoS.AT_LEAST_ONCE.getQosNumber(), response.getReasonCodes().get(0).getCode());

        verify(clientSessionSubscriptionPersistence).addSubscription(eq("client"), same(topic));
    }

    @Test
    public void test_subscribe_single_not_authorized() throws Exception {

        final Topic topic = new Topic("test", QoS.AT_LEAST_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic)), 10);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("#").type(TopicPermission.PermissionType.DENY).build());

        channel.attr(ChannelAttributes.AUTH_PERMISSIONS).set(permissions);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final SUBACK response = channel.readOutbound();

        assertEquals(1, response.getReasonCodes().size());
        assertEquals(Mqtt5SubAckReasonCode.NOT_AUTHORIZED, response.getReasonCodes().get(0));

        verify(clientSessionSubscriptionPersistence, never()).addSubscription(eq("client"), same(topic));
    }

    @Test
    public void test_subscribe_multiple_authorized() throws Exception {

        final Topic topic1 = new Topic("test1", QoS.AT_LEAST_ONCE);
        final Topic topic2 = new Topic("test2", QoS.AT_MOST_ONCE);
        final Topic topic3 = new Topic("test3", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic1, topic2, topic3)), 10);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("#").type(TopicPermission.PermissionType.ALLOW).build());

        channel.attr(ChannelAttributes.AUTH_PERMISSIONS).set(permissions);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final SUBACK response = channel.readOutbound();
        assertEquals(3, response.getReasonCodes().size());
        assertEquals((byte) QoS.AT_LEAST_ONCE.getQosNumber(), response.getReasonCodes().get(0).getCode());
        assertEquals((byte) QoS.AT_MOST_ONCE.getQosNumber(), response.getReasonCodes().get(1).getCode());
        assertEquals((byte) QoS.EXACTLY_ONCE.getQosNumber(), response.getReasonCodes().get(2).getCode());

        verify(clientSessionSubscriptionPersistence).addSubscriptions(eq("client"), any(ImmutableSet.class));
    }

    @Test
    public void test_subscribe_multiple_all_not_authorized() throws Exception {

        final ArgumentCaptor<ImmutableSet> captor = ArgumentCaptor.forClass(ImmutableSet.class);
        final Topic topic1 = new Topic("test1", QoS.AT_LEAST_ONCE);
        final Topic topic2 = new Topic("test2", QoS.AT_MOST_ONCE);
        final Topic topic3 = new Topic("test3", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic1, topic2, topic3)), 10);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("#").type(TopicPermission.PermissionType.DENY).build());

        channel.attr(ChannelAttributes.AUTH_PERMISSIONS).set(permissions);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final SUBACK response = channel.readOutbound();
        assertEquals(3, response.getReasonCodes().size());
        assertEquals(Mqtt5SubAckReasonCode.NOT_AUTHORIZED, response.getReasonCodes().get(0));
        assertEquals(Mqtt5SubAckReasonCode.NOT_AUTHORIZED, response.getReasonCodes().get(1));
        assertEquals(Mqtt5SubAckReasonCode.NOT_AUTHORIZED, response.getReasonCodes().get(2));
        assertEquals("Not authorized to subscribe to topic 'test1' with QoS '1'. " +
                "Not authorized to subscribe to topic 'test2' with QoS '0'. " +
                "Not authorized to subscribe to topic 'test3' with QoS '2'. ", response.getReasonString());

        verify(clientSessionSubscriptionPersistence).addSubscriptions(eq("client"), captor.capture());
        assertEquals(0, captor.getValue().size());
    }

    @Test
    public void test_subscribe_multiple_some_not_authorized() throws Exception {

        final ArgumentCaptor<ImmutableSet> captor = ArgumentCaptor.forClass(ImmutableSet.class);
        final Topic topic1 = new Topic("test1", QoS.AT_LEAST_ONCE);
        final Topic topic2 = new Topic("test2", QoS.AT_MOST_ONCE);
        final Topic topic3 = new Topic("test3", QoS.EXACTLY_ONCE);
        final Topic topic4 = new Topic("test4", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic1, topic2, topic3, topic4)), 10);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("test1").type(TopicPermission.PermissionType.ALLOW).build());
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("test4").type(TopicPermission.PermissionType.ALLOW).build());
        permissions.setDefaultBehaviour(DefaultAuthorizationBehaviour.DENY);

        channel.attr(ChannelAttributes.AUTH_PERMISSIONS).set(permissions);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final SUBACK response = channel.readOutbound();
        assertEquals(4, response.getReasonCodes().size());
        assertEquals(Mqtt5SubAckReasonCode.GRANTED_QOS_1, response.getReasonCodes().get(0));
        assertEquals(Mqtt5SubAckReasonCode.NOT_AUTHORIZED, response.getReasonCodes().get(1));
        assertEquals(Mqtt5SubAckReasonCode.NOT_AUTHORIZED, response.getReasonCodes().get(2));
        assertEquals(Mqtt5SubAckReasonCode.GRANTED_QOS_2, response.getReasonCodes().get(3));
        assertEquals("Not authorized to subscribe to topic 'test2' with QoS '0'. " +
                "Not authorized to subscribe to topic 'test3' with QoS '2'. ", response.getReasonString());

        verify(clientSessionSubscriptionPersistence).addSubscriptions(eq("client"), captor.capture());
        assertEquals(2, captor.getValue().size());
        final ImmutableList<Topic> immutableList = captor.getValue().asList();
        for (Topic topic : immutableList) {
            assertTrue(topic.getTopic().equals("test1") || topic.getTopic().equals("test4"));
        }
    }

    @Test
    public void test_subscribe_topic_length_exceeded() throws Exception {
        when(restrictionsConfigurationService.maxTopicLength()).thenReturn(5);

        final ArgumentCaptor<ImmutableSet> captor = ArgumentCaptor.forClass(ImmutableSet.class);
        final Topic topic1 = new Topic("123456", QoS.AT_LEAST_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic1)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        assertFalse(channel.isActive());
    }

    @Test
    public void test_process_authorizers_present_no_default() throws Exception {

        final ArgumentCaptor<ImmutableSet> authorizedTopicsCaptor = ArgumentCaptor.forClass(ImmutableSet.class);
        final Topic topic1 = new Topic("test1", QoS.AT_LEAST_ONCE);
        final Topic topic2 = new Topic("test2", QoS.AT_MOST_ONCE);
        final Topic topic3 = new Topic("test3", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic1, topic2, topic3)), 10);

        channel.attr(ChannelAttributes.AUTH_PERMISSIONS).set(null);

        incomingSubscribeService.processSubscribe(ctx, subscribe,
                new Mqtt5SubAckReasonCode[]{Mqtt5SubAckReasonCode.GRANTED_QOS_1, null, null},
                new String[3], true);

        final SUBACK response = channel.readOutbound();

        assertEquals(3, response.getReasonCodes().size());
        assertEquals(Mqtt5SubAckReasonCode.GRANTED_QOS_1, response.getReasonCodes().get(0));
        assertEquals(Mqtt5SubAckReasonCode.NOT_AUTHORIZED, response.getReasonCodes().get(1));
        assertEquals(Mqtt5SubAckReasonCode.NOT_AUTHORIZED, response.getReasonCodes().get(2));

        verify(clientSessionSubscriptionPersistence).addSubscriptions(eq("client"), authorizedTopicsCaptor.capture());
        assertEquals(1, authorizedTopicsCaptor.getValue().size());
    }

}