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
package com.hivemq.extensions.packets.pubrel;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.pubrel.PubrelReasonCode;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class ModifiablePubrelPacketImplTest {

    private @NotNull FullConfigurationService configurationService;

    @Before
    public void setUp() {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
    }

    @Test
    public void setReasonString() {
        final PubrelPacketImpl packet = new PubrelPacketImpl(
                1, PubrelReasonCode.SUCCESS, null, UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiablePubrelPacketImpl modifiablePacket =
                new ModifiablePubrelPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonString("reason");

        assertEquals(Optional.of("reason"), modifiablePacket.getReasonString());
        assertTrue(modifiablePacket.isModified());
    }

    @Test
    public void setReasonString_null() {
        final PubrelPacketImpl packet = new PubrelPacketImpl(
                1, PubrelReasonCode.SUCCESS, "reason", UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiablePubrelPacketImpl modifiablePacket =
                new ModifiablePubrelPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonString(null);

        assertEquals(Optional.empty(), modifiablePacket.getReasonString());
        assertTrue(modifiablePacket.isModified());
    }

    @Test
    public void setReasonString_same() {
        final PubrelPacketImpl packet = new PubrelPacketImpl(
                1, PubrelReasonCode.SUCCESS, "same", UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiablePubrelPacketImpl modifiablePacket =
                new ModifiablePubrelPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonString("same");

        assertEquals(Optional.of("same"), modifiablePacket.getReasonString());
        assertFalse(modifiablePacket.isModified());
    }

    @Test
    public void copy_noChanges() {
        final PubrelPacketImpl packet = new PubrelPacketImpl(
                1, PubrelReasonCode.SUCCESS, null, UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiablePubrelPacketImpl modifiablePacket =
                new ModifiablePubrelPacketImpl(packet, configurationService);

        final PubrelPacketImpl copy = modifiablePacket.copy();

        assertEquals(packet, copy);
    }

    @Test
    public void copy_changes() {
        final PubrelPacketImpl packet = new PubrelPacketImpl(
                1, PubrelReasonCode.SUCCESS, null, UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiablePubrelPacketImpl modifiablePacket =
                new ModifiablePubrelPacketImpl(packet, configurationService);

        modifiablePacket.setReasonString("reason");
        modifiablePacket.getUserProperties().addUserProperty("testName", "testValue");
        final PubrelPacketImpl copy = modifiablePacket.copy();

        final PubrelPacketImpl expectedPacket = new PubrelPacketImpl(
                1,
                PubrelReasonCode.SUCCESS,
                "reason",
                UserPropertiesImpl.of(ImmutableList.of(MqttUserProperty.of("testName", "testValue"))));
        assertEquals(expectedPacket, copy);
    }
}