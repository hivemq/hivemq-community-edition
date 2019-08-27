package com.hivemq.extensions.interceptor.pubrec;

import com.hivemq.extensions.packets.pubrec.PubrecPacketImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;
import util.TestMessageUtil;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

public class PubrecInboundInputImplTest {

    @Test
    public void test_construction_success() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);


        final PubrecPacketImpl pubrecPacket = new PubrecPacketImpl(TestMessageUtil.createFullMqtt5Pubrec());

        final PubrecInboundInputImpl input = new PubrecInboundInputImpl(pubrecPacket, "client", embeddedChannel);
        assertNotNull(input.getClientInformation());
        assertNotNull(input.getConnectionInformation());
        assertNotNull(input.getPubrecPacket());
    }

    @Test(expected = NullPointerException.class)
    public void test_client_id_null() {
        final PubrecPacketImpl pubrecPacket = new PubrecPacketImpl(TestMessageUtil.createFullMqtt5Pubrec());
        final PubrecInboundInputImpl input = new PubrecInboundInputImpl(pubrecPacket, null, new EmbeddedChannel());
    }

    @Test(expected = NullPointerException.class)
    public void test_channel_null() {
        final PubrecPacketImpl pubrecPacket = new PubrecPacketImpl(TestMessageUtil.createFullMqtt5Pubrec());
        final PubrecInboundInputImpl input = new PubrecInboundInputImpl(pubrecPacket, "client", null);
    }

    @Test(expected = NullPointerException.class)
    public void test_packet_null() {
        final PubrecInboundInputImpl input = new PubrecInboundInputImpl(null, "client", new EmbeddedChannel());
    }

    @Test
    public void test_update() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);


        final PubrecPacketImpl pubrecPacket1 = new PubrecPacketImpl(TestMessageUtil.createFullMqtt5Pubrec());
        final PubrecPacketImpl pubrecPacket2 = new PubrecPacketImpl(TestMessageUtil.createFullMqtt5Pubrec());

        final PubrecInboundInputImpl input = new PubrecInboundInputImpl(pubrecPacket1, "client", embeddedChannel);
        input.updatePubrec(pubrecPacket2);

        assertNotSame(pubrecPacket1, input.getPubrecPacket());
        assertNotSame(pubrecPacket2, input.getPubrecPacket());
    }

}
