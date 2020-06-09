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
package com.hivemq.extensions.packets.disconnect;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class ModifiableOutboundDisconnectPacketImplTest {

    private @NotNull FullConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
    }

    @Test
    public void setReasonCode() {
        final DisconnectPacketImpl packet = new DisconnectPacketImpl(
                DisconnectReasonCode.ADMINISTRATIVE_ACTION,
                "reason",
                5,
                "serverReference",
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableOutboundDisconnectPacketImpl modifiablePacket =
                new ModifiableOutboundDisconnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonCode(DisconnectReasonCode.NOT_AUTHORIZED);

        assertTrue(modifiablePacket.isModified());
        assertEquals(DisconnectReasonCode.NOT_AUTHORIZED, modifiablePacket.getReasonCode());
    }

    @Test
    public void setReasonCode_same() {
        final DisconnectPacketImpl packet = new DisconnectPacketImpl(
                DisconnectReasonCode.ADMINISTRATIVE_ACTION,
                "reason",
                5,
                "serverReference",
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableOutboundDisconnectPacketImpl modifiablePacket =
                new ModifiableOutboundDisconnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonCode(DisconnectReasonCode.ADMINISTRATIVE_ACTION);

        assertFalse(modifiablePacket.isModified());
        assertEquals(DisconnectReasonCode.ADMINISTRATIVE_ACTION, modifiablePacket.getReasonCode());
    }

    @Test(expected = NullPointerException.class)
    public void setReasonCode_null() {
        final DisconnectPacketImpl packet = new DisconnectPacketImpl(
                DisconnectReasonCode.ADMINISTRATIVE_ACTION,
                "reason",
                5,
                "serverReference",
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableOutboundDisconnectPacketImpl modifiablePacket =
                new ModifiableOutboundDisconnectPacketImpl(packet, configurationService);

        modifiablePacket.setReasonCode(null);
    }

    @Test
    public void setReasonString() {
        final DisconnectPacketImpl packet = new DisconnectPacketImpl(
                DisconnectReasonCode.ADMINISTRATIVE_ACTION,
                null,
                5,
                "serverReference",
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableOutboundDisconnectPacketImpl modifiablePacket =
                new ModifiableOutboundDisconnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonString("reason");

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of("reason"), modifiablePacket.getReasonString());
    }

    @Test
    public void setReasonString_null() {
        final DisconnectPacketImpl packet = new DisconnectPacketImpl(
                DisconnectReasonCode.ADMINISTRATIVE_ACTION,
                "reason",
                5,
                "serverReference",
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableOutboundDisconnectPacketImpl modifiablePacket =
                new ModifiableOutboundDisconnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonString(null);

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.empty(), modifiablePacket.getReasonString());
    }

    @Test
    public void setReasonString_same() {
        final DisconnectPacketImpl packet = new DisconnectPacketImpl(
                DisconnectReasonCode.ADMINISTRATIVE_ACTION,
                "same",
                5,
                "serverReference",
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableOutboundDisconnectPacketImpl modifiablePacket =
                new ModifiableOutboundDisconnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonString("same");

        assertFalse(modifiablePacket.isModified());
        assertEquals(Optional.of("same"), modifiablePacket.getReasonString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setReasonString_invalid() {
        final DisconnectPacketImpl packet = new DisconnectPacketImpl(
                DisconnectReasonCode.ADMINISTRATIVE_ACTION,
                "reason",
                5,
                "serverReference",
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableOutboundDisconnectPacketImpl modifiablePacket =
                new ModifiableOutboundDisconnectPacketImpl(packet, configurationService);

        modifiablePacket.setReasonString("topic" + '\u0001');
    }

    @Test(expected = IllegalArgumentException.class)
    public void setReasonString_exceedsMaxLength() {
        final DisconnectPacketImpl packet = new DisconnectPacketImpl(
                DisconnectReasonCode.ADMINISTRATIVE_ACTION,
                "reason",
                5,
                "serverReference",
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableOutboundDisconnectPacketImpl modifiablePacket =
                new ModifiableOutboundDisconnectPacketImpl(packet, configurationService);

        final StringBuilder s = new StringBuilder("s");
        for (int i = 0; i < 65535; i++) {
            s.append("s");
        }
        modifiablePacket.setReasonString(s.toString());
    }

    @Test
    public void setServerReference() {
        final DisconnectPacketImpl packet = new DisconnectPacketImpl(
                DisconnectReasonCode.ADMINISTRATIVE_ACTION,
                "reason",
                5,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableOutboundDisconnectPacketImpl modifiablePacket =
                new ModifiableOutboundDisconnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setServerReference("serverReference");

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of("serverReference"), modifiablePacket.getServerReference());
    }

    @Test
    public void setServerReference_null() {
        final DisconnectPacketImpl packet = new DisconnectPacketImpl(
                DisconnectReasonCode.ADMINISTRATIVE_ACTION,
                "reason",
                5,
                "serverReference",
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableOutboundDisconnectPacketImpl modifiablePacket =
                new ModifiableOutboundDisconnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setServerReference(null);

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.empty(), modifiablePacket.getServerReference());
    }

    @Test
    public void setServerReference_same() {
        final DisconnectPacketImpl packet = new DisconnectPacketImpl(
                DisconnectReasonCode.ADMINISTRATIVE_ACTION,
                "same",
                5,
                "serverReference",
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableOutboundDisconnectPacketImpl modifiablePacket =
                new ModifiableOutboundDisconnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setServerReference("serverReference");

        assertFalse(modifiablePacket.isModified());
        assertEquals(Optional.of("serverReference"), modifiablePacket.getServerReference());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setServerReference_invalid() {
        final DisconnectPacketImpl packet = new DisconnectPacketImpl(
                DisconnectReasonCode.ADMINISTRATIVE_ACTION,
                "same",
                5,
                "serverReference",
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableOutboundDisconnectPacketImpl modifiablePacket =
                new ModifiableOutboundDisconnectPacketImpl(packet, configurationService);

        modifiablePacket.setServerReference("topic" + '\u0001');
    }

    @Test(expected = IllegalArgumentException.class)
    public void setServerReference_exceedsMaxLength() {
        final DisconnectPacketImpl packet = new DisconnectPacketImpl(
                DisconnectReasonCode.ADMINISTRATIVE_ACTION,
                "same",
                5,
                "serverReference",
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableOutboundDisconnectPacketImpl modifiablePacket =
                new ModifiableOutboundDisconnectPacketImpl(packet, configurationService);

        final StringBuilder s = new StringBuilder("s");
        for (int i = 0; i < 65535; i++) {
            s.append("s");
        }
        modifiablePacket.setServerReference(s.toString());
    }

    @Test
    public void copy_noChanges() {
        final DisconnectPacketImpl packet = new DisconnectPacketImpl(
                DisconnectReasonCode.ADMINISTRATIVE_ACTION,
                "same",
                5,
                "serverReference",
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableOutboundDisconnectPacketImpl modifiablePacket =
                new ModifiableOutboundDisconnectPacketImpl(packet, configurationService);

        final DisconnectPacketImpl copy = modifiablePacket.copy();

        assertEquals(packet, copy);
    }

    @Test
    public void copy_changes() {
        final DisconnectPacketImpl packet = new DisconnectPacketImpl(
                DisconnectReasonCode.ADMINISTRATIVE_ACTION,
                "same",
                5,
                "serverReference",
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableOutboundDisconnectPacketImpl modifiablePacket =
                new ModifiableOutboundDisconnectPacketImpl(packet, configurationService);

        modifiablePacket.setReasonCode(DisconnectReasonCode.UNSPECIFIED_ERROR);
        modifiablePacket.setReasonString("modifiedReasonString");
        modifiablePacket.setServerReference("modifiedServerReference");
        modifiablePacket.getUserProperties().addUserProperty("testName", "testValue");
        final DisconnectPacketImpl copy = modifiablePacket.copy();

        final DisconnectPacketImpl expectedPacket = new DisconnectPacketImpl(
                DisconnectReasonCode.UNSPECIFIED_ERROR,
                "modifiedReasonString",
                5,
                "modifiedServerReference",
                UserPropertiesImpl.of(ImmutableList.of(MqttUserProperty.of("testName", "testValue"))));
        assertEquals(expectedPacket, copy);
    }
}