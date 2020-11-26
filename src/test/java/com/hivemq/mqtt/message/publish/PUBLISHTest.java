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
package com.hivemq.mqtt.message.publish;

import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.ObjectMemoryEstimation;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class PUBLISHTest {

    private static final int FIXED_SIZE =
            ObjectMemoryEstimation.objectShellSize() +  // shell size
                    ObjectMemoryEstimation.intSize() +  // size size
                    ObjectMemoryEstimation.longSize() +  // timestamp
            24 + // user props overhead
            ObjectMemoryEstimation.booleanSize() +  // duplicateDelivery
            ObjectMemoryEstimation.booleanSize() +  // retain
            ObjectMemoryEstimation.booleanSize() +  // isNewTopicAlias
            ObjectMemoryEstimation.longSize() +  // messageExpiryInterval
            ObjectMemoryEstimation.longSize() +  // publishId
            ObjectMemoryEstimation.longWrapperSize() + // payloadId
            ObjectMemoryEstimation.enumSize() +  // QoS
            ObjectMemoryEstimation.enumSize();   // payloadFormatIndicator

    @Test(expected = NullPointerException.class)
    public void test_publish_qos_null() {

        new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("hivemqId")
                .withUserProperties(Mqtt5UserProperties.of())
                .withTopic("topic")
                .build();

    }

    @Test(expected = NullPointerException.class)
    public void test_publish_topic_null() {

        new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("hivemqId")
                .withUserProperties(Mqtt5UserProperties.of())
                .withQoS(QoS.AT_MOST_ONCE)
                .build();

    }

    @Test(expected = NullPointerException.class)
    public void test_publish_hivemq_id_null() {

        new PUBLISHFactory.Mqtt5Builder()
                .withQoS(QoS.AT_MOST_ONCE)
                .withTopic("topic")
                .withUserProperties(Mqtt5UserProperties.of())
                .build();

    }

    @Test
    public void test_publish_ok_with_payload() {

        final PUBLISH publishMqtt5 = new PUBLISHFactory.Mqtt5Builder()
                .withQoS(QoS.AT_MOST_ONCE)
                .withHivemqId("hivemqId")
                .withPayload(new byte[0])
                .withTopic("topic")
                .withUserProperties(Mqtt5UserProperties.of())
                .build();

        final PUBLISH publishMqtt3 = new PUBLISHFactory.Mqtt3Builder()
                .withQoS(QoS.AT_MOST_ONCE)
                .withHivemqId("hivemqId")
                .withPayload(new byte[0])
                .withTopic("topic")
                .build();

        assertNotNull(publishMqtt5);
        assertNotNull(publishMqtt3);

    }

    @Test
    public void test_publish_ok_with_payload_id_and_persistence() {

        final PUBLISH publishMqtt5 = new PUBLISHFactory.Mqtt5Builder()
                .withQoS(QoS.AT_MOST_ONCE)
                .withHivemqId("hivemqId")
                .withPublishId(1L)
                .withPersistence(Mockito.mock(PublishPayloadPersistence.class))
                .withTopic("topic")
                .withUserProperties(Mqtt5UserProperties.of())
                .build();

        final PUBLISH publishMqtt3 = new PUBLISHFactory.Mqtt3Builder()
                .withQoS(QoS.AT_MOST_ONCE)
                .withHivemqId("hivemqId")
                .withPublishId(1L)
                .withPersistence(Mockito.mock(PublishPayloadPersistence.class))
                .withTopic("topic")
                .build();

        assertNotNull(publishMqtt5);
        assertNotNull(publishMqtt3);

    }

    @Test
    public void test_estimated_size_always_the_same() throws InterruptedException {

        final PUBLISH publishMqtt5 = new PUBLISHFactory.Mqtt5Builder()
                .withQoS(QoS.AT_MOST_ONCE)
                .withHivemqId("hivemqId") // 16+38 = 54 bytes
                .withPayload("payload".getBytes()) // 7+12 = 19 bytes
                .withPublishId(1L)
                .withTopic("topic") // 10+38 = 48 bytes
                .withResponseTopic("response") // 16+38 = 54 bytes
                .withCorrelationData("correlation".getBytes()) // 11+12 = 23 bytes
                .withUserProperties(Mqtt5UserProperties.of(MqttUserProperty.of("name", "value"))) //   ((4 + 5) * 2) + 24 + 38 + 38 = 118
                .build();

        final List<Thread> threadList = new ArrayList<>();

        final List<Integer> sizeList = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            threadList.add(new Thread(() -> sizeList.add(publishMqtt5.getEstimatedSizeInMemory())));
        }

        for (final Thread thread : threadList) {
            thread.start();
        }

        for (final Thread thread : threadList) {
            thread.join();
        }

        for (final int size : sizeList) {
            //19 + 48 + 54 + 23 + 118 = 262
            assertEquals(262 + 54 + FIXED_SIZE + ObjectMemoryEstimation.stringSize(publishMqtt5.getUniqueId()), size);
        }

    }

    @Test
    public void test_estimated_size_min() throws InterruptedException {

        final PUBLISH publishMqtt5 = new PUBLISHFactory.Mqtt5Builder()
                .withQoS(QoS.AT_MOST_ONCE)
                .withHivemqId("hivemqId") // 16+38 = 54 bytes
                .withPayload("payload".getBytes()) // 7+12 = 19 bytes
                .withTopic("topic") // 10+38 = 48 bytes
                .build();

        assertEquals(67 + 54 + FIXED_SIZE + ObjectMemoryEstimation.stringSize(publishMqtt5.getUniqueId()), publishMqtt5.getEstimatedSizeInMemory());

    }

    @Test
    public void test_estimated_size_without_payload() throws InterruptedException {

        final PUBLISH publishMqtt5 = new PUBLISHFactory.Mqtt5Builder()
                .withQoS(QoS.AT_MOST_ONCE)
                .withHivemqId("hivemqId") // 16+38 = 54 bytes
                .withPublishId(1L)
                .withPersistence(Mockito.mock(PublishPayloadPersistence.class))
                .withTopic("topic") // 10+38 = 48 bytes
                .build();

        assertEquals(48 + 54 + FIXED_SIZE + ObjectMemoryEstimation.stringSize(publishMqtt5.getUniqueId()), publishMqtt5.getEstimatedSizeInMemory());

    }

    @Test
    public void test_estimated_size_very_large() throws InterruptedException {

        final PUBLISH publishMqtt5 = new PUBLISHFactory.Mqtt5Builder()
                .withQoS(QoS.AT_MOST_ONCE)
                .withHivemqId("hivemqId") // 16+38 = 54 bytes
                .withPayload(new byte[1024 * 1024 * 5]) // 5MB + 12 bytes
                .withCorrelationData(new byte[1024 * 1024 * 5])  // 5MB + 12 bytes
                .withResponseTopic(RandomStringUtils.randomAlphanumeric(65000)) // 130.038 bytes
                .withTopic(RandomStringUtils.randomAlphanumeric(65000)) // 130.038 bytes
                .withUserProperties(getManyProperties()) // 12.777.790 bytes
                .build();

        final long estimatedSize = ((1024 * 1024 * 5) * 2) + 54 + 24 + (130_038 * 2) + 12_777_790 + FIXED_SIZE + ObjectMemoryEstimation.stringSize(publishMqtt5.getUniqueId()); // 23_523_857 bytes + UniqueID Bytes
        assertEquals(estimatedSize, publishMqtt5.getEstimatedSizeInMemory());

    }

    private Mqtt5UserProperties getManyProperties() {
        final AtomicInteger counter = new AtomicInteger();
        final Set<MqttUserProperty> userProperties =
                Stream.generate(() -> MqttUserProperty.of("name" + counter.incrementAndGet(), "value"))
                        .limit(100000)
                        .collect(Collectors.toSet());
        return Mqtt5UserProperties.of(userProperties.toArray(new MqttUserProperty[]{}));

    }
}