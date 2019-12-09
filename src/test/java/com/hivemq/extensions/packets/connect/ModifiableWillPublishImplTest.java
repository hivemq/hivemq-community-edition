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
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extensions.services.builder.WillPublishBuilderImpl;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 * @author Lukas Brandl
 */
public class ModifiableWillPublishImplTest {

    private ModifiableWillPublishImpl willPublish;

    private WillPublishPacket original;

    private FullConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();

        original = new WillPublishBuilderImpl(configurationService)
                .willDelay(10)
                .topic("topic")
                .payload(ByteBuffer.wrap("message".getBytes()))
                .build();
        willPublish = new ModifiableWillPublishImpl(configurationService, original);

    }

    @Test
    public void test_modify() {
        willPublish.setWillDelay(15);
        final MqttWillPublish result = MqttWillPublish.fromWillPacket("clusterId", willPublish);
        assertEquals(15, result.getDelayInterval());
        assertTrue(willPublish.isModified());
    }

    @Test
    public void test_modify_same_value() {
        original = new WillPublishBuilderImpl(configurationService)
                .willDelay(15)
                .topic("topic")
                .payload(ByteBuffer.wrap("message".getBytes()))
                .build();
        willPublish = new ModifiableWillPublishImpl(configurationService, original);
        
        willPublish.setWillDelay(15);
        final MqttWillPublish result = MqttWillPublish.fromWillPacket("clusterId", willPublish);
        assertEquals(15, result.getDelayInterval());
        assertFalse(willPublish.isModified());
    }
}