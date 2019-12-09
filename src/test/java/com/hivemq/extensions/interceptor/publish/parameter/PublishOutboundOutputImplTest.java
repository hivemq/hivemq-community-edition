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
package com.hivemq.extensions.interceptor.publish.parameter;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

/**
 * @author Lukas Brandl
 */
public class PublishOutboundOutputImplTest {

    private FullConfigurationService configurationService;

    @Mock
    private PluginOutPutAsyncer asyncer;

    private PublishOutboundOutputImpl output;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        output = new PublishOutboundOutputImpl(configurationService, asyncer, TestMessageUtil.createFullMqtt5Publish());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_prevent_twice() {
        output.preventPublishDelivery();
        output.preventPublishDelivery();
    }
}