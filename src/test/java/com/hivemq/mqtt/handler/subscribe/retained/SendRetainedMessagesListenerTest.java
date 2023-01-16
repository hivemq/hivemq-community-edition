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
package com.hivemq.mqtt.handler.subscribe.retained;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientsession.callback.SubscriptionResult;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.retained.RetainedMessagePersistence;

import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@SuppressWarnings("NullabilityAnnotations")
public class SendRetainedMessagesListenerTest {

    @Mock
    private RetainedMessagePersistence retainedMessagePersistence;

    private Set<Topic> ignoredTopics;

    @Mock
    private ChannelFuture channelFuture;

    @Mock
    private ClientQueuePersistence queuePersistence;

    @Mock
    private MqttConfigurationService mqttConfigurationService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ignoredTopics = new LinkedHashSet<>();
    }

    @Test
    public void test_no_retained_message_available() throws Exception {

        final List<SubscriptionResult> subscriptions =
                newArrayList(subResult(new Topic("#", QoS.AT_LEAST_ONCE), false));
        final SendRetainedMessagesListener listener = createListener(subscriptions, ignoredTopics);
        final EmbeddedChannel channel = new EmbeddedChannel();
        final RetainedMessage nullMessage = null;

        when(retainedMessagePersistence.get("topic")).thenReturn(Futures.immediateFuture(nullMessage));

        final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        final Set<String> set = builder.build();
        when(retainedMessagePersistence.getWithWildcards("#")).thenReturn(Futures.immediateFuture(set));

        listener.operationComplete(channel.newSucceededFuture());
        channel.runPendingTasks();

        assertEquals(0, channel.outboundMessages().size());
    }

    @Test
    public void test_channel_null() throws Exception {

        final List<SubscriptionResult> subscriptions =
                newArrayList(subResult(new Topic("#", QoS.AT_LEAST_ONCE), false));
        final SendRetainedMessagesListener listener = createListener(subscriptions, ignoredTopics);

        when(channelFuture.isSuccess()).thenReturn(true);
        when(channelFuture.channel()).thenReturn(null);

        listener.operationComplete(channelFuture);

        verify(retainedMessagePersistence, never()).get(anyString());

    }

    @Test
    public void test_channel_inactive() throws Exception {

        final List<SubscriptionResult> subscriptions =
                newArrayList(subResult(new Topic("#", QoS.AT_LEAST_ONCE), false));
        final SendRetainedMessagesListener listener = createListener(subscriptions, ignoredTopics);

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.close();

        when(channelFuture.isSuccess()).thenReturn(true);
        when(channelFuture.channel()).thenReturn(channel);

        listener.operationComplete(channelFuture);

        verify(retainedMessagePersistence, never()).get(anyString());

    }

    @Test
    public void test_subscription_null() throws Exception {

        final List<SubscriptionResult> subscriptions = new ArrayList<>();
        subscriptions.add(null);

        final SendRetainedMessagesListener listener = createListener(subscriptions, ignoredTopics);

        final EmbeddedChannel channel = new EmbeddedChannel();

        listener.operationComplete(channel.newSucceededFuture());

        verify(retainedMessagePersistence, never()).get(anyString());

    }

    @Test
    public void test_filter_ignored_topics() throws Exception {
        final Topic anothertopic = new Topic("anothertopic", QoS.AT_LEAST_ONCE);
        final List<SubscriptionResult> subscriptions = newArrayList(
                subResult(new Topic("topic", QoS.AT_LEAST_ONCE), false),
                subResult(anothertopic, false));
        ignoredTopics.add(anothertopic);
        final SendRetainedMessagesListener listener = createListener(subscriptions, ignoredTopics);
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientId("client");

        when(retainedMessagePersistence.get("topic")).thenReturn(Futures.immediateFuture(
                new RetainedMessage("test".getBytes(UTF_8), QoS.EXACTLY_ONCE, 1L,
                        MqttConfigurationDefaults.TTL_DISABLED)));
        when(retainedMessagePersistence.get("anothertopic")).thenReturn(Futures.immediateFuture(
                new RetainedMessage("test".getBytes(UTF_8), QoS.EXACTLY_ONCE, 1L,
                        MqttConfigurationDefaults.TTL_DISABLED)));


        listener.operationComplete(channel.newSucceededFuture());
        channel.runPendingTasks();

        verify(queuePersistence).add(eq("client"), eq(false), anyList(), eq(true), anyLong());
    }

    @Test
    public void test_wildcard_subscription_retained_messages_available_send() throws Exception {

        when(retainedMessagePersistence.get("topic")).thenReturn(Futures.immediateFuture(
                new RetainedMessage("test".getBytes(UTF_8), QoS.EXACTLY_ONCE, 1L,
                        MqttConfigurationDefaults.TTL_DISABLED)));

        final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        builder.add("topic");
        final Set<String> set = builder.build();
        when(retainedMessagePersistence.getWithWildcards("#")).thenReturn(Futures.immediateFuture(set));

        final Topic topic = new Topic("#", QoS.EXACTLY_ONCE, false, false, Mqtt5RetainHandling.SEND, 1);
        final List<SubscriptionResult> subscriptions = newArrayList(subResult(topic, false));
        final SendRetainedMessagesListener listener = createListener(subscriptions, ignoredTopics);
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientId("client");

        listener.operationComplete(channel.newSucceededFuture());

        channel.runPendingTasks();

        final ArgumentCaptor<List<PUBLISH>> captor =
                ArgumentCaptor.forClass((Class<List<PUBLISH>>) (Class) ArrayList.class);
        verify(queuePersistence).add(eq("client"), eq(false), captor.capture(), eq(true), anyLong());

        final PUBLISH publish = captor.getValue().get(0);
        assertEquals("topic", publish.getTopic());
        assertEquals(QoS.EXACTLY_ONCE, publish.getQoS());
        assertArrayEquals("test".getBytes(UTF_8), publish.getPayload());
        assertEquals(true, publish.isRetain());
    }

    @Test
    public void test_wildcard_subscription_retained_messages_available_do_not_send() throws Exception {

        when(retainedMessagePersistence.get("topic")).thenReturn(Futures.immediateFuture(
                new RetainedMessage("test".getBytes(UTF_8), QoS.EXACTLY_ONCE, 1L,
                        MqttConfigurationDefaults.TTL_DISABLED)));

        final Set<String> set = ImmutableSet.of("topic");
        when(retainedMessagePersistence.getWithWildcards("#")).thenReturn(Futures.immediateFuture(set));

        final Topic topic = new Topic("#", QoS.EXACTLY_ONCE, false, false, Mqtt5RetainHandling.DO_NOT_SEND, 1);
        final List<SubscriptionResult> subscriptions = newArrayList(subResult(topic, false));
        final SendRetainedMessagesListener listener = createListener(subscriptions, ignoredTopics);
        final EmbeddedChannel channel = new EmbeddedChannel();

        listener.operationComplete(channel.newSucceededFuture());

        channel.runPendingTasks();

        assertEquals(0, channel.outboundMessages().size());
    }

    @Test
    public void test_wildcard_subscription_retained_messages_available_send_if_not_existing_exists() throws Exception {

        when(retainedMessagePersistence.get("topic")).thenReturn(Futures.immediateFuture(
                new RetainedMessage("test".getBytes(UTF_8), QoS.EXACTLY_ONCE, 1L,
                        MqttConfigurationDefaults.TTL_DISABLED)));

        final Set<String> set = ImmutableSet.of("topic");
        when(retainedMessagePersistence.getWithWildcards("#")).thenReturn(Futures.immediateFuture(set));

        final Topic topic =
                new Topic("#", QoS.EXACTLY_ONCE, false, false, Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST,
                        1);
        final List<SubscriptionResult> subscriptions = newArrayList(subResult(topic, true));
        final SendRetainedMessagesListener listener = createListener(subscriptions, ignoredTopics);
        final EmbeddedChannel channel = new EmbeddedChannel();

        listener.operationComplete(channel.newSucceededFuture());

        channel.runPendingTasks();

        assertEquals(0, channel.outboundMessages().size());
    }

    @Test
    public void test_wildcard_subscription_retained_messages_available_send_if_not_existing_does_not_exist()
            throws Exception {

        when(retainedMessagePersistence.get("topic")).thenReturn(Futures.immediateFuture(
                new RetainedMessage("test".getBytes(UTF_8), QoS.EXACTLY_ONCE, 1L,
                        MqttConfigurationDefaults.TTL_DISABLED)));

        final Set<String> set = ImmutableSet.of("topic");
        when(retainedMessagePersistence.getWithWildcards("#")).thenReturn(Futures.immediateFuture(set));

        final Topic topic =
                new Topic("#", QoS.EXACTLY_ONCE, false, false, Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST,
                        1);
        final List<SubscriptionResult> subscriptions = newArrayList(subResult(topic, false));
        final SendRetainedMessagesListener listener = createListener(subscriptions, ignoredTopics);
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientId("client");

        listener.operationComplete(channel.newSucceededFuture());

        channel.runPendingTasks();

        final ArgumentCaptor<List<PUBLISH>> captor =
                ArgumentCaptor.forClass((Class<List<PUBLISH>>) (Class) ArrayList.class);
        verify(queuePersistence).add(eq("client"), eq(false), captor.capture(), eq(true), anyLong());

        final PUBLISH publish = captor.getValue().get(0);
        assertEquals("topic", publish.getTopic());
        assertEquals(QoS.EXACTLY_ONCE, publish.getQoS());
        assertArrayEquals("test".getBytes(UTF_8), publish.getPayload());
        assertEquals(true, publish.isRetain());
    }

    @Test
    public void test_wildcard_subscription_retained_messages_available_no_wildcard() throws Exception {

        when(retainedMessagePersistence.get("topic")).thenReturn(Futures.immediateFuture(
                new RetainedMessage("test".getBytes(UTF_8), QoS.EXACTLY_ONCE, 1L,
                        MqttConfigurationDefaults.TTL_DISABLED)));
        when(retainedMessagePersistence.get("topic2")).thenReturn(Futures.immediateFuture(
                new RetainedMessage("test".getBytes(UTF_8), QoS.AT_MOST_ONCE, 1L,
                        MqttConfigurationDefaults.TTL_DISABLED)));

        final ImmutableSet<String> set = ImmutableSet.of("topic", "topic2");
        when(retainedMessagePersistence.getWithWildcards("#")).thenReturn(Futures.immediateFuture(set));
        final List<SubscriptionResult> subscriptions = newArrayList(
                subResult(new Topic("topic", QoS.EXACTLY_ONCE), false),
                subResult(new Topic("topic2", QoS.AT_MOST_ONCE), false));
        final SendRetainedMessagesListener listener = createListener(subscriptions, ignoredTopics);
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientId("client");

        listener.operationComplete(channel.newSucceededFuture());

        channel.runPendingTasks();
        channel.runPendingTasks();

        final ArgumentCaptor<List<PUBLISH>> captor =
                ArgumentCaptor.forClass((Class<List<PUBLISH>>) (Class) ArrayList.class);
        verify(queuePersistence).add(eq("client"), eq(false), captor.capture(), eq(true), anyLong());

        final PUBLISH publish = captor.getAllValues().get(0).get(0);
        assertEquals("topic", publish.getTopic());
        assertEquals(QoS.EXACTLY_ONCE, publish.getQoS());
        assertArrayEquals("test".getBytes(UTF_8), publish.getPayload());
        assertEquals(true, publish.isRetain());

        final PUBLISH publish2 = (PUBLISH) channel.outboundMessages().poll();
        assertEquals("topic2", publish2.getTopic());
        assertEquals(QoS.AT_MOST_ONCE, publish2.getQoS());
        assertArrayEquals("test".getBytes(UTF_8), publish2.getPayload());
        assertEquals(true, publish2.isRetain());
    }

    @Test
    public void test_wildcard_subscription_qos_downgraded_to_actual_subscription() throws Exception {

        when(retainedMessagePersistence.get("topic")).thenReturn(Futures.immediateFuture(
                new RetainedMessage("test".getBytes(UTF_8), QoS.EXACTLY_ONCE, 1L,
                        MqttConfigurationDefaults.TTL_DISABLED)));

        final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        builder.add("topic");
        final Set<String> set = builder.build();
        when(retainedMessagePersistence.getWithWildcards("#")).thenReturn(Futures.immediateFuture(set));

        final List<SubscriptionResult> subscriptions = newArrayList(subResult(new Topic("#", QoS.AT_MOST_ONCE), false));
        final SendRetainedMessagesListener listener = createListener(subscriptions, ignoredTopics);
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));

        listener.operationComplete(channel.newSucceededFuture());

        channel.runPendingTasks();

        final PUBLISH publish = (PUBLISH) channel.outboundMessages().element();
        assertEquals(QoS.AT_MOST_ONCE, publish.getQoS());
    }

    @Test
    public void test_wildcard_subscription_qos_not_upgraded_to_actual_subscription() throws Exception {

        when(retainedMessagePersistence.get("topic")).thenReturn(Futures.immediateFuture(
                new RetainedMessage("test".getBytes(UTF_8), QoS.AT_MOST_ONCE, 1L,
                        MqttConfigurationDefaults.TTL_DISABLED)));

        final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        builder.add("topic");
        final Set<String> set = builder.build();
        when(retainedMessagePersistence.getWithWildcards("#")).thenReturn(Futures.immediateFuture(set));

        final List<SubscriptionResult> subscriptions = newArrayList(subResult(new Topic("#", QoS.EXACTLY_ONCE), false));
        final SendRetainedMessagesListener listener = createListener(subscriptions, ignoredTopics);
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));

        listener.operationComplete(channel.newSucceededFuture());
        channel.runPendingTasks();

        final PUBLISH publish = (PUBLISH) channel.outboundMessages().element();
        assertEquals(QoS.AT_MOST_ONCE, publish.getQoS());
    }

    @Test
    public void test_on_failure_exception_handling() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));

        final SendRetainedMessageResultListener sendRetainedMessageResultListener =
                createSendRetainedMessageSingleListener(channel);
        sendRetainedMessageResultListener.onFailure(new ClosedChannelException());

        // We can not test Errors.NativeIoException. This exception can not be initialized.
        //sendRetainedMessageSingleListener.onFailure(new Errors.NativeIoException("some IOException", 1));

        // Channel still connected
        Assert.assertTrue(channel.isActive());

        sendRetainedMessageResultListener.onFailure(new IOException("Broken pipe"));

        Assert.assertTrue(channel.isActive());

        sendRetainedMessageResultListener.onFailure(new Exception("test"));

        Assert.assertTrue(channel.isActive());
    }

    @Test
    public void test_on_failure_throwable_handling() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        createSendRetainedMessageSingleListener(channel).onFailure(new Throwable("test"));

        // tests if the test finish successfully. No need for assertion.
    }

    @Test(expected = Error.class)
    public void test_on_failure_error_handling() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        createSendRetainedMessageSingleListener(channel).onFailure(new Error());
    }

    @Test
    public void test_subscription_shared() throws Exception {

        when(retainedMessagePersistence.get("topic")).thenReturn(Futures.immediateFuture(
                new RetainedMessage("test".getBytes(UTF_8), QoS.EXACTLY_ONCE, 1L,
                        MqttConfigurationDefaults.TTL_DISABLED)));

        final Set<String> set = ImmutableSet.of("topic");
        when(retainedMessagePersistence.getWithWildcards("#")).thenReturn(Futures.immediateFuture(set));

        final Topic topic = new Topic("#", QoS.EXACTLY_ONCE);
        final List<SubscriptionResult> subscriptions = newArrayList(new SubscriptionResult(topic, false, "shareName"));
        final SendRetainedMessagesListener listener = createListener(subscriptions, ignoredTopics);
        final EmbeddedChannel channel = new EmbeddedChannel();

        listener.operationComplete(channel.newSucceededFuture());

        channel.runPendingTasks();

        assertEquals(0, channel.outboundMessages().size());
    }

    @Test
    public void test_wildcard_subscription_batched_send() throws Exception {

        when(retainedMessagePersistence.get("topic")).thenReturn(Futures.immediateFuture(
                new RetainedMessage("test".getBytes(UTF_8), QoS.AT_LEAST_ONCE, 1L,
                        MqttConfigurationDefaults.TTL_DISABLED)));
        when(retainedMessagePersistence.get("topic2")).thenReturn(Futures.immediateFuture(
                new RetainedMessage("test".getBytes(UTF_8), QoS.AT_LEAST_ONCE, 1L,
                        MqttConfigurationDefaults.TTL_DISABLED)));

        final ImmutableSet<String> set = ImmutableSet.of("topic", "topic2");
        when(retainedMessagePersistence.getWithWildcards("#")).thenReturn(Futures.immediateFuture(set));
        when(queuePersistence.add(eq("client"), eq(false), anyList(), eq(true), anyLong())).thenReturn(
                Futures.immediateFuture(null));
        final List<SubscriptionResult> subscriptions = newArrayList(
                subResult(new Topic("topic", QoS.AT_LEAST_ONCE), false),
                subResult(new Topic("topic2", QoS.AT_LEAST_ONCE), false));
        final SendRetainedMessagesListener listener = createListener(subscriptions, ignoredTopics);
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientId("client");

        listener.operationComplete(channel.newSucceededFuture());

        channel.runPendingTasks();
        channel.runPendingTasks();

        final ArgumentCaptor<List<PUBLISH>> captor =
                ArgumentCaptor.forClass((Class<List<PUBLISH>>) (Class) ArrayList.class);
        verify(queuePersistence, timeout(5000).times(2)).add(eq("client"), eq(false),
                captor.capture(), eq(true), anyLong());

        final PUBLISH publish = captor.getAllValues().get(0).get(0);
        assertEquals("topic", publish.getTopic());
        assertEquals(QoS.AT_LEAST_ONCE, publish.getQoS());
        assertArrayEquals("test".getBytes(UTF_8), publish.getPayload());
        assertEquals(true, publish.isRetain());

        final PUBLISH publish2 = captor.getAllValues().get(1).get(0);
        assertEquals("topic2", publish2.getTopic());
        assertEquals(QoS.AT_LEAST_ONCE, publish2.getQoS());
        assertArrayEquals("test".getBytes(UTF_8), publish2.getPayload());
        assertEquals(true, publish2.isRetain());
    }

    private SendRetainedMessagesListener createListener(
            final List<SubscriptionResult> subscriptions, final Set<Topic> ignoredTopics) {

        final RetainedMessagesSender retainedMessagesSender = new RetainedMessagesSender(new HivemqId(),
                mock(PublishPayloadPersistence.class), retainedMessagePersistence, queuePersistence,
                mqttConfigurationService);

        return new SendRetainedMessagesListener(
                subscriptions, ignoredTopics, retainedMessagePersistence, retainedMessagesSender);
    }

    private SendRetainedMessageResultListener createSendRetainedMessageSingleListener(final EmbeddedChannel channel) {
        final Topic topic = new Topic("topic", QoS.AT_LEAST_ONCE);

        final RetainedMessagesSender retainedMessagesSender = new RetainedMessagesSender(new HivemqId(),
                mock(PublishPayloadPersistence.class), retainedMessagePersistence, queuePersistence,
                mqttConfigurationService);

        return new SendRetainedMessageResultListener(channel, topic, retainedMessagesSender);

    }

    private SubscriptionResult subResult(final Topic topic, final boolean subscriptionAlreadyExisted) {
        return new SubscriptionResult(topic, subscriptionAlreadyExisted, null);
    }

}