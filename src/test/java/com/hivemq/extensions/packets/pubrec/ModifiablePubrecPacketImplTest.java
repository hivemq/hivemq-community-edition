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
package com.hivemq.extensions.packets.pubrec;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
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
public class ModifiablePubrecPacketImplTest {

    private @NotNull FullConfigurationService configurationService;

    @Before
    public void setUp() {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
    }

    @Test
    public void setReasonString() {
        final PubrecPacketImpl packet = new PubrecPacketImpl(
                1, AckReasonCode.SUCCESS, null, UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiablePubrecPacketImpl modifiablePacket =
                new ModifiablePubrecPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonString("reason");

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of("reason"), modifiablePacket.getReasonString());
    }

    @Test
    public void setReasonString_null() {
        final PubrecPacketImpl packet = new PubrecPacketImpl(
                1, AckReasonCode.SUCCESS, "reason", UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiablePubrecPacketImpl modifiablePacket =
                new ModifiablePubrecPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonString(null);

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.empty(), modifiablePacket.getReasonString());
    }

    @Test
    public void setReasonString_same() {
        final PubrecPacketImpl packet = new PubrecPacketImpl(
                1, AckReasonCode.SUCCESS, "same", UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiablePubrecPacketImpl modifiablePacket =
                new ModifiablePubrecPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonString("same");

        assertFalse(modifiablePacket.isModified());
        assertEquals(Optional.of("same"), modifiablePacket.getReasonString());
    }

    @Test
    public void setReasonCode() {
        final PubrecPacketImpl packet = new PubrecPacketImpl(
                1, AckReasonCode.SUCCESS, null, UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiablePubrecPacketImpl modifiablePacket =
                new ModifiablePubrecPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonCode(AckReasonCode.NO_MATCHING_SUBSCRIBERS);

        assertTrue(modifiablePacket.isModified());
        assertEquals(AckReasonCode.NO_MATCHING_SUBSCRIBERS, modifiablePacket.getReasonCode());
    }

    @Test(expected = NullPointerException.class)
    public void setReasonCode_null() {
        final PubrecPacketImpl packet = new PubrecPacketImpl(
                1, AckReasonCode.SUCCESS, null, UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiablePubrecPacketImpl modifiablePacket =
                new ModifiablePubrecPacketImpl(packet, configurationService);

        modifiablePacket.setReasonCode(null);
    }

    @Test
    public void setReasonCode_same() {
        final PubrecPacketImpl packet = new PubrecPacketImpl(
                1, AckReasonCode.SUCCESS, null, UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiablePubrecPacketImpl modifiablePacket =
                new ModifiablePubrecPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonCode(AckReasonCode.SUCCESS);

        assertFalse(modifiablePacket.isModified());
        assertEquals(AckReasonCode.SUCCESS, modifiablePacket.getReasonCode());
    }

    @Test
    public void setReasonCode_error() {
        final PubrecPacketImpl packet = new PubrecPacketImpl(
                1, AckReasonCode.UNSPECIFIED_ERROR, null, UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiablePubrecPacketImpl modifiablePacket =
                new ModifiablePubrecPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonCode(AckReasonCode.NOT_AUTHORIZED);

        assertTrue(modifiablePacket.isModified());
        assertEquals(AckReasonCode.NOT_AUTHORIZED, modifiablePacket.getReasonCode());
    }

    @Test
    public void setReasonCode_sameError() {
        final PubrecPacketImpl packet = new PubrecPacketImpl(
                1, AckReasonCode.UNSPECIFIED_ERROR, null, UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiablePubrecPacketImpl modifiablePacket =
                new ModifiablePubrecPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonCode(AckReasonCode.UNSPECIFIED_ERROR);

        assertFalse(modifiablePacket.isModified());
        assertEquals(AckReasonCode.UNSPECIFIED_ERROR, modifiablePacket.getReasonCode());
    }

    @Test(expected = IllegalStateException.class)
    public void setReasonCode_switchToError() {
        final PubrecPacketImpl packet = new PubrecPacketImpl(
                1, AckReasonCode.SUCCESS, null, UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiablePubrecPacketImpl modifiablePacket =
                new ModifiablePubrecPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonCode(AckReasonCode.UNSPECIFIED_ERROR);
    }

    @Test(expected = IllegalStateException.class)
    public void setReasonCode_switchFromError() {
        final PubrecPacketImpl packet = new PubrecPacketImpl(
                1, AckReasonCode.UNSPECIFIED_ERROR, null, UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiablePubrecPacketImpl modifiablePacket =
                new ModifiablePubrecPacketImpl(packet, configurationService);

        modifiablePacket.setReasonCode(AckReasonCode.SUCCESS);
    }

    @Test
    public void copy_noChanges() {
        final PubrecPacketImpl packet = new PubrecPacketImpl(
                1, AckReasonCode.SUCCESS, null, UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiablePubrecPacketImpl modifiablePacket =
                new ModifiablePubrecPacketImpl(packet, configurationService);

        final PubrecPacketImpl copy = modifiablePacket.copy();

        assertEquals(packet, copy);
    }

    @Test
    public void copy_changes() {
        final PubrecPacketImpl packet = new PubrecPacketImpl(
                1, AckReasonCode.SUCCESS, null, UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiablePubrecPacketImpl modifiablePacket =
                new ModifiablePubrecPacketImpl(packet, configurationService);

        modifiablePacket.setReasonString("reason");
        modifiablePacket.getUserProperties().addUserProperty("testName", "testValue");
        final PubrecPacketImpl copy = modifiablePacket.copy();

        final PubrecPacketImpl expectedPacket = new PubrecPacketImpl(
                1,
                AckReasonCode.SUCCESS,
                "reason",
                UserPropertiesImpl.of(ImmutableList.of(MqttUserProperty.of("testName", "testValue"))));
        assertEquals(expectedPacket, copy);
    }
}