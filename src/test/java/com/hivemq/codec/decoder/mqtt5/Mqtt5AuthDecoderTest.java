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
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Waldemar Ruck
 */
public class Mqtt5AuthDecoderTest extends AbstractMqtt5DecoderTest {

    private final Mqtt5AuthReasonCode reasonCode = Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION;

    @Before
    public void before() {

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
    }

    // Tests for Fixed/Variable Header
    @Test
    public void test_fixed_header() {
        final byte[] encoded0001 = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0001,
                //   remaining length
                6,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                4,
                //     auth method
                0x15, 0, 1, 'x'
        };

        decodeNok(encoded0001);

        final byte[] encoded0010 = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0010,
                //   remaining length
                6,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                4,
                //     auth method
                0x15, 0, 1, 'x'
        };

        decodeNok(encoded0010);

        final byte[] encoded0100 = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0100,
                //   remaining length
                6,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                4,
                //     auth method
                0x15, 0, 1, 'x'
        };

        decodeNok(encoded0100);

        final byte[] encoded1000 = {
                // fixed header
                //   type, flags
                (byte) 0b1111_1000,
                //   remaining length
                6,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                4,
                //     auth method
                0x15, 0, 1, 'x'
        };

        decodeNok(encoded1000);
    }

    @Test
    public void test_decode_reason_code_success_with_props() {

        final Mqtt5AuthReasonCode successCode = Mqtt5AuthReasonCode.SUCCESS;

        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                6,
                // variable header
                //   reason code (continue)
                (byte) successCode.getCode(),
                //   properties
                4,
                //     auth method
                0x15, 0, 1, 'x'
        };

        final AUTH auth = decodeAuth(encoded);
        assertNotNull(auth);

        assertEquals(successCode, auth.getReasonCode());
        assertEquals("x", auth.getAuthMethod());
        assertNull(auth.getAuthData());
        assertEquals(0, auth.getUserProperties().encodedLength());
    }


    @Test
    public void test_decode_reason_code_success_without_code() {

        final Mqtt5AuthReasonCode successCode = Mqtt5AuthReasonCode.SUCCESS;

        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                0,
                // variable header
        };

        final AUTH auth = decodeAuth(encoded);
        assertNotNull(auth);

        assertEquals(successCode, auth.getReasonCode());
        assertEquals("", auth.getAuthMethod());
        assertNull(auth.getAuthData());
        assertEquals(0, auth.getUserProperties().encodedLength());
    }

    @Test
    public void test_decode_reason_code_success_with_code() {

        final Mqtt5AuthReasonCode successCode = Mqtt5AuthReasonCode.SUCCESS;

        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                1,
                // variable header
                (byte) successCode.getCode()
        };

        final AUTH auth = decodeAuth(encoded);
        assertNotNull(auth);

        assertEquals(successCode, auth.getReasonCode());
        assertEquals("", auth.getAuthMethod());
        assertNull(auth.getAuthData());
        assertEquals(0, auth.getUserProperties().encodedLength());
    }

    @Test
    public void test_decode_reason_code_reauthenticate() {

        final Mqtt5AuthReasonCode successReauthenticate = Mqtt5AuthReasonCode.REAUTHENTICATE;

        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                6,
                // variable header
                //   reason code (continue)
                (byte) successReauthenticate.getCode(),
                //   properties
                4,
                //     auth method
                0x15, 0, 1, 'x'
        };

        final AUTH auth = decodeAuth(encoded);
        assertNotNull(auth);

        assertEquals(successReauthenticate, auth.getReasonCode());
        assertEquals("x", auth.getAuthMethod());
    }

    @Test
    public void test_decode_reason_code_unknown() {

        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                6,
                // variable header
                //   reason code (continue)
                0x20,
                //   properties
                4,
                //     auth method
                0x15, 0, 1, 'x'
        };

        decodeNok(encoded);
    }

    // Tests for properties (generic decode)
    @Test
    public void test_decode_allProperties() {

        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length (132)
                // with reason code (byte) (128 + 4), 1,
                (byte) (128 + 4), 1,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties (129)
                (byte) (128 + 1), 1,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                //     auth data
                0x16, 0, 60, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                //     reason string
                0x1F, 0, 8, 'c', 'o', 'n', 't', 'i', 'n', 'u', 'e',
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 4, 't', 'e', 's', 't', 0, 6, 'v', 'a', 'l', 'u', 'e', '2', //
                0x26, 0, 5, 't', 'e', 's', 't', '2', 0, 5, 'v', 'a', 'l', 'u', 'e',
        };

        final AUTH auth = decodeAuth(encoded);
        assertNotNull(auth);

        assertEquals(reasonCode, auth.getReasonCode());
        assertEquals("GS2-KRB5", auth.getAuthMethod());
        assertEquals("continue", auth.getReasonString());

        assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, auth.getAuthData());

        final ImmutableList<MqttUserProperty> userProperties = auth.getUserProperties().asList();
        assertEquals(3, userProperties.size());

        assertEquals("test", userProperties.get(0).getName());
        assertEquals("value", userProperties.get(0).getValue());

        assertEquals("test", userProperties.get(1).getName());
        assertEquals("value2", userProperties.get(1).getValue());

        assertEquals("test2", userProperties.get(2).getName());
        assertEquals("value", userProperties.get(2).getValue());

    }

    @Test
    public void test_decode_simple() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                13,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5'
        };

        final AUTH auth = decodeAuth(encoded);
        assertNotNull(auth);

        assertEquals(reasonCode, auth.getReasonCode());
        assertEquals("GS2-KRB5", auth.getAuthMethod());
    }

    @Test
    public void test_decode_min() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                6,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                4,
                //     auth method
                0x15, 0, 1, 'x'
        };

        final AUTH auth = decodeAuth(encoded);
        assertNotNull(auth);
        assertNotNull(auth.getReasonCode());
        assertEquals(reasonCode, auth.getReasonCode());
        assertEquals("x", auth.getAuthMethod());
    }

    @Test
    public void test_decode_omit_auth_method() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                7,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                5,
                //     reason string
                0x1F, 0, 2, 'r', 'e'
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_success_omit_auth_method() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                7,
                // variable header
                //   reason code (continue)
                (byte) 0x00,
                //   properties
                5,
                //     reason string
                0x1F, 0, 2, 'r', 'e'
        };

        decodeNok(encoded);

    }

    // Tests for UTF-8 should not characters.
    @Test
    public void test_decode_should_not_character_in_auth_method() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                13,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 0x7F, 'S', '2', '-', 'K', 'R', 'B', '5'
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_should_not_character_reason_string() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                11,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                9,
                //     auth method
                0x15, 0, 1, 'x',
                //     reason string
                0x1F, 0, 2, 0x7F, 'e'
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_should_not_character_auth_method() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                5,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 0x7F, 'S', '2', '-', 'K', 'R', 'B', '5'
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_should_not_character_user_properties() {

        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                20,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                18,
                //     auth method
                0x15, 0, 1, 'x',
                //     user properties
                0x26, 0, 4, 0x7F, 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e'
        };

        decodeNok(encoded);
    }

    // Tests for UTF-8 must not characters.
    @Test
    public void test_decode_must_not_character_in_auth_method() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                13,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 0, 'S', '2', '-', 'K', 'R', 'B', '5'
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_must_not_character_reason_string() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                11,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                9,
                //     auth method
                0x15, 0, 1, 'x',
                //     reason string
                0x1F, 0, 2, 0, 'e'
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_must_not_character_auth_method() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                5,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 0, 'S', '2', '-', 'K', 'R', 'B', '5'
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_must_not_character_user_properties() {

        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                20,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                18,
                //     auth method
                0x15, 0, 1, 'x',
                //     user properties
                0x26, 0, 4, 0, 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e'
        };

        decodeNok(encoded);
    }

    // Tests for broken packet
    @Test
    public void test_decode_broken_packet_incorrect_properties_length() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                10,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                8,
                //     auth method
                0x15, 0, 1, 'x',
                //     auth data
                0x16, 0, 8, 1, 1, 1, 1, 1, 1, 1, 1
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_broken_packet_incorrect_remaining_length() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                11,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                8,
                //     auth method
                0x15, 0, 1, 'x',
                //     auth data
                0x16, 0, 1, 1
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_broken_packet_no_reason_code() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                10,
                // variable header
                //   reason code (continue)
                //(byte) reasonCode.getCode(),
                //   properties
                8,
                //     auth method
                0x15, 0, 1, 'x',
                //     auth data
                0x16, 0, 1, 1
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_broken_packet_properties_length_but_no_properties() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                10,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                8
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_without_remaining_length() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_reason_code_in_property() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                14,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                12,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                //     auth method
                0x18
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_incorrect_property() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                14,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                12,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                0x04
        };

        decodeNok(encoded);
    }

    // Tests for multiple entries
    @Test
    public void test_decode_same_auth_data_twice() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                11,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                9,
                //     auth method
                0x15, 0, 1, 'x',
                //     auth data
                0x16, 0, 1, 1,
                0x16, 0, 1, 1
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_same_auth_method_twice() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                28,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                26,
                //     auth method
                0x15, 0, 1, 'x',
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5'
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_same_reason_string_twice() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                28,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                26,
                //     auth method
                0x15, 0, 1, 'x',
                //     reason string
                0x1F, 0, 8, 'c', 'o', 'n', 't', 'i', 'n', 'u', 'e',
                0x1F, 0, 8, 'c', 'o', 'n', 't', 'i', 'n', 'u', 'e'
        };

        decodeNok(encoded);
    }

    // Tests for Packet/Attribute Length
    @Test
    public void test_decode_data_length_gt_package_length() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                13,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                //     auth data
                0x16, 0, 60, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        };

        final AUTH auth = decodeAuth(encoded);
        assertNotNull(auth);

        assertEquals(reasonCode, auth.getReasonCode());
        assertEquals("GS2-KRB5", auth.getAuthMethod());
        assertArrayEquals(null, auth.getAuthData());
    }

    @Test
    public void test_decode_reason_string_length_gt_packet_length() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                6,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                4,
                //     auth method
                0x15, 0, 1, 'x',
                //     reason string
                0x1F, 0, 7, 'a', 'e', 'a', 'b', 'c', 'd', 'r'
        };

        final AUTH auth = decodeAuth(encoded);
        assertNotNull(auth);

        assertEquals(reasonCode, auth.getReasonCode());
        assertEquals("x", auth.getAuthMethod());
        assertNull(auth.getReasonString());
    }

    @Test
    public void test_decode_user_properties_length_gt_packet_length() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                6,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                4,
                //     auth method
                0x15, 0, 1, 'x',
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e'
        };

        final AUTH auth = decodeAuth(encoded);
        assertNotNull(auth);

        assertEquals(reasonCode, auth.getReasonCode());
        assertEquals("x", auth.getAuthMethod());

        assertEquals(0, auth.getUserProperties().asList().size());
    }

    @Test
    public void test_decode_user_properties_incorrect_key_length_gt_must_be() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                20,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                18,
                //     auth method
                0x15, 0, 1, 'x',
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', '2', 0, 5, 'v', 'a', 'l', 'u', 'e'
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_user_properties_incorrect_key_length_lt_must_be() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                20,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                18,
                //     auth method
                0x15, 0, 1, 'x',
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 0, 5, 'v', 'a', 'l', 'u', 'e'
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_invalid_property_length() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                2,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                -1,
                //     auth method
                0x15, 0, 1, 'x'
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_invalid_remaining_length() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                -1,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5'
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_invalid_remaining_length_and_property_length() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                -1,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                -3,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5'
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_property_length_too_short() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                13,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                10,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5'
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_property_length_gt_packet_length() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                13,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                22,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                //     reason string
                0x1F, 0, 8, 'c', 'o', 'n', 't', 'i', 'n', 'u', 'e',
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_property_length_eq_packet_length() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                22,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                22,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                //     reason string
                0x1F, 0, 8, 'c', 'o', 'n', 't', 'i', 'n', 'u', 'e',
        };

        decodeNok(encoded);
    }

    @Test
    public void test_decode_incorrect_property_length() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                13,
                // variable header
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                15,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5'
        };

        decodeNok(encoded);
    }

    @NotNull
    private AUTH decodeAuth(final byte[] encoded) {
        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeBytes(encoded);
        channel.writeInbound(byteBuf);

        final AUTH auth = channel.readInbound();
        assertNotNull(auth);

        return auth;
    }

    private void decodeNok(final byte[] encoded) {
        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeBytes(encoded);
        channel.writeInbound(byteBuf);

        final AUTH auth = channel.readInbound();
        assertNull(auth);

        createChannel();
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
    }

}
