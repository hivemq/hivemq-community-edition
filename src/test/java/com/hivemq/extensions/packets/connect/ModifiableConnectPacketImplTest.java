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
package com.hivemq.extensions.packets.connect;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.extensions.packets.publish.ModifiableWillPublishImpl;
import com.hivemq.extensions.packets.publish.WillPublishPacketImpl;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import java.nio.ByteBuffer;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Lukas Brandl
 * @author Silvio Giebl
 */
public class ModifiableConnectPacketImplTest {

    private @NotNull FullConfigurationService configurationService;

    @Before
    public void setUp() {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
    }

    @Test
    public void setClientId() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setClientId("modifiedClientId");

        assertTrue(modifiablePacket.isModified());
        assertEquals("modifiedClientId", modifiablePacket.getClientId());
    }

    @Test
    public void setClientId_same() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setClientId("clientId");

        assertFalse(modifiablePacket.isModified());
        assertEquals("clientId", modifiablePacket.getClientId());
    }

    @Test(expected = NullPointerException.class)
    public void setClientId_null() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        modifiablePacket.setClientId(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setClientId_empty() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        modifiablePacket.setClientId("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setClientId_invalid() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        modifiablePacket.setClientId("\0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setClientId_tooLong() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        configurationService.restrictionsConfiguration().setMaxClientIdLength(10);
        modifiablePacket.setClientId("0123456789_0123456789");
    }

    @Test
    public void setCleanStart() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setCleanStart(true);

        assertTrue(modifiablePacket.isModified());
        assertEquals(true, modifiablePacket.getCleanStart());
    }

    @Test
    public void setCleanStart_same() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setCleanStart(false);

        assertFalse(modifiablePacket.isModified());
        assertEquals(false, modifiablePacket.getCleanStart());
    }

    @Test
    public void setSessionExpiryInterval() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setSessionExpiryInterval(60);

        assertTrue(modifiablePacket.isModified());
        assertEquals(60, modifiablePacket.getSessionExpiryInterval());
    }

    @Test
    public void setSessionExpiryInterval_same() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setSessionExpiryInterval(100);

        assertFalse(modifiablePacket.isModified());
        assertEquals(100, modifiablePacket.getSessionExpiryInterval());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setSessionExpiryInterval_largerThanMax() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        configurationService.mqttConfiguration().setMaxSessionExpiryInterval(60);
        modifiablePacket.setSessionExpiryInterval(61);
    }

    @Test
    public void setKeepAlive() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setKeepAlive(10);

        assertTrue(modifiablePacket.isModified());
        assertEquals(10, modifiablePacket.getKeepAlive());
    }

    @Test
    public void setKeepAlive_same() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setKeepAlive(60);

        assertFalse(modifiablePacket.isModified());
        assertEquals(60, modifiablePacket.getKeepAlive());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setKeepAlive_largerThanMax() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        configurationService.mqttConfiguration().setKeepAliveMax(60);
        modifiablePacket.setKeepAlive(61);
    }

    @Test
    public void setReceiveMaximum() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReceiveMaximum(2);

        assertTrue(modifiablePacket.isModified());
        assertEquals(2, modifiablePacket.getReceiveMaximum());
    }

    @Test
    public void setReceiveMaximum_same() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReceiveMaximum(3);

        assertFalse(modifiablePacket.isModified());
        assertEquals(3, modifiablePacket.getReceiveMaximum());
    }

    @Test
    public void setMaximumPacketSize() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setMaximumPacketSize(100);

        assertTrue(modifiablePacket.isModified());
        assertEquals(100, modifiablePacket.getMaximumPacketSize());
    }

    @Test
    public void setMaximumPacketSize_same() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setMaximumPacketSize(1000);

        assertFalse(modifiablePacket.isModified());
        assertEquals(1000, modifiablePacket.getMaximumPacketSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setMaximumPacketSize_largerThanMax() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        configurationService.mqttConfiguration().setMaxPacketSize(60);
        modifiablePacket.setMaximumPacketSize(61);
    }

    @Test
    public void setTopicAliasMaximum() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setTopicAliasMaximum(1);

        assertTrue(modifiablePacket.isModified());
        assertEquals(1, modifiablePacket.getTopicAliasMaximum());
    }

    @Test
    public void setTopicAliasMaximum_same() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setTopicAliasMaximum(10);

        assertFalse(modifiablePacket.isModified());
        assertEquals(10, modifiablePacket.getTopicAliasMaximum());
    }

    @Test
    public void setRequestProblemInformation() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setRequestProblemInformation(false);

        assertTrue(modifiablePacket.isModified());
        assertEquals(false, modifiablePacket.getRequestProblemInformation());
    }

    @Test
    public void setRequestProblemInformation_same() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setRequestProblemInformation(true);

        assertFalse(modifiablePacket.isModified());
        assertEquals(true, modifiablePacket.getRequestProblemInformation());
    }

    @Test
    public void setRequestResponseInformation() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setRequestResponseInformation(false);

        assertTrue(modifiablePacket.isModified());
        assertEquals(false, modifiablePacket.getRequestResponseInformation());
    }

    @Test
    public void setRequestResponseInformation_same() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setRequestResponseInformation(true);

        assertFalse(modifiablePacket.isModified());
        assertEquals(true, modifiablePacket.getRequestResponseInformation());
    }

    @Test
    public void setUserName() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setUserName("username");

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of("username"), modifiablePacket.getUserName());
    }

    @Test
    public void setUserName_same() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                "username",
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setUserName("username");

        assertFalse(modifiablePacket.isModified());
        assertEquals(Optional.of("username"), modifiablePacket.getUserName());
    }

    @Test
    public void setUserName_null() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                "username",
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setUserName(null);

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.empty(), modifiablePacket.getUserName());
    }

    @Test
    public void setPassword() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setPassword(ByteBuffer.wrap("password".getBytes()));

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of(ByteBuffer.wrap("password".getBytes())), modifiablePacket.getPassword());
    }

    @Test
    public void setPassword_same() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                ByteBuffer.wrap("password".getBytes()),
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setPassword(ByteBuffer.wrap("password".getBytes()));

        assertFalse(modifiablePacket.isModified());
        assertEquals(Optional.of(ByteBuffer.wrap("password".getBytes())), modifiablePacket.getPassword());
    }

    @Test
    public void setPassword_null() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                ByteBuffer.wrap("password".getBytes()),
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setPassword(null);

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.empty(), modifiablePacket.getPassword());
    }

    @Test
    public void setAuthenticationMethod() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setAuthenticationMethod("authMethod");

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of("authMethod"), modifiablePacket.getAuthenticationMethod());
    }

    @Test
    public void setAuthenticationMethod_same() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                "authMethod",
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setAuthenticationMethod("authMethod");

        assertFalse(modifiablePacket.isModified());
        assertEquals(Optional.of("authMethod"), modifiablePacket.getAuthenticationMethod());
    }

    @Test
    public void setAuthenticationMethod_null() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                "authMethod",
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setAuthenticationMethod(null);

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.empty(), modifiablePacket.getAuthenticationMethod());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setAuthenticationMethod_invalid() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        modifiablePacket.setAuthenticationMethod("\0");
    }

    @Test
    public void setAuthenticationData() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setAuthenticationData(ByteBuffer.wrap("authData".getBytes()));

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of(ByteBuffer.wrap("authData".getBytes())), modifiablePacket.getAuthenticationData());
    }

    @Test
    public void setAuthenticationData_same() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                ByteBuffer.wrap("authData".getBytes()),
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setAuthenticationData(ByteBuffer.wrap("authData".getBytes()));

        assertFalse(modifiablePacket.isModified());
        assertEquals(Optional.of(ByteBuffer.wrap("authData".getBytes())), modifiablePacket.getAuthenticationData());
    }

    @Test
    public void setAuthenticationData_null() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                ByteBuffer.wrap("authData".getBytes()),
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setAuthenticationData(null);

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.empty(), modifiablePacket.getAuthenticationData());
    }

    @Test
    public void setWillPublish() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        final WillPublishPacketImpl willPublishPacket = new WillPublishPacketImpl(
                "topic", Qos.AT_LEAST_ONCE, null, false, 10, null, null, null, null,
                UserPropertiesImpl.of(ImmutableList.of()), 0, 1234L);
        modifiablePacket.setWillPublish(willPublishPacket);

        assertTrue(modifiablePacket.isModified());
        assertEquals(
                Optional.of(new ModifiableWillPublishImpl(willPublishPacket, configurationService)),
                modifiablePacket.getWillPublish());
    }

    @Test
    public void setWillPublish_same() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                new WillPublishPacketImpl(
                        "topic", Qos.AT_LEAST_ONCE, null, false, 10, null, null, null, null,
                        UserPropertiesImpl.of(ImmutableList.of()), 0, 1234L),
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        final WillPublishPacketImpl willPublishPacket = new WillPublishPacketImpl(
                "topic", Qos.AT_LEAST_ONCE, null, false, 10, null, null, null, null,
                UserPropertiesImpl.of(ImmutableList.of()), 0, 1234L);
        modifiablePacket.setWillPublish(willPublishPacket);

        assertFalse(modifiablePacket.isModified());
        assertEquals(
                Optional.of(new ModifiableWillPublishImpl(willPublishPacket, configurationService)),
                modifiablePacket.getWillPublish());
    }

    @Test
    public void setWillPublish_null() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                new WillPublishPacketImpl(
                        "topic", Qos.AT_LEAST_ONCE, null, false, 10, null, null, null, null,
                        UserPropertiesImpl.of(ImmutableList.of()), 0, 1234L),
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setWillPublish(null);

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.empty(), modifiablePacket.getWillPublish());
    }

    @Test
    public void modifyWillPublish() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                new WillPublishPacketImpl(
                        "topic", Qos.AT_LEAST_ONCE, null, false, 10, null, null, null, null,
                        UserPropertiesImpl.of(ImmutableList.of()), 0, 1234L),
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.getModifiableWillPublish().ifPresent(
                modifiableWillPublish -> modifiableWillPublish.setTopic("modifiedTopic"));

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of("modifiedTopic"), modifiablePacket.getWillPublish().map(WillPublishPacket::getTopic));
    }

    @Test
    public void modifyUserProperties() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.getUserProperties().addUserProperty("testName", "testValue");

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of("testValue"), modifiablePacket.getUserProperties().getFirst("testName"));
    }

    @Test
    public void copy_noChanges() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        final ConnectPacketImpl copy = modifiablePacket.copy();

        assertEquals(packet, copy);
    }

    @Test
    public void copy_changes() {
        final ConnectPacketImpl packet = new ConnectPacketImpl(
                MqttVersion.V_5,
                "clientId",
                false,
                100,
                60,
                3,
                1000,
                10,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);

        modifiablePacket.setClientId("modifiedClientId");
        modifiablePacket.setCleanStart(true);
        modifiablePacket.setSessionExpiryInterval(60);
        modifiablePacket.setKeepAlive(10);
        modifiablePacket.setReceiveMaximum(2);
        modifiablePacket.setMaximumPacketSize(100);
        modifiablePacket.setTopicAliasMaximum(1);
        modifiablePacket.setRequestProblemInformation(false);
        modifiablePacket.setRequestResponseInformation(false);
        modifiablePacket.setUserName("username");
        modifiablePacket.setPassword(ByteBuffer.wrap("password".getBytes()));
        modifiablePacket.setAuthenticationMethod("authMethod");
        modifiablePacket.setAuthenticationData(ByteBuffer.wrap("authData".getBytes()));
        modifiablePacket.setWillPublish(new WillPublishPacketImpl(
                "topic", Qos.AT_LEAST_ONCE, null, false, 10, null, null, null, null,
                UserPropertiesImpl.of(ImmutableList.of()), 0, 1234L));
        modifiablePacket.getUserProperties().addUserProperty("testName", "testValue");
        final ConnectPacketImpl copy = modifiablePacket.copy();

        final ConnectPacketImpl expectedPacket = new ConnectPacketImpl(
                MqttVersion.V_5,
                "modifiedClientId",
                true,
                60,
                10,
                2,
                100,
                1,
                false,
                false,
                "username",
                ByteBuffer.wrap("password".getBytes()),
                "authMethod",
                ByteBuffer.wrap("authData".getBytes()),
                new WillPublishPacketImpl(
                        "topic", Qos.AT_LEAST_ONCE, null, false, 10, null, null, null, null,
                        UserPropertiesImpl.of(ImmutableList.of()), 0, 1234L),
                UserPropertiesImpl.of(ImmutableList.of(new MqttUserProperty("testName", "testValue"))));
        assertEquals(expectedPacket, copy);
    }
}