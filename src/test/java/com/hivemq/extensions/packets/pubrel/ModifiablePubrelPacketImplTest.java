package com.hivemq.extensions.packets.pubrel;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.reason.Mqtt5PubRelReasonCode;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import static org.junit.Assert.*;

/**
 * @author Yannick Weber
 */
public class ModifiablePubrelPacketImplTest {

    private FullConfigurationService fullConfigurationService;
    private ModifiablePubrelPacketImpl modifiablePubrelPacket;
    private PUBREL fullMqtt5Pubrel;

    @Before
    public void setUp() throws Exception {
        fullConfigurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        fullMqtt5Pubrel = new PUBREL(1, Mqtt5PubRelReasonCode.SUCCESS, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
        modifiablePubrelPacket = new ModifiablePubrelPacketImpl(fullConfigurationService, fullMqtt5Pubrel);
    }

    @Test(expected = IllegalStateException.class)
    public void test_set_reason_string_to_success_code() {
        modifiablePubrelPacket.setReasonString("reason");
    }

    @Test
    public void test_set_reason_string_to_failed() {
        final PUBREL pubrel = new PUBREL(1, Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND, null,
                Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubrelPacketImpl
                modifiablePubrelPacket = new ModifiablePubrelPacketImpl(fullConfigurationService, pubrel);
        modifiablePubrelPacket.setReasonString("reason");
        assertTrue(modifiablePubrelPacket.isModified());
        assertTrue(modifiablePubrelPacket.getReasonString().isPresent());
        assertEquals("reason", modifiablePubrelPacket.getReasonString().get());
    }

    @Test
    public void test_set_reason_string_to_null() {
        final PUBREL pubrel = new PUBREL(1, Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND, "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubrelPacketImpl
                modifiablePubrelPacket = new ModifiablePubrelPacketImpl(fullConfigurationService, pubrel);
        modifiablePubrelPacket.setReasonString(null);
        assertTrue(modifiablePubrelPacket.isModified());
        assertFalse(modifiablePubrelPacket.getReasonString().isPresent());
    }

    @Test
    public void test_set_reason_string_to_same() {
        final PUBREL pubrel = new PUBREL(1, Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND, "same",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubrelPacketImpl
                modifiablePubrelPacket = new ModifiablePubrelPacketImpl(fullConfigurationService, pubrel);
        modifiablePubrelPacket.setReasonString("same");
        assertFalse(modifiablePubrelPacket.isModified());
    }

    @Test
    public void test_all_values_set() {
        final PubrelPacketImpl pubrelPacket = new PubrelPacketImpl(fullMqtt5Pubrel);
        assertEquals(fullMqtt5Pubrel.getPacketIdentifier(), pubrelPacket.getPacketIdentifier());
        assertEquals(fullMqtt5Pubrel.getReasonCode().name(), pubrelPacket.getReasonCode().name());
        assertFalse(pubrelPacket.getReasonString().isPresent());
        assertEquals(fullMqtt5Pubrel.getUserProperties().size(), pubrelPacket.getUserProperties().asList().size());
    }

    @Test
    public void test_change_modifiable_does_not_change_copy_of_packet() {
        final PUBREL pubrel = new PUBREL(1, Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND, "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubrelPacketImpl
                modifiablePubrelPacket = new ModifiablePubrelPacketImpl(fullConfigurationService, pubrel);

        final PubrelPacketImpl pubrelPacket = new PubrelPacketImpl(modifiablePubrelPacket);

        modifiablePubrelPacket.setReasonString("OTHER REASON STRING");

        assertTrue(pubrelPacket.getReasonString().isPresent());
        assertEquals(pubrel.getReasonString(), pubrelPacket.getReasonString().get());
        assertEquals(pubrel.getReasonCode().name(), pubrelPacket.getReasonCode().name());
    }
}