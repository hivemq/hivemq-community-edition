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
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static com.hivemq.mqtt.message.disconnect.DISCONNECT.SESSION_EXPIRY_NOT_SET;
import static com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode.*;

/**
 * @author Florian Limpöck
 */
public class Mqtt5DisconnectEncoderTest extends AbstractMqtt5EncoderTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        testMessageEncoder.getSecurityConfigurationService().setAllowRequestProblemInformation(true);
    }

    @Test
    public void encode_allProperties() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1110_0000,
                // remaining length
                49,
                // variable header
                //   reason code Malformed Packet
                (byte) 0x81,
                //  Properties
                47,
                //    Server Reference
                0x1C, 0, 6, 's', 'e', 'r', 'v', 'e', 'r',
                //    Reason String
                0x1F, 0, 6, 'r', 'e', 'a', 's', 'o', 'n',
                // User Properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 4, 't', 'e', 's', 't', 0, 6, 'v', 'a', 'l', 'u', 'e', '2'

        };
        final String reasonString = "reason";
        final String serverReference = "server";
        final String test = "test";
        final String value = "value";
        final String value2 = "value2";
        final MqttUserProperty userProperty1 = new MqttUserProperty(test, value);
        final MqttUserProperty userProperty2 = new MqttUserProperty(test, value2);
        final Mqtt5UserProperties userProperties =
                Mqtt5UserProperties.of(ImmutableList.of(userProperty1, userProperty2));

        final DISCONNECT disconnect =
                new DISCONNECT(MALFORMED_PACKET, reasonString, userProperties, serverReference, SESSION_EXPIRY_NOT_SET);

        encodeTestBufferSize(expected, disconnect);
    }

    @Test
    public void encode_reasonCode() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1110_0000,
                // remaining length
                1,
                // variable header
                //   reason code Malformed Packet
                (byte) 0x81
        };

        final DISCONNECT disconnect =
                new DISCONNECT(MALFORMED_PACKET, null, Mqtt5UserProperties.NO_USER_PROPERTIES, null, SESSION_EXPIRY_NOT_SET);
        encodeTestBufferSize(expected, disconnect);
    }

    @Test
    public void encode_allReasonCodes() {

        for (final Mqtt5DisconnectReasonCode reasonCode : Mqtt5DisconnectReasonCode.values()) {

            if (reasonCode.equals(NORMAL_DISCONNECTION)) {
                continue;
            }

            final byte[] expected = {
                    // fixed header
                    //   type, flags
                    (byte) 0b1110_0000,
                    // remaining length
                    1,
                    // variable header
                    //   reason code placeholder
                    (byte) 0xFF
            };

            expected[2] = (byte) reasonCode.getCode();
            final DISCONNECT disconnect =
                    new DISCONNECT(reasonCode, null, Mqtt5UserProperties.NO_USER_PROPERTIES, null, SESSION_EXPIRY_NOT_SET);
            encodeTestBufferSize(expected, disconnect);
        }
    }

    @Test
    public void encode_noReasonCodeIfNormalWithoutProperties() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1110_0000,
                // remaining length
                0
        };

        final DISCONNECT disconnect =
                new DISCONNECT(NORMAL_DISCONNECTION, null, Mqtt5UserProperties.NO_USER_PROPERTIES, null, SESSION_EXPIRY_NOT_SET);
        encodeTestBufferSize(expected, disconnect);
    }

    @Test
    public void encode_reasonString() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1110_0000,
                // remaining length
                11,
                // variable header
                //   reason code Malformed Packet
                (byte) 0x81,
                //  Properties
                9,
                //    Reason String
                0x1F, 0, 6, 'r', 'e', 'a', 's', 'o', 'n'


        };
        final String reasonString = "reason";
        final DISCONNECT disconnect =
                new DISCONNECT(MALFORMED_PACKET, reasonString, Mqtt5UserProperties.NO_USER_PROPERTIES, null, SESSION_EXPIRY_NOT_SET);
        encodeTestBufferSize(expected, disconnect);
    }

    @Test
    public void encode_reasonString_request_problem_information_false() {

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setRequestProblemInformation(false);

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1110_0000,
                // remaining length
                11,
                // variable header
                //   reason code Malformed Packet
                (byte) 0x81,
                //  Properties
                9,
                //    Reason String
                0x1F, 0, 6, 'r', 'e', 'a', 's', 'o', 'n'


        };
        final String reasonString = "reason";
        final DISCONNECT disconnect =
                new DISCONNECT(MALFORMED_PACKET, reasonString, Mqtt5UserProperties.NO_USER_PROPERTIES, null, SESSION_EXPIRY_NOT_SET);

        // Do not omit reason string because it is a DISCONNECT packet!
        encodeTestBufferSize(expected, disconnect);
    }

    @Test
    public void encode_allProperties_request_problem_information_false() {

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setRequestProblemInformation(false);

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1110_0000,
                // remaining length
                49,
                // variable header
                //   reason code Malformed Packet
                (byte) 0x81,
                //  Properties
                47,
                //    Server Reference
                0x1C, 0, 6, 's', 'e', 'r', 'v', 'e', 'r',
                //    Reason String
                0x1F, 0, 6, 'r', 'e', 'a', 's', 'o', 'n',
                // User Properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 4, 't', 'e', 's', 't', 0, 6, 'v', 'a', 'l', 'u', 'e', '2'

        };
        final String reasonString = "reason";
        final String serverReference = "server";
        final String test = "test";
        final String value = "value";
        final String value2 = "value2";
        final MqttUserProperty userProperty1 = new MqttUserProperty(test, value);
        final MqttUserProperty userProperty2 = new MqttUserProperty(test, value2);
        final Mqtt5UserProperties userProperties =
                Mqtt5UserProperties.of(ImmutableList.of(userProperty1, userProperty2));

        final DISCONNECT disconnect =
                new DISCONNECT(MALFORMED_PACKET, reasonString, userProperties, serverReference, SESSION_EXPIRY_NOT_SET);

        // Do not omit user properties because it is a DISCONNECT packet!
        encodeTestBufferSize(expected, disconnect);
    }

    @Test
    public void encode_serverReference() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1110_0000,
                // remaining length
                11,
                // variable header
                //   reason code Malformed Packet
                (byte) 0x81,
                //  Properties
                9,
                //    Server Reference
                0x1C, 0, 6, 's', 'e', 'r', 'v', 'e', 'r',


        };
        final String serverReference = "server";
        final DISCONNECT disconnect =
                new DISCONNECT(MALFORMED_PACKET, null, Mqtt5UserProperties.NO_USER_PROPERTIES, serverReference, SESSION_EXPIRY_NOT_SET);
        encodeTestBufferSize(expected, disconnect);
    }

    @Test
    public void encode_minimum() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1110_0000,
                // remaining length
                1,
                // variable header
                //   reason code Malformed Packet
                (byte) 0x81,

        };

        final DISCONNECT disconnect =
                new DISCONNECT(MALFORMED_PACKET, null, Mqtt5UserProperties.NO_USER_PROPERTIES, null, SESSION_EXPIRY_NOT_SET);
        encodeTestBufferSize(expected, disconnect);
    }

    @Test
    public void encode_maximumPacketSizeExceededOnSuccess_omitUserProperties() {
        final MaximumPacketBuilder maxPacket = new MaximumPacketBuilder().build(MAX_PACKET_SIZE);
        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1110_0000,
                // remaining length
                0
        };

        final DISCONNECT disconnect = new DISCONNECT(NORMAL_DISCONNECTION,
                null,
                getUserProperties(maxPacket.getMaxUserPropertiesCount() + 1),
                null,
                SESSION_EXPIRY_NOT_SET);

        encodeTestBufferSize(expected, disconnect);
    }

    @Test
    public void encode_maximumPacketSizeExceeded_omitReasonString() {
        final MaximumPacketBuilder maxPacket = new MaximumPacketBuilder().build(MAX_PACKET_SIZE);
        final Mqtt5UserProperties maxUserProperties = getUserProperties(maxPacket.getMaxUserPropertiesCount());
        final String reasonString = getPaddedUtf8String(maxPacket.remainingPropertyBytes + 1);

        final int userPropertiesLength = userPropertyBytes * maxPacket.getMaxUserPropertiesCount();

        final ByteBuf expected = Unpooled.buffer(4 + userPropertiesLength, 4 + userPropertiesLength);

        // fixed header
        // type, reserved
        expected.writeByte(0b1110_0000);
        // remaining length (1 + 4 + (userPropertyBytes * maxPossibleUserPropertiesCount = 268435445
        expected.writeByte(userPropertiesLength + 2);
        // reason code
        expected.writeByte((byte) 0x82);
        // properties length
        expected.writeByte(userPropertiesLength);
        // user properties
        maxUserProperties.encode(expected);

        final DISCONNECT disconnect =
                new DISCONNECT(PROTOCOL_ERROR, reasonString, maxUserProperties, null, SESSION_EXPIRY_NOT_SET);

        encodeTestBufferSize(expected.array(), disconnect);
        expected.release();
    }

    @Test
    public void encode_maximumPacketSizeExceeded_omitUserProperties() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1110_0000,
                // remaining length
                1, (byte) 0x82
        };

        final DISCONNECT disconnect =
                new DISCONNECT(PROTOCOL_ERROR, null, getUserProperties((MAX_PACKET_SIZE / userPropertyBytes) + 1), null, SESSION_EXPIRY_NOT_SET);

        encodeTestBufferSize(expected, disconnect);
    }

    @Test
    public void encode_propertyLengthExceededOnSuccess_omitUserProperties() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1110_0000,
                // remaining length
                0
        };

        final DISCONNECT disconnect =
                new DISCONNECT(NORMAL_DISCONNECTION, null, getUserProperties((MAX_PACKET_SIZE / userPropertyBytes) + 1), null, SESSION_EXPIRY_NOT_SET);

        encodeTestBufferSize(expected, disconnect);
    }

    @Test
    public void encode_propertyLengthExceeded_omitUserProperties() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1110_0000,
                // remaining length
                1, (byte) 0x81
        };

        final DISCONNECT disconnect =
                new DISCONNECT(MALFORMED_PACKET, null, getUserProperties((MAX_PACKET_SIZE / userPropertyBytes) + 1), null, SESSION_EXPIRY_NOT_SET);

        encodeTestBufferSize(expected, disconnect);
    }

    @Test
    public void encode_propertyLengthExceeded_omitReasonString() {

        final int maxUserPropertiesCount = MAX_PACKET_SIZE / userPropertyBytes;
        final Mqtt5UserProperties maxUserProperties = getUserProperties(maxUserPropertiesCount);
        final int maxReasonStringLength = MAX_PACKET_SIZE % userPropertyBytes;
        final char[] reasonStringBytes = new char[maxReasonStringLength];
        Arrays.fill(reasonStringBytes, 'r');
        final String reasonString = new String(reasonStringBytes);

        final int userPropertiesLength = userPropertyBytes * maxUserPropertiesCount;

        final ByteBuf expected = Unpooled.buffer(userPropertiesLength + 4, userPropertiesLength + 4);

        // fixed header
        // type, reserved
        expected.writeByte(0b1110_0000);
        // remaining length (1 + 4 + (userPropertyBytes * maxPossibleUserPropertiesCount = 268435445
        expected.writeByte(userPropertiesLength + 2);
        // reason code
        expected.writeByte((byte) 0x81);
        // properties length
        expected.writeByte(userPropertiesLength);
        // user properties
        maxUserProperties.encode(expected);

        final DISCONNECT disconnect =
                new DISCONNECT(MALFORMED_PACKET, reasonString, maxUserProperties, null, SESSION_EXPIRY_NOT_SET);

        encodeTestBufferSize(expected.array(), disconnect);
        expected.release();
    }
}