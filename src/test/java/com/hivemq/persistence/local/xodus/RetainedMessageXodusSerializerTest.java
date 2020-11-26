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
package com.hivemq.persistence.local.xodus;

import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.mqtt5.PropertiesSerializationUtil;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.util.Bytes;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

/**
 * @author Christoph Schäbel
 */
public class RetainedMessageXodusSerializerTest {

    private RetainedMessageXodusSerializer serializer;


    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        serializer = new RetainedMessageXodusSerializer();
    }

    @Test
    public void test_serializeKey() throws Exception {

        final byte[] key = serializer.serializeKey("topic");

        assertTrue(Arrays.equals("topic".getBytes(UTF_8), key));
    }

    @Test
    public void test_deserializeKey() throws Exception {
        final String key = serializer.deserializeKey("topic".getBytes(UTF_8));

        assertEquals("topic", key);
    }

    @Test
    public void test_serializeValue_qos0() throws Exception {


        final byte[] value = serializer.serializeValue(new RetainedMessage(new byte[]{5, 5, 5}, QoS.AT_MOST_ONCE, 10L, 10, Mqtt5UserProperties.NO_USER_PROPERTIES, null, null, null, null, 1231321231320L));

        final byte[] expected = new byte[38 + PropertiesSerializationUtil.encodedSize(Mqtt5UserProperties.NO_USER_PROPERTIES)];
        expected[0] = 0b0000_0000;
        Bytes.copyLongToByteArray(1231321231320L, expected, 1);
        Bytes.copyLongToByteArray(10L, expected, 9);
        Bytes.copyLongToByteArray(10, expected, 17);
        Bytes.copyIntToByteArray(0, expected, 25);
        Bytes.copyIntToByteArray(0, expected, 29);
        Bytes.copyIntToByteArray(0, expected, 33);
        expected[37] = -1;

        assertTrue(Arrays.equals(expected, value));
    }

    @Test
    public void test_deserializeValue_qos0() throws Exception {

        final byte[] serialized = new byte[38 + PropertiesSerializationUtil.encodedSize(Mqtt5UserProperties.NO_USER_PROPERTIES)];
        serialized[0] = 0b0000_0000;
        Bytes.copyLongToByteArray(1231321231320L, serialized, 1);
        Bytes.copyLongToByteArray(10L, serialized, 9);
        Bytes.copyLongToByteArray(10, serialized, 17);
        Bytes.copyIntToByteArray(0, serialized, 25);
        Bytes.copyIntToByteArray(0, serialized, 29);
        Bytes.copyIntToByteArray(0, serialized, 33);
        serialized[37] = -1;

        final RetainedMessage message = serializer.deserializeValue(serialized);

        assertEquals(1231321231320L, message.getTimestamp());
        assertEquals(10L, message.getPublishId());
        assertEquals(QoS.AT_MOST_ONCE, message.getQos());
        assertEquals(10, message.getMessageExpiryInterval());
    }

    @Test
    public void test_serializeValue_qos1() throws Exception {

        final byte[] value = serializer.serializeValue(new RetainedMessage(new byte[]{5, 5, 5}, QoS.AT_LEAST_ONCE, 10L, 10, Mqtt5UserProperties.NO_USER_PROPERTIES, null, null, null, null, 1231321231321L));

        final byte[] expected = new byte[38 + PropertiesSerializationUtil.encodedSize(Mqtt5UserProperties.NO_USER_PROPERTIES)];
        expected[0] = 0b0000_0001;
        Bytes.copyLongToByteArray(1231321231321L, expected, 1);
        Bytes.copyLongToByteArray(10L, expected, 9);
        Bytes.copyLongToByteArray(10, expected, 17);
        Bytes.copyIntToByteArray(0, expected, 25);
        Bytes.copyIntToByteArray(0, expected, 29);
        Bytes.copyIntToByteArray(0, expected, 33);
        expected[37] = -1;

        assertTrue(Arrays.equals(expected, value));
    }

    @Test
    public void test_deserializeValue_qos1() throws Exception {

        final byte[] serialized = new byte[38 + PropertiesSerializationUtil.encodedSize(Mqtt5UserProperties.NO_USER_PROPERTIES)];
        serialized[0] = 0b0000_0001;

        Bytes.copyLongToByteArray(1231321231321L, serialized, 1);
        Bytes.copyLongToByteArray(10L, serialized, 9);
        Bytes.copyLongToByteArray(10, serialized, 17);
        Bytes.copyIntToByteArray(0, serialized, 25);
        Bytes.copyIntToByteArray(0, serialized, 29);
        Bytes.copyIntToByteArray(0, serialized, 33);
        serialized[37] = -1;

        final RetainedMessage message = serializer.deserializeValue(serialized);

        assertEquals(1231321231321L, message.getTimestamp());
        assertEquals(10, message.getPublishId());
        assertEquals(QoS.AT_LEAST_ONCE, message.getQos());
        assertEquals(10, message.getMessageExpiryInterval());
    }

    @Test
    public void test_serializeValue_qos2() throws Exception {


        final byte[] value = serializer.serializeValue(new RetainedMessage(new byte[]{5, 5, 5}, QoS.EXACTLY_ONCE, 10L, 10, Mqtt5UserProperties.NO_USER_PROPERTIES, null, null, null, null, 1231321231302L));

        final byte[] expected = new byte[38 + PropertiesSerializationUtil.encodedSize(Mqtt5UserProperties.NO_USER_PROPERTIES)];
        expected[0] = 0b0000_0010;
        Bytes.copyLongToByteArray(1231321231302L, expected, 1);
        Bytes.copyLongToByteArray(10L, expected, 9);
        Bytes.copyLongToByteArray(10, expected, 17);
        Bytes.copyIntToByteArray(0, expected, 25);
        Bytes.copyIntToByteArray(0, expected, 29);
        Bytes.copyIntToByteArray(0, expected, 33);
        expected[37] = -1;

        assertTrue(Arrays.equals(expected, value));
    }

    @Test
    public void test_deserializeValue_qos2() throws Exception {

        final byte[] serialized = new byte[38 + PropertiesSerializationUtil.encodedSize(Mqtt5UserProperties.NO_USER_PROPERTIES)];
        serialized[0] = 0b0000_0010;
        Bytes.copyLongToByteArray(1231321231302L, serialized, 1);
        Bytes.copyLongToByteArray(10L, serialized, 9);
        Bytes.copyLongToByteArray(10, serialized, 17);
        Bytes.copyIntToByteArray(0, serialized, 25);
        Bytes.copyIntToByteArray(0, serialized, 29);
        Bytes.copyIntToByteArray(0, serialized, 33);
        serialized[37] = -1;


        final RetainedMessage message = serializer.deserializeValue(serialized);

        assertEquals(1231321231302L, message.getTimestamp());
        assertEquals(10L, message.getPublishId());
        assertEquals(QoS.EXACTLY_ONCE, message.getQos());
        assertEquals(10, message.getMessageExpiryInterval());
    }

    @Test
    public void test_serialize_properties() {

        final long now = System.currentTimeMillis();

        final RetainedMessage retainedMessage = new RetainedMessage(new byte[]{5, 5, 5}, QoS.AT_MOST_ONCE, 1L, 10, Mqtt5UserProperties.of(MqttUserProperty.of("name", "value")),
                "responseTopic", "contentType", new byte[]{1, 2, 3}, Mqtt5PayloadFormatIndicator.UTF_8, now);
        final byte[] bytes = serializer.serializeValue(retainedMessage);
        final RetainedMessage messageFromStore = serializer.deserializeValue(bytes);
        final Mqtt5UserProperties userProperties = messageFromStore.getUserProperties();
        assertEquals(1, userProperties.asList().size());
        final MqttUserProperty property = userProperties.asList().get(0);
        assertEquals("name", property.getName());
        assertEquals("value", property.getValue());
        assertEquals("responseTopic", messageFromStore.getResponseTopic());
        assertEquals("contentType", messageFromStore.getContentType());
        assertArrayEquals(new byte[]{1, 2, 3}, messageFromStore.getCorrelationData());
        assertEquals(Mqtt5PayloadFormatIndicator.UTF_8, messageFromStore.getPayloadFormatIndicator());
    }
}