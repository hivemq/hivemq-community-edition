package com.hivemq.extensions.interceptor.pubrec;

import com.hivemq.extensions.packets.pubrec.PubrecPacketImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;
import util.TestMessageUtil;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

/**
 * @author Yannick Weber
 */
public class PubrecInboundInputImplTest {

    @Test
    public void test_construction_success() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PubrecInboundInputImpl input = new PubrecInboundInputImpl(
                "client", embeddedChannel, TestMessageUtil.createSuccessPubrec());
        assertNotNull(input.getClientInformation());
        assertNotNull(input.getConnectionInformation());
        assertNotNull(input.getPubrecPacket());
    }

    @Test(expected = NullPointerException.class)
    public void test_client_id_null() {
        new PubrecInboundInputImpl(null, new EmbeddedChannel(), TestMessageUtil.createSuccessPubrec());
    }

    @Test(expected = NullPointerException.class)
    public void test_channel_null() {
        new PubrecInboundInputImpl("client", null, TestMessageUtil.createSuccessPubrec());
    }

    @Test(expected = NullPointerException.class)
    public void test_packet_null() {
        new PubrecInboundInputImpl("client", new EmbeddedChannel(), null);
    }

    @Test
    public void test_update() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBREC pubrecPacket1 = TestMessageUtil.createSuccessPubrec();
        final PUBREC pubrecPacket2 = TestMessageUtil.createSuccessPubrec();

        final PubrecInboundInputImpl input = new PubrecInboundInputImpl("client", embeddedChannel, pubrecPacket1);
        input.update(new PubrecPacketImpl(pubrecPacket2));

        assertNotSame(pubrecPacket1, input.getPubrecPacket());
        assertNotSame(pubrecPacket2, input.getPubrecPacket());
    }

}
