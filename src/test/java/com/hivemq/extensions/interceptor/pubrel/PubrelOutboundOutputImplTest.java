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
package com.hivemq.extensions.interceptor.pubrel;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.packets.pubrel.ModifiablePubrelPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Yannick Weber
 */
public class PubrelOutboundOutputImplTest {

    private PUBREL pubrel;

    @Mock
    private PluginOutPutAsyncer asyncer;
    private PubrelOutboundOutputImpl output;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final FullConfigurationService fullConfigurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();
        pubrel = TestMessageUtil.createSuccessPubrel();
        output = new PubrelOutboundOutputImpl(fullConfigurationService, asyncer, pubrel);
    }

    @Test
    public void test_getModifiable() {
        @Immutable final ModifiablePubrelPacket modifiablePubrelPacket = output.get().getPubrelPacket();
        assertEquals(pubrel.getPacketIdentifier(), modifiablePubrelPacket.getPacketIdentifier());
        assertEquals(pubrel.getReasonCode().name(), modifiablePubrelPacket.getReasonCode().name());
        assertFalse(modifiablePubrelPacket.getReasonString().isPresent());
        assertEquals(pubrel.getUserProperties().size(), modifiablePubrelPacket.getUserProperties().asList().size());
    }
}