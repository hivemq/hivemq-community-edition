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
package com.hivemq.extensions.packets.pubcomp;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.reason.Mqtt5PubCompReasonCode;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import static org.junit.Assert.*;

/**
 * @author Yannick Weber
 */
public class ModifiablePubcompPacketImplTest {

    private FullConfigurationService fullConfigurationService;
    private ModifiablePubcompPacketImpl modifiablePubcompPacket;
    private PUBCOMP fullMqtt5Pubcomp;

    @Before
    public void setUp() throws Exception {
        fullConfigurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        fullMqtt5Pubcomp = new PUBCOMP(1, Mqtt5PubCompReasonCode.SUCCESS, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
        modifiablePubcompPacket = new ModifiablePubcompPacketImpl(fullConfigurationService, fullMqtt5Pubcomp);
    }

    @Test
    public void test_set_reason_string_to_failed() {
        final PUBCOMP pubcomp = new PUBCOMP(1, Mqtt5PubCompReasonCode.PACKET_IDENTIFIER_NOT_FOUND, null,
                Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubcompPacketImpl
                modifiablePubcompPacket = new ModifiablePubcompPacketImpl(fullConfigurationService, pubcomp);
        modifiablePubcompPacket.setReasonString("reason");
        assertTrue(modifiablePubcompPacket.isModified());
        assertTrue(modifiablePubcompPacket.getReasonString().isPresent());
        assertEquals("reason", modifiablePubcompPacket.getReasonString().get());
    }

    @Test
    public void test_set_reason_string_to_null() {
        final PUBCOMP pubcomp = new PUBCOMP(1, Mqtt5PubCompReasonCode.PACKET_IDENTIFIER_NOT_FOUND, "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubcompPacketImpl
                modifiablePubcompPacket = new ModifiablePubcompPacketImpl(fullConfigurationService, pubcomp);
        modifiablePubcompPacket.setReasonString(null);
        assertTrue(modifiablePubcompPacket.isModified());
        assertFalse(modifiablePubcompPacket.getReasonString().isPresent());
    }

    @Test
    public void test_set_reason_string_to_same() {
        final PUBCOMP pubcomp = new PUBCOMP(1, Mqtt5PubCompReasonCode.PACKET_IDENTIFIER_NOT_FOUND, "same",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubcompPacketImpl
                modifiablePubcompPacket = new ModifiablePubcompPacketImpl(fullConfigurationService, pubcomp);
        modifiablePubcompPacket.setReasonString("same");
        assertFalse(modifiablePubcompPacket.isModified());
    }

    @Test
    public void test_all_values_set() {
        final PubcompPacketImpl pubcompPacket = new PubcompPacketImpl(fullMqtt5Pubcomp);
        assertEquals(fullMqtt5Pubcomp.getPacketIdentifier(), pubcompPacket.getPacketIdentifier());
        assertEquals(fullMqtt5Pubcomp.getReasonCode().name(), pubcompPacket.getReasonCode().name());
        assertFalse(pubcompPacket.getReasonString().isPresent());
        assertEquals(fullMqtt5Pubcomp.getUserProperties().size(), pubcompPacket.getUserProperties().asList().size());
    }

    @Test
    public void test_change_modifiable_does_not_change_copy_of_packet() {
        final PUBCOMP pubcomp = new PUBCOMP(1, Mqtt5PubCompReasonCode.PACKET_IDENTIFIER_NOT_FOUND, "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubcompPacketImpl
                modifiablePubcompPacket = new ModifiablePubcompPacketImpl(fullConfigurationService, pubcomp);

        final PubcompPacketImpl pubcompPacket = new PubcompPacketImpl(modifiablePubcompPacket);

        modifiablePubcompPacket.setReasonString("OTHER REASON STRING");

        assertTrue(pubcompPacket.getReasonString().isPresent());
        assertEquals(pubcomp.getReasonString(), pubcompPacket.getReasonString().get());
        assertEquals(pubcomp.getReasonCode().name(), pubcompPacket.getReasonCode().name());
    }
}