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
package com.hivemq.persistence.clientqueue;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableIntArray;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import jetbrains.exodus.ByteIterable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;

import static com.hivemq.persistence.clientqueue.ClientQueuePersistenceImpl.Key;
import static org.junit.Assert.*;

/**
 * @author Lukas Brandl
 */
public class ClientQueuePersistenceSerializerTest {

    @Mock
    private PublishPayloadPersistence payloadPersistence;

    private ClientQueuePersistenceSerializer serializer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        serializer = new ClientQueuePersistenceSerializer(payloadPersistence);
        ClientQueuePersistenceSerializer.NEXT_PUBLISH_NUMBER.set(Long.MAX_VALUE / 2);
    }

    @Test
    public void test_deserialize_index() {
        final Key key = new Key("client", false);
        final ByteIterable keyBytes = serializer.serializeNewPublishKey(key);
        final long index = serializer.deserializeIndex(keyBytes);
        assertEquals(Long.MAX_VALUE / 2, index);
    }

    @Test
    public void test_serializeNewPublishKey() {
        final ByteIterable serializedKey1 = serializer.serializeNewPublishKey(new Key("client", false));
        final Key key = serializer.deserializeKeyId(serializedKey1);
        final int serializedClientIdLength = "client".getBytes(StandardCharsets.UTF_8).length;
        assertEquals("client", key.getQueueId());
        assertEquals(false, key.isShared());
        assertEquals(serializedClientIdLength + 1 + 8, serializedKey1.getLength());

        final ByteIterable serializedKey2 = serializer.serializeNewPublishKey(new Key("client", false));
        assertTrue(serializedKey1.compareTo(serializedKey2) < 0);
    }

    @Test
    public void test_serializeUnknownPubRelKey() {
        final ByteIterable serializedKeyBefore = serializer.serializeNewPublishKey(new Key("client", false));
        final ByteIterable serializedKey1 = serializer.serializeUnknownPubRelKey(new Key("client", false));

        final Key key = serializer.deserializeKeyId(serializedKey1);
        final int serializedClientIdLength = "client".getBytes(StandardCharsets.UTF_8).length;
        assertEquals("client", key.getQueueId());
        assertEquals(false, key.isShared());
        assertEquals(serializedClientIdLength + 1 + 8, serializedKey1.getLength());

        assertTrue(serializedKeyBefore.compareTo(serializedKey1) > 0);
    }

    @Test
    public void test_serialize_pubrel() {
        final PUBREL pubrel = new PUBREL(10);
        final ByteIterable bytes = serializer.serializePubRel(pubrel, true);
        assertEquals(10, serializer.deserializeValue(bytes).getPacketIdentifier());
        assertTrue(serializer.deserializeRetained(bytes));
    }

    @Test
    public void test_serialize_pubrel_with_expiry() {
        final PUBREL pubrel = new PUBREL(10);
        pubrel.setExpiryInterval(1L);
        pubrel.setPublishTimestamp(2L);
        final ByteIterable bytes = serializer.serializePubRel(pubrel, true);
        final PUBREL deserializedPubrel = (PUBREL) serializer.deserializeValue(bytes);
        assertEquals(10, deserializedPubrel.getPacketIdentifier());
        assertTrue(serializer.deserializeRetained(bytes));
        assertEquals(1L, deserializedPubrel.getExpiryInterval().longValue());
        assertEquals(2L, deserializedPubrel.getPublishTimestamp().longValue());
    }

    @Test
    public void test_serialize_minimal_publish() {
        final PUBLISH publish = new PUBLISHFactory.Mqtt3Builder().withPacketIdentifier(10)
                .withQoS(QoS.AT_LEAST_ONCE)
                .withPublishId(123)
                .withTimestamp(456)
                .withHivemqId("hivemqId")
                .withPersistence(payloadPersistence)
                .withTopic("topic")
                .withDuplicateDelivery(false)
                .build();

        ByteIterable serializedValue = serializer.serializePublishWithoutPacketId(publish, false);

        serializedValue = serializer.serializeAndSetPacketId(serializedValue, publish.getPacketIdentifier());
        final Message messageWithID = serializer.deserializeValue(serializedValue);

        assertEquals(true, messageWithID instanceof PUBLISH);
        final PUBLISH readPublish = (PUBLISH) messageWithID;

        assertEquals(10, readPublish.getPacketIdentifier());
        assertEquals(QoS.AT_LEAST_ONCE, readPublish.getQoS());
        assertEquals(123, readPublish.getPublishId());
        assertEquals(456, readPublish.getTimestamp());
        assertEquals("hivemqId", readPublish.getHivemqId());
        assertEquals(PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET, readPublish.getMessageExpiryInterval());
        assertEquals(false, readPublish.isRetain());
        assertEquals(false, readPublish.isDuplicateDelivery());

    }

    @Test
    public void test_serialize_mqtt_5_publish() {
        final Mqtt5UserProperties properties = Mqtt5UserProperties.of(
                ImmutableList.of(new MqttUserProperty("name1", "value1"), new MqttUserProperty("name2", "value2")));

        final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder().withPacketIdentifier(10)
                .withQoS(QoS.AT_LEAST_ONCE)
                .withPublishId(123)
                .withTimestamp(456)
                .withHivemqId("hivemqId")
                .withPersistence(payloadPersistence)
                .withMessageExpiryInterval(PUBLISH.MESSAGE_EXPIRY_INTERVAL_MAX)
                .withTopic("topic")
                .withRetain(true)
                .withDuplicateDelivery(false)
                .withUserProperties(properties)
                .withResponseTopic("responseTopic")
                .withContentType("contentType")
                .withCorrelationData(new byte[]{1, 2, 3})
                .withPayloadFormatIndicator(Mqtt5PayloadFormatIndicator.UTF_8)
                .withSubscriptionIdentifiers(ImmutableIntArray.of(1, 2, 3))
                .build();

        ByteIterable serializedValue = serializer.serializePublishWithoutPacketId(publish, true);
        serializedValue = serializer.serializeAndSetPacketId(serializedValue, publish.getPacketIdentifier());
        final Message messageWithID = serializer.deserializeValue(serializedValue);

        assertEquals(true, messageWithID instanceof PUBLISH);
        final PUBLISH readPublish = (PUBLISH) messageWithID;

        assertEquals(10, readPublish.getPacketIdentifier());
        assertEquals(QoS.AT_LEAST_ONCE, readPublish.getQoS());
        assertEquals(123, readPublish.getPublishId());
        assertEquals(456, readPublish.getTimestamp());
        assertEquals("hivemqId", readPublish.getHivemqId());
        assertEquals(PUBLISH.MESSAGE_EXPIRY_INTERVAL_MAX, readPublish.getMessageExpiryInterval());
        assertEquals(true, readPublish.isRetain());
        assertEquals(false, readPublish.isDuplicateDelivery());


        assertEquals(2, readPublish.getUserProperties().asList().size());
        assertEquals("name1", readPublish.getUserProperties().asList().get(0).getName());
        assertEquals("value1", readPublish.getUserProperties().asList().get(0).getValue());
        assertEquals("name2", readPublish.getUserProperties().asList().get(1).getName());
        assertEquals("value2", readPublish.getUserProperties().asList().get(1).getValue());
        assertEquals("responseTopic", readPublish.getResponseTopic());
        assertEquals("contentType", readPublish.getContentType());
        assertArrayEquals(new byte[]{1, 2, 3}, readPublish.getCorrelationData());
        assertEquals(Mqtt5PayloadFormatIndicator.UTF_8, readPublish.getPayloadFormatIndicator());
        assertEquals(3, readPublish.getSubscriptionIdentifiers().length());
    }

    @Test
    public void test_serialize_mqtt_5_publish_null_properties() {

        final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder().withPacketIdentifier(10)
                .withQoS(QoS.AT_LEAST_ONCE)
                .withPublishId(123)
                .withTimestamp(456)
                .withHivemqId("hivemqId")
                .withPersistence(payloadPersistence)
                .withMessageExpiryInterval(PUBLISH.MESSAGE_EXPIRY_INTERVAL_MAX)
                .withTopic("topic")
                .withRetain(true)
                .withDuplicateDelivery(false)
                .build();

        ByteIterable serializedValue = serializer.serializePublishWithoutPacketId(publish, false);
        serializedValue = serializer.serializeAndSetPacketId(serializedValue, publish.getPacketIdentifier());
        final Message messageWithID = serializer.deserializeValue(serializedValue);

        assertEquals(true, messageWithID instanceof PUBLISH);
        final PUBLISH readPublish = (PUBLISH) messageWithID;

        assertEquals(10, readPublish.getPacketIdentifier());
        assertEquals(QoS.AT_LEAST_ONCE, readPublish.getQoS());
        assertEquals(123, readPublish.getPublishId());
        assertEquals(456, readPublish.getTimestamp());
        assertEquals("hivemqId", readPublish.getHivemqId());
        assertEquals(PUBLISH.MESSAGE_EXPIRY_INTERVAL_MAX, readPublish.getMessageExpiryInterval());
        assertEquals(true, readPublish.isRetain());
        assertEquals(false, readPublish.isDuplicateDelivery());


        assertEquals(0, readPublish.getUserProperties().asList().size());
        assertNull(readPublish.getResponseTopic());
        assertNull(readPublish.getContentType());
        assertNull(readPublish.getCorrelationData());
    }

    @Test(expected = NullPointerException.class)
    public void test_deserializeClientId_not_null() {
        final ByteIterable iterable = null;
        serializer.deserializeKeyId(iterable);
    }

    @Test(expected = NullPointerException.class)
    public void test_serializeKey_client_null() {
        serializer.serializeNewPublishKey(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_deserializeValue_null() {
        final ByteIterable iterable = null;
        serializer.deserializeValue(iterable);
    }
}