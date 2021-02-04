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
import com.google.common.primitives.Bytes;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.codec.encoder.mqtt5.MqttVariableByteInteger;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;
import util.TestMqttDecoder;

import java.nio.charset.StandardCharsets;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.*;
import static com.hivemq.mqtt.message.connect.MqttWillPublish.WILL_DELAY_INTERVAL_NOT_SET;
import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 */
public class Mqtt5ConnectDecoderTest extends AbstractMqtt5DecoderTest {

    @Override
    @Before
    public void setUp() {
        super.setUp();
        //protocol version must not be set when a CONNECT is sent.
        channel.attr(ChannelAttributes.MQTT_VERSION).set(null);
    }

    @Test
    public void test_decode_all_properties() {

        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0001_0000,
                // remaining length (223)
                (byte) (128 + 95), 1,
                // variable header
                //   protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b1110_1110,
                //   keep alive
                0, 10,
                //   properties
                88,
                //     session expiry interval
                0x11, 0, 0, 0, 10,
                //     request response information
                0x19, 1,
                //     request problem information
                0x17, 0,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                //     auth data
                0x16, 0, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                //     receive maximum
                0x21, 0, 5,
                //     topic alias maximum
                0x22, 0, 10,
                //     maximum packet size
                0x27, 0, 0, 0, 100,
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 4, 't', 'e', 's', 't', 0, 6, 'v', 'a', 'l', 'u', 'e', '2', //
                0x26, 0, 5, 't', 'e', 's', 't', '2', 0, 5, 'v', 'a', 'l', 'u', 'e',
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't',
                //   will properties
                82,
                //     message expiry interval
                0x02, 0, 0, 0, 10,
                //     payload format indicator
                0x01, 1,
                //     content type
                0x03, 0, 4, 't', 'e', 'x', 't',
                //     response topic
                0x08, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',
                //     correlation data
                0x09, 0, 5, 5, 4, 3, 2, 1,
                //     user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 4, 't', 'e', 's', 't', 0, 6, 'v', 'a', 'l', 'u', 'e', '2', //
                0x26, 0, 5, 't', 'e', 's', 't', '2', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //     will delay interval
                0x18, 0, 0, 0, 5,
                //   will topic
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   will payload
                0, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                //   username
                0, 8, 'u', 's', 'e', 'r', 'n', 'a', 'm', 'e',
                //   password
                0, 4, 'p', 'a', 's', 's'
        };

        final CONNECT connect = decodeInternal(encoded);

