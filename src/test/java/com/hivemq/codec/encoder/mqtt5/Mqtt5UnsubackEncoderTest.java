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

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5UnsubAckReasonCode;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class Mqtt5UnsubackEncoderTest extends AbstractMqtt5EncoderTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        testMessageEncoder.getSecurityConfigurationService().setAllowRequestProblemInformation(true);
    }

    @Test
    public void encode_simple() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1011_0000,
                //   remaining length
                28,
                // variable header
                //   packet identifier
                0, 3,
                //   properties
                24,
                //     reason string
                0x1F, 0, 7, 's', 'u', 'c', 'c', 'e', 's', 's',
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                // payload
                0x00
        };

        final MqttUserProperty mqttUserProperty = new MqttUserProperty("test", "value");

        final UNSUBACK unsubAck = new UNSUBACK(3, Lists.newArrayList(Mqtt5UnsubAckReasonCode.SUCCESS), "success", Mqtt5UserProperties.of(mqttUserProperty));

        encodeTestBufferSize(expected, unsubAck);
    }

    @Test
    public void encode_reason_string_and_user_properties_request_problem_information_false() {

        testMessageEncoder.getSecurityConfigurationService().setAllowRequestProblemInformation(true);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setRequestProblemInformation(false);
        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1011_0000,
                //   remaining length
                4,
                // variable header
                //   packet identifier
                0, 3,
                //   properties
                0,
                // payload
                0x00
        };

        final MqttUserProperty mqttUserProperty = new MqttUserProperty("test", "value");

        final UNSUBACK unsubAck = new UNSUBACK(3, Lists.newArrayList(Mqtt5UnsubAckReasonCode.SUCCESS), "success", Mqtt5UserProperties.of(mqttUserProperty));
        encodeTestBufferSize(expected, unsubAck);
    }

    @Test
    public void encode_reason_string_request_problem_information_false() {

        testMessageEncoder.getSecurityConfigurationService().setAllowRequestProblemInformation(false);

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1011_0000,
                //   remaining length
                4,
                // variable header
                //   packet identifier
                0, 3,
                //   properties
                0,
                // payload
                0x00
        };

        final UNSUBACK unsubAck = new UNSUBACK(3, Lists.newArrayList(Mqtt5UnsubAckReasonCode.SUCCESS), "success", Mqtt5UserProperties.NO_USER_PROPERTIES);
        encodeTestBufferSize(expected, unsubAck);
    }

    @Test
    public void encode_user_property_request_problem_information_false() {

        testMessageEncoder.getSecurityConfigurationService().setAllowRequestProblemInformation(true);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setRequestProblemInformation(false);

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1011_0000,
                //   remaining length
                4,
                // variable header
                //   packet identifier
                0, 3,
                //   properties
                0,
                // payload
                0x00
        };

        final MqttUserProperty mqttUserProperty = new MqttUserProperty("test", "value");

        final UNSUBACK unsubAck = new UNSUBACK(3, Lists.newArrayList(Mqtt5UnsubAckReasonCode.SUCCESS), null, Mqtt5UserProperties.of(mqttUserProperty));
        encodeTestBufferSize(expected, unsubAck);
    }

    @Test
    public void encode_multiple_user_props() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1011_0000,
                //   remaining length
                56,
                // variable header
                //   packet identifier
                0, 3,
                //   properties
                52,
                //     reason string
                0x1F, 0, 7, 's', 'u', 'c', 'c', 'e', 's', 's',
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                // payload
                0x00
        };

        final MqttUserProperty mqttUserProperty = new MqttUserProperty("test", "value");

        final UNSUBACK unsubAck = new UNSUBACK(3, Lists.newArrayList(Mqtt5UnsubAckReasonCode.SUCCESS), "success", Mqtt5UserProperties.of(mqttUserProperty, mqttUserProperty, mqttUserProperty));

        encodeTestBufferSize(expected, unsubAck);
    }

    @Test
    public void encode_all_reason_codes() {
        byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1011_0000,
                //   remaining length
                34,
                // variable header
                //   packet identifier
                0, 3,
                //   properties
                24,
                //     reason string
                0x1F, 0, 7, 's', 'u', 'c', 'c', 'e', 's', 's',
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                // payload
        };


        for (final Mqtt5UnsubAckReasonCode mqtt5SubAckReasonCode : Mqtt5UnsubAckReasonCode.values()) {
            expected = Bytes.concat(expected, new byte[]{(byte) mqtt5SubAckReasonCode.getCode()});
        }

        final MqttUserProperty mqttUserProperty = new MqttUserProperty("test", "value");

        final UNSUBACK unsubAck = new UNSUBACK(3, Arrays.asList(Mqtt5UnsubAckReasonCode.values()), "success", Mqtt5UserProperties.of(mqttUserProperty));

        encodeTestBufferSize(expected, unsubAck);
    }

    @Test
    public void encode_propertyLengthExceeded_omitReasonString() {

        final int maxPacketSize = 130;
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setMaxPacketSizeSend((long) maxPacketSize);

        final int maxUserPropertiesCount = maxPacketSize / userPropertyBytes;
        final Mqtt5UserProperties maxUserProperties = getUserProperties(maxUserPropertiesCount);
        final int maxReasonStringLength = maxPacketSize % userPropertyBytes;
        final char[] reasonStringBytes = new char[maxReasonStringLength];
        Arrays.fill(reasonStringBytes, 'r');
        final String reasonString = new String(reasonStringBytes);

        final int userPropertiesLength = userPropertyBytes * maxUserPropertiesCount;

        final ByteBuf expected = Unpooled.buffer(userPropertiesLength + 6, userPropertiesLength + 6);

        // fixed header
        // type, reserved
        expected.writeByte(0b1011_0000);
        // remaining length userpropertiesLength + 2 bytes ID + 1 byte length + 1 byte reason code
        expected.writeByte(userPropertiesLength + 2 + 1 + 1);
        // packet identifier
        expected.writeBytes(new byte[]{0, 3});
        // properties length
        expected.writeByte(userPropertiesLength);
        // user properties
        maxUserProperties.encode(expected);
        // reason code
        expected.writeByte(0x00);

        final UNSUBACK unsubAck = new UNSUBACK(3, Lists.newArrayList(Mqtt5UnsubAckReasonCode.SUCCESS),
                reasonString, maxUserProperties);

        encodeTestBufferSize(expected.array(), unsubAck);
        expected.release();
    }

    @Test
    public void encode_propertyLengthExceeded_omitUserProperties() {

        final int maxPacketSize = 130;
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setMaxPacketSizeSend((long) maxPacketSize);

        final int maxUserPropertiesCount = maxPacketSize / userPropertyBytes;
        final Mqtt5UserProperties maxUserProperties = getUserProperties(maxUserPropertiesCount + 1);
        final int maxReasonStringLength = maxPacketSize % userPropertyBytes;
        final char[] reasonStringBytes = new char[maxReasonStringLength];
        Arrays.fill(reasonStringBytes, 'r');
        final String reasonString = new String(reasonStringBytes);

        final ByteBuf expected = Unpooled.buffer(6, 6);

        // fixed header
        // type, reserved
        expected.writeByte(0b1011_0000);
        // remaining length 2 bytes ID + 1 byte length + 1 byte reason code
        expected.writeByte(4);
        // packet identifier
        expected.writeBytes(new byte[]{0, 3});
        // no properties
        expected.writeByte(0);
        // reason code
        expected.writeByte(0x00);

        final UNSUBACK unsubAck = new UNSUBACK(3, Lists.newArrayList(Mqtt5UnsubAckReasonCode.SUCCESS),
                reasonString, maxUserProperties);

        encodeTestBufferSize(expected.array(), unsubAck);
        expected.release();
    }
}
