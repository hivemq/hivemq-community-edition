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
package com.hivemq.extensions.packets.subscribe;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.subscribe.RetainHandling;
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
public class ModifiableSubscribePacketImplTest {

    private @NotNull FullConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();

        final SubscribePacketImpl packet = new SubscribePacketImpl(
                ImmutableList.of(new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false)),
                UserPropertiesImpl.of(ImmutableList.of()),
                1,
                1);
        final ModifiableSubscribePacketImpl modifiablePacket =
                new ModifiableSubscribePacketImpl(packet, configurationService);
    }

    @Test
    public void modifySubscription() {
        final SubscribePacketImpl packet = new SubscribePacketImpl(
                ImmutableList.of(new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false)),
                UserPropertiesImpl.of(ImmutableList.of()),
                1,
                1);
        final ModifiableSubscribePacketImpl modifiablePacket =
                new ModifiableSubscribePacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.getSubscriptions().get(0).setTopicFilter("test");

        assertTrue(modifiablePacket.isModified());
        assertEquals("test", modifiablePacket.getSubscriptions().get(0).getTopicFilter());
    }

    @Test
    public void modifyUserProperties() {
        final SubscribePacketImpl packet = new SubscribePacketImpl(
                ImmutableList.of(new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false)),
                UserPropertiesImpl.of(ImmutableList.of()),
                1,
                1);
        final ModifiableSubscribePacketImpl modifiablePacket =
                new ModifiableSubscribePacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.getUserProperties().addUserProperty("testName", "testValue");

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of("testValue"), modifiablePacket.getUserProperties().getFirst("testName"));
    }

    @Test
    public void copy_noChanges() {
        final SubscribePacketImpl packet = new SubscribePacketImpl(
                ImmutableList.of(new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false)),
                UserPropertiesImpl.of(ImmutableList.of()),
                1,
                1);
        final ModifiableSubscribePacketImpl modifiablePacket =
                new ModifiableSubscribePacketImpl(packet, configurationService);

        final SubscribePacketImpl copy = modifiablePacket.copy();

        assertEquals(packet, copy);
    }

    @Test
    public void copy_changes() {
        final SubscribePacketImpl packet = new SubscribePacketImpl(
                ImmutableList.of(new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false)),
                UserPropertiesImpl.of(ImmutableList.of()),
                1,
                1);
        final ModifiableSubscribePacketImpl modifiablePacket =
                new ModifiableSubscribePacketImpl(packet, configurationService);

        modifiablePacket.getSubscriptions().get(0).setTopicFilter("test");
        modifiablePacket.getUserProperties().addUserProperty("testName", "testValue");
        final SubscribePacketImpl copy = modifiablePacket.copy();

        final SubscribePacketImpl expectedPacket = new SubscribePacketImpl(
                ImmutableList.of(new SubscriptionImpl("test", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false)),
                UserPropertiesImpl.of(ImmutableList.of(new MqttUserProperty("testName", "testValue"))),
                1,
                1);
        assertEquals(expectedPacket, copy);
    }
}