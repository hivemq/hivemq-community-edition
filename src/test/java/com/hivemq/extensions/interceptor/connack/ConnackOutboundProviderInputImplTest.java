package com.hivemq.extensions.interceptor.connack;

import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extensions.client.parameter.ServerInformationImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertNotNull;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class ConnackOutboundProviderInputImplTest {



    @Mock
    private ServerInformation serverInformation;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = NullPointerException.class)
    public void test_server_information_null() {
        new ConnackOutboundProviderInputImpl(null, new EmbeddedChannel(), "client");
    }

    @Test(expected = NullPointerException.class)
    public void test_channel_null() {
        new ConnackOutboundProviderInputImpl(serverInformation, null, "client");
    }

    @Test(expected = NullPointerException.class)
    public void test_id_null() {
        new ConnackOutboundProviderInputImpl(serverInformation, new EmbeddedChannel(), null);
    }

    @Test
    public void test_success() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final ConnackOutboundProviderInputImpl input = new ConnackOutboundProviderInputImpl(serverInformation, channel, "client");

        assertNotNull(input.getClientInformation());
        assertNotNull(input.getConnectionInformation());
        assertNotNull(input.getServerInformation());

    }
}