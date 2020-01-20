/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.extensions.packets.connack;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connect.Mqtt5CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class ModifiableConnackPacketImplTest {

    private ModifiableConnackPacketImpl modifiableConnackPacket;
    private FullConfigurationService fullConfigurationService;
    private CONNACK fullMqtt5Connack;

    @Before
    public void setUp() throws Exception {
        fullConfigurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        fullMqtt5Connack = TestMessageUtil.createFullMqtt5Connack();
        modifiableConnackPacket = new ModifiableConnackPacketImpl(fullConfigurationService, fullMqtt5Connack, true);
    }

    @Test(expected = IllegalStateException.class)
    public void test_set_reason_string_to_success_code() {
        modifiableConnackPacket.setReasonString("reason");
    }

    @Test
    public void test_set_reason_string_to_failed() {
        fullMqtt5Connack = new CONNACK.Mqtt5Builder().withReasonCode(Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR).build();
        modifiableConnackPacket = new ModifiableConnackPacketImpl(fullConfigurationService, fullMqtt5Connack, true);
        modifiableConnackPacket.setReasonString("reason");
        assertTrue(modifiableConnackPacket.isModified());
        assertEquals("reason", modifiableConnackPacket.getReasonString().get());
    }

    @Test
    public void test_set_reason_string_to_null() {
        fullMqtt5Connack = new CONNACK.Mqtt5Builder().withReasonCode(Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR).withReasonString("reason").build();
        modifiableConnackPacket = new ModifiableConnackPacketImpl(fullConfigurationService, fullMqtt5Connack, true);
        modifiableConnackPacket.setReasonString(null);
        assertTrue(modifiableConnackPacket.isModified());
        assertEquals(false, modifiableConnackPacket.getReasonString().isPresent());
    }

    @Test
    public void test_set_reason_string_to_null_from_success() {
        fullMqtt5Connack = new CONNACK.Mqtt5Builder().withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS).withReasonString("reason").build();
        modifiableConnackPacket = new ModifiableConnackPacketImpl(fullConfigurationService, fullMqtt5Connack, true);
        modifiableConnackPacket.setReasonString(null);
        assertTrue(modifiableConnackPacket.isModified());
        assertEquals(false, modifiableConnackPacket.getReasonString().isPresent());
    }

    @Test
    public void test_set_reason_string_same() {
        fullMqtt5Connack = new CONNACK.Mqtt5Builder().withReasonCode(Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR).withReasonString("success").build();
        modifiableConnackPacket = new ModifiableConnackPacketImpl(fullConfigurationService, fullMqtt5Connack, true);
        modifiableConnackPacket.setReasonString("success");
        assertFalse(modifiableConnackPacket.isModified());
    }

    @Test(expected = IllegalStateException.class)
    public void test_switch_reason_code_to_failed_code() {
        modifiableConnackPacket.setReasonCode(ConnackReasonCode.UNSPECIFIED_ERROR);
    }

    @Test(expected = IllegalStateException.class)
    public void test_switch_reason_code_to_success_code() {
        fullMqtt5Connack = new CONNACK.Mqtt5Builder().withReasonCode(Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR).build();
        modifiableConnackPacket = new ModifiableConnackPacketImpl(fullConfigurationService, fullMqtt5Connack, true);
        modifiableConnackPacket.setReasonCode(ConnackReasonCode.SUCCESS);
    }

    @Test
    public void test_switch_reason_code_to_failed_from_failed_code() {
        fullMqtt5Connack = new CONNACK.Mqtt5Builder().withReasonCode(Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR).build();
        modifiableConnackPacket = new ModifiableConnackPacketImpl(fullConfigurationService, fullMqtt5Connack, true);
        modifiableConnackPacket.setReasonCode(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD);
        assertTrue(modifiableConnackPacket.isModified());
        assertEquals(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD, modifiableConnackPacket.getReasonCode());
    }

    @Test
    public void test_switch_reason_code_to_same_failed_from_failed_code() {
        fullMqtt5Connack = new CONNACK.Mqtt5Builder().withReasonCode(Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR).build();
        modifiableConnackPacket = new ModifiableConnackPacketImpl(fullConfigurationService, fullMqtt5Connack, true);
        modifiableConnackPacket.setReasonCode(ConnackReasonCode.UNSPECIFIED_ERROR);
        assertFalse(modifiableConnackPacket.isModified());
    }

    @Test
    public void test_set_response_information() {
        modifiableConnackPacket.setResponseInformation("response-response");
        assertEquals("response-response", modifiableConnackPacket.getResponseInformation().get());
        assertTrue(modifiableConnackPacket.isModified());
    }

    @Test
    public void test_set_response_information_same() {
        modifiableConnackPacket.setResponseInformation(fullMqtt5Connack.getResponseInformation());
        assertFalse(modifiableConnackPacket.isModified());
    }

    @Test(expected = IllegalStateException.class)
    public void test_set_response_information_not_allowed() {
        modifiableConnackPacket = new ModifiableConnackPacketImpl(fullConfigurationService, fullMqtt5Connack, false);
        modifiableConnackPacket.setResponseInformation("response-response");
    }

    @Test
    public void test_set_server_reference() {
        modifiableConnackPacket.setServerReference("server-reference-test");
        assertEquals("server-reference-test", modifiableConnackPacket.getServerReference().get());
        assertTrue(modifiableConnackPacket.isModified());
    }

    @Test
    public void test_set_server_reference_same() {
        modifiableConnackPacket.setServerReference(fullMqtt5Connack.getServerReference());
        assertFalse(modifiableConnackPacket.isModified());
    }

    @Test
    public void test_add_user_prop() {
        modifiableConnackPacket.getUserProperties().addUserProperty("user", "prop");
        assertTrue(modifiableConnackPacket.isModified());
    }

    @Test
    public void test_all_values_set() {

        final ConnackPacketImpl connackPacket = new ConnackPacketImpl(fullMqtt5Connack);

        assertEquals(fullMqtt5Connack.getSessionExpiryInterval(), connackPacket.getSessionExpiryInterval().orElse(Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET).longValue());
        assertEquals(fullMqtt5Connack.getServerKeepAlive(), connackPacket.getServerKeepAlive().orElse(Mqtt5CONNECT.KEEP_ALIVE_NOT_SET).intValue());
        assertEquals(fullMqtt5Connack.getReceiveMaximum(), connackPacket.getReceiveMaximum());
        assertEquals(fullMqtt5Connack.getMaximumPacketSize(), connackPacket.getMaximumPacketSize());
        assertEquals(fullMqtt5Connack.getTopicAliasMaximum(), connackPacket.getTopicAliasMaximum());
        assertEquals(fullMqtt5Connack.getMaximumQoS().getQosNumber(), connackPacket.getMaximumQoS().get().getQosNumber());
        assertEquals(fullMqtt5Connack.getUserProperties().size(), connackPacket.getUserProperties().asList().size());
        assertEquals(fullMqtt5Connack.getReasonCode(), Mqtt5ConnAckReasonCode.from(connackPacket.getReasonCode()));
        assertEquals(fullMqtt5Connack.isSessionPresent(), connackPacket.getSessionPresent());
        assertEquals(fullMqtt5Connack.isRetainAvailable(), connackPacket.getRetainAvailable());
        assertEquals(fullMqtt5Connack.getAssignedClientIdentifier(), connackPacket.getAssignedClientIdentifier().orElse(null));
        assertEquals(fullMqtt5Connack.getReasonString(), connackPacket.getReasonString().orElse(null));
        assertEquals(fullMqtt5Connack.isWildcardSubscriptionAvailable(), connackPacket.getWildCardSubscriptionAvailable());
        assertEquals(fullMqtt5Connack.isSubscriptionIdentifierAvailable(), connackPacket.getSubscriptionIdentifiersAvailable());
        assertEquals(fullMqtt5Connack.isSharedSubscriptionAvailable(), connackPacket.getSharedSubscriptionsAvailable());
        assertEquals(fullMqtt5Connack.getResponseInformation(), connackPacket.getResponseInformation().orElse(null));
        assertEquals(fullMqtt5Connack.getServerReference(), connackPacket.getServerReference().orElse(null));
        assertArrayEquals(fullMqtt5Connack.getAuthData(), connackPacket.getAuthenticationData().orElse(null).array());
        assertEquals(fullMqtt5Connack.getAuthMethod(), connackPacket.getAuthenticationMethod().orElse(null));

    }

    @Test
    public void test_change_mofifiable_does_not_change_copy_of_packet() {

        fullMqtt5Connack = new CONNACK.Mqtt5Builder()
                .withServerReference("ref")
                .withResponseInformation("info")
                .withReasonString("reason")
                .withReasonCode(Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR)
                .build();
        modifiableConnackPacket = new ModifiableConnackPacketImpl(fullConfigurationService, fullMqtt5Connack, true);

        final ConnackPacketImpl connackPacket2 = new ConnackPacketImpl(modifiableConnackPacket);

        modifiableConnackPacket.setResponseInformation("OTHER_INFO");
        modifiableConnackPacket.setReasonCode(ConnackReasonCode.BAD_AUTHENTICATION_METHOD);
        modifiableConnackPacket.setReasonString("OTHER_STRING");
        modifiableConnackPacket.setServerReference("OTHER_REFERENCE");

        assertTrue(connackPacket2.getResponseInformation().get().equals(fullMqtt5Connack.getResponseInformation()));
        assertTrue(connackPacket2.getServerReference().get().equals(fullMqtt5Connack.getServerReference()));
        assertTrue(connackPacket2.getReasonString().get().equals(fullMqtt5Connack.getReasonString()));
        assertTrue(connackPacket2.getReasonCode().name().equals(fullMqtt5Connack.getReasonCode().name()));
    }
}