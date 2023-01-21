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
package com.hivemq.codec.encoder.mqtt5;

import com.google.common.collect.ImmutableList;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import util.TestMessageUtil;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.TreeSet;

import static com.hivemq.codec.encoder.mqtt5.MqttVariableByteInteger.MAXIMUM_PACKET_SIZE_LIMIT;
import static com.hivemq.mqtt.message.mqtt5.MessageProperties.CORRELATION_DATA;
import static com.hivemq.mqtt.message.mqtt5.MessageProperties.PAYLOAD_FORMAT_INDICATOR;
import static com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties.NO_USER_PROPERTIES;
import static org.junit.Assert.assertEquals;

/**
 * @author Florian Limpöck
 */
public class Mqtt5PublishEncoderTest extends AbstractMqtt5EncoderTest {

    private HivemqId hiveMQId;

    @Before
    public void setUp() throws Exception {
        hiveMQId = new HivemqId();
        super.setUp();
        testMessageEncoder.getSecurityConfigurationService().setAllowRequestProblemInformation(true);
    }

    @Test
    public void test_encode_all() {

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setMaxPacketSizeSend(200L);

        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length (157)
                (byte) (134 + 23), 1,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties (143)
                (byte) (134 + 9), 1,
                //     message expiry interval
                0x02, 0, 0, 0, 10,
                //     payload format indicator
                0x01, 0,
                //     content type
                0x03, 0, 13, 'm', 'y', 'C', 'o', 'n', 't', 'e', 'n', 't', 'T', 'y', 'p', 'e',
                //     response topic
                0x08, 0, 13, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e', 'T', 'o', 'p', 'i', 'c',
                //     correlation data
                0x09, 0, 5, 1, 2, 3, 4, 5,
                //     user properties
                0x26, 0, 5, 't', 'e', 's', 't', '1', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '1', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '1', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '1', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '1', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '1', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                //     subscription identifier
                0x0B, 10,
                0x0B, 20,
                0x0B, 30,
                // payload
                1, 2, 3, 4, 5
        };

        final MqttUserProperty userProperty = new MqttUserProperty("test1", "value");
        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(userProperty, userProperty, userProperty, userProperty, userProperty, userProperty);

