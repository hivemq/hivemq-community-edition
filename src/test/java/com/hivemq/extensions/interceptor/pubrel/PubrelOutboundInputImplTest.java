package com.hivemq.extensions.interceptor.pubrel;

import com.hivemq.extensions.packets.pubrel.PubrelPacketImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.pubrel.PUBREL;
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

        final PubrelOutboundInputImpl input = new PubrelOutboundInputImpl(
                "client", embeddedChannel, TestMessageUtil.createSuccessPubrel());
        assertNotNull(input.getClientInformation());
        assertNotNull(input.getConnectionInformation());
        assertNotNull(input.getPubrelPacket());
    }

    @Test(expected = NullPointerException.class)
    public void test_client_id_null() {
        new PubrelOutboundInputImpl(null, new EmbeddedChannel(), TestMessageUtil.createSuccessPubrel());
    }

    @Test(expected = NullPointerException.class)
    public void test_channel_null() {
        new PubrelOutboundInputImpl("client", null, TestMessageUtil.createSuccessPubrel());
    }

    @Test(expected = NullPointerException.class)
    public void test_packet_null() {
        new PubrelOutboundInputImpl("client", new EmbeddedChannel(), null);
    }

    @Test
    public void test_update() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBREL pubrelPacket1 = TestMessageUtil.createSuccessPubrel();
        final PUBREL pubrelPacket2 = TestMessageUtil.createSuccessPubrel();

        final PubrelOutboundInputImpl input = new PubrelOutboundInputImpl("client", embeddedChannel, pubrelPacket1);
        input.update(new PubrelPacketImpl(pubrelPacket2));

        assertNotSame(pubrelPacket1, input.getPubrelPacket());
        assertNotSame(pubrelPacket2, input.getPubrelPacket());
    }
}