package com.hivemq.extensions.interceptor.pubcomp.parameter;

import com.hivemq.extensions.packets.pubcomp.PubcompPacketImpl;
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
public class PubcompOutboundInputImplTest {

    @Test
    public void test_construction_success() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);


        final PubcompPacketImpl pubcomPacket = new PubcompPacketImpl(TestMessageUtil.createSuccessPupcomp());

        final PubcompOutboundInputImpl input = new PubcompOutboundInputImpl(pubcomPacket, "client", embeddedChannel);
        assertNotNull(input.getClientInformation());
        assertNotNull(input.getConnectionInformation());
        assertNotNull(input.getPubcompPacket());
    }

    @Test(expected = NullPointerException.class)
    public void test_client_id_null() {
        final PubcompPacketImpl pubcomPacket = new PubcompPacketImpl(TestMessageUtil.createSuccessPupcomp());
        final PubcompOutboundInputImpl input = new PubcompOutboundInputImpl(pubcomPacket, null, new EmbeddedChannel());
    }

    @Test(expected = NullPointerException.class)
    public void test_channel_null() {
        final PubcompPacketImpl pubcomPacket = new PubcompPacketImpl(TestMessageUtil.createSuccessPupcomp());
        final PubcompOutboundInputImpl input = new PubcompOutboundInputImpl(pubcomPacket, "client", null);
    }

    @Test(expected = NullPointerException.class)
    public void test_packet_null() {
        final PubcompOutboundInputImpl input = new PubcompOutboundInputImpl(null, "client", new EmbeddedChannel());
    }

    @Test
    public void test_update() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);


        final PubcompPacketImpl pubcomPacket1 = new PubcompPacketImpl(TestMessageUtil.createSuccessPupcomp());
        final PubcompPacketImpl pubcomPacket2 = new PubcompPacketImpl(TestMessageUtil.createSuccessPupcomp());

        final PubcompOutboundInputImpl input = new PubcompOutboundInputImpl(pubcomPacket1, "client", embeddedChannel);
        input.updatePubcomp(pubcomPacket2);

        assertNotSame(pubcomPacket1, input.getPubcompPacket());
        assertNotSame(pubcomPacket2, input.getPubcompPacket());
    }
}