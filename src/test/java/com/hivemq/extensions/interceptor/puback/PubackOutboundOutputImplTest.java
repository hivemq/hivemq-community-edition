package com.hivemq.extensions.interceptor.puback;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.packets.puback.ModifiablePubackPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.mqtt.message.puback.PUBACK;
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
public class PubackOutboundOutputImplTest {

    private PUBACK puback;

    @Mock
    private PluginOutPutAsyncer asyncer;
    private PubackOutboundOutputImpl output;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final FullConfigurationService fullConfigurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        puback = TestMessageUtil.createFullMqtt5Puback();
        output = new PubackOutboundOutputImpl(fullConfigurationService, asyncer, puback);
    }

    @Test
    public void test_getModifiable() {
        @Immutable final ModifiablePubackPacket modifiablePubackPacket = output.get().getPubackPacket();
        assertEquals(puback.getPacketIdentifier(), modifiablePubackPacket.getPacketIdentifier());
        assertEquals(puback.getReasonCode().name(), modifiablePubackPacket.getReasonCode().name());
        assertFalse(modifiablePubackPacket.getReasonString().isPresent());
        assertEquals(puback.getUserProperties().size(), modifiablePubackPacket.getUserProperties().asList().size());
    }
}