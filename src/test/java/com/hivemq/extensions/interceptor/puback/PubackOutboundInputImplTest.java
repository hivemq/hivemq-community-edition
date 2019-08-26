package com.hivemq.extensions.interceptor.puback;

import com.hivemq.extensions.packets.puback.PubackPacketImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;
import util.TestMessageUtil;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

/**
 * @author Yannick Weber
 */
public class PubackOutboundInputImplTest {

    @Test
    public void test_construction_success() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);


        final PubackPacketImpl pubackPacket = new PubackPacketImpl(TestMessageUtil.createFullMqtt5Puback());

        final PubackOutboundInputImpl input = new PubackOutboundInputImpl(pubackPacket, "client", embeddedChannel);
        assertNotNull(input.getClientInformation());
        assertNotNull(input.getConnectionInformation());
        assertNotNull(input.getPubackPacket());
    }

    @Test(expected = NullPointerException.class)
    public void test_client_id_null() {
        final PubackPacketImpl pubackPacket = new PubackPacketImpl(TestMessageUtil.createFullMqtt5Puback());
        final PubackOutboundInputImpl input = new PubackOutboundInputImpl(pubackPacket, null, new EmbeddedChannel());
    }

    @Test(expected = NullPointerException.class)
    public void test_channel_null() {
        final PubackPacketImpl pubackPacket = new PubackPacketImpl(TestMessageUtil.createFullMqtt5Puback());
        final PubackOutboundInputImpl input = new PubackOutboundInputImpl(pubackPacket, "client", null);
    }

    @Test(expected = NullPointerException.class)
    public void test_packet_null() {
        final PubackOutboundInputImpl input = new PubackOutboundInputImpl(null, "client", new EmbeddedChannel());
    }

    @Test
    public void test_update() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);


        final PubackPacketImpl pubackPacket1 = new PubackPacketImpl(TestMessageUtil.createFullMqtt5Puback());
        final PubackPacketImpl pubackPacket2 = new PubackPacketImpl(TestMessageUtil.createFullMqtt5Puback());

        final PubackOutboundInputImpl input = new PubackOutboundInputImpl(pubackPacket1, "client", embeddedChannel);
        input.updatePuback(pubackPacket2);

        assertNotSame(pubackPacket1, input.getPubackPacket());
        assertNotSame(pubackPacket2, input.getPubackPacket());
    }
}