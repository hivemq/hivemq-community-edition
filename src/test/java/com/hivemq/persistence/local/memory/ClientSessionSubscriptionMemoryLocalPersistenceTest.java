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
package com.hivemq.persistence.local.memory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.iteration.BucketChunkResult;
import com.hivemq.metrics.HiveMQMetrics;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.persistence.IterablePersistenceEntry;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.util.LocalPersistenceFileUtil;
import com.hivemq.util.ObjectMemoryEstimation;
import net.jodah.concurrentunit.Waiter;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.hivemq.mqtt.message.subscribe.Mqtt5Topic.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 */
@SuppressWarnings("NullabilityAnnotations")
public class ClientSessionSubscriptionMemoryLocalPersistenceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private LocalPersistenceFileUtil localPersistenceFileUtil;

    private ClientSessionSubscriptionMemoryLocalPersistence persistence;

    private final int bucketCount = 4;
    private MetricRegistry metricRegistry;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);

        InternalConfigurations.PERSISTENCE_BUCKET_COUNT.set(bucketCount);
        when(localPersistenceFileUtil.getVersionedLocalPersistenceFolder(anyString(), anyString())).thenReturn(temporaryFolder.newFolder());
        metricRegistry = new MetricRegistry();

        persistence = new ClientSessionSubscriptionMemoryLocalPersistence(metricRegistry);
    }

    @Test
    public void test_add_get_subscriptions() {

        persistence.addSubscriptions("clientid", ImmutableSet.of(new Topic("topic1", QoS.AT_MOST_ONCE), new Topic("topic2", QoS.AT_MOST_ONCE), new Topic("topic3", QoS.AT_MOST_ONCE)), 123L, BucketUtils.getBucket("clientid", bucketCount));

        final ImmutableSet<Topic> subscriptions = persistence.getSubscriptions("clientid");

        assertEquals(3, subscriptions.size());

        final Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();
        assertTrue(value > 0L);

        int size = 0;
        for (final Topic subscription : subscriptions) {
            size += ObjectMemoryEstimation.objectRefSize();
            size += subscription.getEstimatedSize();
        }
        size += ObjectMemoryEstimation.stringSize("clientid");
        size += IterablePersistenceEntry.getFixedSize();

        assertEquals(size, value.intValue());

    }

    @Test
    public void test_add_remove_subscriptions_none_remaining() {

        persistence.addSubscriptions("clientid", ImmutableSet.of(new Topic("topic1", QoS.AT_MOST_ONCE), new Topic("topic2", QoS.AT_MOST_ONCE), new Topic("topic3", QoS.AT_MOST_ONCE)), 123L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.removeSubscriptions("clientid", ImmutableSet.of("topic1", "topic2", "topic3"), 123L, BucketUtils.getBucket("clientid", bucketCount));

        final ImmutableSet<Topic> subscriptions = persistence.getSubscriptions("clientid");

        assertEquals(0, subscriptions.size());

        final Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();
        assertEquals(0, value.intValue());
    }

    @Test
    public void test_add_remove_subscriptions_with_remaining() {

        persistence.addSubscriptions("clientid", ImmutableSet.of(new Topic("topic1", QoS.AT_MOST_ONCE), new Topic("topic2", QoS.AT_MOST_ONCE), new Topic("topic3", QoS.AT_MOST_ONCE)), 123L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.removeSubscriptions("clientid", ImmutableSet.of("topic1", "topic2"), 123L, BucketUtils.getBucket("clientid", bucketCount));

        final ImmutableSet<Topic> subscriptions = persistence.getSubscriptions("clientid");

        assertEquals(1, subscriptions.size());

        final Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();
        assertTrue(value > 0L);

        int size = 0;
        for (final Topic subscription : subscriptions) {
            size += ObjectMemoryEstimation.objectRefSize();
            size += subscription.getEstimatedSize();
        }
        size += ObjectMemoryEstimation.stringSize("clientid");
        size += IterablePersistenceEntry.getFixedSize();

        assertEquals(size, value.intValue());
    }

    @Test(expected = NullPointerException.class)
    public void test_add_get_subscriptions_client_id_null_check() {

        persistence.addSubscriptions(null, ImmutableSet.of(new Topic("topic1", QoS.AT_MOST_ONCE), new Topic("topic2", QoS.AT_MOST_ONCE), new Topic("topic3", QoS.AT_MOST_ONCE)), 123L, BucketUtils.getBucket("clientid", bucketCount));
        final Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();
        assertEquals(0, value.intValue());
    }

    @Test(expected = NullPointerException.class)
    public void test_add_get_subscriptions_client_topics_null_check() {

        persistence.addSubscriptions("clientid", null, 123L, BucketUtils.getBucket("clientid", bucketCount));
        final Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();
        assertEquals(0, value.intValue());
    }

    @Test(expected = IllegalStateException.class)
    public void test_add_get_subscriptions_client_timestamp_state_check() {

        persistence.addSubscriptions("clientid", ImmutableSet.of(new Topic("topic1", QoS.AT_MOST_ONCE), new Topic("topic2", QoS.AT_MOST_ONCE), new Topic("topic3", QoS.AT_MOST_ONCE)), -123L, BucketUtils.getBucket("clientid", bucketCount));
        final Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();
        assertEquals(0, value.intValue());
    }

    @Test
    public void test_add_get_subscription() {

        persistence.addSubscription("clientid", new Topic("topic", QoS.AT_LEAST_ONCE), 123L, BucketUtils.getBucket("clientid", bucketCount));

        final ImmutableSet<Topic> subscriptions = persistence.getSubscriptions("clientid");

        assertEquals(1, subscriptions.size());
        final Topic next = subscriptions.iterator().next();
        assertEquals("topic", next.getTopic());
        assertEquals(QoS.AT_LEAST_ONCE, next.getQoS());

        persistence.addSubscription("clientid", new Topic("topic2", QoS.EXACTLY_ONCE), 431L, BucketUtils.getBucket("clientid", bucketCount));

        final ImmutableSet<Topic> subscriptions2 = persistence.getSubscriptions("clientid");

        assertEquals(2, subscriptions2.size());

        final UnmodifiableIterator<Topic> iterator = subscriptions2.iterator();
        boolean topic2Found = false;
        while (iterator.hasNext()) {
            final Topic topic = iterator.next();
            if (topic.getTopic().equals("topic2")) {
                assertEquals(QoS.EXACTLY_ONCE, topic.getQoS());
                topic2Found = true;
            }
        }
        assertTrue(topic2Found);

    }

    @Test
    public void test_add_get_subscription_with_same_topic() {

        persistence.addSubscription("clientid", new Topic("topic", QoS.AT_LEAST_ONCE), 123L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid", new Topic("topic", QoS.EXACTLY_ONCE), 124L, BucketUtils.getBucket("clientid", bucketCount));

        final ImmutableSet<Topic> subscriptions = persistence.getSubscriptions("clientid");

        assertEquals(1, subscriptions.size());
        final Topic next = subscriptions.iterator().next();
        assertEquals("topic", next.getTopic());
        assertEquals(QoS.EXACTLY_ONCE, next.getQoS());

        final Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();
        assertTrue(value > 0L);
    }

    @Test
    public void test_get_not_existing() {

        final ImmutableSet<Topic> subscriptions = persistence.getSubscriptions("noclientid");

        assertNotNull(subscriptions);
        assertEquals(0, subscriptions.size());

        final Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();
        assertEquals(0, value.intValue());
    }


    @Test
    public void test_remove_not_existing() {

        //check for no exception here
        persistence.remove("noclientid", "topic", 123L, BucketUtils.getBucket("noclientid", bucketCount));
        final Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();
        assertEquals(0, value.intValue());
    }

    @Test
    public void test_remove() {
        final Topic topic = new Topic("topic", QoS.AT_LEAST_ONCE);
        persistence.addSubscription("clientid", topic, 123L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid", new Topic("topic2", QoS.EXACTLY_ONCE), 431L, BucketUtils.getBucket("clientid", bucketCount));
        final Topic topic4 = new Topic("topic4", QoS.EXACTLY_ONCE);
        persistence.addSubscription("clientid", topic4, 5431L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid2", new Topic("topic3", QoS.AT_MOST_ONCE), 1234567890L, BucketUtils.getBucket("clientid2", bucketCount));

        assertEquals(3, persistence.getSubscriptions("clientid").size());
        assertEquals(1, persistence.getSubscriptions("clientid2").size());

        persistence.remove("clientid", topic.getTopic(), 1234567891L, BucketUtils.getBucket("clientid", bucketCount));

        final ImmutableSet<Topic> subscriptions = persistence.getSubscriptions("clientid");
        assertEquals(2, subscriptions.size());
        assertEquals(1, persistence.getSubscriptions("clientid2").size());

        boolean topic2Found = false;
        boolean topic4Found = false;

        for (final Topic subscription : subscriptions) {
            if (subscription.getTopic().equals("topic2")) {
                topic2Found = true;
            }
            if (subscription.getTopic().equals("topic4")) {
                topic4Found = true;
            }
            assertEquals(QoS.EXACTLY_ONCE, subscription.getQoS());
        }

        assertTrue(topic2Found);
        assertTrue(topic4Found);

        persistence.remove("clientid", topic4.getTopic(), 9876543L, BucketUtils.getBucket("clientid", bucketCount));

        assertEquals(1, persistence.getSubscriptions("clientid").size());

        final Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();
        assertTrue(value > 0L);
    }

    @Test
    public void test_removeAll() {
        persistence.addSubscription("clientid", new Topic("topic", QoS.AT_LEAST_ONCE), 123L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid", new Topic("topic2", QoS.EXACTLY_ONCE), 431L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid2", new Topic("topic3", QoS.AT_MOST_ONCE), 1234567890L, BucketUtils.getBucket("clientid2", bucketCount));

        assertEquals(2, persistence.getSubscriptions("clientid").size());
        assertEquals(1, persistence.getSubscriptions("clientid2").size());

        persistence.removeAll("clientid", 12345678901L, BucketUtils.getBucket("clientid", bucketCount));

        assertEquals(0, persistence.getSubscriptions("clientid").size());
        assertEquals(1, persistence.getSubscriptions("clientid2").size());

        final Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();
        assertTrue(value > 0L);

    }

    @Test
    public void test_removeAll_empty() {

        assertEquals(0, persistence.getSubscriptions("clientid").size());
        assertEquals(0, persistence.getSubscriptions("clientid2").size());

        persistence.removeAll("clientid", 12345678901L, BucketUtils.getBucket("clientid", bucketCount));

        assertEquals(0, persistence.getSubscriptions("clientid").size());
        assertEquals(0, persistence.getSubscriptions("clientid2").size());


        final Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();
        assertEquals(0L, value.longValue());

    }

    @Test
    public void test_remove_subscriptions() {
        persistence.addSubscription("clientid", new Topic("topic", QoS.AT_LEAST_ONCE), 123L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid", new Topic("topic2", QoS.EXACTLY_ONCE), 431L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid", new Topic("topic3", QoS.EXACTLY_ONCE), 567L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid2", new Topic("topic", QoS.EXACTLY_ONCE), 567L, BucketUtils.getBucket("clientid2", bucketCount));

        assertEquals(3, persistence.getSubscriptions("clientid").size());

        persistence.removeSubscriptions("clientid", ImmutableSet.of("topic", "topic2"), 12345678901L, BucketUtils.getBucket("clientid", bucketCount));

        assertEquals(1, persistence.getSubscriptions("clientid").size());
        assertEquals(1, persistence.getSubscriptions("clientid2").size());

        final Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();
        assertTrue(value > 0L);

    }

    @Test
    public void test_add_after_closeDB() {
        persistence.addSubscription("clientid", new Topic("topic", QoS.AT_LEAST_ONCE), 123L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid", new Topic("topic2", QoS.EXACTLY_ONCE), 431L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid", new Topic("topic3", QoS.EXACTLY_ONCE), 567L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid2", new Topic("topic", QoS.EXACTLY_ONCE), 567L, BucketUtils.getBucket("clientid2", bucketCount));

        for (int i = 0; i < bucketCount; i++) {
            persistence.closeDB(i);
        }

        Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();
        assertEquals(0, value.intValue());

        persistence.addSubscription("clientid", new Topic("topic", QoS.AT_LEAST_ONCE), 123L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid", new Topic("topic2", QoS.EXACTLY_ONCE), 431L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid", new Topic("topic3", QoS.EXACTLY_ONCE), 567L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid2", new Topic("topic", QoS.EXACTLY_ONCE), 567L, BucketUtils.getBucket("clientid2", bucketCount));

        assertEquals(3, persistence.getSubscriptions("clientid").size());

        value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();

        assertTrue(value > 0L);


    }

    @Test
    public void test_closeDB() {
        persistence.addSubscription("clientid", new Topic("topic", QoS.AT_LEAST_ONCE), 123L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid", new Topic("topic2", QoS.EXACTLY_ONCE), 431L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid", new Topic("topic3", QoS.EXACTLY_ONCE), 567L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid2", new Topic("topic", QoS.EXACTLY_ONCE), 567L, BucketUtils.getBucket("clientid2", bucketCount));

        for (int i = 0; i < bucketCount; i++) {
            persistence.closeDB(i);
        }

        final Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();
        assertEquals(0, value.intValue());

    }

    @Test
    public void test_remove_subscriptions_non_existent() {
        assertEquals(0, persistence.getSubscriptions("clientid").size());

        persistence.removeSubscriptions("clientid", ImmutableSet.of("topic"), 12345678901L, BucketUtils.getBucket("clientid", bucketCount));

        assertEquals(0, persistence.getSubscriptions("clientid").size());

        final Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();
        assertEquals(0, value.intValue());
    }

    @Test
    public void test_concurrent_access() throws Exception {
        final Waiter waiter = new Waiter();
        final AtomicBoolean adding = new AtomicBoolean(true);
        final Thread thread1 = new Thread(() -> {
            try {
                for (int i = 0; i < 10000; i++) {
                    persistence.addSubscription("client", new Topic("topic" + i, QoS.AT_LEAST_ONCE), System.currentTimeMillis(), BucketUtils.getBucket("client", bucketCount));
                    waiter.resume();
                }
                adding.set(false);
            } catch (final Throwable t) {
                t.printStackTrace();
                waiter.fail();
            }
        });

        final Thread thread2 = new Thread(() -> {
            try {
                while (adding.get()) {
                    persistence.getSubscriptions("client");
                }
            } catch (final Throwable t) {
                t.printStackTrace();
                waiter.fail();
            }
        });
        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        waiter.await(5, TimeUnit.SECONDS, 10000);

        final ImmutableSet<Topic> subs = persistence.getSubscriptions("client");

        assertEquals(10000, subs.size());

        final Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();

        int size = 0;
        for (final Topic subscription : subs) {
            size += ObjectMemoryEstimation.objectRefSize();
            size += subscription.getEstimatedSize();
        }
        size += ObjectMemoryEstimation.stringSize("client");
        size += IterablePersistenceEntry.getFixedSize();

        assertEquals(size, value.intValue());

    }

    @Test
    public void test_get_with_subscription_identifier() {
        final Topic topic1 = new Topic("topic/a", QoS.AT_LEAST_ONCE, DEFAULT_NO_LOCAL, DEFAULT_RETAIN_AS_PUBLISHED,
                DEFAULT_RETAIN_HANDLING, 1);
        final Topic topic2 = new Topic("topic/#", QoS.AT_LEAST_ONCE, DEFAULT_NO_LOCAL, DEFAULT_RETAIN_AS_PUBLISHED,
                DEFAULT_RETAIN_HANDLING, 2);
        final Topic topic3 = new Topic("topic/+", QoS.AT_LEAST_ONCE, DEFAULT_NO_LOCAL, DEFAULT_RETAIN_AS_PUBLISHED,
                DEFAULT_RETAIN_HANDLING, 3);

        persistence.addSubscription("clientid", topic1, 123L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid", topic2, 124L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid", topic3, 125L, BucketUtils.getBucket("clientid", bucketCount));

        final ImmutableSet<Topic> subscriptions = persistence.getSubscriptions("clientid");
        assertEquals(3, subscriptions.size());

        int found = 0;
        for (final Topic subscription : subscriptions) {
            switch (subscription.getTopic()) {
                case "topic/a":
                    assertEquals(Objects.requireNonNull(subscription.getSubscriptionIdentifier()).intValue(), 1);
                    found++;
                    break;
                case "topic/#":
                    assertEquals(Objects.requireNonNull(subscription.getSubscriptionIdentifier()).intValue(), 2);
                    found++;
                    break;
                case "topic/+":
                    assertEquals(Objects.requireNonNull(subscription.getSubscriptionIdentifier()).intValue(), 3);
                    found++;
                    break;
            }
        }
        assertEquals(3, found);
    }

    @Test
    public void test_get_chunk_match_all() {
        persistence.addSubscription("clientid", new Topic("topic", QoS.AT_LEAST_ONCE), 123L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid", new Topic("topic2", QoS.EXACTLY_ONCE), 431L, BucketUtils.getBucket("clientid", bucketCount));
        persistence.addSubscription("clientid2", new Topic("topic3", QoS.AT_MOST_ONCE), 1234567890L, BucketUtils.getBucket("clientid2", bucketCount));


        final Map<String, ImmutableSet<Topic>> client1Entries = persistence.getAllSubscribersChunk(BucketUtils.getBucket("clientid", bucketCount), null, 10).getValue();
        final Map<String, ImmutableSet<Topic>> client2Entries = persistence.getAllSubscribersChunk(BucketUtils.getBucket("clientid2", bucketCount), null, 10).getValue();

        assertEquals(2, client1Entries.get("clientid").size());
        assertEquals(1, client2Entries.get("clientid2").size());

        final Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();
        assertTrue(value > 0L);
    }

    @Test
    public void test_get_chunk_multiple_subscriptions() {
        for (int i = 0; i < 60; i++) {
            persistence.addSubscription("client" + i, new Topic("A" + i, QoS.AT_LEAST_ONCE), 123L, BucketUtils.getBucket("client" + i, bucketCount));
            persistence.addSubscription("client" + i, new Topic("B" + i, QoS.AT_LEAST_ONCE), 123L, BucketUtils.getBucket("client" + i, bucketCount));
        }

        final Map<String, Set<Topic>> all = new HashMap<>();
        for (int i = 0; i < bucketCount; i++) {
            all.putAll(persistence.getAllSubscribersChunk(i, null, 10).getValue());
        }

        int size = 0;
        for (final Map.Entry<String, Set<Topic>> entry : all.entrySet()) {
            assertEquals(2, entry.getValue().size());
            for (final Topic topic : entry.getValue()) {
                size += ObjectMemoryEstimation.objectRefSize();
                size += topic.getEstimatedSize();
            }
            size += ObjectMemoryEstimation.stringSize(entry.getKey());
            size += IterablePersistenceEntry.getFixedSize();
        }
        final Long value = (Long) metricRegistry.getGauges()
                .get(HiveMQMetrics.CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE.name())
                .getValue();
        assertEquals(size, value.intValue());
    }

    @Test
    public void test_get_chunk_single_client_multiple_subscriptions_max_results_ignored() {

        persistence.addSubscription("1", new Topic("A1", QoS.AT_LEAST_ONCE), 123L, 1);
        persistence.addSubscription("1", new Topic("B1", QoS.AT_LEAST_ONCE), 123L, 1);

        persistence.addSubscription("2", new Topic("A2", QoS.AT_LEAST_ONCE), 123L, 1);
        persistence.addSubscription("2", new Topic("B2", QoS.AT_LEAST_ONCE), 123L, 1);

        persistence.addSubscription("3", new Topic("A3", QoS.AT_LEAST_ONCE), 123L, 1);
        persistence.addSubscription("3", new Topic("B3", QoS.AT_LEAST_ONCE), 123L, 1);

        final BucketChunkResult<Map<String, ImmutableSet<Topic>>> chunk =
                persistence.getAllSubscribersChunk(1, null, 3);

        final Map<String, ImmutableSet<Topic>> all = chunk.getValue();

        assertEquals(3, all.size());
        for (final Map.Entry<String, ImmutableSet<Topic>> entry : all.entrySet()) {
            assertEquals(2, entry.getValue().size());

            for (final Topic topic : entry.getValue()) {
                assertTrue(topic.getTopic().endsWith(entry.getKey()));
            }
        }

        final BucketChunkResult<Map<String, ImmutableSet<Topic>>> chunk2 =
                persistence.getAllSubscribersChunk(1, chunk.getLastKey(), 1);

        final Map<String, ImmutableSet<Topic>> all2 = chunk2.getValue();

        assertEquals(3, all2.size());
        for (final Map.Entry<String, ImmutableSet<Topic>> entry2 : all2.entrySet()) {
            assertEquals(2, entry2.getValue().size());

            for (final Topic topic : entry2.getValue()) {
                assertTrue(topic.getTopic().endsWith(entry2.getKey()));
            }
        }
    }

    @Test
    public void test_get_chunk_duplicate_topics() {
        persistence.addSubscription("clientid", new Topic("topic", QoS.AT_LEAST_ONCE), 123L, 1);
        persistence.addSubscription("clientid", new Topic("topic", QoS.EXACTLY_ONCE), 431L, 1);

        final Map<String, ImmutableSet<Topic>> client1Entries = persistence.getAllSubscribersChunk(1, null, 100).getValue();

        final Set<Topic> topics = client1Entries.get("clientid");
        assertEquals(1, topics.size());
        final Topic topic = topics.iterator().next();
        assertEquals("topic", topic.getTopic());
        assertEquals(QoS.EXACTLY_ONCE, topic.getQoS());
    }

    @Test
    public void test_add_subscriptions_duplicate_topics() {
        final int bucket = BucketUtils.getBucket("clientid", bucketCount);
        persistence.addSubscriptions("clientid", ImmutableSet.of(new Topic("topic", QoS.AT_LEAST_ONCE), new Topic("topic1", QoS.AT_LEAST_ONCE)), 123L, bucket);
        persistence.addSubscriptions("clientid", ImmutableSet.of(new Topic("topic", QoS.EXACTLY_ONCE), new Topic("topic1", QoS.EXACTLY_ONCE)), 123L, bucket);

        final ImmutableSet<Topic> topics = persistence.getSubscriptions("clientid");
        assertEquals(2, topics.size());
        for (final Topic topic : topics) {
            assertEquals(QoS.EXACTLY_ONCE, topic.getQoS());
        }
    }

    @Test(timeout = 10_000)
    public void test_get_chunk_many_clients_no_duplicates() {

        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 10; j++) {
                persistence.addSubscription("sub-" + i, new Topic(i + "/" + j, QoS.AT_LEAST_ONCE), 123L, 1);
            }
        }

        final ArrayList<String> clientIds = Lists.newArrayList();
        BucketChunkResult<Map<String, ImmutableSet<Topic>>> chunk = null;

        do {
            chunk = persistence.getAllSubscribersChunk(1, chunk != null ? chunk.getLastKey() : null, 16);
            clientIds.addAll(chunk.getValue().keySet());
        } while (!chunk.isFinished());

        final Set<String> seenIds = new HashSet<>();
        for (final String clientId : clientIds) {
            if (seenIds.contains(clientId)) {
                System.out.println(clientIds);
                fail("clientid " + clientId + " is duplicated. Total result count:" + clientIds.size());
            }
            seenIds.add(clientId);
        }

        assertEquals(100, clientIds.size());

    }

    @Test(timeout = 10_000)
    public void test_get_chunk_remove_last_key_between_iterations() {

        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 10; j++) {
                persistence.addSubscription("sub-" + i, new Topic(i + "/" + j, QoS.AT_LEAST_ONCE), 123L, 1);
            }
        }

        final ArrayList<String> clientIds = Lists.newArrayList();
        BucketChunkResult<Map<String, ImmutableSet<Topic>>> chunk = null;

        do {
            if (chunk != null && chunk.getLastKey() != null) {
                persistence.removeAll(chunk.getLastKey(), System.currentTimeMillis(), 1);
            }
            chunk = persistence.getAllSubscribersChunk(1, chunk != null ? chunk.getLastKey() : null, 1);
            clientIds.addAll(chunk.getValue().keySet());
        } while (!chunk.isFinished());

        final Set<String> seenIds = new HashSet<>();
        for (final String clientId : clientIds) {
            if (seenIds.contains(clientId)) {
                System.out.println(clientIds);
                fail("clientid " + clientId + " is duplicated. Total result count:" + clientIds.size());
            }
            seenIds.add(clientId);
        }

        assertEquals(100, clientIds.size());
    }


    @Test(timeout = 30_000)
    public void test_get_chunk_many_clients_no_duplicates_random_ids() {

        final ArrayList<String> clientIdList = getRandomUniqueIds();

        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 10; j++) {
                persistence.addSubscription(clientIdList.get(i), new Topic(i + "/" + j, QoS.AT_LEAST_ONCE), 123L, 1);
            }
        }

        final ArrayList<String> clientIds = Lists.newArrayList();
        BucketChunkResult<Map<String, ImmutableSet<Topic>>> chunk = null;

        do {
            chunk = persistence.getAllSubscribersChunk(1, chunk != null ? chunk.getLastKey() : null, 16);
            clientIds.addAll(chunk.getValue().keySet());
        } while (!chunk.isFinished());

        final Set<String> seenIds = new HashSet<>();
        for (final String clientId : clientIds) {
            if (seenIds.contains(clientId)) {
                System.out.println(clientIds);
                fail("clientid " + clientId + " is duplicated. Total result count:" + clientIds.size());
            }
            seenIds.add(clientId);
        }

        assertEquals(100, clientIds.size());

    }

    @NotNull
    public ArrayList<String> getRandomUniqueIds() {
        final Set<String> clientIdSet = new HashSet<>();

        final Random random = new Random();
        while (clientIdSet.size() < 100) {
            clientIdSet.add(RandomStringUtils.randomAlphanumeric(random.nextInt(100)));
        }
        return new ArrayList<>(clientIdSet);
    }

}
