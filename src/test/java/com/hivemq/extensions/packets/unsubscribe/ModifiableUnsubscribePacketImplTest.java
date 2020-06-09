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
package com.hivemq.extensions.packets.unsubscribe;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class ModifiableUnsubscribePacketImplTest {

    private @NotNull FullConfigurationService configurationService;

    @Before
    public void setUp() {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
    }

    @Test
    public void setTopicFilters() {
        final UnsubscribePacketImpl packet = new UnsubscribePacketImpl(
                ImmutableList.of("topic1", "topic2"),
                UserPropertiesImpl.of(ImmutableList.of(MqttUserProperty.of("name", "value"))),
                1);
        final ModifiableUnsubscribePacketImpl modifiablePacket =
                new ModifiableUnsubscribePacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setTopicFilters(ImmutableList.of("test1", "test2"));

        assertTrue(modifiablePacket.isModified());
        assertEquals(ImmutableList.of("test1", "test2"), modifiablePacket.getTopicFilters());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTopicFilters_tooMany() {
        final UnsubscribePacketImpl packet = new UnsubscribePacketImpl(
                ImmutableList.of("topic1", "topic2"),
                UserPropertiesImpl.of(ImmutableList.of(MqttUserProperty.of("name", "value"))),
                1);
        final ModifiableUnsubscribePacketImpl modifiablePacket =
                new ModifiableUnsubscribePacketImpl(packet, configurationService);

        modifiablePacket.setTopicFilters(ImmutableList.of("test1", "test2", "test3"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTopicFilters_tooFew() {
        final UnsubscribePacketImpl packet = new UnsubscribePacketImpl(
                ImmutableList.of("topic1", "topic2"),
                UserPropertiesImpl.of(ImmutableList.of(MqttUserProperty.of("name", "value"))),
                1);
        final ModifiableUnsubscribePacketImpl modifiablePacket =
                new ModifiableUnsubscribePacketImpl(packet, configurationService);

        modifiablePacket.setTopicFilters(ImmutableList.of("test1"));
    }

    @Test(expected = NullPointerException.class)
    public void setTopicFilters_null() {
        final UnsubscribePacketImpl packet = new UnsubscribePacketImpl(
                ImmutableList.of("topic1", "topic2"),
                UserPropertiesImpl.of(ImmutableList.of(MqttUserProperty.of("name", "value"))),
                1);
        final ModifiableUnsubscribePacketImpl modifiablePacket =
                new ModifiableUnsubscribePacketImpl(packet, configurationService);

        modifiablePacket.setTopicFilters(null);
    }

    @Test(expected = NullPointerException.class)
    public void setTopicFilters_nullElement() {
        final UnsubscribePacketImpl packet = new UnsubscribePacketImpl(
                ImmutableList.of("topic1", "topic2"),
                UserPropertiesImpl.of(ImmutableList.of(MqttUserProperty.of("name", "value"))),
                1);
        final ModifiableUnsubscribePacketImpl modifiablePacket =
                new ModifiableUnsubscribePacketImpl(packet, configurationService);

        modifiablePacket.setTopicFilters(Arrays.asList("test1", null));
    }

    @Test
    public void copy_noChanges() {
        final UnsubscribePacketImpl packet = new UnsubscribePacketImpl(
                ImmutableList.of("topic1", "topic2"),
                UserPropertiesImpl.of(ImmutableList.of(MqttUserProperty.of("name", "value"))),
                1);
        final ModifiableUnsubscribePacketImpl modifiablePacket =
                new ModifiableUnsubscribePacketImpl(packet, configurationService);

        final UnsubscribePacketImpl copy = modifiablePacket.copy();

        assertEquals(packet, copy);
    }

    @Test
    public void copy_changes() {
        final UnsubscribePacketImpl packet = new UnsubscribePacketImpl(
                ImmutableList.of("topic1", "topic2"),
                UserPropertiesImpl.of(ImmutableList.of(MqttUserProperty.of("name", "value"))),
                1);
        final ModifiableUnsubscribePacketImpl modifiablePacket =
                new ModifiableUnsubscribePacketImpl(packet, configurationService);

        modifiablePacket.setTopicFilters(ImmutableList.of("test1", "test2"));
        modifiablePacket.getUserProperties().removeName("name");
        modifiablePacket.getUserProperties().addUserProperty("testName", "testValue");
        final UnsubscribePacketImpl copy = modifiablePacket.copy();

        final UnsubscribePacketImpl expectedPacket = new UnsubscribePacketImpl(
                ImmutableList.of("test1", "test2"),
                UserPropertiesImpl.of(ImmutableList.of(MqttUserProperty.of("testName", "testValue"))),
                1);
        assertEquals(expectedPacket, copy);
    }
}