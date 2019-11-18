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

package com.hivemq.persistence.local.xodus;

import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import com.hivemq.util.ThreadPreConditions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class RetainedMessageRocksDBLocalPersistenceTest {

    private static final int BUCKETSIZE = 4;
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private RetainedMessageRocksDBLocalPersistence persistence;

    @Mock
    private LocalPersistenceFileUtil localPersistenceFileUtil;

    @Mock
    private PublishPayloadPersistence payloadPersistence;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InternalConfigurations.PERSISTENCE_CLOSE_RETRIES.set(3);
        InternalConfigurations.PERSISTENCE_CLOSE_RETRY_INTERVAL.set(5);
        InternalConfigurations.PERSISTENCE_BUCKET_COUNT.set(BUCKETSIZE);
        when(localPersistenceFileUtil.getVersionedLocalPersistenceFolder(anyString(), anyString())).thenReturn(
                temporaryFolder.newFolder());

        when(payloadPersistence.getPayloadOrNull(0)).thenReturn("message0".getBytes());
        when(payloadPersistence.get(0)).thenReturn("message0".getBytes());
        when(payloadPersistence.getPayloadOrNull(1)).thenReturn("message1".getBytes());
        when(payloadPersistence.get(1)).thenReturn("message1".getBytes());
        when(payloadPersistence.getPayloadOrNull(2)).thenReturn("message2".getBytes());
        when(payloadPersistence.get(2)).thenReturn("message2".getBytes());
        when(payloadPersistence.getPayloadOrNull(3)).thenReturn("message3".getBytes());
        when(payloadPersistence.get(3)).thenReturn("message3".getBytes());
        when(payloadPersistence.getPayloadOrNull(4)).thenReturn("message4".getBytes());
        when(payloadPersistence.get(4)).thenReturn("message4".getBytes());

        persistence = new RetainedMessageRocksDBLocalPersistence(localPersistenceFileUtil, payloadPersistence, new PersistenceStartup());
        persistence.start();
    }

    @After
    public void cleanUp() {
        for (int i = 0; i < BUCKETSIZE; i++) {
            persistence.closeDB(i);
        }
    }

    @Test
    public void test_persist_get_no_payload_found() {

        persistence.put(
                new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 100L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/0", BucketUtils.getBucket("topic/0", BUCKETSIZE));

        assertNull(persistence.get("topic/0", BucketUtils.getBucket("topic/0", BUCKETSIZE)));
    }

    @Test
    public void test_persist_same_topic() {
        persistence.put(
                new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 0L, MqttConfigurationDefaults.TTL_DISABLED), "topic",
                BucketUtils.getBucket("topic", BUCKETSIZE));
        persistence.put(
                new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 0L, MqttConfigurationDefaults.TTL_DISABLED), "topic",
                BucketUtils.getBucket("topic", BUCKETSIZE));

        //existing entry has newer timestamp, so we expect the "old" value
        assertEquals(
                "message0",
                new String(persistence.get("topic", BucketUtils.getBucket("topic", BUCKETSIZE)).getMessage()));

        persistence.put(
                new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 3L, MqttConfigurationDefaults.TTL_DISABLED), "topic",
                BucketUtils.getBucket("topic", BUCKETSIZE));

        assertEquals(
                "message3",
                new String(persistence.get("topic", BucketUtils.getBucket("topic", BUCKETSIZE)).getMessage()));

        persistence.put(
                new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 4L, MqttConfigurationDefaults.TTL_DISABLED), "topic",
                BucketUtils.getBucket("topic", BUCKETSIZE));

        assertEquals("message4", new String(persistence.get("topic", BucketUtils.getBucket("topic", BUCKETSIZE))
                .getMessage()));
    }

    @Test
    public void test_getAllTopics() {

        persistence.put(
                new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 0L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/0", 0);
        persistence.put(
                new RetainedMessage(new byte[0], QoS.AT_LEAST_ONCE, 1L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/1", 0);
        persistence.put(
                new RetainedMessage(new byte[0], QoS.EXACTLY_ONCE, 2L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/2", 0);
        persistence.put(
                new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 3L, MqttConfigurationDefaults.TTL_DISABLED), "topic",
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
                new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 0L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/0", 0);
        persistence.put(
                new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 1L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/1", 0);

        persistence.remove("topic/0", 0);
        persistence.remove("topic/1", 0);

        verify(payloadPersistence).decrementReferenceCounter(0);
        verify(payloadPersistence).decrementReferenceCounter(1);

        final Set<String> topics = persistence.topicTrees[0].get("#");
        assertTrue(topics.isEmpty());
    }

    @Test
    public void decrement_payload_reference_count_put() {
        persistence.put(
                new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 0L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/0", 0);
        persistence.put(
                new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 1L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/1", 0);

        persistence.put(
                new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 0L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/0", 0);
        persistence.put(
                new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 1L, MqttConfigurationDefaults.TTL_DISABLED),
                "topic/1", 0);

        verify(payloadPersistence).decrementReferenceCounter(0);
        verify(payloadPersistence).decrementReferenceCounter(1);

        final Set<String> topics = persistence.topicTrees[0].get("#");
        assertEquals(2, topics.size());
        assertTrue(topics.contains("topic/0"));
        assertTrue(topics.contains("topic/1"));
    }

    @Test
    public void test_clean_up_expiry() {

        persistence.put(
                new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 1L, 1, Mqtt5UserProperties.NO_USER_PROPERTIES, null,
                        null, null, null, System.currentTimeMillis() - 2000), "topic", 0);
        persistence.put(
                new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 2L, 1, Mqtt5UserProperties.NO_USER_PROPERTIES, null,
                        null, null, null, System.currentTimeMillis()), "topic2", 0);

        persistence.cleanUp(BucketUtils.getBucket("topic", BUCKETSIZE));
        persistence.cleanUp(BucketUtils.getBucket("topic2", BUCKETSIZE));

        assertNull(persistence.get("topic", 0));
        assertNotNull(persistence.get("topic2", 0));

        verify(payloadPersistence).decrementReferenceCounter(1L);
        verify(payloadPersistence, never()).decrementReferenceCounter(2L);
    }

    @Test
    public void test_expiry() {
        persistence.put(
                new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 1L, 1, Mqtt5UserProperties.NO_USER_PROPERTIES, null,
                        null, null, null, System.currentTimeMillis() - 2000), "topic",
                BucketUtils.getBucket("topic", BUCKETSIZE));

        final RetainedMessage message = persistence.get("topic", BucketUtils.getBucket("topic", BUCKETSIZE));
        assertNull(message);
    }

    @Test
    public void test_read_user_properties_stored() {

        persistence.put(new RetainedMessage(new byte[0], QoS.AT_MOST_ONCE, 0L, MqttConfigurationDefaults.TTL_DISABLED,
                        Mqtt5UserProperties.of(MqttUserProperty.of("name", "value")), "responseTopic", "contentType",
                        new byte[]{1, 2, 3}, Mqtt5PayloadFormatIndicator.UTF_8, System.currentTimeMillis()),
                "topic/0", BucketUtils.getBucket("topic", BUCKETSIZE));

        final RetainedMessage retainedMessage = persistence.get("topic/0", BucketUtils.getBucket("topic", BUCKETSIZE));
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

        for (int i = 0; i < BUCKETSIZE; i++) {
            persistence.put(
                    new RetainedMessage(new byte[0], QoS.AT_LEAST_ONCE, (long) i + 1,
                            MqttConfigurationDefaults.TTL_DISABLED), "topic" + i, 0);
        }

        assertEquals(BUCKETSIZE, persistence.size());

        for (int i = 0; i < BUCKETSIZE; i++) {
            persistence.clear(i);
        }

        assertEquals(0, persistence.size());
        final Set<String> allEntries = persistence.getAllTopics("#", 1);
        assertEquals(0, allEntries.size());

    }
}