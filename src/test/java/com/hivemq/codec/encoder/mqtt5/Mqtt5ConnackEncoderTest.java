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
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connect.Mqtt5CONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static com.hivemq.mqtt.message.connack.Mqtt5CONNACK.*;
import static com.hivemq.mqtt.message.connack.CONNACK.KEEP_ALIVE_NOT_SET;
import static com.hivemq.mqtt.message.connack.CONNACK.SESSION_EXPIRY_NOT_SET;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class Mqtt5ConnackEncoderTest extends AbstractMqtt5EncoderTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        testMessageEncoder.getSecurityConfigurationService().setAllowRequestProblemInformation(true);
    }

    @Test
    public void test_all_props() {

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setMaxPacketSizeSend(150L);

        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0010_0000,
                //   remaining length (138)
                (byte) (128 + 10), 1,
                // variable header
                //   connack flags
                0b0000_0001,
                //   reason code (success)
                0x00,
                //   properties (134)
                (byte) (128 + 6), 1,
                //     session expiry interval
                0x11, 0, 0, 0, 10,
                //     receive maximum
                0x21, 0, 100,
                //     maximum qos
                0x24, 1,
                //     retain available
                0x25, 0,
                //     maximum packet size
                0x27, 0, 0, 0, 100,
                //     assigned client identifier
                0x12, 0, 4, 't', 'e', 's', 't',
                //     topic alias maximum
                0x22, 0, 5,
                //     wildcard subscription available
                0x28, 0,
                //     subscription identifiers available
                0x29, 0,
                //     shared subscription available
                0x2A, 0,
                //     server keep alive
                0x13, 0, 10,
                //     response information
                0x1A, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',
                //     server reference
                0x1C, 0, 6, 's', 'e', 'r', 'v', 'e', 'r',
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                //     auth data
                0x16, 0, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                //     reason string
                0x1F, 0, 7, 's', 'u', 'c', 'c', 'e', 's', 's',
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 4, 't', 'e', 's', 't', 0, 6, 'v', 'a', 'l', 'u', 'e', '2', //
                0x26, 0, 5, 't', 'e', 's', 't', '2', 0, 5, 'v', 'a', 'l', 'u', 'e', //
        };

        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(
                new MqttUserProperty("test", "value"),
                new MqttUserProperty("test", "value2"),
                new MqttUserProperty("test2", "value"));

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString("success")
                .withUserProperties(userProperties)
                .withSessionPresent(true)
                .withSessionExpiryInterval(10)
                .withServerKeepAlive(10)
                .withAssignedClientIdentifier("test")
                .withAuthMethod("GS2-KRB5")
                .withAuthData(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
                .withReceiveMaximum(100)
                .withTopicAliasMaximum(5)
                .withMaximumPacketSize(100)
                .withMaximumQoS(QoS.AT_LEAST_ONCE)
                .withRetainAvailable(false)
                .withWildcardSubscriptionAvailable(false)
                .withSubscriptionIdentifierAvailable(false)
                .withSharedSubscriptionAvailable(false)
                .withResponseInformation("response")
                .withServerReference("server")
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_minimum() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                3,
                // variable header
                //   session present
                0,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                0
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(false)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(null)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_reason_string() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                12,
                // variable header
                //   session present
                0,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                9,
                // reason string
                0x1F, 0, 6, 'r', 'e', 'a', 's', 'o', 'n'
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString("reason")
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(false)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(null)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_reason_string_request_problem_information_false() {

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setRequestProblemInformation(false);

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                12,
                // variable header
                //   session present
                0,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                9,
                // reason string
                0x1F, 0, 6, 'r', 'e', 'a', 's', 'o', 'n'
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString("reason")
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(false)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(null)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        // Do not omit reason string because it is a CONNACK packet!
        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_user_props_request_problem_information_false() {

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setRequestProblemInformation(false);

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                47,
                // variable header
                //   session present
                0,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                44,
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 4, 't', 'e', 's', 't', 0, 6, 'v', 'a', 'l', 'u', 'e', '2', //
                0x26, 0, 5, 't', 'e', 's', 't', '2', 0, 5, 'v', 'a', 'l', 'u', 'e', //
        };

        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(
                new MqttUserProperty("test", "value"),
                new MqttUserProperty("test", "value2"),
                new MqttUserProperty("test2", "value"));

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(userProperties)
                .withSessionPresent(false)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(null)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        // Do not omit user properties because it is a CONNACK packet!
        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_user_props() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                47,
                // variable header
                //   session present
                0,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                44,
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 4, 't', 'e', 's', 't', 0, 6, 'v', 'a', 'l', 'u', 'e', '2', //
                0x26, 0, 5, 't', 'e', 's', 't', '2', 0, 5, 'v', 'a', 'l', 'u', 'e', //
        };

        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(
                new MqttUserProperty("test", "value"),
                new MqttUserProperty("test", "value2"),
                new MqttUserProperty("test2", "value"));

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(userProperties)
                .withSessionPresent(false)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(null)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_session_present() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                3,
                // variable header
                //   session present
                1,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                0
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(true)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(null)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_session_expiry_interval() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                8,
                // variable header
                //   session present
                1,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                5,
                // session expiry interval
                0x11, 0, 0, 0, 100
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(true)
                .withSessionExpiryInterval(100)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(null)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_receive_maximum() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                6,
                // variable header
                //   session present
                1,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                3,
                // receive maximum
                0x21, 0, 100
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(true)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(100)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(null)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_max_qos() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                5,
                // variable header
                //   session present
                1,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                2,
                // maximum qos
                0x24, 1
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(true)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(QoS.AT_LEAST_ONCE)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_max_qos_2_not_encoded() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                3,
                // variable header
                //   session present
                1,
                //   reason code
                (byte) 0x00,
                // properties length
                0
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(true)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(QoS.EXACTLY_ONCE)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_retain_available() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                5,
                // variable header
                //   session present
                1,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                2,
                // retain available
                0x25, 0
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(true)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(null)
                .withRetainAvailable(false)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_wildcard_subs_available() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                5,
                // variable header
                //   session present
                1,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                2,
                // wildcard subs available
                0x28, 0
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(true)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(null)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(false)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_shared_subs_available() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                5,
                // variable header
                //   session present
                1,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                2,
                // shared subs available
                0x2A, 0
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(true)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(null)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(false)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_sub_ids_available() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                5,
                // variable header
                //   session present
                1,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                2,
                // sub identifier available
                0x29, 0
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(true)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(null)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(false)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_maximum_packet_size() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                8,
                // variable header
                //   session present
                1,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                5,
                // maximum packet size
                0x27, 0, 0, 0, 100
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(true)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(100)
                .withMaximumQoS(null)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_assigned_client_id() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                10,
                // variable header
                //   session present
                1,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                7,
                // maximum packet size
                0x12, 0, 4, 't', 'e', 's', 't'
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(true)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier("test")
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(Mqtt5CONNECT.DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(null)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_topic_alias_maximum() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                6,
                // variable header
                //   session present
                1,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                3,
                // topic alias maximum
                0x22, 0, 5,
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(true)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(5)
                .withMaximumPacketSize(Mqtt5CONNECT.DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(null)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_server_keep_alive() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                6,
                // variable header
                //   session present
                1,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                3,
                // server keep alive
                0x13, 0, 120,
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(true)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(120)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(null)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_response_information() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                14,
                // variable header
                //   session present
                1,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                11,
                // response information
                0x1A, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(true)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(null)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation("response")
                .withServerReference(null)
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_server_reference() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                12,
                // variable header
                //   session present
                1,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                9,
                // server reference
                0x1C, 0, 6, 's', 'e', 'r', 'v', 'e', 'r',
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(true)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod(null)
                .withAuthData(null)
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(null)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference("server")
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void test_auth() {

        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                27,
                //   session present
                1,
                //   reason code Malformed Packet
                (byte) 0x00,
                // properties length
                24,
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                //     auth data
                0x16, 0, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        };

        final CONNACK connack = new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString(null)
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                .withSessionPresent(true)
                .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                .withAssignedClientIdentifier(null)
                .withAuthMethod("GS2-KRB5")
                .withAuthData(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
                .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                .withMaximumQoS(null)
                .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                .withResponseInformation(null)
                .withServerReference(null)
                .build();

        encodeTestBufferSize(expected, connack);
    }

    @Test
    public void encode_allReasonCodes() {

        for (final Mqtt5ConnAckReasonCode reasonCode : Mqtt5ConnAckReasonCode.values()) {

            final byte[] expected = {
                    // fixed header
                    //   type, flags
                    (byte) 0b0010_0000,
                    // remaining length
                    3,
                    // variable header
                    //   session present
                    0,
                    //   reason code placeholder
                    (byte) 0xFF,
                    // properties length
                    0
            };

            expected[3] = (byte) reasonCode.getCode();
            final CONNACK connack = new CONNACK.Mqtt5Builder()
                    .withReasonCode(reasonCode)
                    .withReasonString(null)
                    .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                    .withSessionPresent(false)
                    .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                    .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                    .withAssignedClientIdentifier(null)
                    .withAuthMethod(null)
                    .withAuthData(null)
                    .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                    .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                    .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                    .withMaximumQoS(null)
                    .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                    .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                    .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                    .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                    .withResponseInformation(null)
                    .withServerReference(null)
                    .build();

            encodeTestBufferSize(expected, connack);
        }
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

        final ByteBuf expected = Unpooled.buffer(userPropertiesLength + 5, userPropertiesLength + 5);

        // fixed header
        // type, reserved
        expected.writeByte(0b0010_0000);
        // remaining length (1 + 4 + (userPropertyBytes * maxPossibleUserPropertiesCount = 121
        expected.writeByte(userPropertiesLength + 3);
        // session present
        expected.writeByte(0b0000_0000);
        // reason code
        expected.writeByte((byte) 0x81);
        // properties length
        expected.writeByte(userPropertiesLength);
        // user properties
        maxUserProperties.encode(expected);

        final CONNACK connack =
                new CONNACK.Mqtt5Builder()
                        .withReasonCode(Mqtt5ConnAckReasonCode.MALFORMED_PACKET)
                        .withReasonString(reasonString)
                        .withUserProperties(maxUserProperties)
                        .withSessionPresent(false)
                        .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                        .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                        .withAssignedClientIdentifier(null)
                        .withAuthMethod(null)
                        .withAuthData(null)
                        .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                        .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                        .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                        .withMaximumQoS(null)
                        .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                        .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                        .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                        .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                        .withResponseInformation(null)
                        .withServerReference(null)
                        .build();

        encodeTestBufferSize(expected.array(), connack);
        expected.release();
    }

    @Test
    public void encode_maximumPacketSizeExceeded_omitUserProperties() {
        final int maxPacketSize = 130;
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setMaxPacketSizeSend((long) maxPacketSize);

        final MaximumPacketBuilder maxPacket = new MaximumPacketBuilder().build(maxPacketSize);
        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0010_0000,
                // remaining length
                3,
                // session present
                0b0000_0000,
                // reason code
                (byte) 0x82,
                // property length
                0
        };

        final Mqtt5UserProperties userProperties = getUserProperties(maxPacket.getMaxUserPropertiesCount() + 1);

        final CONNACK connack =
                new CONNACK.Mqtt5Builder()
                        .withReasonCode(Mqtt5ConnAckReasonCode.PROTOCOL_ERROR)
                        .withReasonString(null)
                        .withUserProperties(userProperties)
                        .withSessionPresent(false)
                        .withSessionExpiryInterval(SESSION_EXPIRY_NOT_SET)
                        .withServerKeepAlive(KEEP_ALIVE_NOT_SET)
                        .withAssignedClientIdentifier(null)
                        .withAuthMethod(null)
                        .withAuthData(null)
                        .withReceiveMaximum(DEFAULT_RECEIVE_MAXIMUM)
                        .withTopicAliasMaximum(DEFAULT_TOPIC_ALIAS_MAXIMUM)
                        .withMaximumPacketSize(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT)
                        .withMaximumQoS(null)
                        .withRetainAvailable(DEFAULT_RETAIN_AVAILABLE)
                        .withWildcardSubscriptionAvailable(DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE)
                        .withSubscriptionIdentifierAvailable(DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE)
                        .withSharedSubscriptionAvailable(DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE)
                        .withResponseInformation(null)
                        .withServerReference(null)
                        .build();

        encodeTestBufferSize(expected, connack);
    }
}