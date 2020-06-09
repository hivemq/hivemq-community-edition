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
package com.hivemq.persistence.local.xodus.clientsession;

import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.subscribe.Topic;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Dominik Obermaier
 */
public class ClientSessionSubscriptionXodusSerializerTest {

    private ClientSessionSubscriptionXodusSerializer serializer;

    @Before
    public void before() {
        serializer = new ClientSessionSubscriptionXodusSerializer();
    }


    @Test(expected = NullPointerException.class)
    public void test_serialize_null_value() throws Exception {
        final byte[] bytes = serializer.serializeValue(null, 123L, 1L);
    }

    @Test
    public void test_serialize_deserialize_key() throws Exception {

        final byte[] bytes = serializer.serializeKey("clientid");

        final String key = serializer.deserializeKey(bytes);

        assertEquals("clientid", key);
    }

    @Test
    public void test_serialize_deserialize_value() throws Exception {

        final byte[] bytes = serializer.serializeValue(new Topic("topic", QoS.AT_MOST_ONCE, false,
                true, Mqtt5RetainHandling.SEND, 1), 123456790L, 3L);

        final Topic topic = serializer.deserializeValue(bytes);

        assertEquals("topic", topic.getTopic());
        assertEquals(QoS.AT_MOST_ONCE, topic.getQoS());
        assertFalse(topic.isNoLocal());
        assertTrue(topic.isRetainAsPublished());
        assertEquals(Mqtt5RetainHandling.SEND, topic.getRetainHandling());
        assertEquals(123456790L, serializer.deserializeTimestamp(bytes));
        assertEquals(3L, serializer.deserializeId(bytes));
    }

    @Test
    public void test_serialize_deserialize_huge_utf8_topic_value() throws Exception {

        final String topicString = RandomStringUtils.random(20000);

        final byte[] bytes = serializer.serializeValue(new Topic(topicString, QoS.EXACTLY_ONCE), 123456790L, 1L);

        final Topic topic = serializer.deserializeValue(bytes);

        assertEquals(topicString, topic.getTopic());
        assertEquals(QoS.EXACTLY_ONCE, topic.getQoS());
        assertEquals(123456790L, serializer.deserializeTimestamp(bytes));
        assertEquals(1L, serializer.deserializeId(bytes));
    }

    @Test
    public void test_serialize_deserialize_max_topic_value() throws Exception {

        final String topicString = RandomStringUtils.randomAlphanumeric(65535);

        final byte[] bytes = serializer.serializeValue(new Topic(topicString, QoS.EXACTLY_ONCE, true, false,
                Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, 1), 123456790L, 1L);

        final Topic topic = serializer.deserializeValue(bytes);

        assertEquals(topicString, topic.getTopic());
        assertEquals(QoS.EXACTLY_ONCE, topic.getQoS());
        assertTrue(topic.isNoLocal());
        assertFalse(topic.isRetainAsPublished());
        assertEquals(Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, topic.getRetainHandling());
        assertEquals(QoS.EXACTLY_ONCE, topic.getQoS());
        assertEquals(123456790L, serializer.deserializeTimestamp(bytes));
    }


    /*
     * This is necessary to search for the subscriptions of a certain topic
     */
    @Test
    public void test_value_starts_with_topic() throws Exception {
        final byte[] valueBytes = serializer.serializeValue(new Topic("topic", QoS.AT_MOST_ONCE), 1234L, 1L);
        final byte[] topicBytes = serializer.serializeTopic("topic");
        for (int i = 0; i < topicBytes.length; i++) {
            assertSame(valueBytes[i], topicBytes[i]);
        }
    }
}