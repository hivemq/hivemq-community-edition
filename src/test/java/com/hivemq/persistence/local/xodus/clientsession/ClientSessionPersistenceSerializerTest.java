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

import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.codec.encoder.mqtt5.UnsignedDataTypes;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.ClientSessionWill;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Christoph Schäbel
 */
public class ClientSessionPersistenceSerializerTest {

    private ClientSessionPersistenceSerializer serializer;

    @Before
    public void before() {
        serializer = new ClientSessionPersistenceSerializer();
    }


    @Test
    public void test_serialize_deserialize_key() throws Exception {

        final String key = RandomStringUtils.randomAlphanumeric(40);

        final byte[] bytes = serializer.serializeKey(key);
        final String result = serializer.deserializeKey(bytes);

        assertEquals(key, result);
    }

    @Test
    public void test_serialize_deserialize_value() throws Exception {

        final ClientSession session = new ClientSession(true, UnsignedDataTypes.UNSIGNED_INT_MAX_VALUE);

        final byte[] bytes = serializer.serializeValue(session, 1234567890L);
        final ClientSession result = serializer.deserializeValue(bytes);
        final long timestamp = serializer.deserializeTimestamp(bytes);

        assertEquals(session.isConnected(), result.isConnected());
        assertEquals(UnsignedDataTypes.UNSIGNED_INT_MAX_VALUE, result.getSessionExpiryInterval());
        assertEquals(1234567890L, timestamp);

        final ClientSession result2 = serializer.deserializeValueWithoutWill(bytes);
        assertEquals(session.isConnected(), result2.isConnected());
        assertEquals(UnsignedDataTypes.UNSIGNED_INT_MAX_VALUE, result2.getSessionExpiryInterval());
        assertNull(result2.getWillPublish());
    }

    @Test
    public void test_serialize_deserialize_positive_ttl_value() throws Exception {

        final ClientSession session = new ClientSession(false, 10000);

        final byte[] bytes = serializer.serializeValue(session, 1234567890L);
        final ClientSession result = serializer.deserializeValue(bytes);
        final long timestamp = serializer.deserializeTimestamp(bytes);

        assertEquals(session.isConnected(), result.isConnected());
        assertEquals(10000L, result.getSessionExpiryInterval());
        assertEquals(1234567890L, timestamp);

        final ClientSession result2 = serializer.deserializeValueWithoutWill(bytes);
        assertEquals(session.isConnected(), result2.isConnected());
        assertEquals(10000L, result2.getSessionExpiryInterval());
        assertNull(result2.getWillPublish());
    }

    @Test
    public void test_will_without_properties() throws Exception {

        final MqttWillPublish willPublish = new MqttWillPublish.Mqtt5Builder().withTopic("topic").withDelayInterval(1).withMessageExpiryInterval(3)
                .withQos(QoS.AT_MOST_ONCE).withRetain(true).withHivemqId("hivemqId").withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES).build();
        final ClientSessionWill clientSessionWill = new ClientSessionWill(willPublish, 2L);

        final ClientSession session = new ClientSession(false, 10000, clientSessionWill, null);

        final byte[] bytes = serializer.serializeValue(session, 1234567890L);
        final ClientSession result = serializer.deserializeValue(bytes);
        final long timestamp = serializer.deserializeTimestamp(bytes);

        assertEquals(session.isConnected(), result.isConnected());
        assertEquals(10000L, result.getSessionExpiryInterval());
        assertEquals(1234567890L, timestamp);

        final ClientSessionWill resultWill = result.getWillPublish();

