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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.ImmutableIntArray;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.metrics.HiveMQMetrics;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.persistence.local.memory.ClientQueueMemoryLocalPersistence.PublishWithRetained;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.ObjectMemoryEstimation;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Set;
import java.util.stream.Collectors;

import static com.hivemq.configuration.service.MqttConfigurationService.QueuedMessagesStrategy.DISCARD;
import static com.hivemq.configuration.service.MqttConfigurationService.QueuedMessagesStrategy.DISCARD_OLDEST;
import static com.hivemq.persistence.clientqueue.ClientQueuePersistenceImpl.SHARED_IN_FLIGHT_MARKER;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Florian Limpöck
 */
@SuppressWarnings("NullabilityAnnotations")
public class ClientQueueMemoryLocalPersistenceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private PublishPayloadPersistence payloadPersistence;

    @Mock
    private MessageDroppedService messageDroppedService;

    private ClientQueueMemoryLocalPersistence persistence;

    private final int bucketCount = 4;

    private final long byteLimit = 5 * 1024 * 1024;
    private MetricRegistry metricRegistry;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InternalConfigurations.PERSISTENCE_BUCKET_COUNT.set(bucketCount);
        InternalConfigurations.QOS_0_MEMORY_HARD_LIMIT_DIVISOR.set(10000);
        InternalConfigurations.QOS_0_MEMORY_LIMIT_PER_CLIENT.set(1024);
        InternalConfigurations.RETAINED_MESSAGE_QUEUE_SIZE.set(5);

        metricRegistry = new MetricRegistry();
        persistence = new ClientQueueMemoryLocalPersistence(
                payloadPersistence,
                messageDroppedService, metricRegistry);
    }

    @After
    public void tearDown() throws Exception {
        InternalConfigurations.EXPIRE_INFLIGHT_PUBRELS = false;
    }

    @Test
    public void test_readNew_lessAvailable() {
        final PUBLISH publish = createPublish(10, QoS.AT_LEAST_ONCE, "topic1");
        final PUBLISH otherPublish = createPublish(11, QoS.EXACTLY_ONCE, "topic2");
        persistence.add("client10", false, otherPublish, 100L, DISCARD, false, 0);
        persistence.add("client1", false, publish, 100L, DISCARD, false, 0);
        persistence.add("client01", false, otherPublish, 100L, DISCARD, false, 0);
        final ImmutableList<PUBLISH> publishes =
                persistence.readNew("client1", false, ImmutableIntArray.of(2, 3, 4), 256000, 0);
        assertEquals(1, publishes.size());
        assertEquals(2, publishes.get(0).getPacketIdentifier());
        assertEquals(publish.getQoS(), publishes.get(0).getQoS());
        assertEquals(publish.getTopic(), publishes.get(0).getTopic());
    }

    @Test
    public void test_readNew_moreAvailable() {
        final PUBLISH[] publishes = new PUBLISH[4];
        for (int i = 0; i < publishes.length; i++) {
            publishes[i] = createPublish(10 + i, (i % 2 == 0) ? QoS.EXACTLY_ONCE : QoS.AT_LEAST_ONCE, "topic" + i);
        }
        final PUBLISH otherPublish = createPublish(14, QoS.EXACTLY_ONCE, "topic5");

        persistence.add("client10", false, otherPublish, 100L, DISCARD, false, 0);
        for (final PUBLISH publish : publishes) {
            persistence.add("client1", false, publish, 100L, DISCARD, false, 0);
        }
        persistence.add("client01", false, otherPublish, 100L, DISCARD, false, 0);

        final ImmutableIntArray packetIds = ImmutableIntArray.of(2, 3, 5);
        final ImmutableList<PUBLISH> readPublishes = persistence.readNew("client1", false, packetIds, 256000, 0);

        assertEquals(3, readPublishes.size());
        for (int i = 0; i < packetIds.length(); i++) {
            assertEquals(packetIds.get(i), readPublishes.get(i).getPacketIdentifier());
            assertEquals(publishes[i].getQoS(), readPublishes.get(i).getQoS());
            assertEquals(publishes[i].getTopic(), readPublishes.get(i).getTopic());
        }
    }

    @Test
    public void test_readNew_twice() {
        final PUBLISH[] publishes = new PUBLISH[4];
        for (int i = 0; i < publishes.length; i++) {
            publishes[i] = createPublish(10 + i, (i % 2 == 0) ? QoS.EXACTLY_ONCE : QoS.AT_LEAST_ONCE, "topic" + i);
        }
        final PUBLISH otherPublish = createPublish(14, QoS.EXACTLY_ONCE, "topic5");

        persistence.add("client10", false, otherPublish, 100L, DISCARD, false, 0);
        for (final PUBLISH publish : publishes) {
            persistence.add("client1", false, publish, 100L, DISCARD, false, 0);
        }
        persistence.add("client01", false, otherPublish, 100L, DISCARD, false, 0);

        final ImmutableList<PUBLISH> messages1 =
                persistence.readNew("client1", false, ImmutableIntArray.of(5), 256000, 0);

        assertEquals(1, messages1.size());
        assertEquals(5, messages1.get(0).getPacketIdentifier());
        assertEquals("topic0", messages1.get(0).getTopic());

        final ImmutableIntArray packetIds = ImmutableIntArray.of(2, 3, 4);
        final ImmutableList<PUBLISH> messages2 = persistence.readNew("client1", false, packetIds, 256000, 0);

        assertEquals(3, messages2.size());
        for (int i = 0; i < packetIds.length(); i++) {
            assertEquals(packetIds.get(i), messages2.get(i).getPacketIdentifier());
            assertEquals(publishes[1 + i].getTopic(), messages2.get(i).getTopic());
        }
    }

    @Test
    public void test_readNew_qos0() {
        final PUBLISH[] publishes = new PUBLISH[4];
        for (int i = 0; i < publishes.length; i++) {
            final PUBLISH publish = createPublish(0, QoS.AT_MOST_ONCE, "topic" + i);
            publishes[i] = publish;
            persistence.add("client", false, publish, 100L, DISCARD, false, 0);
        }

        final ImmutableList<PUBLISH> messages =
                persistence.readNew("client", false, ImmutableIntArray.of(1, 2, 3), 256000, 0);

        assertEquals(1, persistence.size("client", false, 0));
        assertEquals(3, messages.size());
        for (int i = 0; i < 3; i++) {
            assertEquals(publishes[i].getTopic(), messages.get(i).getTopic());
        }
    }

    @Test
    public void test_readNew_qos0_and_qos1() {
        final PUBLISH[] qos0Publishes = new PUBLISH[3];
        for (int i = 0; i < qos0Publishes.length; i++) {
            final PUBLISH publish = createPublish(0, QoS.AT_MOST_ONCE, "topic" + i);
            qos0Publishes[i] = publish;
            persistence.add("client", false, publish, 100L, DISCARD, false, 0);
        }

        final PUBLISH[] qos1Publishes = new PUBLISH[3];
        for (int i = 0; i < qos1Publishes.length; i++) {
            final PUBLISH publish = createPublish(1 + i, QoS.AT_LEAST_ONCE, "topic" + i);
            qos1Publishes[i] = publish;
            persistence.add("client", false, publish, 100L, DISCARD, false, 0);
        }

        final ImmutableList<PUBLISH> messages =
                persistence.readNew("client", false, ImmutableIntArray.of(1, 2, 3, 4, 5, 6, 7), 256000, 0);

        assertEquals(3, persistence.size("client", false, 0));
        assertEquals(6, messages.size());

        assertEquals(0, messages.get(1).getPacketIdentifier());
        assertEquals(QoS.AT_MOST_ONCE, messages.get(1).getQoS());
        assertEquals(0, messages.get(3).getPacketIdentifier());
        assertEquals(QoS.AT_MOST_ONCE, messages.get(3).getQoS());
        assertEquals(0, messages.get(5).getPacketIdentifier());
        assertEquals(QoS.AT_MOST_ONCE, messages.get(5).getQoS());

        assertEquals(1, messages.get(0).getPacketIdentifier());
        assertEquals(QoS.AT_LEAST_ONCE, messages.get(0).getQoS());
        assertEquals(2, messages.get(2).getPacketIdentifier());
        assertEquals(QoS.AT_LEAST_ONCE, messages.get(2).getQoS());
        assertEquals(3, messages.get(4).getPacketIdentifier());
        assertEquals(QoS.AT_LEAST_ONCE, messages.get(4).getQoS());
    }

    @Test
    public void test_read_inflight() {
        final PUBLISH[] publishes = new PUBLISH[4];
        for (int i = 0; i < publishes.length; i++) {
            publishes[i] = createPublish(10 + i, (i % 2 == 0) ? QoS.EXACTLY_ONCE : QoS.AT_LEAST_ONCE, "topic" + i);
        }
        for (final PUBLISH publish : publishes) {
            persistence.add("client1", false, publish, 100L, DISCARD, false, 0);
        }

        final ImmutableList<PUBLISH> messages1 =
                persistence.readNew("client1", false, ImmutableIntArray.of(5, 6, 7), 256000, 0);

        assertEquals(3, messages1.size());
        assertEquals(5, messages1.get(0).getPacketIdentifier());
        assertEquals(6, messages1.get(1).getPacketIdentifier());
        assertEquals(7, messages1.get(2).getPacketIdentifier());
    }

    @Test
    public void test_read_inflight_pubrel() {
        final PUBREL[] pubrels = new PUBREL[4];
        for (int i = 0; i < pubrels.length; i++) {
            pubrels[i] = new PUBREL(i + 1);
        }
        for (final PUBREL pubrel : pubrels) {
            persistence.add("client1", false, createPublish(pubrel.getPacketIdentifier(), QoS.EXACTLY_ONCE, "topic"), 100L, DISCARD, false, 0);
            persistence.replace("client1", pubrel, 0);
        }

        final ImmutableList<MessageWithID> messages2 = persistence.readInflight("client1", false, 10, 256000, 0);
        assertEquals(4, messages2.size());
    }

    @Test
    public void test_read_inflight_pubrel_and_publish() {
        final PUBREL[] pubrels = new PUBREL[4];
        for (int i = 0; i < pubrels.length; i++) {
            pubrels[i] = new PUBREL(i + 1);
        }
        for (final PUBREL pubrel : pubrels) {
            persistence.add("client1", false, createPublish(pubrel.getPacketIdentifier(), QoS.EXACTLY_ONCE, "topic"), 100L, DISCARD, false, 0);
            persistence.replace("client1", pubrel, 0);
        }
        final PUBLISH[] publishes = new PUBLISH[4];
        for (int i = 0; i < publishes.length; i++) {
            publishes[i] = createPublish(10 + i, (i % 2 == 0) ? QoS.EXACTLY_ONCE : QoS.AT_LEAST_ONCE, "topic" + i);
        }
        for (final PUBLISH publish : publishes) {
            persistence.add("client1", false, publish, 100L, DISCARD, false, 0);
        }

        // Assign packet ID's
        persistence.readNew("client1", false, ImmutableIntArray.of(1, 2, 3, 4), 256000, 0);

        final ImmutableList<MessageWithID> messages = persistence.readInflight("client1", false, 10, 256000, 0);
        assertEquals(8, messages.size());
        assertTrue(messages.get(0) instanceof PUBREL);
        assertTrue(messages.get(1) instanceof PUBREL);
        assertTrue(messages.get(2) instanceof PUBREL);
        assertTrue(messages.get(3) instanceof PUBREL);
        assertTrue(messages.get(4) instanceof PUBLISH);
        assertTrue(messages.get(5) instanceof PUBLISH);
        assertTrue(messages.get(6) instanceof PUBLISH);
        assertTrue(messages.get(7) instanceof PUBLISH);
    }

    @Test
    public void test_add_discard() {
        for (int i = 1; i <= 6; i++) {
            persistence.add("client", false, createPublish(i, QoS.AT_LEAST_ONCE, "topic" + i), 3L, DISCARD, false, 0);
        }
        assertEquals(3, persistence.size("client", false, 0));

        final ImmutableList<PUBLISH> publishes =
                persistence.readNew("client", false, ImmutableIntArray.of(1, 2, 3, 4, 5, 6), byteLimit, 0);

        assertEquals(3, publishes.size());
        assertEquals(1, publishes.get(0).getPacketIdentifier());
        assertEquals(2, publishes.get(1).getPacketIdentifier());
        assertEquals(3, publishes.get(2).getPacketIdentifier());

        verify(messageDroppedService, times(3)).queueFull(eq("client"), anyString(), anyInt());
    }

    @Test
    public void test_add_discard_oldest() {
        for (int i = 1; i <= 6; i++) {
            persistence.add(
                    "client", false, createPublish(i, QoS.AT_LEAST_ONCE, "topic" + i), 3L, DISCARD_OLDEST, false, 0);
        }
        assertEquals(3, persistence.size("client", false, 0));
        final ImmutableList<PUBLISH> publishes =
                persistence.readNew("client", false, ImmutableIntArray.of(1, 2, 3, 4, 5, 6), byteLimit, 0);
        assertEquals(3, publishes.size());
        assertEquals("topic4", publishes.get(0).getTopic());
        assertEquals("topic5", publishes.get(1).getTopic());
        assertEquals("topic6", publishes.get(2).getTopic());
        verify(messageDroppedService, times(3)).queueFull(eq("client"), anyString(), anyInt());
    }

    @Test
    public void test_clear() {
        for (int i = 0; i < 5; i++) {
            persistence.add("client1", false, createPublish(1, QoS.AT_LEAST_ONCE), 100L, DISCARD, false, 0);
        }

        persistence.add("client1", false, createPublish(0, QoS.AT_MOST_ONCE), 100L, DISCARD, false, 0);
        persistence.add("client2", false, createPublish(1, QoS.AT_LEAST_ONCE), 100L, DISCARD, false, 0);
        persistence.clear("client1", false, 0);

        final ImmutableList<PUBLISH> publishes1 =
                persistence.readNew("client1", false, ImmutableIntArray.of(1, 2, 3, 4, 5, 6), byteLimit, 0);
        assertEquals(0, publishes1.size());

        final ImmutableList<PUBLISH> publishes2 =
                persistence.readNew("client2", false, ImmutableIntArray.of(1, 2, 3, 4, 5, 6), byteLimit, 0);
        assertEquals(1, publishes2.size());
    }

    @Test
    public void test_replace() {
        for (int i = 0; i < 3; i++) {
            persistence.add("client", false, createPublish(1, QoS.AT_LEAST_ONCE, "topic", i), 100L, DISCARD, false, 0);
        }
        persistence.readNew("client", false, ImmutableIntArray.of(2, 3, 4), 256000, 0);
        final String uniqueId = persistence.replace("client", new PUBREL(4), 0);
        assertEquals("hivemqId_pub_2", uniqueId);
        final ImmutableList<MessageWithID> messages = persistence.readInflight("client", false, 10, byteLimit, 0);
        assertTrue(messages.get(2) instanceof PUBREL);
    }

    @Test
    public void test_replace_pubrel() {
        for (int i = 0; i < 3; i++) {
            persistence.add("client", false, createPublish(1, QoS.AT_LEAST_ONCE, "topic", i), 100L, DISCARD, false, 0);
        }
        persistence.readNew("client", false, ImmutableIntArray.of(2, 3, 4), 256000, 0);
        String uniqueId = persistence.replace("client", new PUBREL(4), 0);
        assertEquals("hivemqId_pub_2", uniqueId);
        uniqueId = persistence.replace("client", new PUBREL(4), 0);
        assertNull(uniqueId);
        final ImmutableList<MessageWithID> messages = persistence.readInflight("client", false, 10, byteLimit, 0);
        assertTrue(messages.get(2) instanceof PUBREL);
    }

    @Test
    public void test_replca_false_id() {
        persistence.add("client", false, createPublish(1, QoS.AT_LEAST_ONCE, "topic", 1), 100L, DISCARD, false, 0);
        persistence.readNew("client", false, ImmutableIntArray.of(1), 256000, 0);
        final String uniqueId = persistence.remove("client", 1, "hivemqId_pub_2", 0);
        assertNull(uniqueId);
        final ImmutableList<MessageWithID> messages = persistence.readInflight("client", false, 10, byteLimit, 0);
        assertEquals(1, messages.size());
        assertEquals(1, messages.get(0).getPacketIdentifier());
    }

    @Test
    public void test_replace_not_found() {
        for (int i = 0; i < 3; i++) {
            persistence.add("client", false, createPublish(1, QoS.AT_LEAST_ONCE, "topic", i), 100L, DISCARD, false, 0);
        }
        final String uniqueId = persistence.replace("client", new PUBREL(4), 0);
        assertEquals(4, persistence.size("client", false, 0));
        assertNull(uniqueId);
    }

    @Test
    public void test_remove() {
        for (int i = 0; i < 3; i++) {
            persistence.add("client", false, createPublish(1, QoS.AT_LEAST_ONCE, "topic", i), 100L, DISCARD, false, 0);
        }
        persistence.readNew("client", false, ImmutableIntArray.of(2, 3, 4), 256000, 0);
        final String uniqueId = persistence.remove("client", 4, 0);
        assertEquals("hivemqId_pub_2", uniqueId);
        final ImmutableList<MessageWithID> messages = persistence.readInflight("client", false, 10, byteLimit, 0);
        assertEquals(2, messages.size());
        assertEquals(2, messages.get(0).getPacketIdentifier());
        assertEquals(3, messages.get(1).getPacketIdentifier());

        assertEquals(2, persistence.size("client", false, 0));

        verify(payloadPersistence, times(1)).decrementReferenceCounter(anyLong());
    }

    @Test
    public void test_remove_not_found() {
        for (int i = 0; i < 3; i++) {
            persistence.add("client", false, createPublish(1, QoS.AT_LEAST_ONCE, "topic", i), 100L, DISCARD, false, 0);
        }
        final String uniqueId = persistence.remove("client", 1, 0);
        assertNull(uniqueId);
    }

    @Test
    public void test_remove_false_id() {
        persistence.add("client", false, createPublish(1, QoS.AT_LEAST_ONCE, "topic", 1), 100L, DISCARD, false, 0);
        persistence.readNew("client", false, ImmutableIntArray.of(1), 256000, 0);
        final String uniqueId = persistence.remove("client", 1, "hivemqId_pub_2", 0);
        assertNull(uniqueId);
        final ImmutableList<MessageWithID> messages = persistence.readInflight("client", false, 10, byteLimit, 0);
        assertEquals(1, messages.size());
        assertEquals(1, messages.get(0).getPacketIdentifier());
    }

    @Test
    public void test_drop_qos_0_memory_exceeded() {

        final int queueLimit = (int) (Runtime.getRuntime().maxMemory() / 10000);

        persistence.add(
                "client", false, createBigPublish(0, QoS.AT_MOST_ONCE, "topic1", 1, queueLimit), 100L, DISCARD, false,
                0);
        persistence.add(
                "client", false, createBigPublish(1, QoS.AT_MOST_ONCE, "topic5", 2, queueLimit), 100L, DISCARD, false,
                0);

        verify(payloadPersistence).decrementReferenceCounter(2);
        verify(messageDroppedService).qos0MemoryExceeded(eq("client"), eq("topic5"), eq(0), anyLong(), anyLong());
    }

    @Test
    public void test_drop_qos_0_memory_exceeded_shared() {

        final int queueLimit = (int) (Runtime.getRuntime().maxMemory() / 10000);

        persistence.add(
                "client", false, createBigPublish(0, QoS.AT_MOST_ONCE, "topic1", 1, queueLimit), 100L, DISCARD, false,
                0);
        persistence.add(
                "group", true, createBigPublish(1, QoS.AT_MOST_ONCE, "topic5", 2, queueLimit), 100L, DISCARD, false, 0);

        verify(payloadPersistence).decrementReferenceCounter(2);
        verify(messageDroppedService).qos0MemoryExceededShared(eq("group"), eq("topic5"), eq(0), anyLong(), anyLong());
    }

    @Test
    public void test_read_new_expired_mixed_qos() {
        persistence.add(
                "client1", false, createPublish(0, QoS.AT_MOST_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client1", false, createPublish(0, QoS.AT_LEAST_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);

        persistence.add(
                "client2", false, createPublish(0, QoS.AT_MOST_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);

        final ImmutableList<PUBLISH> messages1 =
                persistence.readNew("client1", false, ImmutableIntArray.of(1, 2), 10000L, 0);
        final ImmutableList<PUBLISH> messages2 =
                persistence.readNew("client2", false, ImmutableIntArray.of(1, 2), 10000L, 0);

        assertEquals(0, messages1.size());
        assertEquals(0, messages2.size());
    }

    @Test
    public void test_read_new_part_expired_qos0() {
        persistence.add(
                "client1", false, createPublish(0, QoS.AT_MOST_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client1", false, createPublish(0, QoS.AT_MOST_ONCE, 100, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);

        persistence.add(
                "client2", false, createPublish(0, QoS.AT_MOST_ONCE, 100, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client2", false, createPublish(0, QoS.AT_MOST_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client2", false, createPublish(0, QoS.AT_MOST_ONCE, 110, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);

        final ImmutableList<PUBLISH> messages1 =
                persistence.readNew("client1", false, ImmutableIntArray.of(1, 2), 10000L, 0);
        final ImmutableList<PUBLISH> messages2 =
                persistence.readNew("client2", false, ImmutableIntArray.of(1, 2), 10000L, 0);

        assertEquals(1, messages1.size());
        assertEquals(2, messages2.size());
    }

    @Test
    public void test_read_new_part_expired_qos1() {
        persistence.add(
                "client1", false, createPublish(0, QoS.AT_LEAST_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client1", false, createPublish(0, QoS.AT_LEAST_ONCE, 100, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);

        persistence.add(
                "client2", false, createPublish(0, QoS.AT_LEAST_ONCE, 100, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client2", false, createPublish(0, QoS.AT_LEAST_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client2", false, createPublish(0, QoS.AT_LEAST_ONCE, 110, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);

        final ImmutableList<PUBLISH> messages1 =
                persistence.readNew("client1", false, ImmutableIntArray.of(1, 2), 10000L, 0);
        final ImmutableList<PUBLISH> messages2 =
                persistence.readNew("client2", false, ImmutableIntArray.of(1, 2), 10000L, 0);

        assertEquals(1, messages1.size());
        assertEquals(2, messages2.size());
    }

    @Test
    public void test_read_new_part_expired_qos2() {
        persistence.add(
                "client1", false, createPublish(0, QoS.EXACTLY_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client1", false, createPublish(0, QoS.EXACTLY_ONCE, 100, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);

        persistence.add(
                "client2", false, createPublish(0, QoS.EXACTLY_ONCE, 100, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client2", false, createPublish(0, QoS.EXACTLY_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client2", false, createPublish(0, QoS.EXACTLY_ONCE, 110, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);

        final ImmutableList<PUBLISH> messages1 =
                persistence.readNew("client1", false, ImmutableIntArray.of(1, 2, 3), 10000L, 0);
        final ImmutableList<PUBLISH> messages2 =
                persistence.readNew("client2", false, ImmutableIntArray.of(1, 2, 3), 10000L, 0);

        assertEquals(1, messages1.size());
        assertEquals(2, messages2.size());
    }

    @Test
    public void test_read_new_part_expired_mixed_qos() {
        persistence.add(
                "client1", false, createPublish(0, QoS.AT_LEAST_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client1", false, createPublish(0, QoS.AT_MOST_ONCE, 100, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);

        persistence.add(
                "client2", false, createPublish(0, QoS.AT_MOST_ONCE, 100, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client2", false, createPublish(0, QoS.AT_LEAST_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client2", false, createPublish(0, QoS.AT_LEAST_ONCE, 110, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);


        persistence.add(
                "client3", false, createPublish(0, QoS.EXACTLY_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client3", false, createPublish(0, QoS.EXACTLY_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client3", false, createPublish(0, QoS.EXACTLY_ONCE, 100, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client3", false, createPublish(0, QoS.EXACTLY_ONCE, 110, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);

        final ImmutableList<PUBLISH> messages1 =
                persistence.readNew("client1", false, ImmutableIntArray.of(1, 2, 3), 10000L, 0);
        final ImmutableList<PUBLISH> messages2 =
                persistence.readNew("client2", false, ImmutableIntArray.of(1, 2, 3), 10000L, 0);
        final ImmutableList<PUBLISH> messages3 =
                persistence.readNew("client3", false, ImmutableIntArray.of(1, 2, 3), 10000L, 0);

        assertEquals(1, messages1.size());
        assertEquals(2, messages2.size());
        assertEquals(2, messages3.size());
    }

    @Test
    public void test_clean_up() {
        persistence.add("removed", false, createPublish(0, QoS.AT_LEAST_ONCE), 10, DISCARD, false, 0);
        persistence.clear("removed", false, 0);

        persistence.readNew("empty", false, ImmutableIntArray.of(1), 100000L, 0);

        persistence.add(
                "client1", false, createPublish(0, QoS.AT_LEAST_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client1", false, createPublish(0, QoS.AT_LEAST_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add("client1", false, createPublish(0, QoS.AT_LEAST_ONCE, "topic2"), 10, DISCARD, false, 0);
        persistence.add(
                "client1", false, createPublish(0, QoS.AT_MOST_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add("client1", false, createPublish(0, QoS.AT_MOST_ONCE, "topic2"), 10, DISCARD, false, 0);

        final ImmutableList<PUBLISH> newMessages =
                persistence.readNew("client1", false, ImmutableIntArray.of(1), 10000L, 0);
        assertEquals(1, newMessages.size());
        assertEquals("topic2", newMessages.get(0).getTopic());

        final ImmutableSet<String> sharedQueues = persistence.cleanUp(0);

        assertTrue(sharedQueues.isEmpty());
        verify(payloadPersistence, times(5)).decrementReferenceCounter(
                anyLong()); // 3 expired + 1 clear + 1 poll(readNew)
        assertEquals(1, persistence.size("client1", false, 0));
    }

    @Test
    public void test_clean_up_expired_qos0() {

        persistence.add(
                "client1", false, createPublish(0, QoS.AT_MOST_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client1", false, createPublish(0, QoS.AT_MOST_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);

        final ImmutableSet<String> sharedQueues = persistence.cleanUp(0);

        assertTrue(sharedQueues.isEmpty());
        verify(payloadPersistence, times(2)).decrementReferenceCounter(
                anyLong()); // 2 expired
        assertEquals(0, persistence.size("client1", false, 0));
    }

    @Test
    public void test_clean_up_expired_qos1() {

        persistence.add(
                "client1", false, createPublish(0, QoS.AT_LEAST_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);
        persistence.add(
                "client1", false, createPublish(0, QoS.AT_LEAST_ONCE, 10, System.currentTimeMillis() - 10000), 10,
                DISCARD, false, 0);

        final ImmutableSet<String> sharedQueues = persistence.cleanUp(0);

        assertTrue(sharedQueues.isEmpty());
        verify(payloadPersistence, times(2)).decrementReferenceCounter(
                anyLong()); // 2 expired
        assertEquals(0, persistence.size("client1", false, 0));
    }

    @Test
    public void test_clean_up_expired_pubrels_not_configured() throws InterruptedException {

        persistence.add(
                "client1", false, createPublish(1, QoS.EXACTLY_ONCE, 2, System.currentTimeMillis()), 10,
                DISCARD, false, 0);
        persistence.add(
                "client1", false, createPublish(2, QoS.EXACTLY_ONCE, 2, System.currentTimeMillis()), 10,
                DISCARD, false, 0);

        persistence.readNew("client1", false, createPacketIds(1, 2), byteLimit, 0);

        //let them expire
        Thread.sleep(3000);

        persistence.replace("client1", new PUBREL(1), 0);
        persistence.replace("client1", new PUBREL(2), 0);

        final ImmutableSet<String> sharedQueues = persistence.cleanUp(0);

        assertTrue(sharedQueues.isEmpty());
        verify(payloadPersistence, times(2)).decrementReferenceCounter(
                anyLong()); // 2 replaces
        assertEquals(2, persistence.size("client1", false, 0));
    }

    @Test
    public void test_clean_up_expired_pubrels_configured() throws InterruptedException {

        InternalConfigurations.EXPIRE_INFLIGHT_PUBRELS = true;

        metricRegistry = new MetricRegistry();
        persistence = new ClientQueueMemoryLocalPersistence(
                payloadPersistence,
                messageDroppedService, metricRegistry);

        persistence.add(
                "client1", false, createPublish(1, QoS.EXACTLY_ONCE, 2, System.currentTimeMillis()), 10,
                DISCARD, false, 0);
        persistence.add(
                "client1", false, createPublish(2, QoS.EXACTLY_ONCE, 2, System.currentTimeMillis()), 10,
                DISCARD, false, 0);

        persistence.readNew("client1", false, createPacketIds(1, 2), byteLimit, 0);

        //let them expire
        Thread.sleep(3000);

        persistence.replace("client1", new PUBREL(1), 0);
        persistence.replace("client1", new PUBREL(2), 0);

        final ImmutableSet<String> sharedQueues = persistence.cleanUp(0);

        assertTrue(sharedQueues.isEmpty());
        verify(payloadPersistence, times(2)).decrementReferenceCounter(
                anyLong()); // 2 replaces
        assertEquals(0, persistence.size("client1", false, 0));
    }

    @Test
    public void test_clean_up_shared() {
        persistence.add(
                "name/topic1", true, createPublish(0, QoS.AT_LEAST_ONCE, 1000, System.currentTimeMillis()), 10, DISCARD,
                false,
                0);
        persistence.add(
                "name/topic2", true, createPublish(1, QoS.AT_LEAST_ONCE, 1000, System.currentTimeMillis()), 10, DISCARD,
                false,
                0);

        final ImmutableSet<String> sharedQueues = persistence.cleanUp(0);
        assertEquals(2, sharedQueues.size());
    }

    @Test
    public void test_overlapping_ids() {

        persistence.add("id", false, createPublish(1, QoS.AT_LEAST_ONCE, "not_shared"), 10, DISCARD, false, 0);
        persistence.add("id", false, createPublish(0, QoS.AT_MOST_ONCE, "not_shared"), 10, DISCARD, false, 0);

        persistence.add("id", true, createPublish(1, QoS.AT_LEAST_ONCE, "shared"), 10, DISCARD, false, 0);
        persistence.add("id", true, createPublish(0, QoS.AT_MOST_ONCE, "shared"), 10, DISCARD, false, 0);

        final ImmutableList<PUBLISH> notSharedMessages =
                persistence.readNew("id", false, ImmutableIntArray.of(1, 2, 3), 10000L, 0);
        final ImmutableList<PUBLISH> sharedMessages =
                persistence.readNew("id", true, ImmutableIntArray.of(1, 2, 3), 10000L, 0);

        assertEquals(2, notSharedMessages.size());
        assertEquals(2, sharedMessages.size());

        assertEquals("not_shared", notSharedMessages.get(0).getTopic());
        assertEquals("not_shared", notSharedMessages.get(1).getTopic());

        assertEquals("shared", sharedMessages.get(0).getTopic());
        assertEquals("shared", sharedMessages.get(1).getTopic());

        assertEquals(1, persistence.size("id", false, 0));
        assertEquals(1, persistence.size("id", true, 0));
    }

    @Test
    public void test_remove_shared() {
        for (int i = 0; i < 3; i++) {
            persistence.add(
                    "group/topic", true, createPublish(1, QoS.AT_LEAST_ONCE, "topic", i), 100L, DISCARD, false, 0);
        }
        persistence.removeShared("group/topic", "hivemqId_pub_2", 0);
        final ImmutableList<PUBLISH> messages =
                persistence.readNew("group/topic", true, ImmutableIntArray.of(1, 2, 3), 10000L, 0);

        assertEquals(2, messages.size());

        assertEquals(2, persistence.size("group/topic", true, 0));

        verify(payloadPersistence, times(1)).decrementReferenceCounter(anyLong());
    }

    @Test
    public void test_remove_in_flight_marker() {
        for (int i = 0; i < 3; i++) {
            persistence.add(
                    "group/topic", true, createPublish(1, QoS.AT_LEAST_ONCE, "topic", i), 100L, DISCARD, false, 0);
        }
        persistence.readNew("group/topic", true,
                ImmutableIntArray.of(SHARED_IN_FLIGHT_MARKER, SHARED_IN_FLIGHT_MARKER, SHARED_IN_FLIGHT_MARKER),
                256000, 0);

        persistence.removeInFlightMarker("group/topic", "hivemqId_pub_2", 0);
        final ImmutableList<MessageWithID> messages = persistence.readInflight("group/topic", true, 10, byteLimit, 0);

        assertEquals(2, messages.size());
        assertEquals(SHARED_IN_FLIGHT_MARKER, messages.get(0).getPacketIdentifier());
        assertEquals(SHARED_IN_FLIGHT_MARKER, messages.get(1).getPacketIdentifier());

        assertEquals(3, persistence.size("group/topic", true, 0));

        verify(payloadPersistence, never()).decrementReferenceCounter(anyLong());
    }

    @Test
    public void test_remove_all_qos_0_messages() {

        final Gauge<Long> gauge = metricRegistry.getGauges().get(HiveMQMetrics.QUEUED_MESSAGES_MEMORY_PERSISTENCE_TOTAL_SIZE.name());

        final PUBLISH publish1 = createPublish(1, QoS.AT_LEAST_ONCE, "topic1", 1);
        final PUBLISH publish2 = createPublish(0, QoS.AT_MOST_ONCE, "topic2", 1);
        final PUBLISH publish3 = createPublish(0, QoS.AT_MOST_ONCE, "topic3", 1);

        persistence.add("client1", false, publish1, 100L, DISCARD, false, 0);
        persistence.add("client1", false, publish2, 100L, DISCARD, false, 0);
        persistence.add("client1", false, publish3, 100L, DISCARD, false, 0);

        final int size = new PublishWithRetained(publish1, false).getEstimatedSize() + ObjectMemoryEstimation.linkedListNodeOverhead() +
                new PublishWithRetained(publish2, false).getEstimatedSize() + ObjectMemoryEstimation.linkedListNodeOverhead() +
                new PublishWithRetained(publish3, false).getEstimatedSize() + ObjectMemoryEstimation.linkedListNodeOverhead();

        assertEquals(size, gauge.getValue().longValue());

        persistence.removeAllQos0Messages("client1", false, 0);

        final ImmutableList<PUBLISH> messages =
                persistence.readNew("client1", false, ImmutableIntArray.of(1, 2, 3), 10000L, 0);
        assertEquals(1, messages.size());

        verify(payloadPersistence, times(2)).decrementReferenceCounter(anyLong());

        assertTrue(gauge.getValue() > 0);
        assertEquals(new PublishWithRetained(messages.get(0), false).getEstimatedSize() + ObjectMemoryEstimation.linkedListNodeOverhead(), gauge.getValue().longValue());
    }

    @Test
    public void test_batched_add() {
        final ImmutableList.Builder<PUBLISH> publishes = ImmutableList.builder();
        for (int i = 0; i < 10; i++) {
            publishes.add(createPublish(1, QoS.AT_LEAST_ONCE, "topic" + i));
        }
        persistence.add("client", false, publishes.build(), 100, DISCARD, false, 0);

        assertEquals(10, persistence.size("client", false, 0));

        final ImmutableList<PUBLISH> all =
                persistence.readNew("client", false, ImmutableIntArray.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 10000L, 0);

        assertEquals(10, all.size());
        assertEquals("topic0", all.get(0).getTopic());
        assertEquals("topic1", all.get(1).getTopic());
    }

    @Test
    public void test_batched_add_discard() {
        final ImmutableList.Builder<PUBLISH> publishes = ImmutableList.builder();
        for (int i = 0; i < 10; i++) {
            publishes.add(createPublish(1, QoS.AT_LEAST_ONCE, "topic" + i));
        }
        persistence.add("client", false, publishes.build(), 5, DISCARD, false, 0);

        assertEquals(5, persistence.size("client", false, 0));

        final ImmutableList<PUBLISH> all =
                persistence.readNew("client", false, ImmutableIntArray.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 10000L, 0);
        assertEquals(5, all.size());
        assertEquals("topic0", all.get(0).getTopic());
        assertEquals("topic1", all.get(1).getTopic());
    }

    @Test
    public void test_batched_add_discard_oldest() {
        final ImmutableList.Builder<PUBLISH> publishes = ImmutableList.builder();

        persistence.add("client", false, createPublish(1, QoS.AT_LEAST_ONCE, "topicA"), 3, DISCARD_OLDEST, false, 0);
        persistence.add("client", false, createPublish(1, QoS.AT_LEAST_ONCE, "topicB"), 3, DISCARD_OLDEST, false, 0);
        persistence.add("client", false, createPublish(1, QoS.AT_LEAST_ONCE, "topicC"), 3, DISCARD_OLDEST, false, 0);

        for (int i = 0; i < 3; i++) {
            publishes.add(createPublish(1, QoS.AT_LEAST_ONCE, "topic" + i));
        }
        persistence.add("client", false, publishes.build(), 3, DISCARD_OLDEST, false, 0);

        assertEquals(3, persistence.size("client", false, 0));

        final ImmutableList<PUBLISH> all =
                persistence.readNew("client", false, ImmutableIntArray.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 10000L, 0);
        assertEquals(3, all.size());
        assertEquals("topic0", all.get(0).getTopic());
        assertEquals("topic1", all.get(1).getTopic());
        assertEquals("topic2", all.get(2).getTopic());
    }

    @Test
    public void test_batched_add_larger_than_queue_discard_oldest() {
        final ImmutableList.Builder<PUBLISH> publishes = ImmutableList.builder();

        for (int i = 0; i < 6; i++) {
            publishes.add(createPublish(1, QoS.AT_LEAST_ONCE, "topic" + i));
        }
        persistence.add("client", false, publishes.build(), 3, DISCARD_OLDEST, false, 0);

        assertEquals(3, persistence.size("client", false, 0));

        final ImmutableList<PUBLISH> all =
                persistence.readNew("client", false, ImmutableIntArray.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 10000L, 0);
        assertEquals(3, all.size());
        assertEquals("topic3", all.get(0).getTopic());
        assertEquals("topic4", all.get(1).getTopic());
        assertEquals("topic5", all.get(2).getTopic());
    }

    @Test
    public void test_batched_drop_qos_0_memory_exceeded() {

        final int queueLimit = (int) (Runtime.getRuntime().maxMemory() / 10000);
        final ImmutableList.Builder<PUBLISH> publishes = ImmutableList.builder();
        publishes.add(createBigPublish(0, QoS.AT_MOST_ONCE, "topic1", 1, queueLimit));
        publishes.add(createBigPublish(1, QoS.AT_MOST_ONCE, "topic2", 2, queueLimit));
        persistence.add("client", false, publishes.build(), 100L, DISCARD, false, 0);

        verify(payloadPersistence).decrementReferenceCounter(2);
        verify(messageDroppedService).qos0MemoryExceeded(eq("client"), eq("topic2"), eq(0), anyLong(), anyLong());

        assertEquals(1, persistence.size("client", false, 0));
        final ImmutableList<PUBLISH> all =
                persistence.readNew("client", false, ImmutableIntArray.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 10000L, 0);
        assertEquals(1, all.size());
    }

    @Test
    public void test_batched_add_retained_dont_discard() {
        final ImmutableList.Builder<PUBLISH> publishes = ImmutableList.builder();
        for (int i = 0; i < 5; i++) {
            publishes.add(createPublish(1, QoS.AT_LEAST_ONCE, "topic" + i));
        }
        persistence.add("client", false, publishes.build(), 2, DISCARD, true, 0);

        assertEquals(5, persistence.size("client", false, 0));

        final ImmutableList<PUBLISH> all =
                persistence.readNew("client", false, ImmutableIntArray.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 10000L, 0);
        assertEquals(5, all.size());
        assertEquals("topic0", all.get(0).getTopic());
        assertEquals("topic1", all.get(1).getTopic());
    }

    @Test
    public void test_batched_add_retained_discard_over_retained_limit() {
        final ImmutableList.Builder<PUBLISH> publishes1 = ImmutableList.builder();
        final ImmutableList.Builder<PUBLISH> publishes2 = ImmutableList.builder();
        for (int i = 0; i < 10; i++) {
            if (i < 5) {
                publishes1.add(createPublish(1, QoS.AT_LEAST_ONCE, "topic" + i));
            } else {
                publishes2.add(createPublish(1, QoS.AT_LEAST_ONCE, "topic" + i));
            }
        }
        persistence.add("client", false, publishes1.build(), 2, DISCARD, true, 0);

        final Gauge<Long> gauge = metricRegistry.getGauges().get(HiveMQMetrics.QUEUED_MESSAGES_MEMORY_PERSISTENCE_TOTAL_SIZE.name());
        final Long value = gauge.getValue();
        assertTrue(value > 0);

        assertEquals(5, persistence.size("client", false, 0));

        persistence.add("client", false, publishes2.build(), 2, DISCARD, true, 0);

        assertEquals(5, persistence.size("client", false, 0));
        assertEquals(value, gauge.getValue());

        final ImmutableList<PUBLISH> all =
                persistence.readNew("client", false, ImmutableIntArray.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 10000L, 0);
        assertEquals(5, all.size());
        assertEquals("topic0", all.get(0).getTopic());
        assertEquals("topic1", all.get(1).getTopic());
    }

    @Test
    public void add_and_poll_mixture_retained() {
        for (int i = 0; i < 12; i++) {
            persistence.add(
                    "client", false, createPublish(1, QoS.EXACTLY_ONCE, "topic" + i), 5, DISCARD_OLDEST, i % 2 != 0, 0);
        }
        final ImmutableList<PUBLISH> all = persistence.readNew("client", false, ImmutableIntArray.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), 10000L, 0);
        assertEquals(10, persistence.size("client", false, 0));
        assertEquals(10, all.size());

        final Set<PUBLISH> notExpectedMessages = all.stream()
                .filter(publish -> publish.getTopic().equals("10") || publish.getTopic().equals("11"))
                .collect(Collectors.toSet());
        assertTrue(notExpectedMessages.isEmpty());

        final Gauge<Long> gauge = metricRegistry.getGauges().get(HiveMQMetrics.QUEUED_MESSAGES_MEMORY_PERSISTENCE_TOTAL_SIZE.name());
        assertTrue(gauge.getValue() > 0);
    }

    @Test(timeout = 5000)
    public void test_add_qos_0_per_client_exceeded() {

        persistence.add("client", false, createBigPublish(1, QoS.AT_MOST_ONCE, "topic", 1, 500), 1000, DISCARD, false, BucketUtils.getBucket("client", 4));
        persistence.add("client", false, createBigPublish(1, QoS.AT_MOST_ONCE, "topic", 1, 500), 1000, DISCARD, false, BucketUtils.getBucket("client", 4));

        verify(messageDroppedService).qos0MemoryExceeded(eq("client"), eq("topic"), eq(0), anyLong(), eq(1024L));

        final Gauge<Long> gauge = metricRegistry.getGauges().get(HiveMQMetrics.QUEUED_MESSAGES_MEMORY_PERSISTENCE_TOTAL_SIZE.name());
        assertTrue(gauge.getValue() > 0);

    }

    @Test(timeout = 5000)
    public void test_add_qos_0_per_client_exactly_exceeded() {


        final PUBLISH exactly1024bytesPublish = createPublish(1, QoS.AT_MOST_ONCE, "topic", 1, new byte[745]);

        assertEquals(1024, exactly1024bytesPublish.getEstimatedSizeInMemory());

        persistence.add("client", false, exactly1024bytesPublish, 1000, DISCARD, false, BucketUtils.getBucket("client", 4));
        persistence.add("client", false, createPublish(2, QoS.AT_MOST_ONCE, "topic", 2), 1000, DISCARD, false, BucketUtils.getBucket("client", 4));

        verify(messageDroppedService).qos0MemoryExceeded(eq("client"), eq("topic"), eq(0), anyLong(), eq(1024L));

        final Gauge<Long> gauge = metricRegistry.getGauges().get(HiveMQMetrics.QUEUED_MESSAGES_MEMORY_PERSISTENCE_TOTAL_SIZE.name());
        assertTrue(gauge.getValue() > 0);

    }

    @Test
    public void test_add_close_add() {

        final ImmutableList.Builder<PUBLISH> publishes = ImmutableList.builder();
        for (int i = 0; i < 100; i++) {
            publishes.add(createPublish(1, QoS.AT_LEAST_ONCE, "topic" + i));
        }
        persistence.add("client", false, publishes.build(), 2, DISCARD, true, 0);

        final Gauge<Long> gauge = metricRegistry.getGauges().get(HiveMQMetrics.QUEUED_MESSAGES_MEMORY_PERSISTENCE_TOTAL_SIZE.name());
        assertTrue(gauge.getValue() > 0);

        for (int i = 0; i < bucketCount; i++) {
            persistence.closeDB(i);
        }

        assertEquals(0, gauge.getValue().longValue());

    }

    @Test
    public void test_read_byte_limit_respected_qos0() {

        InternalConfigurations.QOS_0_MEMORY_LIMIT_PER_CLIENT.set(1024 * 100);

        metricRegistry = new MetricRegistry();
        persistence = new ClientQueueMemoryLocalPersistence(
                payloadPersistence,
                messageDroppedService, metricRegistry);

        final ImmutableList.Builder<PUBLISH> publishes = ImmutableList.builder();
        int totalPublishBytes = 0;
        for (int i = 0; i < 100; i++) {
            final PUBLISH publish = createPublish(i + 1, QoS.AT_MOST_ONCE, "topic" + i);
            totalPublishBytes += publish.getEstimatedSizeInMemory();
            publishes.add(publish);
        }
        persistence.add("client", false, publishes.build(), 2, DISCARD, false, 0);

        final Gauge<Long> gauge = metricRegistry.getGauges().get(HiveMQMetrics.QUEUED_MESSAGES_MEMORY_PERSISTENCE_TOTAL_SIZE.name());
        assertTrue(gauge.getValue() > 0);

        int byteLimit = totalPublishBytes / 2;
        final ImmutableList<PUBLISH> allReadPublishes = persistence.readNew("client", false, createPacketIds(1, 100), byteLimit, 0);
        assertEquals(51, allReadPublishes.size());

        final ImmutableList<PUBLISH> allReadPublishes2 = persistence.readNew("client", false, createPacketIds(52, 100), byteLimit, 0);
        assertEquals(49, allReadPublishes2.size());

        assertEquals(0, gauge.getValue().longValue());

    }

    @Test
    public void test_read_byte_limit_respected_qos1() {

        InternalConfigurations.QOS_0_MEMORY_LIMIT_PER_CLIENT.set(1024 * 100);

        metricRegistry = new MetricRegistry();
        persistence = new ClientQueueMemoryLocalPersistence(
                payloadPersistence,
                messageDroppedService, metricRegistry);

        final ImmutableList.Builder<PUBLISH> publishes = ImmutableList.builder();
        int totalPublishBytes = 0;
        for (int i = 0; i < 100; i++) {
            final PUBLISH publish = createPublish(i + 1, QoS.AT_LEAST_ONCE, "topic" + i);
            totalPublishBytes += publish.getEstimatedSizeInMemory();
            publishes.add(publish);
        }
        persistence.add("client", false, publishes.build(), 100, DISCARD, false, 0);

        final Gauge<Long> gauge = metricRegistry.getGauges().get(HiveMQMetrics.QUEUED_MESSAGES_MEMORY_PERSISTENCE_TOTAL_SIZE.name());
        assertTrue(gauge.getValue() > 0);

        int byteLimit = totalPublishBytes / 2;
        System.out.println(byteLimit);
        final ImmutableList<PUBLISH> allReadPublishes = persistence.readNew("client", false, createPacketIds(1, 100), byteLimit, 0);
        assertEquals(51, allReadPublishes.size());

        final ImmutableList<PUBLISH> allReadPublishes2 = persistence.readNew("client", false, createPacketIds(52, 100), byteLimit, 0);
        assertEquals(49, allReadPublishes2.size());

        assertTrue(gauge.getValue() > 0);

        for (final PUBLISH pub : allReadPublishes) {
            persistence.remove("client", pub.getPacketIdentifier(), pub.getUniqueId(), 0);
        }
        for (final PUBLISH pub : allReadPublishes2) {
            persistence.remove("client", pub.getPacketIdentifier(), pub.getUniqueId(), 0);
        }

        assertEquals(0, gauge.getValue().longValue());

    }

    @Test
    public void test_read_byte_limit_respected_qos0_and_qos1() {

        InternalConfigurations.QOS_0_MEMORY_LIMIT_PER_CLIENT.set(1024 * 100);

        metricRegistry = new MetricRegistry();
        persistence = new ClientQueueMemoryLocalPersistence(
                payloadPersistence,
                messageDroppedService, metricRegistry);

        final ImmutableList.Builder<PUBLISH> publishes = ImmutableList.builder();
        int totalPublishBytes = 0;
        for (int i = 0; i < 100; i++) {
            final PUBLISH publish = createPublish(i + 1, QoS.valueOf(i % 2), "topic" + i);
            totalPublishBytes += publish.getEstimatedSizeInMemory();
            publishes.add(publish);
        }
        persistence.add("client", false, publishes.build(), 100, DISCARD, false, 0);

        final Gauge<Long> gauge = metricRegistry.getGauges().get(HiveMQMetrics.QUEUED_MESSAGES_MEMORY_PERSISTENCE_TOTAL_SIZE.name());
        assertTrue(gauge.getValue() > 0);

        int byteLimit = totalPublishBytes / 2;
        final ImmutableList<PUBLISH> allReadPublishes = persistence.readNew("client", false, createPacketIds(1, 100), byteLimit, 0);
        assertEquals(51, allReadPublishes.size());

        for (final PUBLISH pub : allReadPublishes) {
            persistence.remove("client", pub.getPacketIdentifier(), pub.getUniqueId(), 0);
        }

        final ImmutableList<PUBLISH> allReadPublishes2 = persistence.readNew("client", false, createPacketIds(52, 100), byteLimit, 0);
        assertEquals(48, allReadPublishes2.size());
        assertTrue(gauge.getValue() > 0);

        for (final PUBLISH pub : allReadPublishes2) {
            persistence.remove("client", pub.getPacketIdentifier(), pub.getUniqueId(), 0);
        }

        //last qos0 message
        final ImmutableList<PUBLISH> allReadPublishes3 = persistence.readNew("client", false, createPacketIds(100, 100), byteLimit, 0);
        assertEquals(1, allReadPublishes3.size());
        assertEquals(0, gauge.getValue().longValue());

    }

    private ImmutableIntArray createPacketIds(final int start, final int size) {
        final ImmutableIntArray.Builder builder = ImmutableIntArray.builder();
        for (int i = start; i < (size + start); i++) {
            builder.add(i);
        }
        return builder.build();
    }

    private PUBLISH createPublish(final int packetId, final QoS qos) {
        return createPublish(packetId, qos, "topic");
    }

    private PUBLISH createPublish(final int packetId, final QoS qos, final long expiryInterval, final long timestamp) {
        return new PUBLISHFactory.Mqtt5Builder().withPacketIdentifier(packetId)
                .withQoS(qos)
                .withPublishId(1L)
                .withPayload("message".getBytes())
                .withTopic("topic")
                .withHivemqId("hivemqId")
                .withPersistence(payloadPersistence)
                .withMessageExpiryInterval(expiryInterval)
                .withTimestamp(timestamp)
                .build();
    }

    private PUBLISH createPublish(final int packetId, final QoS qos, final String topic) {
        return new PUBLISHFactory.Mqtt5Builder().withPacketIdentifier(packetId)
                .withQoS(qos)
                .withPublishId(1L)
                .withPayload("message".getBytes())
                .withTopic(topic)
                .withHivemqId("hivemqId")
                .withPersistence(payloadPersistence)
                .build();
    }

    private PUBLISH createPublish(final int packetId, final QoS qos, final String topic, final int publishId) {
        return new PUBLISHFactory.Mqtt5Builder().withPacketIdentifier(packetId)
                .withQoS(qos)
                .withPublishId(1L)
                .withPayload("message".getBytes())
                .withTopic(topic)
                .withHivemqId("hivemqId")
                .withPersistence(payloadPersistence)
                .withPublishId(publishId)
                .build();
    }


    private PUBLISH createPublish(final int packetId, final QoS qos, final String topic, final int publishId, final byte[] message) {
        return new PUBLISHFactory.Mqtt5Builder().withPacketIdentifier(packetId)
                .withQoS(qos)
                .withPublishId(1L)
                .withPayload(message)
                .withTopic(topic)
                .withHivemqId("hivemqId")
                .withPersistence(payloadPersistence)
                .withPublishId(publishId)
                .build();
    }

    private PUBLISH createBigPublish(
            final int packetId, final QoS qos, final String topic, final int publishId, final int queueLimit) {
        return new PUBLISHFactory.Mqtt5Builder().withPacketIdentifier(packetId)
                .withQoS(qos)
                .withPublishId(1L)
                .withPayload(RandomStringUtils.randomAlphanumeric(queueLimit).getBytes())
                .withCorrelationData(RandomStringUtils.randomAlphanumeric(65000).getBytes())
                .withResponseTopic(RandomStringUtils.randomAlphanumeric(65000))
                .withTopic(topic)
                .withHivemqId("hivemqId")
                .withPublishId(publishId)
                .withPersistence(payloadPersistence)
                .build();
    }
}