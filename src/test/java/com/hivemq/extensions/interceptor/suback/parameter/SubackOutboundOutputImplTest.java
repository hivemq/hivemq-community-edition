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

package com.hivemq.extensions.interceptor.suback.parameter;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.suback.ModifiableSubackPacket;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Robin Atherton
 */
public class SubackOutboundOutputImplTest {


    private @NotNull SUBACK subAck;

    @Mock
    private @NotNull PluginOutPutAsyncer asyncer;
    private @NotNull SubackOutboundOutputImpl output;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final FullConfigurationService configurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();
        subAck = TestMessageUtil.createFullMqtt5Suback();
        output = new com.hivemq.extensions.interceptor.suback.parameter.SubackOutboundOutputImpl(configurationService, asyncer, subAck);
    }

    @Test
    public void test_getModifiable() {
        final ModifiableSubackPacket modifiableSubAckPacket = output.get().getSubackPacket();
        assertEquals(subAck.getPacketIdentifier(), modifiableSubAckPacket.getPacketIdentifier());
        assertEquals(
                subAck.getUserProperties().size(), modifiableSubAckPacket.getUserProperties().asList().size());
        assertEquals(subAck.getReasonString(), modifiableSubAckPacket.getReasonString().get());
        final List<SubackReasonCode> reasonCodes = modifiableSubAckPacket.getReasonCodes();
        for (int i = 0; i < reasonCodes.size(); i++) {
            assertEquals(subAck.getReasonCodes().get(i), Mqtt5SubAckReasonCode.from(reasonCodes.get(i)));
        }
    }

}