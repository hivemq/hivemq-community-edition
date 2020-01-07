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
package com.hivemq.extensions.interceptor.unsubscribe.parameter;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.unsubscribe.ModifiableUnsubscribePacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Robin Atherton
 */
public class UnsubscribeInboundOutputImplTest {

    @Mock
    private PluginOutPutAsyncer pluginOutPutAsyncer;
    private UnsubscribeInboundOutputImpl output;
    private UNSUBSCRIBE unsubscribe;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final FullConfigurationService config = new TestConfigurationBootstrap().getFullConfigurationService();
        unsubscribe = TestMessageUtil.createFullMqtt5Unsubscribe();
        output = new UnsubscribeInboundOutputImpl(pluginOutPutAsyncer, config, unsubscribe);
        assertEquals(output, output.get());
    }

    @Test
    public void test_getModifiable() {
        final ModifiableUnsubscribePacket modifiableUnsubscribePacket = output.get().getUnsubscribePacket();
        final ArrayList<String> strings = new ArrayList<>();
        strings.add("different1");
        strings.add("different2");
        strings.add("different3");
        modifiableUnsubscribePacket.setTopicFilters(strings);
        assertNotEquals(unsubscribe.getTopics(), modifiableUnsubscribePacket.getTopicFilters());
    }
}