        assertEquals("topic", resultWill.getTopic());
        assertEquals(1, resultWill.getDelayInterval());
        assertEquals(2L, resultWill.getPublishId());
        assertEquals(3, resultWill.getMessageExpiryInterval());
        assertEquals(QoS.AT_MOST_ONCE, resultWill.getQos());
        assertEquals(true, resultWill.isRetain());
        assertEquals("hivemqId", resultWill.getHivemqId());
    }

    @Test
    public void test_will_with_properties() throws Exception {

        final MqttWillPublish willPublish = new MqttWillPublish.Mqtt5Builder().withTopic("topic").withDelayInterval(1).withMessageExpiryInterval(3)
                .withQos(QoS.AT_MOST_ONCE).withRetain(true).withHivemqId("hivemqId").withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withPayloadFormatIndicator(Mqtt5PayloadFormatIndicator.UTF_8).withResponseTopic("responseTopic").withContentType("contentType")
                .withCorrelationData(new byte[]{1, 2, 3}).build();
        final ClientSessionWill clientSessionWill = new ClientSessionWill(willPublish, 2L);

        final ClientSession session = new ClientSession(false, 10000, clientSessionWill, null);

        final byte[] bytes = serializer.serializeValue(session, 1234567890L);
        final ClientSession result = serializer.deserializeValue(bytes);
        final long timestamp = serializer.deserializeTimestamp(bytes);

        assertEquals(session.isConnected(), result.isConnected());
        assertEquals(10000L, result.getSessionExpiryInterval());
        assertEquals(1234567890L, timestamp);

        final ClientSessionWill resultWill = result.getWillPublish();

        assertEquals("topic", resultWill.getTopic());
        assertEquals(1, resultWill.getDelayInterval());
        assertEquals(2, resultWill.getPublishId());
        assertEquals(3, resultWill.getMessageExpiryInterval());
        assertEquals(QoS.AT_MOST_ONCE, resultWill.getQos());
        assertEquals(true, resultWill.isRetain());
        assertEquals("hivemqId", resultWill.getHivemqId());

        assertEquals(Mqtt5PayloadFormatIndicator.UTF_8, resultWill.getPayloadFormatIndicator());
        assertEquals("responseTopic", resultWill.getResponseTopic());
        assertEquals("contentType", resultWill.getContentType());
        assertArrayEquals(new byte[]{1, 2, 3}, resultWill.getCorrelationData());


        final ClientSession result2 = serializer.deserializeValueWithoutWill(bytes);
        assertEquals(session.isConnected(), result2.isConnected());
        assertEquals(10000L, result2.getSessionExpiryInterval());
        assertNull(result2.getWillPublish());
    }

    @Test
    public void test_will_with_one_properties() throws Exception {

        final MqttWillPublish willPublish = new MqttWillPublish.Mqtt5Builder().withTopic("topic").withDelayInterval(1).withMessageExpiryInterval(3)
                .withQos(QoS.AT_MOST_ONCE).withRetain(true).withHivemqId("hivemqId").withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withContentType("contentType").build();
        final ClientSessionWill clientSessionWill = new ClientSessionWill(willPublish, 2L);

        final ClientSession session = new ClientSession(false, 10000, clientSessionWill, null);

        final byte[] bytes = serializer.serializeValue(session, 1234567890L);
        final ClientSession result = serializer.deserializeValue(bytes);
        final long timestamp = serializer.deserializeTimestamp(bytes);

        assertEquals(session.isConnected(), result.isConnected());
        assertEquals(10000L, result.getSessionExpiryInterval());
        assertEquals(1234567890L, timestamp);

        final ClientSessionWill resultWill = result.getWillPublish();

        assertEquals("topic", resultWill.getTopic());
        assertEquals(1, resultWill.getDelayInterval());
        assertEquals(2, resultWill.getPublishId());
        assertEquals(3, resultWill.getMessageExpiryInterval());
        assertEquals(QoS.AT_MOST_ONCE, resultWill.getQos());
        assertEquals(true, resultWill.isRetain());
        assertEquals("hivemqId", resultWill.getHivemqId());

        assertNull(resultWill.getPayloadFormatIndicator());
        assertNull(resultWill.getResponseTopic());
        assertEquals("contentType", resultWill.getContentType());
        assertNull(resultWill.getCorrelationData());
    }

    @Test
    public void test_value_with_queue_limit() throws Exception {

        final ClientSession session = new ClientSession(true, UnsignedDataTypes.UNSIGNED_INT_MAX_VALUE,
                null, 123L);

        final byte[] bytes = serializer.serializeValue(session, 1234567890L);
        final ClientSession result = serializer.deserializeValue(bytes);
        final long timestamp = serializer.deserializeTimestamp(bytes);

        assertEquals(session.isConnected(), result.isConnected());
        assertEquals(UnsignedDataTypes.UNSIGNED_INT_MAX_VALUE, result.getSessionExpiryInterval());
        assertEquals(123L, result.getQueueLimit().longValue());
        assertEquals(1234567890L, timestamp);
    }

}
