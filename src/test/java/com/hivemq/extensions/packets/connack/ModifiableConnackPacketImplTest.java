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
package com.hivemq.extensions.packets.connack;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
public class ModifiableConnackPacketImplTest {

    private @NotNull FullConfigurationService configurationService;

    @Before
    public void setUp() {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
    }

    @Test
    public void setReasonString() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.UNSPECIFIED_ERROR,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, true);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonString("reasonString");

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of("reasonString"), modifiablePacket.getReasonString());
    }

    @Test
    public void setReasonString_null() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.UNSPECIFIED_ERROR,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                null,
                null,
                "reasonString",
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, true);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonString(null);

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.empty(), modifiablePacket.getReasonString());
    }

    @Test(expected = IllegalStateException.class)
    public void setReasonString_successCode() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.SUCCESS,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, true);

        modifiablePacket.setReasonString("reasonString");
    }

    @Test
    public void setReasonString_same() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.UNSPECIFIED_ERROR,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                null,
                null,
                "reasonString",
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, true);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonString("reasonString");

        assertFalse(modifiablePacket.isModified());
        assertEquals(Optional.of("reasonString"), modifiablePacket.getReasonString());
    }

    @Test
    public void setReasonCode_error() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.UNSPECIFIED_ERROR,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, true);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonCode(ConnackReasonCode.NOT_AUTHORIZED);

        assertTrue(modifiablePacket.isModified());
        assertEquals(ConnackReasonCode.NOT_AUTHORIZED, modifiablePacket.getReasonCode());
    }

    @Test
    public void setReasonCode_sameError() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.UNSPECIFIED_ERROR,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, true);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonCode(ConnackReasonCode.UNSPECIFIED_ERROR);

        assertFalse(modifiablePacket.isModified());
        assertEquals(ConnackReasonCode.UNSPECIFIED_ERROR, modifiablePacket.getReasonCode());
    }

    @Test(expected = NullPointerException.class)
    public void setReasonCode_null() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.UNSPECIFIED_ERROR,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, true);

        modifiablePacket.setReasonCode(null);
    }

    @Test(expected = IllegalStateException.class)
    public void setReasonString_switchToError() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.SUCCESS,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, true);

        modifiablePacket.setReasonCode(ConnackReasonCode.UNSPECIFIED_ERROR);
    }

    @Test(expected = IllegalStateException.class)
    public void setReasonString_switchFromError() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.UNSPECIFIED_ERROR,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, true);

        modifiablePacket.setReasonCode(ConnackReasonCode.SUCCESS);
    }

    @Test
    public void setResponseInformation() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.UNSPECIFIED_ERROR,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, true);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setResponseInformation("responseInformation");

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of("responseInformation"), modifiablePacket.getResponseInformation());
    }

    @Test
    public void setResponseInformation_null() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.UNSPECIFIED_ERROR,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                "responseInformation",
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, true);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setResponseInformation(null);

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.empty(), modifiablePacket.getResponseInformation());
    }

    @Test
    public void setResponseInformation_same() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.UNSPECIFIED_ERROR,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                "responseInformation",
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, true);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setResponseInformation("responseInformation");

        assertFalse(modifiablePacket.isModified());
        assertEquals(Optional.of("responseInformation"), modifiablePacket.getResponseInformation());
    }

    @Test(expected = IllegalStateException.class)
    public void setResponseInformation_notAllowed() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.UNSPECIFIED_ERROR,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, false);

        modifiablePacket.setResponseInformation("responseInformation");
    }

    @Test
    public void setServerReference() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.UNSPECIFIED_ERROR,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, true);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setServerReference("serverReference");

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of("serverReference"), modifiablePacket.getServerReference());
    }

    @Test
    public void setServerReference_null() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.UNSPECIFIED_ERROR,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                null,
                "serverReference",
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, true);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setServerReference(null);

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.empty(), modifiablePacket.getServerReference());
    }

    @Test
    public void setServerReference_same() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.UNSPECIFIED_ERROR,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                null,
                "serverReference",
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, true);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setServerReference("serverReference");

        assertFalse(modifiablePacket.isModified());
        assertEquals(Optional.of("serverReference"), modifiablePacket.getServerReference());
    }

    @Test
    public void modifyUserProperties() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.UNSPECIFIED_ERROR,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, true);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.getUserProperties().addUserProperty("testName", "testValue");

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of("testValue"), modifiablePacket.getUserProperties().getFirst("testName"));
    }

    @Test
    public void copy_noChanges() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.SUCCESS,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                null,
                null,
                "reason",
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, true);

        final ConnackPacketImpl copy = modifiablePacket.copy();

        assertEquals(packet, copy);
    }

    @Test
    public void copy_changes() {
        final ConnackPacketImpl packet = new ConnackPacketImpl(
                ConnackReasonCode.UNSPECIFIED_ERROR,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, true);

        modifiablePacket.setReasonCode(ConnackReasonCode.NOT_AUTHORIZED);
        modifiablePacket.setResponseInformation("responseInformation");
        modifiablePacket.setServerReference("serverReference");
        modifiablePacket.setReasonString("reason");
        modifiablePacket.getUserProperties().addUserProperty("testName", "testValue");
        final ConnackPacketImpl copy = modifiablePacket.copy();

        final ConnackPacketImpl expectedPacket = new ConnackPacketImpl(
                ConnackReasonCode.NOT_AUTHORIZED,
                true,
                10,
                60,
                null,
                null,
                null,
                3,
                1000,
                10,
                Qos.AT_LEAST_ONCE,
                true,
                true,
                true,
                true,
                "responseInformation",
                "serverReference",
                "reason",
                UserPropertiesImpl.of(ImmutableList.of(new MqttUserProperty("testName", "testValue"))));
        assertEquals(expectedPacket, copy);
    }
}