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