        final TreeSet<Integer> identifiers = new TreeSet<>();
        identifiers.add(10);
        identifiers.add(20);
        identifiers.add(30);
        final PUBLISH publish = TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[]{1, 2, 3, 4, 5},
                QoS.AT_MOST_ONCE, QoS.AT_MOST_ONCE, false, 10, Mqtt5PayloadFormatIndicator.UNSPECIFIED,
                "myContentType", "responseTopic", new byte[]{1, 2, 3, 4, 5},
                userProperties,
                -1, false, true, ImmutableList.copyOf(identifiers));

        encodeTestBufferSize(expected, publish);
    }

    @Test
    public void test_encode_simple() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                15,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                2,
                //     payload format indicator
                0x01, 0,
                // payload
                1, 2, 3, 4, 5
        };

        final PUBLISH publish =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[]{1, 2, 3, 4, 5},
                        QoS.AT_MOST_ONCE, QoS.AT_MOST_ONCE, false, MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT,
                        Mqtt5PayloadFormatIndicator.UNSPECIFIED, null, null, null, NO_USER_PROPERTIES,
                        -1, false, true, null);

        encodeTestBufferSize(expected, publish);
    }

    @Test
    public void test_encode_minimum() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                5,
                // variable header
                //   topic name
                0, 1, 't',
                //   properties
                0,
                // payload
                1
        };

        final PUBLISH publish =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "t", new byte[]{1},
                        QoS.AT_MOST_ONCE, QoS.AT_MOST_ONCE, false, MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT,
                        null, null, null, null, NO_USER_PROPERTIES,
                        -1, false, true, null);

        encodeTestBufferSize(expected, publish);
    }

    @Test
    public void test_encode_retainTrue() {
        final byte[] expected = {
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
                0x01, 0
        };

        final PUBLISH publish =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[0], QoS.AT_MOST_ONCE, QoS.AT_MOST_ONCE, true,
                        MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT, Mqtt5PayloadFormatIndicator.UNSPECIFIED, null,
                        null, null, NO_USER_PROPERTIES,
                        -1, false, true, null);
        encodeTestBufferSize(expected, publish);
    }

    @Test
    public void test_encode_retainFalse() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0011_1010,
                //   remaining length
                12,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                // Packet Identifier
                0, 15,
                //   properties
                2,
                //     payload format indicator
                0x01, 0
        };

        final PUBLISH publish =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[0], QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, false,
                        MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT, Mqtt5PayloadFormatIndicator.UNSPECIFIED, null,
                        null, null, NO_USER_PROPERTIES,
                        15, true, true, null);
        encodeTestBufferSize(expected, publish);
    }

    @Test
    public void test_encode_isDupFalse() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0011_0010,
                //   remaining length
                12,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                // Packet Identifier
                0, 15,
                //   properties
                2,
                //     payload format indicator
                0x01, 0
        };

        final PUBLISH publish =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[0], QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, false,
                        MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT, Mqtt5PayloadFormatIndicator.UNSPECIFIED, null,
                        null, null, NO_USER_PROPERTIES,
                        15, false, true, null);
        encodeTestBufferSize(expected, publish);
    }

    @Test
    public void test_encode_isDupTrue() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0011_1010,
                //   remaining length
                12,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                // Packet Identifier
                0, 17,
                //   properties
                2,
                //     payload format indicator
                0x01, 0
        };

        final PUBLISH publish =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[0], QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, false,
                        MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT, Mqtt5PayloadFormatIndicator.UNSPECIFIED, null,
                        null, null, NO_USER_PROPERTIES,
                        17, true, true, null);
        encodeTestBufferSize(expected, publish);
    }

    @Test
    public void test_encode_formatIndicatorUtf8() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0011_0010,
                //   remaining length
                12,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                // Packet Identifier
                0, 15,
                //   properties
                2,
                //     payload format indicator
                0x01, 1
        };

        final PUBLISH publish =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[0], QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, false,
                        MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT, Mqtt5PayloadFormatIndicator.UTF_8, null, null,
                        null, NO_USER_PROPERTIES,
                        15, false, true, null);
        encodeTestBufferSize(expected, publish);
    }

    @Test
    public void test_encode_expiryInterval() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0011_0010,
                //   remaining length
                17,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   Packet Identifier
                0, 15,
                //   properties
                7,
                //     message expiry interval
                0x02, 0, 0, 0x3, (byte) 0xE8,
                //     payload format indicator
                0x01, 1
        };

        final PUBLISH publish =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[0], QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, false, 1000,
                        Mqtt5PayloadFormatIndicator.UTF_8, null, null, null, NO_USER_PROPERTIES,
                        15, false, true, null);


        encodeTestBufferSize(expected, publish);
    }

    @Test
    public void test_encode_contentType() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0011_0010,
                //   remaining length
                28,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   Packet Identifier
                0, 15,
                //   properties
                18,
                //     payload format indicator
                0x01, 1,
                //     content type
                0x03, 0, 13, 'm', 'y', 'C', 'o', 'n', 't', 'e', 'n', 't', 'T', 'y', 'p', 'e'
        };

        final PUBLISH publish =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[0], QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, false,
                        MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT, Mqtt5PayloadFormatIndicator.UTF_8,
                        "myContentType", null, null, NO_USER_PROPERTIES,
                        15, false, true, null);
        encodeTestBufferSize(expected, publish);
    }

    @Test
    public void test_encode_responseTopic() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0011_0010,
                //   remaining length
                28,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   Packet Identifier
                0, 15,
                //   properties
                18,
                //     payload format indicator
                0x01, 1,
                //     response topic
                0x08, 0, 13, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e', 'T', 'o', 'p', 'i', 'c'
        };

        final PUBLISH publish =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[0], QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, false,
                        MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT, Mqtt5PayloadFormatIndicator.UTF_8, null,
                        "responseTopic", null, NO_USER_PROPERTIES,
                        15, false, true, null);
        encodeTestBufferSize(expected, publish);
    }


    @Test
    public void test_encode_correlationData() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0011_0010,
                //   remaining length
                20,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   Packet Identifier
                0, 15,
                //   properties
                10,
                //     payload format indicator
                0x01, 1,
                //     correlation data
                0x09, 0, 5, 1, 2, 3, 4, 5
        };

        final byte[] correlationData = {1, 2, 3, 4, 5};
        final PUBLISH publish =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[0], QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, false,
                        MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT, Mqtt5PayloadFormatIndicator.UTF_8, null, null,
                        correlationData, NO_USER_PROPERTIES,
                        15, false, true, null);
        encodeTestBufferSize(expected, publish);
    }

    @Test
    public void test_encode_withoutTopicAlias() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0011_0010,
                //   remaining length
                10,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   Packet Identifier
                0, 15,
                //   properties
                0
        };

        final PUBLISH publish =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[0], QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, false,
                        MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT, null, null, null, null,
                        NO_USER_PROPERTIES,
                        15, false, true, null);
        encodeTestBufferSize(expected, publish);
    }

    @Test
    public void test_encode_withoutTopicAliasUsingDefault() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0011_1010,
                //   remaining length
                10,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   Packet Identifier
                0, 2,
                //   properties
                0
        };

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[0], QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, false,
                MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT, null, null, null, null,
                NO_USER_PROPERTIES,
                2, true, true, null);
        encodeTestBufferSize(expected, publish);
    }

    @Test
    public void test_encode_userProperties() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0011_0010,
                //   remaining length
                46,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   Packet Identifier
                0, 15,
                //   properties
                36,
                //     payload format indicator
                0x01, 1,
                //     user properties
                0x26, 0, 4, 'u', 's', 'e', 'r', 0, 8, 'p', 'r', 'o', 'p', 'e', 'r', 't', 'y',
                0x26, 0, 4, 'u', 's', 'e', 'r', 0, 8, 'p', 'r', 'o', 'p', 'e', 'r', 't', 'y'
        };

        final MqttUserProperty property = new MqttUserProperty("user", "property");
        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(property, property);

        final PUBLISH publish =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[0], QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, false,
                        MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT, Mqtt5PayloadFormatIndicator.UTF_8, null, null,
                        null, userProperties,
                        15, false, true, null);
        encodeTestBufferSize(expected, publish);
    }

    @Test
    public void test_encode_userProperties_request_problem_information_false() {

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setRequestProblemInformation(false);

        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0011_0010,
                //   remaining length
                46,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   Packet Identifier
                0, 15,
                //   properties
                36,
                //     payload format indicator
                0x01, 1,
                //     user properties
                0x26, 0, 4, 'u', 's', 'e', 'r', 0, 8, 'p', 'r', 'o', 'p', 'e', 'r', 't', 'y',
                0x26, 0, 4, 'u', 's', 'e', 'r', 0, 8, 'p', 'r', 'o', 'p', 'e', 'r', 't', 'y'
        };

        final MqttUserProperty property = new MqttUserProperty("user", "property");
        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(property, property);

        final PUBLISH publish =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[0], QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, false,
                        MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT, Mqtt5PayloadFormatIndicator.UTF_8, null, null,
                        null, userProperties,
                        15, false, true, null);

        // Do not omit reason string because it is a PUBLISH packet!
        encodeTestBufferSize(expected, publish);
    }

    @Test
    public void test_encode_singleSubscriptionIdentifier() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0011_0010,
                //   remaining length
                12,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   Packet Identifier
                0, 15,
                //   properties
                2,
                // subscription identifier
                0x0b, 3
        };

        final PUBLISH publish =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[0], QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, false,
                        MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT, null, null, null, null,
                        NO_USER_PROPERTIES,
                        15, false, true, ImmutableList.copyOf(Collections.singleton(3)));
        encodeTestBufferSize(expected, publish);
    }

    @Test
    public void test_encode_multipleSubscriptionIdentifiers() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0011_0010,
                //   remaining length
                14,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   Packet Identifier
                0, 15,
                //   properties
                4,
                // subscription identifier
                0x0b, 3,
                // subscription identifier
                0x0b, 4
        };

        final TreeSet<Integer> identifiers = new TreeSet<>();
        identifiers.add(3);
        identifiers.add(4);


        final PUBLISH publish =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[0], QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, false,
                        MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT, null, null, null, null,
                        NO_USER_PROPERTIES,
                        15, false, true, ImmutableList.copyOf(identifiers));
        encodeTestBufferSize(expected, publish);
    }

    @Test
    public void test_encode_qos() {
        final byte[] expectedQos0 = {
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
                //     payload format indicator
                0x01, 0
        };

        final byte[] expectedQos1 = {
                // fixed header
                //   type, flags
                0b0011_0010,
                //   remaining length
                12,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                // Packet Identifier
                0, 7,
                //   properties
                2,
                //     payload format indicator
                0x01, 0
        };

        final byte[] expectedQos2 = {
                // fixed header
                //   type, flags
                0b0011_0100,
                //   remaining length
                12,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                // Packet Identifier
                0, 7,
                //   properties
                2,
                //     payload format indicator
                0x01, 0
        };
        final PUBLISH publishQos0 =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[0], QoS.AT_MOST_ONCE, QoS.AT_MOST_ONCE, false,
                        MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT, Mqtt5PayloadFormatIndicator.UNSPECIFIED, null,
                        null, null, NO_USER_PROPERTIES,
                        7, false, true, null);
        encodeTestBufferSize(expectedQos0, publishQos0);

        final PUBLISH publishQos1 =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[0], QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, false,
                        MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT, Mqtt5PayloadFormatIndicator.UNSPECIFIED, null,
                        null, null, NO_USER_PROPERTIES,
                        7, false, true, null);
        encodeTestBufferSize(expectedQos1, publishQos1);

        final PUBLISH publishQos2 =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[0], QoS.EXACTLY_ONCE, QoS.EXACTLY_ONCE, false,
                        MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT, Mqtt5PayloadFormatIndicator.UNSPECIFIED, null,
                        null, null, NO_USER_PROPERTIES,
                        7, false, true, null);
        encodeTestBufferSize(expectedQos2, publishQos2);
    }

    @Test
    public void test_encode_max_packet_size_exceeded() {
        final ByteBuf expected = Unpooled.buffer(MAXIMUM_PACKET_SIZE_LIMIT, MAXIMUM_PACKET_SIZE_LIMIT + 3);

        // 5 header + remaining length, 7 topic, 4 property length, 2 payload format, 5 payload
        // exceed packet size by 3 bytes since correlation data header is to much
        final int correlationDataLength = MAXIMUM_PACKET_SIZE_LIMIT - 5 - 7 - 4 - 2 - 5;

        // fixed header
        //   type, flags
        expected.writeByte(0b0011_0000);
        //   remaining length
        expected.writeByte(0xff);
        expected.writeByte(0xff);
        expected.writeByte(0xff);
        expected.writeByte(0x7f);
        // variable header
        //   topic name
        expected.writeBytes(new byte[]{0, 5, 't', 'o', 'p', 'i', 'c'});
        //   properties
        expected.writeByte(0xef);
        expected.writeByte(0xff);
        expected.writeByte(0xff);
        expected.writeByte(0x7f);
        //     payload format indicator
        expected.writeBytes(new byte[]{PAYLOAD_FORMAT_INDICATOR, 0});
        //     correlation data
        expected.writeByte(CORRELATION_DATA);
        expected.writeShort(correlationDataLength);
        for (int i = 0; i < correlationDataLength; i++) {
            expected.writeByte(i);
        }
        // payload
        expected.writeBytes(new byte[]{1, 2, 3, 4, 5});

        final byte[] correlationData = ByteBuffer.wrap(expected.array(), 21, correlationDataLength).array();
        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(new MqttUserProperty("user", "property"));

        final PUBLISH publish =
                TestMessageUtil.createMqtt5Publish(hiveMQId.get(), "topic", new byte[]{1, 2, 3, 4, 5},
                        QoS.AT_MOST_ONCE, QoS.AT_MOST_ONCE, false, MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT,
                        Mqtt5PayloadFormatIndicator.UNSPECIFIED, null, null, correlationData, userProperties,
                        -1, false, true, null);

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientId("clientid");
        channel.writeOutbound(publish);
        final ByteBuf buf = channel.readOutbound();
        assertEquals(0, buf.readableBytes());

        expected.release();
    }

}