package com.hivemq.extensions.interceptor.puback;

import com.hivemq.extensions.packets.puback.PubackPacketImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Test;
import util.TestMessageUtil;

/**
 * @author Yannick Weber
 */
public class PubackOutboundInputImplTest {

    @Test
    public void test_construction() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBACK pubackPacket = TestMessageUtil.createSuccessMqtt5Puback();
        final PubackOutboundInputImpl input =
                new PubackOutboundInputImpl("client", embeddedChannel, pubackPacket);
        Assert.assertNotNull(input.getClientInformation());
        Assert.assertNotNull(input.getConnectionInformation());
        Assert.assertNotNull(input.getPubackPacket());
    }

    @Test(expected = NullPointerException.class)
    public void test_clientId_null() {
        final PUBACK pubackPacket = TestMessageUtil.createSuccessMqtt5Puback();
        new PubackOutboundInputImpl(null, new EmbeddedChannel(), pubackPacket);
    }

    @Test(expected = NullPointerException.class)
    public void test_channel_null() {
        final PUBACK pubackPacket = TestMessageUtil.createSuccessMqtt5Puback();
        new PubackOutboundInputImpl("client", null, pubackPacket);
    }

    @Test(expected = NullPointerException.class)
    public void test_packet_null() {
        new PubackOutboundInputImpl(null, new EmbeddedChannel(), null);
    }

    @Test
    public void test_update() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PUBACK puback1 = TestMessageUtil.createSuccessMqtt5Puback();
        final PUBACK puback2 = TestMessageUtil.createSuccessMqtt5Puback();

        final PubackOutboundInputImpl input =
                new PubackOutboundInputImpl("client", embeddedChannel, puback1);
        input.update(new PubackPacketImpl(puback1));

        Assert.assertNotSame(input.getPubackPacket(), puback1);
        Assert.assertNotSame(input.getPubackPacket(), puback2);
    }
}