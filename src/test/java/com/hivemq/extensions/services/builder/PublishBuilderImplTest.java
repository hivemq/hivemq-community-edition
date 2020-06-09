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
package com.hivemq.extensions.services.builder;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.publish.Publish;
import com.hivemq.mqtt.message.QoS;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import java.nio.ByteBuffer;
import java.util.Optional;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Lukas Brandl
 */
@SuppressWarnings("NullabilityAnnotations")
public class PublishBuilderImplTest {

    private FullConfigurationService configurationService;

    @Before
    public void before() {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_qos_validation() {
        configurationService.mqttConfiguration().setMaximumQos(QoS.AT_LEAST_ONCE);
        new PublishBuilderImpl(configurationService).qos(Qos.EXACTLY_ONCE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_retained_validation() {
        configurationService.mqttConfiguration().setRetainedMessagesEnabled(false);
        new PublishBuilderImpl(configurationService).retain(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_message_expiry_validation() {
        configurationService.mqttConfiguration().setMaxMessageExpiryInterval(10);
        new PublishBuilderImpl(configurationService).messageExpiryInterval(11);
    }

    @Test
    public void test_custom_max_message_expiry_value_validation() {
        configurationService.mqttConfiguration().setMaxMessageExpiryInterval(10);
        new PublishBuilderImpl(configurationService).messageExpiryInterval(10);
    }

    @Test
    public void test_max_message_expiry_value_validation() {
        new PublishBuilderImpl(configurationService).messageExpiryInterval(4_294_967_296L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_message_expiry_less_than_zero() {
        new PublishBuilderImpl(configurationService).messageExpiryInterval(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_topic_validation() {
        new PublishBuilderImpl(configurationService).topic("#");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_topic_validation_utf_8_should_not() {
        new PublishBuilderImpl(configurationService).topic("topic" + '\u0001');
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_topic_validation_utf_8_must_not() {
        new PublishBuilderImpl(configurationService).topic("topic" + '\uD800');
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_response_topic_validation_utf_8_should_not() {
        new PublishBuilderImpl(configurationService).responseTopic("topic" + '\u0001');
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_response_topic_validation_utf_8_must_not() {
        new PublishBuilderImpl(configurationService).responseTopic("topic" + '\uD800');
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_content_type_validation_utf_8_should_not() {
        new PublishBuilderImpl(configurationService).contentType("topic" + '\u0001');
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_content_type_validation_utf_8_must_not() {
        new PublishBuilderImpl(configurationService).contentType("topic" + '\uD800');
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_user_property_name_validation_utf_8_should_not() {
        new PublishBuilderImpl(configurationService).userProperty("topic" + '\u0001', "val");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_user_property_name_validation_utf_8_must_not() {
        new PublishBuilderImpl(configurationService).userProperty("topic" + '\uD800', "val");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_user_property_value_validation_utf_8_should_not() {
        new PublishBuilderImpl(configurationService).userProperty("key", "val" + '\u0001');
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_user_property_value_validation_utf_8_must_not() {
        new PublishBuilderImpl(configurationService).userProperty("key", "val" + '\uD800');
    }


    @Test(expected = NullPointerException.class)
    public void test_null_qos() {
        new PublishBuilderImpl(configurationService).qos(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_null_topic() {
        new PublishBuilderImpl(configurationService).topic(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_null_user_property_key() {
        new PublishBuilderImpl(configurationService).userProperty(null, "value");
    }

    @Test(expected = NullPointerException.class)
    public void test_null_user_property_value() {
        new PublishBuilderImpl(configurationService).userProperty("key", null);
    }

    @Test(expected = NullPointerException.class)
    public void test_topic_not_set() {
        new PublishBuilderImpl(configurationService).payload(ByteBuffer.wrap(new byte[]{1, 2, 3})).build();
    }

    @Test(expected = NullPointerException.class)
    public void test_payload_not_set() {
        new PublishBuilderImpl(configurationService).topic("topic").build();
    }

    @Test(expected = DoNotImplementException.class)
    public void test_from_invalid_publish_implementation() {
        new PublishBuilderImpl(configurationService).fromPublish(new TestPublish()).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_user_property_name_too_long() {
        new PublishBuilderImpl(configurationService).userProperty(RandomStringUtils.randomAlphanumeric(65536), "val");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_user_property_value_too_long() {
        new PublishBuilderImpl(configurationService).userProperty("name", RandomStringUtils.randomAlphanumeric(65536));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_response_topic_too_long() {
        new PublishBuilderImpl(configurationService).responseTopic(RandomStringUtils.randomAlphanumeric(65536));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_content_type_too_long() {
        new PublishBuilderImpl(configurationService).contentType(RandomStringUtils.randomAlphanumeric(65536));
    }


    @Test
    public void test_all_values_set() {
        final Publish publish = new PublishBuilderImpl(configurationService)
                .topic("topic")
                .payload(ByteBuffer.wrap(new byte[]{1, 2, 3}))
                .qos(Qos.EXACTLY_ONCE)
                .retain(true)
                .contentType("TYPE")
                .correlationData(ByteBuffer.wrap(new byte[]{1, 2, 3, 4}))
                .responseTopic("responseTopic")
                .messageExpiryInterval(10)
                .payloadFormatIndicator(PayloadFormatIndicator.UTF_8)
                .userProperty("key", "value").build();

        assertEquals("topic", publish.getTopic());
        assertArrayEquals(new byte[]{1, 2, 3}, publish.getPayload().get().array());
        assertEquals(2, publish.getQos().getQosNumber());
        assertEquals(true, publish.getRetain());
        assertEquals("TYPE", publish.getContentType().get());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, publish.getCorrelationData().get().array());
        assertEquals("responseTopic", publish.getResponseTopic().get());
        assertEquals(10L, publish.getMessageExpiryInterval().get().longValue());
        assertEquals(PayloadFormatIndicator.UTF_8, publish.getPayloadFormatIndicator().get());
        assertEquals("value", publish.getUserProperties().getFirst("key").get());
    }

    @Test
    public void test_from_publish() {
        final Publish original = new PublishBuilderImpl(configurationService)
                .topic("topic")
                .payload(ByteBuffer.wrap(new byte[]{1, 2, 3}))
                .qos(Qos.EXACTLY_ONCE)
                .retain(true)
                .contentType("TYPE")
                .correlationData(ByteBuffer.wrap(new byte[]{1, 2, 3, 4}))
                .responseTopic("responseTopic")
                .messageExpiryInterval(10)
                .payloadFormatIndicator(PayloadFormatIndicator.UTF_8)
                .userProperty("key", "value").build();

        assertEquals("topic", original.getTopic());
        assertArrayEquals(new byte[]{1, 2, 3}, original.getPayload().get().array());
        assertEquals(2, original.getQos().getQosNumber());
        assertEquals(true, original.getRetain());
        assertEquals("TYPE", original.getContentType().get());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, original.getCorrelationData().get().array());
        assertEquals("responseTopic", original.getResponseTopic().get());
        assertEquals(10L, original.getMessageExpiryInterval().get().longValue());
        assertEquals(PayloadFormatIndicator.UTF_8, original.getPayloadFormatIndicator().get());
        assertEquals("value", original.getUserProperties().getFirst("key").get());

        final Publish copy = new PublishBuilderImpl(configurationService).fromPublish(original).build();
        assertEquals("topic", copy.getTopic());
        assertArrayEquals(new byte[]{1, 2, 3}, copy.getPayload().get().array());
        assertEquals(2, copy.getQos().getQosNumber());
        assertEquals(true, copy.getRetain());
        assertEquals("TYPE", copy.getContentType().get());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, copy.getCorrelationData().get().array());
        assertEquals("responseTopic", copy.getResponseTopic().get());
        assertEquals(10L, copy.getMessageExpiryInterval().get().longValue());
        assertEquals(PayloadFormatIndicator.UTF_8, copy.getPayloadFormatIndicator().get());
        assertEquals("value", copy.getUserProperties().getFirst("key").get());

    }

    private class TestPublish implements Publish {

        @Override
        public Qos getQos() {
            return null;
        }

        @Override
        public boolean getRetain() {
            return false;
        }

        @Override
        public String getTopic() {
            return null;
        }

        @Override
        public Optional<PayloadFormatIndicator> getPayloadFormatIndicator() {
            return Optional.empty();
        }

        @Override
        public Optional<Long> getMessageExpiryInterval() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getResponseTopic() {
            return Optional.empty();
        }

        @Override
        public Optional<ByteBuffer> getCorrelationData() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getContentType() {
            return Optional.empty();
        }

        @Override
        public Optional<ByteBuffer> getPayload() {
            return Optional.empty();
        }

        @Override
        public UserProperties getUserProperties() {
            return null;
        }
    }
}