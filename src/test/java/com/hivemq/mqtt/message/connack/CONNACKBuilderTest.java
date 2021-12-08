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
package com.hivemq.mqtt.message.connack;

import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.hivemq.mqtt.message.connack.CONNACK.KEEP_ALIVE_NOT_SET;
import static com.hivemq.mqtt.message.connack.CONNACK.SESSION_EXPIRY_NOT_SET;
import static org.junit.Assert.*;

/**
 * @author Waldemar Ruck
 * @since 4.0
 */
public class CONNACKBuilderTest {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    private final CONNACK.Mqtt5Builder builder = new CONNACK.Mqtt5Builder().withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS);
    private final String dataExceeded = new String(new char[65535 + 1]);
    private final int sizeExceeded = 65535 + 1;

    @Test
    public void test_builder_default_values() {

        final CONNACK connack = builder.build();

        assertEquals(connack.getReturnCode(), Mqtt3ConnAckReturnCode.ACCEPTED);
        assertNull(connack.getAssignedClientIdentifier());
        assertNull(connack.getAuthData());
        assertNull(connack.getAuthMethod());
        assertEquals(connack.getMaximumPacketSize(), CONNECT.DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT);
        assertNull(connack.getMaximumQoS());
        assertEquals(connack.getReceiveMaximum(), CONNECT.DEFAULT_RECEIVE_MAXIMUM);
        assertNull(connack.getResponseInformation());
        assertEquals(connack.getServerKeepAlive(), KEEP_ALIVE_NOT_SET);
        assertNull(connack.getServerReference());
        assertEquals(connack.getSessionExpiryInterval(), SESSION_EXPIRY_NOT_SET);
        assertEquals(connack.getTopicAliasMaximum(), CONNECT.DEFAULT_TOPIC_ALIAS_MAXIMUM);
        assertEquals(connack.getType(), MessageType.CONNACK);
        assertEquals(connack.getPacketIdentifier(), 0);
        assertEquals(connack.getReasonCode(), Mqtt5ConnAckReasonCode.SUCCESS);
        assertEquals(connack.getUserProperties().asList().size(), 0);
        assertFalse(connack.isSessionPresent());
        assertNull(connack.getReasonString());
        assertTrue(connack.isRetainAvailable());
        assertTrue(connack.isSharedSubscriptionAvailable());
        assertTrue(connack.isSubscriptionIdentifierAvailable());
        assertTrue(connack.isWildcardSubscriptionAvailable());
    }

    @Test
    public void test_builder_set_values() {

        final int serverKeepAlive = 30;
        final int topicAliasMaximum = 5;
        final int maximumPacketSize = 300;
        final long sessionExpiryInterval = 100;
        final String serverReference = "HiveMQ 4";
        final String responseInformation = "INFO";
        final byte[] authData = new byte[65535];
        final String authMethod = "Method";
        final int receiveMaximum = 100;
        final QoS maximumQoS = QoS.AT_MOST_ONCE;
        final boolean sessionPresent = true;
        final String assignedClientIdentifier = "subscriber";
        final String reasonString = "human readable ...";
        final boolean retainAvailable = true;
        final boolean sharedSubscriptionAvailable = true;
        final boolean subscriptionIdentifierAvailable = true;

        final MqttUserProperty userProperty = new MqttUserProperty("test1", "value");
        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(userProperty, userProperty);

        final boolean wildcardSubscriptionAvailable = true;

        final CONNACK connack = builder
                .withServerKeepAlive(serverKeepAlive)
                .withTopicAliasMaximum(topicAliasMaximum)
                .withMaximumPacketSize(maximumPacketSize)
                .withSessionExpiryInterval(sessionExpiryInterval)
                .withServerReference(serverReference)
                .withResponseInformation(responseInformation)
                .withAuthData(authData)
                .withAuthMethod(authMethod)
                .withReceiveMaximum(receiveMaximum)
                .withMaximumQoS(maximumQoS)
                .withSessionPresent(sessionPresent)
                .withAssignedClientIdentifier(assignedClientIdentifier)
                .withReasonString(reasonString)
                .withRetainAvailable(retainAvailable)
                .withSharedSubscriptionAvailable(sharedSubscriptionAvailable)
                .withSubscriptionIdentifierAvailable(subscriptionIdentifierAvailable)
                .withUserProperties(userProperties)
                .withWildcardSubscriptionAvailable(wildcardSubscriptionAvailable)
                .build();

        assertEquals(connack.getServerKeepAlive(), serverKeepAlive);
        assertEquals(connack.getTopicAliasMaximum(), topicAliasMaximum);
        assertEquals(connack.getMaximumPacketSize(), maximumPacketSize);
        assertEquals(connack.getSessionExpiryInterval(), sessionExpiryInterval);
        assertEquals(connack.getServerReference(), serverReference);
        assertEquals(connack.getResponseInformation(), responseInformation);
        assertEquals(connack.getAuthData(), authData);
        assertEquals(connack.getAuthMethod(), authMethod);
        assertEquals(connack.getReceiveMaximum(), receiveMaximum);
        assertEquals(connack.getMaximumQoS(), maximumQoS);
        assertEquals(connack.isSessionPresent(), sessionPresent);
        assertEquals(connack.getAssignedClientIdentifier(), assignedClientIdentifier);
        assertEquals(connack.getReasonString(), reasonString);
        assertEquals(connack.isRetainAvailable(), retainAvailable);
        assertEquals(connack.isSharedSubscriptionAvailable(), sharedSubscriptionAvailable);
        assertEquals(connack.isSubscriptionIdentifierAvailable(), subscriptionIdentifierAvailable);
        assertEquals(connack.getUserProperties(), userProperties);
        assertEquals(connack.isWildcardSubscriptionAvailable(), wildcardSubscriptionAvailable);

        assertEquals(connack.getReturnCode(), Mqtt3ConnAckReturnCode.ACCEPTED);
        assertEquals(connack.getType(), MessageType.CONNACK);
        assertEquals(connack.getPacketIdentifier(), 0);
        assertEquals(connack.getReasonCode(), Mqtt5ConnAckReasonCode.SUCCESS);
    }

    @Test
    public void test_receiveMaximum_precondition() {
        checkForException("Receive maximum must never be zero");
        builder.withReceiveMaximum(0).build();
    }

    @Test
    public void test_authMethod_precondition() {
        checkForException("An auth method must never exceed 65.535 bytes");
        builder.withAuthMethod(dataExceeded).build();
    }

    @Test
    public void test_authData_method_precondition() {
        exceptionRule.expect(NullPointerException.class);
        exceptionRule.expectMessage("Auth method must be set if auth data is set");

        final byte[] dataExceeded = new byte[65535];
        builder.withAuthData(dataExceeded).build();
    }

    @Test
    public void test_authData_precondition() {
        checkForException("An auth data must never exceed 65.535 bytes");
        final byte[] dataExceeded = new byte[sizeExceeded];
        builder.withAuthMethod("Method").withAuthData(dataExceeded).build();
    }

    @Test
    public void test_responseInformation_precondition() {
        checkForException("A response information must never exceed 65.535 bytes");
        builder.withResponseInformation(dataExceeded).build();
    }

    @Test
    public void test_serverReference_precondition() {
        checkForException("A server reference must never exceed 65.535 bytes");
        builder.withServerReference(dataExceeded).build();
    }

    @Test
    public void test_sessionExpiryInterval_precondition() {
        checkForException("A session expiry interval must never be larger than 4.294.967.296");
        builder.withSessionExpiryInterval(4294967296L + 1).build();
    }

    @Test
    public void test_maximumPacketSize_precondition() {
        checkForException("A maximum packet size must never be larger than 268.435.460");
        builder.withMaximumPacketSize(268435460 + 1).build();
    }

    @Test
    public void test_topicAliasMaximum_precondition() {
        checkForException("A topic alias maximum must never be larger than 65.535");
        builder.withTopicAliasMaximum(sizeExceeded).build();
    }

    @Test
    public void test_serverKeepAlive_precondition() {
        checkForException("A server keep alive must never be larger than 65.535");
        builder.withServerKeepAlive(sizeExceeded).build();
    }

    private void checkForException(final String message) {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(message);
    }
}