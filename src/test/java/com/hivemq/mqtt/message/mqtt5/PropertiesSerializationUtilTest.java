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
package com.hivemq.mqtt.message.mqtt5;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Lukas Brandl
 */
public class PropertiesSerializationUtilTest {

    @Test
    public void test_no_property() {
        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(ImmutableList.of());
        final int encodedSize = PropertiesSerializationUtil.encodedSize(userProperties);
        assertEquals(4, encodedSize);

        final byte[] bytes = new byte[encodedSize];
        PropertiesSerializationUtil.write(userProperties, bytes, 0);
        final Mqtt5UserProperties read = PropertiesSerializationUtil.read(bytes, 0);
        final ImmutableList<MqttUserProperty> readProperties = read.asList();
        assertEquals(0, readProperties.size());
    }

    @Test
    public void test_one_property() {
        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(ImmutableList.of(new MqttUserProperty("name", "value")));
        final int encodedSize = PropertiesSerializationUtil.encodedSize(userProperties);
        assertEquals("name".length() + "value".length() + (4 * userProperties.asList().size()) + 4, encodedSize);

        final byte[] bytes = new byte[encodedSize];
        PropertiesSerializationUtil.write(userProperties, bytes, 0);
        final Mqtt5UserProperties read = PropertiesSerializationUtil.read(bytes, 0);
        final ImmutableList<MqttUserProperty> readProperties = read.asList();
        assertEquals(1, readProperties.size());
        assertEquals("name", readProperties.get(0).getName());
        assertEquals("value", readProperties.get(0).getValue());
    }

    @Test
    public void test_multiple_property() {
        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(ImmutableList.of(new MqttUserProperty("name1", "value1"),
                new MqttUserProperty("name2", "value2"), new MqttUserProperty("name3", "value3")));
        final int encodedSize = PropertiesSerializationUtil.encodedSize(userProperties);
        assertEquals("name1".length() + "value1".length() +
                "name2".length() + "value3".length() +
                "name3".length() + "value3".length() + (4 * userProperties.asList().size()) + 4, encodedSize);

        final byte[] bytes = new byte[encodedSize];
        PropertiesSerializationUtil.write(userProperties, bytes, 0);
        final Mqtt5UserProperties read = PropertiesSerializationUtil.read(bytes, 0);
        final ImmutableList<MqttUserProperty> readProperties = read.asList();
        assertEquals(3, readProperties.size());
        assertEquals("name1", readProperties.get(0).getName());
        assertEquals("value1", readProperties.get(0).getValue());
        assertEquals("name2", readProperties.get(1).getName());
        assertEquals("value2", readProperties.get(1).getValue());
        assertEquals("name3", readProperties.get(2).getName());
        assertEquals("value3", readProperties.get(2).getValue());
    }
}