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
package com.hivemq.mqtt;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.services.InternalPublishServiceImpl;
import com.hivemq.mqtt.services.PublishDistributor;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import com.hivemq.mqtt.topic.SubscriptionFlags;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.InitFutureUtilsExecutorRule;
import util.TestException;
import util.TestMessageUtil;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static com.hivemq.mqtt.handler.publish.PublishReturnCode.FAILED;
import static com.hivemq.mqtt.handler.publish.PublishReturnCode.NO_MATCHING_SUBSCRIBERS;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("unchecked")
public class InternalPublishServiceImplTest {

    @Rule
    public InitFutureUtilsExecutorRule initFutureUtilsExecutorRule = new InitFutureUtilsExecutorRule();

    @Mock
    private RetainedMessagePersistence retainedMessagePersistence;

    @Mock
    private LocalTopicTree topicTree;

    @Mock
    private PublishDistributor publishDistributor;

    private ExecutorService executorService;

    private InternalPublishServiceImpl publishService;

    @Before
    public void before() {

        MockitoAnnotations.initMocks(this);

        executorService = MoreExecutors.newDirectExecutorService();

        when(publishDistributor.distributeToNonSharedSubscribers(anyMap(), any(PUBLISH.class), eq(executorService))).thenReturn(Futures.immediateFuture(null));
        when(publishDistributor.distributeToSharedSubscribers(anySet(), any(PUBLISH.class), eq(executorService))).thenReturn(Futures.immediateFuture(null));

        publishService = new InternalPublishServiceImpl(
                retainedMessagePersistence,
                topicTree,
                publishDistributor);
    }

    @Test(timeout = 20000)
    public void test_retained_message_remove() throws Exception {

        when(topicTree.getSubscribers(anyString())).thenReturn(ImmutableSet.of());
        publishService = new InternalPublishServiceImpl(retainedMessagePersistence, topicTree, publishDistributor);

        final PUBLISH publish = TestMessageUtil.createMqtt3Publish("hivemqId", "subonly", QoS.AT_LEAST_ONCE, new byte[0], true);

        when(retainedMessagePersistence.remove(anyString())).thenReturn(Futures.immediateFuture(null));

        publishService.publish(publish, executorService, "sender").get();

        verify(retainedMessagePersistence).remove("subonly");

    }

    @Test(timeout = 20000)
    public void test_retained_message_remove_failed() throws Exception {

        when(topicTree.getSubscribers(anyString())).thenReturn(ImmutableSet.of());
        publishService = new InternalPublishServiceImpl(retainedMessagePersistence, topicTree, publishDistributor);

        final PUBLISH publish = TestMessageUtil.createMqtt3Publish("hivemqId", "subonly", QoS.AT_LEAST_ONCE, new byte[0], true);

        when(retainedMessagePersistence.remove(anyString())).thenReturn(Futures.immediateFailedFuture(TestException.INSTANCE));

        publishService.publish(publish, executorService, "sender").get();

        verify(retainedMessagePersistence).remove("subonly");
    }

    @Test(timeout = 20000)
    public void test_no_subs() throws ExecutionException, InterruptedException {

        when(topicTree.getSubscribers(anyString())).thenReturn(ImmutableSet.of());

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic");

        final PublishReturnCode returnCode = publishService.publish(publish, executorService, "sub1").get();

        verify(publishDistributor, never()).distributeToNonSharedSubscribers(anyMap(), any(), any());

        assertEquals(NO_MATCHING_SUBSCRIBERS, returnCode);
    }

    @Test(timeout = 20000)
    public void test_no_local() {

        final byte noLocalFlag = SubscriptionFlags.getDefaultFlags(false, false, true);
        final SubscriberWithIdentifiers sub1 = new SubscriberWithIdentifiers("sub1", 1, noLocalFlag, null);
        final SubscriberWithIdentifiers sub2 = new SubscriberWithIdentifiers("sub2", 1, (byte) 0, null);

        when(topicTree.getSubscribers("topic")).thenReturn(ImmutableSet.of(sub1, sub2));

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic");

        publishService.publish(publish, executorService, "sub1");

        final ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(publishDistributor, atLeastOnce()).distributeToNonSharedSubscribers(mapArgumentCaptor.capture(), any(), any());

        final Map map = mapArgumentCaptor.getAllValues().get(0);

        assertEquals(1, map.size());
        assertNull(map.get("sub1"));
        assertNotNull(map.get("sub2"));
    }

    @Test(timeout = 20000)
    public void test_multiple_subs() {

        final SubscriberWithIdentifiers sub1 = new SubscriberWithIdentifiers("sub1", 1, (byte) 0, null);
        final SubscriberWithIdentifiers sub2 = new SubscriberWithIdentifiers("sub2", 1, (byte) 0, null);

        when(topicTree.getSubscribers("topic")).thenReturn(ImmutableSet.of(sub1, sub2));

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic");

        publishService.publish(publish, executorService, "sub1");

        final ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(publishDistributor, atLeastOnce()).distributeToNonSharedSubscribers(mapArgumentCaptor.capture(), any(), any());

        final Map map = mapArgumentCaptor.getAllValues().get(0);

        assertEquals(2, map.size());
        assertNotNull(map.get("sub1"));
        assertNotNull(map.get("sub2"));
    }

