package com.hivemq.extensions.interceptor.pubrel.parameter;

import com.hivemq.extensions.packets.pubrel.PubrelPacketImpl;
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
public class PubrelOutboundInputImplTest {

    @Test
    public void test_construction_success() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);


        final PubrelPacketImpl pubrelPacket = new PubrelPacketImpl(TestMessageUtil.createSuccessPubrel());

        final PubrelOutboundInputImpl input = new PubrelOutboundInputImpl(pubrelPacket, "client", embeddedChannel);
        assertNotNull(input.getClientInformation());
        assertNotNull(input.getConnectionInformation());
        assertNotNull(input.getPubrelPacket());
    }

    @Test(expected = NullPointerException.class)
    public void test_client_id_null() {
        final PubrelPacketImpl pubrelPacket = new PubrelPacketImpl(TestMessageUtil.createSuccessPubrel());
        final PubrelOutboundInputImpl input = new PubrelOutboundInputImpl(pubrelPacket, null, new EmbeddedChannel());
    }

    @Test(expected = NullPointerException.class)
    public void test_channel_null() {
        final PubrelPacketImpl pubrelPacket = new PubrelPacketImpl(TestMessageUtil.createSuccessPubrel());
        final PubrelOutboundInputImpl input = new PubrelOutboundInputImpl(pubrelPacket, "client", null);
    }

    @Test(expected = NullPointerException.class)
    public void test_packet_null() {
        final PubrelOutboundInputImpl input = new PubrelOutboundInputImpl(null, "client", new EmbeddedChannel());
    }

    @Test
    public void test_update() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);


        final PubrelPacketImpl pubrelPacket1 = new PubrelPacketImpl(TestMessageUtil.createSuccessPubrel());
        final PubrelPacketImpl pubrelPacket2 = new PubrelPacketImpl(TestMessageUtil.createSuccessPubrel());

        final PubrelOutboundInputImpl input = new PubrelOutboundInputImpl(pubrelPacket1, "client", embeddedChannel);
        input.updatePubrel(pubrelPacket2);

        assertNotSame(pubrelPacket1, input.getPubrelPacket());
        assertNotSame(pubrelPacket2, input.getPubrelPacket());
    }
}