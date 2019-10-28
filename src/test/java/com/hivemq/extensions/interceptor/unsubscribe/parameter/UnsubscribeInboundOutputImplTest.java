package com.hivemq.extensions.interceptor.unsubscribe.parameter;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import static org.junit.Assert.assertEquals;

/**
 * @author Robin Atherton
 */
public class UnsubscribeInboundOutputImplTest {

    private UnsubscribeInboundOutputImpl unsubscribeInboundOutput;

    private FullConfigurationService config;

    @Mock
    private PluginOutPutAsyncer pluginOutPutAsyncer;

    private UNSUBSCRIBE unsubscribe;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        config = new TestConfigurationBootstrap().getFullConfigurationService();
        unsubscribe = TestMessageUtil.createFullMqtt5Unsubscribe();
        unsubscribeInboundOutput = new UnsubscribeInboundOutputImpl(pluginOutPutAsyncer, config, unsubscribe);
        assertEquals(unsubscribeInboundOutput, unsubscribeInboundOutput.get());
    }

}