    @Test(timeout = 20000)
    public void test_reset_dup_flag() {

        final SubscriberWithIdentifiers sub1 = new SubscriberWithIdentifiers("sub1", 1, (byte) 0, null);
        final SubscriberWithIdentifiers sub2 = new SubscriberWithIdentifiers("sub2", 1, (byte) 0, null);

        when(topicTree.getSubscribers("topic")).thenReturn(ImmutableSet.of(sub1, sub2));

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic");
        publish.setDuplicateDelivery(true);

        publishService.publish(publish, executorService, "sub1");

        final ArgumentCaptor<PUBLISH> captor = ArgumentCaptor.forClass(PUBLISH.class);
        verify(publishDistributor, atLeastOnce()).distributeToNonSharedSubscribers(anyMap(), captor.capture(), any());

        final PUBLISH value = captor.getValue();

        assertFalse(value.isDuplicateDelivery());
    }

    @Test(timeout = 20000)
    public void test_multiple_subs_failed() throws ExecutionException, InterruptedException {

        final SubscriberWithIdentifiers sub1 = new SubscriberWithIdentifiers("sub1", 1, (byte) 0, null);
        final SubscriberWithIdentifiers sub2 = new SubscriberWithIdentifiers("sub2", 1, (byte) 0, null);

        when(topicTree.getSubscribers("topic")).thenReturn(ImmutableSet.of(sub1, sub2));

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic");

        when(publishDistributor.distributeToNonSharedSubscribers(anyMap(), any(), any())).thenReturn(Futures.immediateFailedFuture(TestException.INSTANCE));

        final PublishReturnCode returnCode = publishService.publish(publish, executorService, "sub1").get();

        assertEquals(FAILED, returnCode);
    }

    @Test(timeout = 20000)
    public void test_shared_subs_different_groups() {

        final SubscriberWithIdentifiers sub1 = new SubscriberWithIdentifiers("sub1", 1, (byte) 2, "group1", null, "topic");
        final SubscriberWithIdentifiers sub2 = new SubscriberWithIdentifiers("sub2", 1, (byte) 2, "group2", null, "topic");

        when(topicTree.getSubscribers("topic")).thenReturn(ImmutableSet.of(sub1, sub2));

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic");

        publishService.publish(publish, executorService, "sub1");

        final ArgumentCaptor<Set> setArgumentCaptor = ArgumentCaptor.forClass(Set.class);
        verify(publishDistributor, atLeastOnce()).distributeToSharedSubscribers(setArgumentCaptor.capture(), any(), any());

        final Set set = setArgumentCaptor.getAllValues().get(0);

        assertEquals(2, set.size());

        assertTrue(set.contains("group1/topic"));
        assertTrue(set.contains("group2/topic"));
    }

    @Test(timeout = 20000)
    public void test_shared_subs_same_group() {

        final SubscriberWithIdentifiers sub1 = new SubscriberWithIdentifiers("sub1", 1, (byte) 2, "group1", null, "topic");
        final SubscriberWithIdentifiers sub2 = new SubscriberWithIdentifiers("sub2", 1, (byte) 2, "group1", null, "#");

        when(topicTree.getSubscribers("topic")).thenReturn(ImmutableSet.of(sub1, sub2));

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic");

        publishService.publish(publish, executorService, "sub1");

        final ArgumentCaptor<Set> sharedSubsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(publishDistributor, atLeastOnce()).distributeToSharedSubscribers(sharedSubsCaptor.capture(), any(), any());

        final Set set = sharedSubsCaptor.getAllValues().get(0);

        assertEquals(2, set.size());

        assertTrue(set.contains("group1/topic"));
        assertTrue(set.contains("group1/#"));
    }

    @Test(timeout = 20000)
    public void test_mixed_subs() {

        final SubscriberWithIdentifiers sub1 = new SubscriberWithIdentifiers("sub1", 1, (byte) 2, "group1", null, "topic");
        final SubscriberWithIdentifiers sub2 = new SubscriberWithIdentifiers("sub2", 1, (byte) 0, null);

        when(topicTree.getSubscribers("topic")).thenReturn(ImmutableSet.of(sub1, sub2));

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic");

        publishService.publish(publish, executorService, "sub1");

        final ArgumentCaptor<Set> sharedSubsCaptor = ArgumentCaptor.forClass(Set.class);
        final ArgumentCaptor<Map> subsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(publishDistributor, atLeastOnce()).distributeToSharedSubscribers(sharedSubsCaptor.capture(), any(), any());
        verify(publishDistributor, atLeastOnce()).distributeToNonSharedSubscribers(subsCaptor.capture(), any(), any());

        final Set<String> sharedSet = sharedSubsCaptor.getAllValues().get(0);
        final Map<String, Integer> map = subsCaptor.getAllValues().get(0);

        assertEquals(1, map.size());
        assertEquals(1, sharedSet.size());

        assertNull(map.get("sub1"));
        assertNotNull(map.get("sub2"));

        assertTrue(sharedSet.contains("group1/topic"));
    }


}