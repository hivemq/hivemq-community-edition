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
package com.hivemq.extensions.packets.connect;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extensions.services.builder.WillPublishBuilderImpl;
import com.hivemq.mqtt.message.connect.CONNECT;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 * @author Lukas Brandl
 */
public class ModifiableConnectPacketImplTest {

    private ModifiableConnectPacketImpl modifiablePacket;

    private CONNECT original;

    private FullConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();

        final CONNECT.Mqtt5Builder builder = new CONNECT.Mqtt5Builder();
        builder.withClientIdentifier("clientId")
                .withKeepAlive(60)
                .withSessionExpiryInterval(60);
        original = builder.build();
        modifiablePacket = new ModifiableConnectPacketImpl(configurationService, original);
    }

    @Test
    public void test_change_all_valid_values() {
        modifiablePacket.setClientId("id modified");
        modifiablePacket.setKeepAlive(1);
        modifiablePacket.setSessionExpiryInterval(2);
        modifiablePacket.setAuthenticationData(ByteBuffer.wrap(new byte[]{3}));
        modifiablePacket.setAuthenticationMethod("auth modified");
        modifiablePacket.setMaximumPacketSize(4);
        modifiablePacket.setPassword(ByteBuffer.wrap(new byte[]{5}));
        modifiablePacket.setRequestProblemInformation(true);
        modifiablePacket.setRequestResponseInformation(true);
        modifiablePacket.setTopicAliasMaximum(6);
        modifiablePacket.setUserName("user modified");

        final CONNECT result = CONNECT.mergeConnectPacket(modifiablePacket, original, "clusterId");

        assertEquals("id modified", result.getClientIdentifier());
        assertEquals(1, result.getKeepAlive());
        assertEquals(2, result.getSessionExpiryInterval());
        assertEquals(3, result.getAuthData()[0]);
        assertEquals("auth modified", result.getAuthMethod());
        assertEquals(4, result.getMaximumPacketSize());
        assertEquals(5, result.getPassword()[0]);
        assertTrue(result.isResponseInformationRequested());
        assertTrue(result.isProblemInformationRequested());
        assertEquals(6, result.getTopicAliasMaximum());
        assertEquals("user modified", result.getUsername());

        assertTrue(modifiablePacket.isModified());
    }

    @Test
    public void test_change_all_valid_values_to_values_before() {
        final CONNECT.Mqtt5Builder builder = new CONNECT.Mqtt5Builder();
        builder.withClientIdentifier("id modified")
                .withKeepAlive(1)
                .withSessionExpiryInterval(2)
                .withAuthData(new byte[]{3})
                .withAuthMethod("auth modified")
                .withMaximumPacketSize(4)
                .withPassword(new byte[]{5})
                .withResponseInformationRequested(true)
                .withProblemInformationRequested(true)
                .withTopicAliasMaximum(6)
                .withUsername("user modified");
        original = builder.build();

        modifiablePacket = new ModifiableConnectPacketImpl(configurationService, original);

        modifiablePacket.setClientId("id modified");
        modifiablePacket.setKeepAlive(1);
        modifiablePacket.setSessionExpiryInterval(2);
        modifiablePacket.setAuthenticationData(ByteBuffer.wrap(new byte[]{3}));
        modifiablePacket.setAuthenticationMethod("auth modified");
        modifiablePacket.setMaximumPacketSize(4);
        modifiablePacket.setPassword(ByteBuffer.wrap(new byte[]{5}));
        modifiablePacket.setRequestProblemInformation(true);
        modifiablePacket.setRequestResponseInformation(true);
        modifiablePacket.setTopicAliasMaximum(6);
        modifiablePacket.setUserName("user modified");

        assertFalse(modifiablePacket.isModified());
    }

    @Test
    public void test_modify_packet() {

        modifiablePacket = new ModifiableConnectPacketImpl(configurationService, original);
        modifiablePacket.setClientId("id modified");
        assertTrue(modifiablePacket.isModified());

        modifiablePacket = new ModifiableConnectPacketImpl(configurationService, original);
        modifiablePacket.setKeepAlive(1);
        assertTrue(modifiablePacket.isModified());

        modifiablePacket = new ModifiableConnectPacketImpl(configurationService, original);
        modifiablePacket.setSessionExpiryInterval(2);
        assertTrue(modifiablePacket.isModified());

        modifiablePacket = new ModifiableConnectPacketImpl(configurationService, original);
        modifiablePacket.setAuthenticationData(ByteBuffer.wrap(new byte[]{3}));
        assertTrue(modifiablePacket.isModified());

        modifiablePacket = new ModifiableConnectPacketImpl(configurationService, original);
        modifiablePacket.setAuthenticationMethod("auth modified");
        assertTrue(modifiablePacket.isModified());

        modifiablePacket = new ModifiableConnectPacketImpl(configurationService, original);
        modifiablePacket.setMaximumPacketSize(4);
        assertTrue(modifiablePacket.isModified());

        modifiablePacket = new ModifiableConnectPacketImpl(configurationService, original);
        modifiablePacket.setPassword(ByteBuffer.wrap(new byte[]{5}));
        assertTrue(modifiablePacket.isModified());

        modifiablePacket = new ModifiableConnectPacketImpl(configurationService, original);
        modifiablePacket.setRequestProblemInformation(false);
        assertTrue(modifiablePacket.isModified());

        modifiablePacket = new ModifiableConnectPacketImpl(configurationService, original);
        modifiablePacket.setRequestResponseInformation(true);
        assertTrue(modifiablePacket.isModified());

        modifiablePacket = new ModifiableConnectPacketImpl(configurationService, original);
        modifiablePacket.setTopicAliasMaximum(6);
        assertTrue(modifiablePacket.isModified());

        modifiablePacket = new ModifiableConnectPacketImpl(configurationService, original);
        modifiablePacket.setUserName("user modified");
        assertTrue(modifiablePacket.isModified());

        modifiablePacket = new ModifiableConnectPacketImpl(configurationService, original);
        modifiablePacket.setWillPublish(new WillPublishBuilderImpl(configurationService).topic("topic")
                .payload(ByteBuffer.wrap("message".getBytes())).build());
        assertTrue(modifiablePacket.isModified());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_client_id_null_char() {
        modifiablePacket.setClientId("\0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_client_id_to_long() {
        configurationService.restrictionsConfiguration().setMaxClientIdLength(10);
        modifiablePacket.setClientId("0123456789_0123456789");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_max_keep_alive() {
        configurationService.mqttConfiguration().setKeepAliveMax(60);
        modifiablePacket.setKeepAlive(61);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_expiry_interval_maximum() {
        configurationService.mqttConfiguration().setMaxSessionExpiryInterval(60);
        modifiablePacket.setSessionExpiryInterval(61);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_max_packet_size() {
        configurationService.mqttConfiguration().setMaxPacketSize(60);
        modifiablePacket.setMaximumPacketSize(61);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_auth_method_null_char() {
        modifiablePacket.setAuthenticationMethod("\0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_client_id_empty() {
        modifiablePacket.setClientId("");
    }
}