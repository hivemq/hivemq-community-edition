package com.hivemq.extensions.interceptor.pubrec;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.packets.pubrec.ModifiablePubrecPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class PubrecInboundOutputImplTest {

    private PUBREC pubrec;

    @Mock
    private PluginOutPutAsyncer asyncer;
    private PubrecInboundOutputImpl output;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final FullConfigurationService fullConfigurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();
        pubrec = TestMessageUtil.createSuccessPubrec();
        output = new PubrecInboundOutputImpl(fullConfigurationService, asyncer, pubrec);
    }

    @Test
    public void test_getModifiable() {
        @Immutable final ModifiablePubrecPacket modifiablePubrecPacket = output.get().getPubrecPacket();
        assertEquals(pubrec.getPacketIdentifier(), modifiablePubrecPacket.getPacketIdentifier());
        assertEquals(pubrec.getReasonCode().name(), modifiablePubrecPacket.getReasonCode().name());
        assertFalse(modifiablePubrecPacket.getReasonString().isPresent());
        assertEquals(pubrec.getUserProperties().size(), modifiablePubrecPacket.getUserProperties().asList().size());
    }

}
