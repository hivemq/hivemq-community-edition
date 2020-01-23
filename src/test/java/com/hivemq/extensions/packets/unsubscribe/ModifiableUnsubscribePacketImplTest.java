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
package com.hivemq.extensions.packets.unsubscribe;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import static org.junit.Assert.assertEquals;

/**
 * @author Robin Atherton
 */
public class ModifiableUnsubscribePacketImplTest {

    private FullConfigurationService configurationService;
    private ModifiableUnsubscribePacketImpl modifiableUnsubscribePacket;

    @Before
    public void setUp() throws Exception {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        modifiableUnsubscribePacket = testUnsubscribePacket();
    }

    @Test
    public void set_topics() {
        modifiableUnsubscribePacket.setTopicFilters(ImmutableList.of("test1", "test2"));
        assertEquals("test1", modifiableUnsubscribePacket.getTopicFilters().get(0));
        assertEquals("test2", modifiableUnsubscribePacket.getTopicFilters().get(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void set_topics_too_many() {
        modifiableUnsubscribePacket.setTopicFilters(ImmutableList.of("test1", "test2", "test3"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void set_topics_too_few() {
        modifiableUnsubscribePacket.setTopicFilters(ImmutableList.of("test1"));
    }

    private ModifiableUnsubscribePacketImpl testUnsubscribePacket() {
        final ImmutableList<String> topics = ImmutableList.of("Test", "Test/Topic");
        final Mqtt5UserProperties props =
                Mqtt5UserProperties.builder().add(MqttUserProperty.of("Test", "TestValue")).build();
        final UNSUBSCRIBE unsubscribe = new UNSUBSCRIBE(topics, 1, props);
        return new ModifiableUnsubscribePacketImpl(configurationService, unsubscribe);
    }
}