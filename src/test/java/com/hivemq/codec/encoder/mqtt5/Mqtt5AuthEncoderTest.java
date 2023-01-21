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

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Waldemar Ruck
 */
public class Mqtt5AuthEncoderTest extends AbstractMqtt5EncoderTest {

    private static final Mqtt5AuthReasonCode CONTINUE = Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        testMessageEncoder.getSecurityConfigurationService().setAllowRequestProblemInformation(true);
    }

    @Test
    public void test_encode_reason_code_continue() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                6,
                // variable header
                //   reason code placeholder
                (byte) CONTINUE.getCode(),
                //   properties
                4,
                //     auth method
                0x15, 0, 1, 'x'
        };

        final AUTH auth = new AUTH("x", null, CONTINUE,
                Mqtt5UserProperties.NO_USER_PROPERTIES, null);

        encodeTestBufferSize(expected, auth);
    }

    @Test
    public void test_encode_reason_code_success() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                6,
                // variable header
                //   reason code placeholder
                0x00,
                //   properties
                4,
                //     auth method
                0x15, 0, 1, 'x'
        };

        final AUTH auth = new AUTH("x", null, Mqtt5AuthReasonCode.SUCCESS,
                Mqtt5UserProperties.NO_USER_PROPERTIES, null);

        encodeTestBufferSize(expected, auth);
    }

    @Test
    public void test_encode_reason_code_reauthenticate() {

        final Mqtt5AuthReasonCode reauthenticateCode = Mqtt5AuthReasonCode.REAUTHENTICATE;

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                6,
                // variable header
                //   reason code placeholder
                (byte) reauthenticateCode.getCode(),
                //   properties
                4,
                //     auth method
                0x15, 0, 1, 'x'
        };

        final AUTH auth = new AUTH("x", null, reauthenticateCode,
                Mqtt5UserProperties.NO_USER_PROPERTIES, null);

        encodeTestBufferSize(expected, auth);
    }

    @Test
    public void test_encode_all_properties() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                33,
                // variable header
                //   reason code (continue)
                0x18,
                //   properties
                31,
                //     auth method
                0x15, 0, 1, 'x',
                //     auth data
                0x16, 0, 1, 1,
                // reason string
                0x1F, 0, 6, 'r', 'e', 'a', 's', 'o', 'n',
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e' //
        };


        final MqttUserProperty userProperty = new MqttUserProperty("test", "value");
        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(userProperty);

        final byte[] data = new byte[]{1};

        final AUTH auth = new AUTH("x", data, Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION,
                userProperties, "reason");


        encodeTestBufferSize(expected, auth);
    }

    @Test
    public void test_encode_simple_auth_method() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                6,
                // variable header
                //   reason code placeholder
                (byte) CONTINUE.getCode(),
                //   properties
                4,
                //     auth method
                0x15, 0, 1, 'x'
        };

        final AUTH auth = new AUTH("x", null, CONTINUE,
                Mqtt5UserProperties.NO_USER_PROPERTIES, null);


        encodeTestBufferSize(expected, auth);
    }

    @Test
    public void test_encode_success_auth() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                0,
        };

        final AUTH auth = AUTH.getSuccessAUTH();


        encodeTestBufferSize(expected, auth);
    }

    @Test
    public void test_encode_simple_auth_data() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                10,
                // variable header
                //   reason code placeholder
                (byte) CONTINUE.getCode(),
                //   properties
                8,
                //     auth method
                0x15, 0, 1, 'x',
                //     auth data
                0x16, 0, 1, 1
        };

        final AUTH auth = new AUTH("x", new byte[]{1}, CONTINUE,
                Mqtt5UserProperties.NO_USER_PROPERTIES, null);


        encodeTestBufferSize(expected, auth);
    }

    @Test
    public void test_encode_simple_reason_string() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                15,
                // variable header
                //   reason code placeholder
                (byte) CONTINUE.getCode(),
                //   properties
                13,
                //     auth method
                0x15, 0, 1, 'x',
                //     reason string
                0x1F, 0, 6, 'r', 'e', 'a', 's', 'o', 'n'
        };

        final AUTH auth = new AUTH("x", null, CONTINUE,
                Mqtt5UserProperties.NO_USER_PROPERTIES, "reason");


        encodeTestBufferSize(expected, auth);
    }

    @Test
    public void test_encode_reason_string_request_problem_information_false() {

        testMessageEncoder.getSecurityConfigurationService().setAllowRequestProblemInformation(false);

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                6,
                // variable header
                //   reason code placeholder
                (byte) CONTINUE.getCode(),
                //   properties
                4,
                //     auth method
                0x15, 0, 1, 'x'
        };

        final AUTH auth = new AUTH("x", null, CONTINUE,
                Mqtt5UserProperties.NO_USER_PROPERTIES, "reason");

        encodeTestBufferSize(expected, auth);
    }

    @Test
    public void test_encode_user_properties_request_problem_information_false() {

        testMessageEncoder.getSecurityConfigurationService().setAllowRequestProblemInformation(true);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setRequestProblemInformation(false);

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                6,
                // variable header
                //   reason code placeholder
                (byte) CONTINUE.getCode(),
                //   properties
                4,
                //     auth method
                0x15, 0, 1, 'x'
        };

        final MqttUserProperty userProperty = new MqttUserProperty("test", "value");
        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(userProperty);

        final AUTH auth = new AUTH("x", null, CONTINUE,
                userProperties, null);

        encodeTestBufferSize(expected, auth);
    }

    @Test
    public void test_encode_reason_string_and_user_properties_request_problem_information_false() {

        testMessageEncoder.getSecurityConfigurationService().setAllowRequestProblemInformation(false);

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                6,
                // variable header
                //   reason code placeholder
                (byte) CONTINUE.getCode(),
                //   properties
                4,
                //     auth method
                0x15, 0, 1, 'x'
        };

        final MqttUserProperty userProperty = new MqttUserProperty("test", "value");
        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(userProperty);

        final AUTH auth = new AUTH("x", null, CONTINUE,
                userProperties, "reason");

        encodeTestBufferSize(expected, auth);
    }


    @Test
    public void test_encode_simple_user_property() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                20,
                // variable header
                //   reason code placeholder
                (byte) CONTINUE.getCode(),
                //   properties
                18,
                //     auth method
                0x15, 0, 1, 'x',
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e' //
        };

        final MqttUserProperty userProperty = new MqttUserProperty("test", "value");
        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(userProperty);

        final AUTH auth = new AUTH("x", null, CONTINUE,
                userProperties, null);

        encodeTestBufferSize(expected, auth);
    }

    @Test
    public void encode_reasonStringEmpty() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                9,
                // variable header
                //   reason code (continue)
                0x18,
                //   properties
                7,
                //     auth method
                0x15, 0, 1, 'x',
                //     reason string
                0x1F, 0, 0
        };

        final AUTH auth = new AUTH("x", null, CONTINUE,
                Mqtt5UserProperties.NO_USER_PROPERTIES, "");

        encodeTestBufferSize(expected, auth);
    }

    @Test
    public void encode_maximumPacketSizeExceededByUserProperties_omitUserPropertiesAndReasonString() {
        final Mqtt5UserProperties tooManyUserProperties = getUserProperties(
                (MAX_PACKET_SIZE / userPropertyBytes) + 1);

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b1111_0000,
                //   remaining length
                6,
                // variable header
                //   reason code (continue)
                0x18,
                //   properties
                4,
                //     auth method
                0x15, 0, 1, 'x'
        };

        final AUTH auth = new AUTH("x", null, Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION,
                tooManyUserProperties, null);

        encodeTestBufferSize(expected, auth);
    }

}
