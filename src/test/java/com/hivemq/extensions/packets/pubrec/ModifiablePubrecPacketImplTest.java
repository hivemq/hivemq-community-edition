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
package com.hivemq.extensions.packets.pubrec;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import static org.junit.Assert.*;

/**
 * @author Yannick Weber
 */
public class ModifiablePubrecPacketImplTest {

    private FullConfigurationService fullConfigurationService;
    private ModifiablePubrecPacketImpl modifiablePubrecPacket;
    private PUBREC fullMqtt5Pubrec;

    @Before
    public void setUp() throws Exception {
        fullConfigurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        fullMqtt5Pubrec = new PUBREC(1, Mqtt5PubRecReasonCode.SUCCESS, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
        modifiablePubrecPacket = new ModifiablePubrecPacketImpl(fullConfigurationService, fullMqtt5Pubrec);
    }

    @Test
    public void test_set_reason_string_to_failed() {
        final PUBREC pubrec =
                new PUBREC(1, Mqtt5PubRecReasonCode.UNSPECIFIED_ERROR, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubrecPacketImpl
                modifiablePubrecPacket = new ModifiablePubrecPacketImpl(fullConfigurationService, pubrec);
        modifiablePubrecPacket.setReasonString("reason");
        assertTrue(modifiablePubrecPacket.isModified());
        assertTrue(modifiablePubrecPacket.getReasonString().isPresent());
        assertEquals("reason", modifiablePubrecPacket.getReasonString().get());
    }

    @Test
    public void test_set_reason_string_to_null() {
        final PUBREC pubrec = new PUBREC(1, Mqtt5PubRecReasonCode.UNSPECIFIED_ERROR, "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubrecPacketImpl
                modifiablePubrecPacket = new ModifiablePubrecPacketImpl(fullConfigurationService, pubrec);
        modifiablePubrecPacket.setReasonString(null);
        assertTrue(modifiablePubrecPacket.isModified());
        assertFalse(modifiablePubrecPacket.getReasonString().isPresent());
    }

    @Test
    public void test_set_reason_string_to_same() {
        final PUBREC pubrec =
                new PUBREC(1, Mqtt5PubRecReasonCode.UNSPECIFIED_ERROR, "same", Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubrecPacketImpl
                modifiablePubrecPacket = new ModifiablePubrecPacketImpl(fullConfigurationService, pubrec);
        modifiablePubrecPacket.setReasonString("same");
        assertFalse(modifiablePubrecPacket.isModified());
    }

    @Test(expected = IllegalStateException.class)
    public void test_switch_reason_code_to_failed_code() {
        modifiablePubrecPacket.setReasonCode(AckReasonCode.UNSPECIFIED_ERROR);
    }

    @Test(expected = IllegalStateException.class)
    public void test_switch_reason_code_from_failed_code() {
        final PUBREC pubrec = new PUBREC(1, Mqtt5PubRecReasonCode.UNSPECIFIED_ERROR, "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubrecPacketImpl
                modifiablePubrecPacket = new ModifiablePubrecPacketImpl(fullConfigurationService, pubrec);
        modifiablePubrecPacket.setReasonCode(AckReasonCode.SUCCESS);
    }

    @Test
    public void test_switch_reason_code_from_failed_to_same_failed() {
        final PUBREC pubrec = new PUBREC(1, Mqtt5PubRecReasonCode.UNSPECIFIED_ERROR, "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubrecPacketImpl
                modifiablePubrecPacket = new ModifiablePubrecPacketImpl(fullConfigurationService, pubrec);
        modifiablePubrecPacket.setReasonCode(AckReasonCode.UNSPECIFIED_ERROR);
        assertFalse(modifiablePubrecPacket.isModified());
    }

    @Test
    public void test_switch_reason_code_from_failed_to_failed() {
        final PUBREC pubrec = new PUBREC(1, Mqtt5PubRecReasonCode.UNSPECIFIED_ERROR, "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubrecPacketImpl
                modifiablePubrecPacket = new ModifiablePubrecPacketImpl(fullConfigurationService, pubrec);
        modifiablePubrecPacket.setReasonCode(AckReasonCode.NOT_AUTHORIZED);
        assertTrue(modifiablePubrecPacket.isModified());
        assertEquals(AckReasonCode.NOT_AUTHORIZED, modifiablePubrecPacket.getReasonCode());
    }

    @Test
    public void test_all_values_set() {
        final PubrecPacketImpl pubrecPacket = new PubrecPacketImpl(fullMqtt5Pubrec);
        assertEquals(fullMqtt5Pubrec.getPacketIdentifier(), pubrecPacket.getPacketIdentifier());
        assertEquals(fullMqtt5Pubrec.getReasonCode().name(), pubrecPacket.getReasonCode().name());
        assertFalse(pubrecPacket.getReasonString().isPresent());
        assertEquals(fullMqtt5Pubrec.getUserProperties().size(), pubrecPacket.getUserProperties().asList().size());
    }

    @Test
    public void test_change_modifiable_does_not_change_copy_of_packet() {
        final PUBREC pubrec = new PUBREC(1, Mqtt5PubRecReasonCode.UNSPECIFIED_ERROR, "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubrecPacketImpl
                modifiablePubrecPacket = new ModifiablePubrecPacketImpl(fullConfigurationService, pubrec);

        final PubrecPacketImpl pubrecPacket = new PubrecPacketImpl(modifiablePubrecPacket);

        modifiablePubrecPacket.setReasonCode(AckReasonCode.NOT_AUTHORIZED);
        modifiablePubrecPacket.setReasonString("OTHER REASON STRING");

        assertTrue(pubrecPacket.getReasonString().isPresent());
        assertEquals(pubrec.getReasonString(), pubrecPacket.getReasonString().get());
        assertEquals(pubrec.getReasonCode().name(), pubrecPacket.getReasonCode().name());
    }

}