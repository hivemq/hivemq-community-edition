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
package com.hivemq.codec.decoder.mqtt5;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.codec.encoder.mqtt5.UnsignedDataTypes;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;
import util.TestMqttDecoder;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 */
public class Mqtt5PublishDecoderTest extends AbstractMqtt5DecoderTest {

    @Before
    public void before() {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
    }

    @Test
    public void test_decode_allProperties() {

        InternalConfigurations.TOPIC_ALIAS_GLOBAL_MEMORY_HARD_LIMIT.set(1024 * 1024 * 200);
        channel.attr(ChannelAttributes.TOPIC_ALIAS_MAPPING).set(new String[3]);

        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0011,
                //   remaining length
                70,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   packet identifier
                0, 12,
                //   properties length
                50,
                //     payload format indicator
                0x01, 0,
                //     message expiry interval
                0x02, 0, 0, 0, 10,
                //     topic alias
                0x23, 0, 3,
                //     response topic
                0x08, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',
                //     correlation data
                0x09, 0, 5, 5, 4, 3, 2, 1,
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //     content type
                0x03, 0, 4, 't', 'e', 'x', 't',
                // payload
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        };

        final PUBLISH publishInternal = decodeInternal(encoded);

        assertNotNull(publishInternal);

        assertEquals(false, publishInternal.isDuplicateDelivery());
        assertEquals(12, publishInternal.getPacketIdentifier());


        assertEquals("topic", publishInternal.getTopic());
        assertEquals(QoS.AT_LEAST_ONCE, publishInternal.getQoS());
        assertEquals(true, publishInternal.isRetain());
        assertEquals(10, publishInternal.getMessageExpiryInterval());

        assertEquals(Mqtt5PayloadFormatIndicator.UNSPECIFIED, publishInternal.getPayloadFormatIndicator());

        assertEquals("text", publishInternal.getContentType());

        assertEquals("response", publishInternal.getResponseTopic());

        assertArrayEquals(new byte[]{5, 4, 3, 2, 1}, publishInternal.getCorrelationData());

        final ImmutableList<MqttUserProperty> userProperties = publishInternal.getUserProperties().asList();
        assertEquals(1, userProperties.size());
        assertEquals("test", userProperties.get(0).getName());
        assertEquals("value", userProperties.get(0).getValue());

        assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, publishInternal.getPayload());

    }

    @Test
    public void test_decode_simple() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0001,
                //   remaining length
                20,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                2,
                //     payload format indicator
                0x01, 0,
                // payload
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        };

        final PUBLISH publish = decodeInternal(encoded);

        assertEquals(false, publish.isDuplicateDelivery());

        assertNotNull(publish);

        assertEquals("topic", publish.getTopic());
        assertEquals(QoS.AT_MOST_ONCE, publish.getQoS());
        assertEquals(true, publish.isRetain());
        assertEquals(UnsignedDataTypes.UNSIGNED_INT_MAX_VALUE, publish.getMessageExpiryInterval());
        assertEquals(Mqtt5PayloadFormatIndicator.UNSPECIFIED, publish.getPayloadFormatIndicator());

        assertArrayEquals((new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}), publish.getPayload());
    }

    @Test
    public void test_decode_minimal() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                4,
                // variable header
                //   topic name
                0, 1, 't',
                // properties
                0
        };
        final PUBLISH publish = decode(encoded);
        assertEquals("t", publish.getTopic());
    }

    @Test
    public void test_decode_topic_alias_exceeds_limit() {


        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        InternalConfigurations.TOPIC_ALIAS_GLOBAL_MEMORY_HARD_LIMIT.set(47);

        channel = new EmbeddedChannel(TestMqttDecoder.create(fullConfig));
        channel.attr(ChannelAttributes.TOPIC_ALIAS_MAPPING).set(new String[3]);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                21,
                // variable header
                //   topic name
                0, 15, 't', 'o', 'p', 'i', 'c', 'n', 'a', 'm', 'e', 't', 'o', 'o', 'b', 'a', 'd',
                // properties
                3,
                // topic alias
                0x23, 0, 1,
        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_topic_alias_overide() {


        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        InternalConfigurations.TOPIC_ALIAS_GLOBAL_MEMORY_HARD_LIMIT.set(100);

        channel = new EmbeddedChannel(TestMqttDecoder.create(fullConfig));
        channel.attr(ChannelAttributes.TOPIC_ALIAS_MAPPING).set(new String[3]);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                11,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                // properties
                3,
                // topic alias
                0x23, 0, 1,
        };
        decode(encoded);


        final byte[] encoded2 = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                11,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'd',
                // properties
                3,
                // topic alias
                0x23, 0, 1,
        };

        decode(encoded2);


        final byte[] encoded3 = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                11,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'e',
                // properties
                3,
                // topic alias
                0x23, 0, 1,
        };

        final PUBLISH publish = decode(encoded3);

        assertNotNull(publish);
    }

    @Test
    public void test_decode_tooShort_fails_at_topic_length() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                0
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_fixedHeaderQos() {
        final byte[] encodedQos0 = {
                // fixed header
                //   type, flags
                0b0011_0001,
                //   remaining length
                12,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   packet identifier
                0, 12,
                //   properties
                2,
                //     payload format indicator
                0x01, 0
        };
        final PUBLISH decodeQos0 = decode(encodedQos0);
        assertEquals(QoS.AT_MOST_ONCE, decodeQos0.getQoS());

        final byte[] encodedQos1 = {
                // fixed header
                //   type, flags
                0b0011_0011,
                //   remaining length
                12,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   packet identifier
                0, 12,
                //   properties
                2,
                //     payload format indicator
                0x01, 0
        };
        final PUBLISH decodeQos1 = decode(encodedQos1);
        assertEquals(QoS.AT_LEAST_ONCE, decodeQos1.getQoS());

        final byte[] encodedQos2 = {
                // fixed header
                //   type, flags
                0b0011_1101,
                //   remaining length
                12,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   packet identifier
                0, 12,
                //   properties
                2,
                //     payload format indicator
                0x01, 0
        };
        final PUBLISH decodeQos2 = decode(encodedQos2);
        assertEquals(QoS.EXACTLY_ONCE, decodeQos2.getQoS());
    }

    @Test
    public void test_decode_fixedHeaderQosInvalid_returnsNull() {
        final byte[] encodedQosInvalid = {
                // fixed header
                //   type, flags
                0b0011_0111,
                //   remaining length
                12,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   packet identifier
                0, 12,
                //   properties
                2,
                //     payload format indicator
                0x01, 0
        };
        decodeNullExpected(encodedQosInvalid);
    }

    @Test
    public void test_decode_dupTrueForQos0_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_1000,
                //   remaining length
                8,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                0
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_retain_not_supported() {

        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfig.mqttConfiguration().setRetainedMessagesEnabled(false);

        channel = new EmbeddedChannel(TestMqttDecoder.create(fullConfig));

        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0001,
                //   remaining length
                8,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                0
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_topicNameInvalidStringLength_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                3,
                // variable header
                //   topic name
                0, 5, 't'
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_messageExpiryInterval() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0001,
                //   remaining length
                16,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                7,
                //     payload format indicator
                0x01, 1,
                //     message expiry interval
                0x02, 0, 0, 0, 10,
                // payload
                0x00
        };
        final PUBLISH publish = decode(encoded);
        assertEquals(10, publish.getMessageExpiryInterval());
    }

    @Test
    public void test_decode_messageExpiryInterval_higher_than_config() {

        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfig.mqttConfiguration().setMaxMessageExpiryInterval(100);

        channel = new EmbeddedChannel(TestMqttDecoder.create(fullConfig));
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0001,
                //   remaining length
                16,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                7,
                //     payload format indicator
                0x01, 1,
                //     message expiry interval
                0x02, 0, 0, 0, 105,
                // payload
                0x00
        };
        final PUBLISH publish = decode(encoded);
        assertEquals(100, publish.getMessageExpiryInterval());
    }

    @Test
    public void test_decode_messageExpiryInterval_tooShort() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0001,
                //   remaining length
                13,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                4,
                //     payload format indicator
                0x01, 1,
                //     message expiry interval
                0x02, 0,
                // payload
                0x00
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_messageExpiryIntervalDuplicate_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0001,
                //   remaining length
                21,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                12,
                //     payload format indicator
                0x01, 1,
                //     message expiry interval
                0x02, 0, 0, 0, 10,
                //     message expiry interval
                0x02, 0, 0, 0, 10,
                // payload
                0x00
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_payloadFormatIndicatorUtf8() {

        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0001,
                //   remaining length
                17,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                2,
                //     payload format indicator
                0x01, 1,
                // payload
                (byte) 0xE4, (byte) 0xBD, (byte) 0xA0, 0x20, (byte) 0xE5, (byte) 0xA5, (byte) 0xBD
        };

        final PUBLISH publish = decode(encoded);

        assertEquals(Mqtt5PayloadFormatIndicator.UTF_8, publish.getPayloadFormatIndicator());

        assertEquals("你 好", new String(publish.getPayload(), StandardCharsets.UTF_8));
    }

    @Test
    public void test_decode_invalidPayloadFormatIndicator_returnsNull() {

        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0001,
                //   remaining length
                10,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                2,
                //     payload format indicator
                0x01, 3
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_invalidPayloadFormatIndicator_tooShort() {

        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0001,
                //   remaining length
                9,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                1,
                //     payload format indicator
                0x01,
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_payloadFormatIndicatorMoreThanOnce_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0001,
                //   remaining length
                13,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                4,
                //     payload format indicator
                0x01, 0,
                //     payload format indicator
                0x01, 0,
                // payload
                0x00
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_PayloadUtf8NotWellFormed_returnsNull() {

        final FullConfigurationService fullConfigurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfigurationService.securityConfiguration().setPayloadFormatValidation(true);

        channel = new EmbeddedChannel(TestMqttDecoder.create(fullConfigurationService));
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0001,
                //   remaining length
                11,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                2,
                //     payload format indicator
                0x01, 1,
                // payload
                (byte) 0xFF
        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_no_payload() {

        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0001,
                //   remaining length
                10,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                2,
                //     payload format indicator
                0x01, 1
        };

        final PUBLISH publish = decode(encoded);
        assertEquals(0, publish.getPayload().length);
    }

    @Test
    public void test_decode_contentType() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                15,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                7,
                //     content type
                0x03, 0, 4, 't', 'e', 'x', 't',

        };
        final PUBLISH publish = decode(encoded);
        assertEquals("text", publish.getContentType());
    }

    @Test
    public void test_decode_contentType_to_often_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                22,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                14,
                //     content type
                0x03, 0, 4, 't', 'e', 'x', 't',
                0x03, 0, 4, 't', 'e', 'x', 't'

        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_invalidContentType_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                15,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                7,
                //     content type
                0x03, 0, 4, 't', 'e', 'x', (byte) 0xFF,
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_content_type_to_short_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                15,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                7,
                //     content type
                0x03, 0, 5, 't', 'e', 'x', 't',
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_malformedContentType_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                11,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                3,
                //     content type
                0x03, 0, 4
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_userProperty() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                22,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                14,
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e'

        };
        final PUBLISH publish = decode(encoded);
        final ImmutableList<MqttUserProperty> userProperties = publish.getUserProperties().asList();
        assertEquals(1, userProperties.size());
        assertEquals("test", userProperties.get(0).getName());
        assertEquals("value", userProperties.get(0).getValue());
    }

    @Test
    public void test_decode_invalidUserProperty_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                11,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                3,
                //     user properties
                0x26, 0, 4

        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_subscriptionIdentifier() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                23,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                15,
                //     response topic
                0x08, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',
                //     subscription identifier
                0x0B, 123,
                //     payload format indicator
                0x01, 0
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_packetIdentifierWithQos0isNotSet() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                21,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                13,
                //     response topic
                0x08, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',
                //     payload format indicator
                0x01, 0
        };
        final PUBLISH publishInternal = decodeInternal(encoded);
        assertEquals(0, publishInternal.getPacketIdentifier());
    }

    @Test
    public void decode_packetIdentifierWithQos1() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0010,
                //   remaining length
                23,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   packet identifier
                0, 12,
                //   properties
                13,
                //     response topic
                0x08, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',
                //     payload format indicator
                0x01, 0
        };
        final PUBLISH publishInternal = decodeInternal(encoded);
        assertEquals(12, publishInternal.getPacketIdentifier());
    }

    @Test
    public void test_decode_packetIdentifierWithQos2() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0100,
                //   remaining length
                23,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   packet identifier
                0, 12,
                //   properties
                13,
                //     response topic
                0x08, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',
                //     payload format indicator
                0x01, 0
        };
        final PUBLISH publishInternal = decodeInternal(encoded);
        assertEquals(12, publishInternal.getPacketIdentifier());
    }

    @Test
    public void test_decode_packetIdentifier_Zero_WithQos2() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0100,
                //   remaining length
                23,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   packet identifier
                0, 0,
                //   properties
                13,
                //     response topic
                0x08, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',
                //     payload format indicator
                0x01, 0
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_packetIdentifierMissingWithQos2() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0100,
                //   remaining length
                8,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                0
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_correlationData() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                29,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                21,
                //     response topic
                0x08, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',
                //     correlation data
                0x09, 0, 5, 5, 4, 3, 2, 1,
                //     payload format indicator
                0x01, 0
        };
        final PUBLISH publish = decode(encoded);
        assertArrayEquals(new byte[]{5, 4, 3, 2, 1}, publish.getCorrelationData());
    }

    @Test
    public void test_decode_correlationDataMalformed_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                26,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                18,
                //     response topic
                0x08, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',
                //     correlation data
                0x09, 0, 5, 5, 4, 3, 2
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_correlationDataMoreThanOnce_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                37,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                29,
                //     response topic
                0x08, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',
                //     correlation data
                0x09, 0, 5, 5, 4, 3, 2, 1,
                //     correlation data
                0x09, 0, 1, 2, 3, 4, 5, 6,
                //     payload format indicator
                0x01, 0
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_responseTopic() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                21,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                13,
                //     response topic
                0x08, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',
                //     payload format indicator
                0x01, 0
        };
        final PUBLISH publish = decode(encoded);
        assertEquals("response", publish.getResponseTopic());
    }

    @Test
    public void test_decode_responseTopicMoreThanOnce_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                32,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                24,
                //     response topic
                0x08, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e', //
                0x08, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',
                //     payload format indicator
                0x01, 0
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_responseTopicWithWildcards_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                21,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                13,
                //     response topic
                0x08, 0, 8, 'r', 't', 'o', 'p', 'i', 'c', '/', 'a',
                //     payload format indicator
                0x01, 0
        };
        PUBLISH publish = decode(encoded);

        assertEquals("rtopic/a", publish.getResponseTopic());

        encoded[20] = '#';
        decodeNullExpected(encoded);

        encoded[20] = 'b';
        publish = decode(encoded);
        assertEquals("rtopic/b", publish.getResponseTopic());

        encoded[20] = '+';
        decodeNullExpected(encoded);

        encoded[20] = 'c';
        publish = decode(encoded);
        assertEquals("rtopic/c", publish.getResponseTopic());
    }

    @Test
    public void test_decode_responseTopicMalFormed_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                21,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                13,
                //     response topic
                0x08, 0, 8, 'r', 't', 'o', 'p', 'i', 'c', '/', (byte) 0xFF,
                //     payload format indicator
                0x01, 0
        };
        decodeNullExpected(encoded);

    }

    @Test
    public void test_decode_topicAliasDuplicate_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                16,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                8,
                //     topic alias
                0x23, 0, 3,
                //     payload format indicator
                0x01, 0,
                //     topic alias duplicate (error!)
                0x23, 0, 3
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_topicAlias_too_short() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                10,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                2,
                //     topic alias
                0x23, 3,
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void decode_noTopicAliasFound_returnsNull() {

        InternalConfigurations.TOPIC_ALIAS_GLOBAL_MEMORY_HARD_LIMIT.set(1024 * 1024 * 200);
        channel = new EmbeddedChannel(TestMqttDecoder.create());
        channel.attr(ChannelAttributes.TOPIC_ALIAS_MAPPING).set(new String[3]);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final byte[] encodedWithTopicName = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                13,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                5,
                //     topic alias
                0x23, 0, 3,
                //     payload format indicator
                0x01, 0
        };
        decodeInternal(encodedWithTopicName);

        final byte[] encodedWithWrongTopicAlias = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                8,
                // variable header
                //   topic name is empty
                0, 0,
                //   properties
                5,
                //     topic alias
                0x23, 0, 1,
                //     payload format indicator
                0x01, 0
        };
        decodeNullExpected(encodedWithWrongTopicAlias);
    }

    @Test
    public void test_decode_to_may_topic_aliases_returnsNull() {

        channel.attr(ChannelAttributes.TOPIC_ALIAS_MAPPING).set(new String[3]);

        final byte[] encodedWithWrongTopicAlias = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                8,
                // variable header
                //   topic name is empty
                0, 0,
                //   properties
                5,
                //     topic alias
                0x23, 0, 4,
                //     payload format indicator
                0x01, 0
        };
        decodeNullExpected(encodedWithWrongTopicAlias);
    }

    @Test
    public void test_decode_topic_alias_remaining_length_to_short_returnsNull() {
        final byte[] encodedWithWrongTopicAlias = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                5,
                // variable header
                //   topic name is empty
                0, 0,
                //   properties
                2,
                //     topic alias
                0x23, 0,
        };
        decodeNullExpected(encodedWithWrongTopicAlias);
    }

    @Test
    public void test_decode_topicAliasZero_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                13,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                5,
                //     topic alias
                0x23, 0, 0,
                //     payload format indicator
                0x01, 0
        };
        decodeNullExpected(encoded);
    }


    @Test
    public void test_decode_topicAliasTooLarge_returnsNull() {

        channel.attr(ChannelAttributes.TOPIC_ALIAS_MAPPING).set(new String[3]);

        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                13,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                5,
                //     topic alias too large
                0x23, 0, 4,
                //     payload format indicator
                0x01, 0
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_topicWithWildcard_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                12,
                // variable header
                //   topic name
                0, 7, 't', 'o', 'p', 'i', 'c', '/', 'a',
                //   properties
                2,
                //     payload format indicator
                0x01, 0
        };
        PUBLISH decode = decode(encoded);
        assertEquals("topic/a", decode.getTopic());

        encoded[10] = '#';
        decodeNullExpected(encoded);

        encoded[10] = 'b';
        decode = decode(encoded);
        assertEquals("topic/b", decode.getTopic());

        encoded[10] = '+';
        decodeNullExpected(encoded);

        encoded[10] = 'c';
        decode = decode(encoded);
        assertEquals("topic/c", decode.getTopic());
    }

    @Test
    public void test_decode_topic_malformed_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                12,
                // variable header
                //   topic name
                0, 7, 't', 'o', 'p', 'i', 'c', '/', 'a',
                //   properties
                2,
                //     payload format indicator
                0x01, 0
        };
        final PUBLISH decode = decode(encoded);
        assertEquals("topic/a", decode.getTopic());

        encoded[10] = (byte) 0xFF;
        decodeNullExpected(encoded);

    }

    @Test
    public void test_decode_negativePropertyIdentifier_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                13,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                5,
                //     invalid negative identifier
                (byte) 0xFF, 0, 3,
                //     payload format indicator
                0x01, 0
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_invalidPropertyIdentifier_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                13,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                5,
                //     invalid identifier
                (byte) 0x05, 0, 3,
                //     payload format indicator
                0x01, 0
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_propertiesLengthNegative_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                8,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties length negative
                -1
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_propertyLengthTooLong_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                10,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                3,
                //     payload format indicator
                0x01, 0
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_propertyLengthTooShort_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                10,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                1,
                //     payload format indicator
                0x01, 0
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_remainingLengthMissing_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                0
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_topicNullNoAlias_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                5,
                // variable header
                //   topic name null, no alias
                0, 0,
                //   properties length
                2,
                //     payload format indicator
                0x01, 0
        };
        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_invalidMessageType_returnsNull() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                5,
                // variable header
                //   topic name null, no alias
                0, 0,
                //   properties length
                2,
                //     payload format indicator
                0x01, 0
        };
        decodeNullExpected(encoded);
    }

    private @NotNull PUBLISH decode(final @NotNull byte[] encoded) {
        return decodeInternal(encoded);
    }

    private @NotNull PUBLISH decodeInternal(final @NotNull byte[] encoded) {
        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeBytes(encoded);
        channel.writeInbound(byteBuf);

        final PUBLISH publishInternal = channel.readInbound();
        assertNotNull(publishInternal);

        return publishInternal;
    }
}