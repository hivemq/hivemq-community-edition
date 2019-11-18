package com.hivemq.extensions.interceptor.disconnect;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableInboundDisconnectPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import static org.junit.Assert.assertEquals;

public class DisconnectInboundOutputImplTest {

    private DISCONNECT disconnect;

    @Mock
    private PluginOutPutAsyncer asyncer;
    private DisconnectInboundOutputImpl output;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final FullConfigurationService configurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();
        disconnect = TestMessageUtil.createFullMqtt5Disconnect();
        output = new DisconnectInboundOutputImpl(configurationService, asyncer, disconnect, 0);
    }

    @Test
    public void test_getModifiable() {
        final ModifiableInboundDisconnectPacket modifiableInboundDisconnectPacket = output.get().getDisconnectPacket();
        assertEquals(disconnect.getServerReference(), modifiableInboundDisconnectPacket.getServerReference().get());
        assertEquals(
                disconnect.getSessionExpiryInterval(),
                (long) modifiableInboundDisconnectPacket.getSessionExpiryInterval().get());
        assertEquals(disconnect.getReasonCode().name(), modifiableInboundDisconnectPacket.getReasonCode().name());
        assertEquals(disconnect.getReasonString(), modifiableInboundDisconnectPacket.getReasonString().get());
        assertEquals(
                disconnect.getUserProperties().size(),
                modifiableInboundDisconnectPacket.getUserProperties().asList().size());
    }
}