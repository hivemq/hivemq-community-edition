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
package com.hivemq.extensions.services.publish;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.persistence.RetainedMessage;
import org.junit.Test;

import java.nio.ByteBuffer;

import static com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator.UTF_8;
import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class RetainedPublishImplTest {

    @Test
    public void test_convert_all_nullable_null() {

        final RetainedPublishImpl retainedPublish =
                new RetainedPublishImpl(Qos.AT_LEAST_ONCE, "topic", null, null, null, null, null, null,
                        UserPropertiesImpl.of(ImmutableList.of()));

        final RetainedMessage convert = RetainedPublishImpl.convert(retainedPublish);

        assertEquals(PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET, convert.getMessageExpiryInterval());
        assertNull(convert.getMessage());
        assertNull(convert.getContentType());
        assertNull(convert.getCorrelationData());
        assertNull(convert.getPayloadFormatIndicator());
        assertNull(convert.getResponseTopic());
        assertEquals(Mqtt5UserProperties.NO_USER_PROPERTIES, convert.getUserProperties());
        assertEquals(QoS.AT_LEAST_ONCE, convert.getQos());

    }

    @Test
    public void test_convert_all_set() {

        final UserPropertiesImpl userProperties = UserPropertiesImpl.of(ImmutableList.of(
                new MqttUserProperty("name", "value"),
                new MqttUserProperty("name", "value2"),
                new MqttUserProperty("name2", "val")));

        final RetainedPublishImpl retainedPublish = new RetainedPublishImpl(Qos.AT_MOST_ONCE, "topic",
                PayloadFormatIndicator.UTF_8, 12345L, "response_topic", ByteBuffer.wrap("correlation_data".getBytes()),
                "content_type", ByteBuffer.wrap("test3".getBytes()), userProperties);

        final RetainedMessage convert = RetainedPublishImpl.convert(retainedPublish);

        assertEquals(12345L, convert.getMessageExpiryInterval());
        assertArrayEquals("test3".getBytes(), convert.getMessage());
        assertArrayEquals("correlation_data".getBytes(), convert.getCorrelationData());
        assertEquals("content_type", convert.getContentType());
        assertEquals(UTF_8, convert.getPayloadFormatIndicator());
        assertEquals("response_topic", convert.getResponseTopic());
        assertEquals(3, convert.getUserProperties().asList().size());
        assertEquals(QoS.AT_MOST_ONCE, convert.getQos());

    }
}