        assertEquals("GS2-KRB5", connect.getAuthMethod());
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, connect.getAuthData());
        assertEquals(5, connect.getReceiveMaximum());
        assertEquals(10, connect.getTopicAliasMaximum());
        assertEquals(100, connect.getMaximumPacketSize());
        assertEquals(10, connect.getKeepAlive());
        assertEquals(true, connect.isCleanStart());
        assertEquals(10, connect.getSessionExpiryInterval());
        assertEquals(true, connect.isResponseInformationRequested());
        assertEquals(false, connect.isProblemInformationRequested());
        assertEquals("test", connect.getClientIdentifier());

        assertEquals("username", connect.getUsername());
        assertArrayEquals(new byte[]{'p', 'a', 's', 's'}, connect.getPassword());
        assertEquals("pass", connect.getPasswordAsUTF8String());

        final ImmutableList<MqttUserProperty> userProperties = connect.getUserProperties().asList();
        assertEquals("test", userProperties.get(0).getName());
        assertEquals("value", userProperties.get(0).getValue());
        assertEquals("test", userProperties.get(1).getName());
        assertEquals("value2", userProperties.get(1).getValue());
        assertEquals("test2", userProperties.get(2).getName());
        assertEquals("value", userProperties.get(2).getValue());


        final MqttWillPublish willPublish = connect.getWillPublish();

        assertEquals(5, willPublish.getDelayInterval());
        assertEquals("topic", willPublish.getTopic());
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, willPublish.getPayload());
        assertEquals(QoS.AT_LEAST_ONCE, willPublish.getQos());
        assertEquals(true, willPublish.isRetain());
        assertEquals(10, willPublish.getMessageExpiryInterval());
        assertEquals(Mqtt5PayloadFormatIndicator.UTF_8, willPublish.getPayloadFormatIndicator());
        assertEquals("text", willPublish.getContentType());
        assertEquals("response", willPublish.getResponseTopic());
        assertArrayEquals(new byte[]{5, 4, 3, 2, 1}, willPublish.getCorrelationData());
        final ImmutableList<MqttUserProperty> willUserProperties = willPublish.getUserProperties().asList();
        assertEquals("test", willUserProperties.get(0).getName());
        assertEquals("value", willUserProperties.get(0).getValue());
        assertEquals("test", willUserProperties.get(1).getName());
        assertEquals("value2", willUserProperties.get(1).getValue());
        assertEquals("test2", willUserProperties.get(2).getName());
        assertEquals("value", willUserProperties.get(2).getValue());

        assertEquals(null, channel.attr(ChannelAttributes.CONNECT_KEEP_ALIVE).get());
        assertEquals("username", channel.attr(ChannelAttributes.AUTH_USERNAME).get());
        assertEquals("pass", new String(channel.attr(ChannelAttributes.AUTH_PASSWORD).get(), StandardCharsets.UTF_8));

    }

    @Test
    public void decode_simple() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                17,
                // variable header
                //   protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                0,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'
        };

        final CONNECT connect = decodeInternal(encoded);
        assertEquals(ProtocolVersion.MQTTv5, connect.getProtocolVersion());
        assertEquals(0, connect.getKeepAlive());
        assertEquals("test", connect.getClientIdentifier());
        assertFalse(connect.isCleanStart());
        assertNull(connect.getWillPublish());
        assertEquals(MAXIMUM_PACKET_SIZE_NOT_SET, connect.getMaximumPacketSize());
        assertEquals(SESSION_EXPIRY_NOT_SET, connect.getSessionExpiryInterval());
        assertEquals(RECEIVE_MAXIMUM_NOT_SET, connect.getReceiveMaximum());
        assertEquals(TOPIC_ALIAS_MAXIMUM_NOT_SET, connect.getTopicAliasMaximum());
        assertEquals(DEFAULT_RESPONSE_INFORMATION_REQUESTED, connect.isResponseInformationRequested());
        assertEquals(DEFAULT_PROBLEM_INFORMATION_REQUESTED, connect.isProblemInformationRequested());

        assertNull(channel.attr(ChannelAttributes.TOPIC_ALIAS_MAPPING).get());
    }

    @Test
    public void decode_header_failed_no_protocol_version() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0001,
                0

        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_header_failed_unknown_protocol_version() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0001,
                7,
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                6,

        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_header_failed_incorrect_variable_header() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                7,
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,

        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_header_failed_invalid_fixed_header() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0001,
                7,
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,

        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_header_failed_invalid_protocol_name() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                10,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'D',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_header_failed_invalid_connect_flags() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                10,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0001,
                //   keep alive
                0, 0,


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_header_failed_invalid_will_qos_3() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                10,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0001_1000,
                //   keep alive
                0, 0,


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_header_failed_no_will_flag_but_will_qos() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                10,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_1000,
                //   keep alive
                0, 0,


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_header_failed_no_will_flag_but_will_retain() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                10,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_0000,
                //   keep alive
                0, 0,


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_properties_length_to_large() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                17,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                27,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_properties_length_negative() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                17,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                -1,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_properties_length_not_enough() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                21,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                4,
                //     session expiry interval
                0x11, 0, 0, 0, 10,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_session_expiry_twice() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                26,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                10,
                //     session expiry interval
                0x11, 0, 0, 0, 10,
                0x11, 0, 0, 0, 10,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_session_expiry_too_short() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                15,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                4,
                //     session expiry interval
                0x11, 0, 0, 10,


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_receive_maximum_twice() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                22,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                6,
                //   receive maximum
                0x21, 0, 3,
                0x21, 0, 3,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_receive_maximum_zero() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                19,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                3,
                //   receive maximum
                0x21, 0, 0,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_receive_maximum_too_short() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                13,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                2,
                //   receive maximum
                0x21, 0,


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_maximum_packet_size_too_short() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                13,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                2,
                //   maximum packet size
                0x27, 0,


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_maximum_packet_size_twice() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                26,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                10,
                //   maximum packet size
                0x27, 0, 0, 0, 100,
                0x27, 0, 0, 0, 100,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_maximum_packet_size_zero() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                21,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                5,
                //   maximum packet size
                0x27, 0, 0, 0, 0,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_topic_alias_maximum_twice() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                22,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                6,
                //     topic alias maximum
                0x22, 0, 10,
                0x22, 0, 10,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_topic_alias_maximum_too_short() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                13,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                2,
                //     topic alias maximum
                0x22, 10,


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_request_response_information_twice() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                20,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                4,
                //     request response information
                0x19, 1,
                0x19, 1,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_request_response_information_too_short() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                12,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                1,
                //     request response information
                0x19,
                // payload


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_request_response_information_3() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                20,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                4,
                //     request response information
                0x19, 3,
                0x19, 1,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_request_response_information_0() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                19,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                2,
                //     request response information
                0x19, 0,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'


        };
        final CONNECT connect = decodeInternal(encoded);
        assertFalse(connect.isResponseInformationRequested());

    }

    @Test
    public void decode_failed_request_problem_information_twice() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                20,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                4,
                //     request problem information
                0x17, 0,
                0x17, 0,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_request_problem_information_3() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                20,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                4,
                //     request problem information
                0x17, 3,
                0x17, 0,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_request_problem_information_too_short() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                12,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                1,
                //     request problem information
                0x17,
                // payload


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_request_problem_information_1() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                19,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                2,
                //     request problem information
                0x17, 1,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'


        };
        final CONNECT connect = decodeInternal(encoded);
        assertTrue(connect.isProblemInformationRequested());

    }

    @Test
    public void decode_failed_auth_data_twice() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                42,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                26,
                //     auth data
                0x16, 0, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                0x16, 0, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_auth_data_no_method() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                30,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                13,
                //     auth data
                0x16, 0, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'


        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_auth_data_malformed_data_too_short() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                23,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                12,
                //     auth data
                0x16, 0, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9,

        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_auth_data_malformed_readable_bytes_1() {
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                13,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                2,
                //     auth data
                0x16, 0,

        };
        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_auth_method_twice() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                38,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                22,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'
        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_auth_method_malformed_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                21,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                10,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B',
        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_auth_method_malformed_readable_bytes_1() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                13,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                2,
                //     auth method
                0x15, 0,
        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_auth_method_malformed_utf8() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                17,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                6,
                //     auth method
                0x15, 0, 3, (byte) 0x7F, 'a', 'b'
        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_auth_method_malformed_utf16_surrogate() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                17,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                6,
                //     auth method
                0x15, 0, 3, -19, -96, 'b'
        };

        decodeNullExpected(encoded);

    }


    @Test
    public void decode_connect_failed_by_property_user_property_value_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                24,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                13,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u',

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_connect_failed_by_property_user_property_key_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                24,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                13,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 0, 5, 'v', 'a', 'l', 'u', 'e'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_connect_failed_by_property_user_property_to_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                13,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                2,
                //   user property
                0x26, 0,

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_connect_failed_by_property_user_property_key_contains_must_not() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                25,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', (byte) 0xFF, 0, 5, 'v', 'a', 'l', 'u', 'e'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_connect_failed_by_property_user_property_key_contains_should_not() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                25,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 0x7F, 0, 5, 'v', 'a', 'l', 'u', 'e'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_connect_failed_by_property_user_property_value_contains_must_not() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                25,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', (byte) 0xFF

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_connect_failed_by_property_user_property_value_contains_should_not() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                25,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 0x7F

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_empty_client_id_assigned() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                24,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 0,
        };

        final CONNECT connect = decodeInternal(encoded);

        assertEquals(44, connect.getClientIdentifier().length());

        assertTrue(channel.attr(ChannelAttributes.CLIENT_ID_ASSIGNED).get());

    }

    @Test
    public void decode_with_client_id_assigned_not_allowed() {

        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfig.securityConfiguration().setAllowServerAssignedClientId(false);

        channel = new EmbeddedChannel(TestMqttDecoder.create(fullConfig));

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                24,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 0,
        };

        decodeNullExpected(encoded);

    }


    @Test
    public void decode_with_client_id_not_assigned() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                28,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u'
        };

        final CONNECT connect = decodeInternal(encoded);

        assertEquals("huhu", connect.getClientIdentifier());

        assertFalse(channel.attr(ChannelAttributes.CLIENT_ID_ASSIGNED).get());

    }

    @Test
    public void decode_failed_wrong_identifier() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                28,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     unknown
                0x18, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u'
        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_failed_client_id_malformed_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                27,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h'
        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_client_id_malformed_utf_8() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                28,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', (byte) 0xFF
        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_client_id_malformed_utf_16_surrogate() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                28,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', -19, -96, 'u'
        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_username_flag_no_username() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                28,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b1000_0000,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u'
        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_password_flag_no_password() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                28,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0100_0000,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u'
        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_password_length_malformed() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                29,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0100_0000,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                // password
                0,
        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_failed_password_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                34,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0100_0000,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                // password
                0, 5, 1, 2, 3, 4
        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_password_empty() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                30,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0100_0000,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                // password
                0, 0
        };

        final CONNECT connect = decodeInternal(encoded);
        assertEquals(0, connect.getPassword().length);

    }

    @Test
    public void decode_failed_username_malformed_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                29,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b1000_0000,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   username
                0,
        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_username_empty() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                30,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b1000_0000,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   username
                0, 0
        };

        final CONNECT connect = decodeInternal(encoded);

        assertEquals("", connect.getUsername());

    }

    @Test
    public void decode_failed_username_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                34,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b1000_0000,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   username
                0, 5, 'h', 'u', 'h', 'u',
        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_will_failed_malformed_properties_length() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                34,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                // will properties
                4,
                //     message expiry interval
                0x02, 0, 0, 0, 10,

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_will_failed_invalid_properties_length_remaining_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                34,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                // will properties
                6,
                //     message expiry interval
                0x02, 0, 0, 0, 10,

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_will_failed_invalid_properties_length_negative() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                29,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                // will properties
                -1,

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_will_failed_invalid_properties_not_enough_bytes() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                28,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_will_failed_by_property_delay_moreThanOnce() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                39,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                10,
                //     will delay interval
                0x18, 0, 0, 0, 5,
                0x18, 0, 0, 0, 5,

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_will_failed_by_property_delay_to_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                33,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                4,
                //     will delay interval
                0x18, 0, 0, 5,

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_will_failed_by_property_payload_format_indicator_illegal() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                31,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                2,
                //   payload format indicator
                0x01, 2

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_will_failed_by_property_payload_format_indicator_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                30,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                1,
                //   payload format indicator
                0x01,

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_will_failed_by_property_payload_format_indicator_moreThanOnce() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                33,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                4,
                //   payload format indicator
                0x01, 1,
                0x01, 1

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_will_failed_by_property_message_expiry_interval_moreThanOnce() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                39,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                10,
                //   message expiry interval
                0x02, 0, 0, 0, 5,
                0x02, 0, 0, 0, 5,

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_will_failed_by_property_message_expiry_interval_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                32,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                3,
                //   message expiry interval
                0x02, 0, 5,

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_property_message_expiry_interval_larger_than_config_max_value() {

        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfig.mqttConfiguration().setMaxMessageExpiryInterval(100);

        channel = new EmbeddedChannel(TestMqttDecoder.create(fullConfig));

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                53,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                19,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //   message expiry interval
                0x02, 0, 0, 0, 110,
                //   will topic
                0, 1, 't',
                //   will payload
                0, 0,

        };

        final CONNECT connect = decodeInternal(encoded);
        assertEquals(110, connect.getWillPublish().getMessageExpiryInterval());

    }

    @Test
    public void decode_will_failed_by_property_content_type_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                31,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                2,
                //   content type
                0x03, 5,

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_will_failed_by_property_content_type_moreThanOnce() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                43,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   content type
                0x03, 0, 4, 't', 'e', 'x', 't',
                0x03, 0, 4, 't', 'e', 'x', 't',

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_will_failed_by_property_response_topic_moreThanOnce() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                51,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                22,
                //   response topic
                0x08, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',
                0x08, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_will_failed_by_property_response_topic_to_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                31,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                2,
                //   response topic
                0x08, 8,

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_will_failed_by_property_response_topic_wild_card_hash() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                40,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                11,
                //   response topic
                0x08, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', '/', '#',

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_will_failed_by_property_response_topic_wild_card_plus() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                40,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                11,
                //   response topic
                0x08, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', '/', '+',

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void decode_will_failed_by_property_correlation_data_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                31,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                2,
                //   correlation data
                0x09, 0,

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_by_property_correlation_data_length_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                33,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                4,
                //   correlation data
                0x09, 0, 5, 1

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_by_property_correlation_data_moreThanOnce() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                37,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                8,
                //   correlation data
                0x09, 0, 1, 1,
                0x09, 0, 1, 1,

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_by_property_user_property_value_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                42,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                13,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u',

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_by_property_user_property_key_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                42,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                13,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 0, 5, 'v', 'a', 'l', 'u', 'e'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_by_property_user_property_to_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                31,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                2,
                //   user property
                0x26, 0,

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_by_property_user_property_key_contains_must_not() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                43,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', (byte) 0xFF, 0, 5, 'v', 'a', 'l', 'u', 'e'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_by_property_user_property_key_contains_should_not() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                43,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 0x7F, 0, 5, 'v', 'a', 'l', 'u', 'e'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_by_property_user_property_value_contains_must_not() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                43,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', (byte) 0xFF

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_by_property_user_property_value_contains_should_not() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                43,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 0x7F

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_by_unknown_property() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                43,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   unknown property
                0x25, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_no_topic() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                43,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_topic_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                50,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //   will topic
                0, 6, 't', 'o', 'p', 'i', 'c',

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_topic_remaining_length() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                44,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //   will topic
                0,

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_topic_empty() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                45,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //   will topic
                0, 0

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_topic_wildcard_plus() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                46,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //   will topic
                0, 1, '+'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_topic_wildcard_hash() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                46,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //   will topic
                0, 1, '#'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_topic_contains_must_not_char() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                46,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //   will topic
                0, 1, 0

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_topic_contains_should_not_char() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                46,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //   will topic
                0, 1, 0x7F

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_no_payload() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                46,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //   will topic
                0, 1, 't'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_failed_payload_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                57,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //   will topic
                0, 1, 't',
                //   will payload
                0, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9,

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_will_payload_empty() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                48,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //   will topic
                0, 1, 't',
                //   will payload
                0, 0,

        };

        final CONNECT connect = decodeInternal(encoded);

        assertEquals(0, connect.getWillPublish().getPayload().length);
    }

    @Test
    public void decode_will_payload_max() {

        final int[] encode = MqttVariableByteInteger.encode(65583);

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                (byte) encode[0], (byte) encode[1], (byte) encode[2],
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //   will topic
                0, 1, 't',
                //   will payload
                (byte) 0xFF, (byte) 0xFF

        };

        final byte[] bytes = Bytes.concat(encoded, new byte[0xFFFF]);

        final CONNECT connect = decodeInternal(bytes);

        assertEquals(65535, connect.getWillPublish().getPayload().length);
    }

    @Test
    public void decode_will_default_will_delay() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                48,
                // protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0010_1100,
                //   keep alive
                0, 0,
                //   properties
                11,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                // payload
                //   client identifier
                0, 4, 'h', 'u', 'h', 'u',
                //   will properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //   will topic
                0, 1, 't',
                //   will payload
                0, 0,

        };

        final CONNECT connect = decodeInternal(encoded);

        assertEquals(WILL_DELAY_INTERVAL_NOT_SET, connect.getWillPublish().getDelayInterval());
    }

    @NotNull
    private CONNECT decodeInternal(final byte[] encoded) {
        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeBytes(encoded);
        channel.writeInbound(byteBuf);

        final CONNECT publishInternal = channel.readInbound();
        assertNotNull(publishInternal);

        return publishInternal;
    }

}