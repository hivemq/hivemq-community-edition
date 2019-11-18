package com.hivemq.extensions.interceptor.disconnect;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableOutboundDisconnectPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import static org.junit.Assert.assertEquals;

public class DisconnectOutboundOutputImplTest {

    private DISCONNECT disconnect;

    @Mock
    private PluginOutPutAsyncer asyncer;
    private DisconnectOutboundOutputImpl output;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final FullConfigurationService configurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();
        disconnect = TestMessageUtil.createFullMqtt5Disconnect();
        output = new DisconnectOutboundOutputImpl(configurationService, asyncer, disconnect);
    }

    @Test
    public void test_getModifiable() {
        final ModifiableOutboundDisconnectPacket modifiableDisconnectPacket = output.get().getDisconnectPacket();
        assertEquals(disconnect.getServerReference(), modifiableDisconnectPacket.getServerReference().get());
        assertEquals(
                disconnect.getSessionExpiryInterval(),
                (long) modifiableDisconnectPacket.getSessionExpiryInterval().get());
        assertEquals(disconnect.getReasonCode().name(), modifiableDisconnectPacket.getReasonCode().name());
        assertEquals(disconnect.getReasonString(), modifiableDisconnectPacket.getReasonString().get());
        assertEquals(
                disconnect.getUserProperties().size(), modifiableDisconnectPacket.getUserProperties().asList().size());
    }

}