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
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.iteration.BucketChunkResult;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Lukas Brandl
 */
public class RetainedMessageMemoryLocalPersistenceTest {

    private @NotNull RetainedMessageMemoryLocalPersistence persistence;

    private final int bucketCount = InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get();

    @Before
    public void setUp() throws Exception {
        persistence = new RetainedMessageMemoryLocalPersistence(new MetricRegistry());
    }

    @Test
    public void test_persist_same_topic() {
        persistence.put(new RetainedMessage("message1".getBytes(), QoS.AT_MOST_ONCE, 0L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic",
                BucketUtils.getBucket("topic", bucketCount));
        final long firstMessageSize = persistence.currentMemorySize.get();
        persistence.put(new RetainedMessage("message2".getBytes(), QoS.AT_MOST_ONCE, 0L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic",
                BucketUtils.getBucket("topic", bucketCount));
        final long secondMessageSize = persistence.currentMemorySize.get();
        assertTrue(firstMessageSize > 0);
        assertEquals(firstMessageSize, secondMessageSize);

        //existing entry has newer timestamp, so we expect the "old" value
        assertEquals("message2",
                new String(persistence.get("topic", BucketUtils.getBucket("topic", bucketCount)).getMessage()));

        persistence.put(new RetainedMessage("message3".getBytes(), QoS.AT_MOST_ONCE, 3L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic",
                BucketUtils.getBucket("topic", bucketCount));

        assertEquals("message3",
                new String(persistence.get("topic", BucketUtils.getBucket("topic", bucketCount)).getMessage()));

        persistence.put(new RetainedMessage("message4".getBytes(), QoS.AT_MOST_ONCE, 4L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic",
                BucketUtils.getBucket("topic", bucketCount));

        assertEquals("message4",
                new String(persistence.get("topic", BucketUtils.getBucket("topic", bucketCount)).getMessage()));
    }

    @Test
    public void test_getAllTopics() {

        persistence.put(
                new RetainedMessage(new byte[]{1, 2, 3}, QoS.AT_MOST_ONCE, 0L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/0",
                0);
        persistence.put(
                new RetainedMessage(new byte[]{1, 2, 3}, QoS.AT_LEAST_ONCE, 1L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/1",
                0);
        persistence.put(
                new RetainedMessage(new byte[]{1, 2, 3}, QoS.EXACTLY_ONCE, 2L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/2",
                0);
        persistence.put(new RetainedMessage(new byte[]{1, 2, 3}, QoS.AT_MOST_ONCE, 3L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic",
                0);

        final Set<String> allTopics1 = persistence.getAllTopics("#", 0);

        assertEquals(4, allTopics1.size());
        assertTrue(allTopics1.contains("topic/0"));
        assertTrue(allTopics1.contains("topic/1"));
        assertTrue(allTopics1.contains("topic/2"));
        assertTrue(allTopics1.contains("topic"));
    }

    @Test
    public void decrement_payload_reference_count_remove() {
        persistence.put(
                new RetainedMessage(new byte[]{1, 2, 3}, QoS.AT_MOST_ONCE, 0L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/0",
                0);
        persistence.put(
                new RetainedMessage(new byte[]{1, 2, 3}, QoS.AT_MOST_ONCE, 1L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/1",
                0);
        assertTrue(persistence.currentMemorySize.get() > 0);

        persistence.remove("topic/0", 0);
        persistence.remove("topic/1", 0);
        persistence.remove("topic/2", 0);

        assertEquals(0, persistence.currentMemorySize.get());


        final Set<String> topics = persistence.topicTrees[0].get("#");
        assertTrue(topics.isEmpty());
    }

    @Test
    public void decrement_payload_reference_count_put() {
        persistence.put(
                new RetainedMessage(new byte[]{1, 2, 3}, QoS.AT_MOST_ONCE, 0L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/0",
                0);
        persistence.put(
                new RetainedMessage(new byte[]{1, 2, 3}, QoS.AT_MOST_ONCE, 1L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/1",
                0);

        persistence.put(
                new RetainedMessage(new byte[]{1, 2, 3}, QoS.AT_MOST_ONCE, 0L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/0",
                0);
        persistence.put(
                new RetainedMessage(new byte[]{1, 2, 3}, QoS.AT_MOST_ONCE, 1L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/1",
                0);

        final Set<String> topics = persistence.topicTrees[0].get("#");
        assertEquals(2, topics.size());
        assertTrue(topics.contains("topic/0"));
        assertTrue(topics.contains("topic/1"));
    }

    @Test
    public void test_clean_up_expiry() {

        persistence.put(new RetainedMessage(new byte[]{1, 2, 3},
                QoS.AT_MOST_ONCE,
                1L,
                1,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                null,
                null,
                null,
                null,
                System.currentTimeMillis() - 2000), "topic", 0);
        persistence.put(new RetainedMessage(new byte[0],
                QoS.AT_MOST_ONCE,
                2L,
                1,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                null,
                null,
                null,
                null,
                System.currentTimeMillis()), "topic2", 0);

        final long sizeBeforeExpiry = persistence.currentMemorySize.get();
        assertTrue(sizeBeforeExpiry > 0);

        persistence.cleanUp(0);

        final long sizeAfterExpiry = persistence.currentMemorySize.get();
        assertTrue(sizeAfterExpiry < sizeBeforeExpiry);

        assertNull(persistence.get("topic", 0));
        assertNotNull(persistence.get("topic2", 0));
    }

    @Test
    public void test_expiry() {
        persistence.put(new RetainedMessage(new byte[]{1, 2, 3},
                QoS.AT_MOST_ONCE,
                1L,
                1,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                null,
                null,
                null,
                null,
                System.currentTimeMillis() - 2000), "topic", BucketUtils.getBucket("topic", bucketCount));

        final RetainedMessage message = persistence.get("topic", BucketUtils.getBucket("topic", bucketCount));
        assertNull(message);
    }

    @Test
    public void test_read_user_properties_stored() {

        persistence.put(new RetainedMessage(new byte[]{1, 2, 3},
                QoS.AT_MOST_ONCE,
                0L,
                MqttConfigurationDefaults.TTL_DISABLED,
                Mqtt5UserProperties.of(MqttUserProperty.of("name", "value")),
                "responseTopic",
                "contentType",
                new byte[]{1, 2, 3},
                Mqtt5PayloadFormatIndicator.UTF_8,
                System.currentTimeMillis()), "topic/0", BucketUtils.getBucket("topic", bucketCount));

        final RetainedMessage retainedMessage = persistence.get("topic/0", BucketUtils.getBucket("topic", bucketCount));
        assertNotNull(retainedMessage);

        assertEquals("responseTopic", retainedMessage.getResponseTopic());
        assertEquals("contentType", retainedMessage.getContentType());
        assertArrayEquals(new byte[]{1, 2, 3}, retainedMessage.getCorrelationData());
        assertEquals(Mqtt5PayloadFormatIndicator.UTF_8, retainedMessage.getPayloadFormatIndicator());

        final MqttUserProperty property = retainedMessage.getUserProperties().asList().get(0);

        assertEquals("name", property.getName());
        assertEquals("value", property.getValue());
    }

    @Test(timeout = 5000)
    public void test_clear() {

        for (int i = 0; i < 10; i++) {
            persistence.put(
                    new RetainedMessage(new byte[]{1, 2, 3},
                            QoS.AT_LEAST_ONCE,
                            (long) i + 1,
                            MqttConfigurationDefaults.TTL_DISABLED),
                    "topic" + i,
                    0);
        }

        assertEquals(10, persistence.size());

        persistence.clear(0);

        assertEquals(0, persistence.size());
        final Set<String> allEntries = persistence.getAllTopics("#", 0);
        assertEquals(0, allEntries.size());

        assertEquals(0, persistence.currentMemorySize.get());
    }

    @Test
    public void getAllRetainedMessagesChunk_emptyPersistence() {
        final BucketChunkResult<Map<String, @NotNull RetainedMessage>> chunk = persistence.getAllRetainedMessagesChunk(1, null, Integer.MAX_VALUE);

        assertEquals(1, chunk.getBucketIndex());
        assertNull(chunk.getLastKey());
        assertTrue(chunk.isFinished());
        assertTrue(chunk.getValue().isEmpty());
    }

    @Test
    public void getAllRetainedMessagesChunk_everyThingInPersistence() {
        persistence.put(new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 1L, 1000), "topic/1", 1);
        persistence.put(new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 2L, 1000), "topic/2", 1);

        final BucketChunkResult<Map<String, @NotNull RetainedMessage>> chunk = persistence.getAllRetainedMessagesChunk(1, null, Integer.MAX_VALUE);

        assertEquals(1, chunk.getBucketIndex());
        assertTrue(chunk.isFinished());
        assertEquals(2, chunk.getValue().size());
    }

    @Test
    public void getAllRetainedMessagesChunk_noExpiredMessages() {
        persistence.put(new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 1L, 1000), "topic/1", 1);
        persistence.put(new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 2L, 0), "topic", 1);

        final BucketChunkResult<Map<String, @NotNull RetainedMessage>> chunk = persistence.getAllRetainedMessagesChunk(1, null, Integer.MAX_VALUE);

        assertEquals(1, chunk.getBucketIndex());
        assertTrue(chunk.isFinished());
        assertEquals(1, chunk.getValue().size());
    }


}