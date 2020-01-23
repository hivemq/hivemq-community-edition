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
package com.hivemq.extensions.packets.puback;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.reason.Mqtt5PubAckReasonCode;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import static org.junit.Assert.*;

/**
 * @author Yannick Weber
 */
public class ModifiablePubackPacketImplTest {

    private FullConfigurationService fullConfigurationService;
    private ModifiablePubackPacketImpl modifiablePubackPacket;
    private PUBACK fullMqtt5Puback;

    @Before
    public void setUp() throws Exception {
        fullConfigurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        fullMqtt5Puback = new PUBACK(1, Mqtt5PubAckReasonCode.SUCCESS, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
        modifiablePubackPacket = new ModifiablePubackPacketImpl(fullConfigurationService, fullMqtt5Puback);
    }

    @Test
    public void test_set_reason_string_to_failed() {
        final PUBACK puback = new PUBACK(1, Mqtt5PubAckReasonCode.UNSPECIFIED_ERROR, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubackPacketImpl
                modifiablePubackPacket = new ModifiablePubackPacketImpl(fullConfigurationService, puback);
        modifiablePubackPacket.setReasonString("reason");
        assertTrue(modifiablePubackPacket.isModified());
        assertTrue(modifiablePubackPacket.getReasonString().isPresent());
        assertEquals("reason", modifiablePubackPacket.getReasonString().get());
    }

    @Test
    public void test_set_reason_string_to_null() {
        final PUBACK puback = new PUBACK(1, Mqtt5PubAckReasonCode.UNSPECIFIED_ERROR, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubackPacketImpl
                modifiablePubackPacket = new ModifiablePubackPacketImpl(fullConfigurationService, puback);
        modifiablePubackPacket.setReasonString(null);
        assertTrue(modifiablePubackPacket.isModified());
        assertFalse(modifiablePubackPacket.getReasonString().isPresent());
    }

    @Test
    public void test_set_reason_string_to_same() {
        final PUBACK puback = new PUBACK(1, Mqtt5PubAckReasonCode.UNSPECIFIED_ERROR, "same", Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubackPacketImpl
                modifiablePubackPacket = new ModifiablePubackPacketImpl(fullConfigurationService, puback);
        modifiablePubackPacket.setReasonString("same");
        assertFalse(modifiablePubackPacket.isModified());
    }

    @Test(expected = IllegalStateException.class)
    public void test_switch_reason_code_to_failed_code() {
        modifiablePubackPacket.setReasonCode(AckReasonCode.UNSPECIFIED_ERROR);
    }

    @Test(expected = IllegalStateException.class)
    public void test_switch_reason_code_from_failed_code() {
        final PUBACK puback = new PUBACK(1, Mqtt5PubAckReasonCode.UNSPECIFIED_ERROR, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubackPacketImpl
                modifiablePubackPacket = new ModifiablePubackPacketImpl(fullConfigurationService, puback);
        modifiablePubackPacket.setReasonCode(AckReasonCode.SUCCESS);
    }

    @Test
    public void test_switch_reason_code_from_failed_to_same_failed() {
        final PUBACK puback = new PUBACK(1, Mqtt5PubAckReasonCode.UNSPECIFIED_ERROR, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubackPacketImpl
                modifiablePubackPacket = new ModifiablePubackPacketImpl(fullConfigurationService, puback);
        modifiablePubackPacket.setReasonCode(AckReasonCode.UNSPECIFIED_ERROR);
        assertFalse(modifiablePubackPacket.isModified());
    }

    @Test
    public void test_switch_reason_code_from_failed_to_failed() {
        final PUBACK puback = new PUBACK(1, Mqtt5PubAckReasonCode.UNSPECIFIED_ERROR, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubackPacketImpl
                modifiablePubackPacket = new ModifiablePubackPacketImpl(fullConfigurationService, puback);
        modifiablePubackPacket.setReasonCode(AckReasonCode.NOT_AUTHORIZED);
        assertTrue(modifiablePubackPacket.isModified());
        assertEquals(AckReasonCode.NOT_AUTHORIZED, modifiablePubackPacket.getReasonCode());
    }

    @Test
    public void test_all_values_set() {
        final PubackPacketImpl pubackPacket = new PubackPacketImpl(fullMqtt5Puback);
        assertEquals(fullMqtt5Puback.getPacketIdentifier(), pubackPacket.getPacketIdentifier());
        assertEquals(fullMqtt5Puback.getReasonCode().name(), pubackPacket.getReasonCode().name());
        assertFalse(pubackPacket.getReasonString().isPresent());
        assertEquals(fullMqtt5Puback.getUserProperties().size(), pubackPacket.getUserProperties().asList().size());
    }

    @Test
    public void test_change_modifiable_does_not_change_copy_of_packet() {
        final PUBACK puback = new PUBACK(1, Mqtt5PubAckReasonCode.UNSPECIFIED_ERROR, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubackPacketImpl
                modifiablePubackPacket = new ModifiablePubackPacketImpl(fullConfigurationService, puback);

        final PubackPacketImpl pubackPacket = new PubackPacketImpl(modifiablePubackPacket);

        modifiablePubackPacket.setReasonCode(AckReasonCode.NOT_AUTHORIZED);
        modifiablePubackPacket.setReasonString("OTHER REASON STRING");

        assertTrue(pubackPacket.getReasonString().isPresent());
        assertEquals(puback.getReasonString(), pubackPacket.getReasonString().get());
        assertEquals(puback.getReasonCode().name(), pubackPacket.getReasonCode().name());
    }
}