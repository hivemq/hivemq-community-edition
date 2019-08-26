package com.hivemq.extensions.interceptor.disconnect;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableDisconnectPacket;
import com.hivemq.extensions.packets.disconnect.DisconnectPacketImpl;
import com.hivemq.extensions.packets.disconnect.ModifiableDisconnectPacketImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Test;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

public class DisconnectInboundInputImplTest {

    @Test
    public void test_construction() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final DisconnectPacket disconnectPacket = new DisconnectPacketImpl(TestMessageUtil.createFullMqtt5Disconnect());
        final DisconnectInboundInputImpl input =
                new DisconnectInboundInputImpl(disconnectPacket, "client", embeddedChannel);
        Assert.assertNotNull(input.getClientInformation());
        Assert.assertNotNull(input.getConnectionInformation());
        Assert.assertNotNull(input.getDisconnectPacket());
    }

    @Test(expected = NullPointerException.class)
    public void test_clientId_null() {
        final DisconnectPacketImpl disconnectPacket =
                new DisconnectPacketImpl(TestMessageUtil.createFullMqtt5Disconnect());
        final DisconnectInboundInputImpl disconnectInboundInput =
                new DisconnectInboundInputImpl(disconnectPacket, null, new EmbeddedChannel());
    }

    @Test(expected = NullPointerException.class)
    public void test_channel_null() {
        final DisconnectPacketImpl disconnectPacket =
                new DisconnectPacketImpl(TestMessageUtil.createFullMqtt5Disconnect());
        final DisconnectInboundInputImpl disconnectInboundInput =
                new DisconnectInboundInputImpl(disconnectPacket, "client", null);
    }

    @Test(expected = NullPointerException.class)
    public void test_packet_null() {
        final DisconnectInboundInputImpl disconnectInboundInput =
                new DisconnectInboundInputImpl(null, null, new EmbeddedChannel());
    }

    @Test
    public void test_update() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final DisconnectPacketImpl disconnectPacket1 =
                new DisconnectPacketImpl(TestMessageUtil.createFullMqtt5Disconnect());
        final DisconnectPacketImpl disconnectPacket2 =
                new DisconnectPacketImpl(TestMessageUtil.createFullMqtt5Disconnect());

        final DisconnectInboundInputImpl input =
                new DisconnectInboundInputImpl(disconnectPacket1, "client", embeddedChannel);
        input.updateDisconnect(disconnectPacket2);

        Assert.assertNotEquals(input.getDisconnectPacket(), disconnectPacket1);
        Assert.assertEquals(input.getDisconnectPacket(), disconnectPacket2);
    }

    @Test
    public void test_overrideReasonString() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final FullConfigurationService fullConfigurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();
        final DISCONNECT disconnect = TestMessageUtil.createFullMqtt5Disconnect();

        final ModifiableDisconnectPacket disconnectPacket =
                new ModifiableDisconnectPacketImpl(fullConfigurationService, disconnect);
        disconnectPacket.setReasonString("modified");

        Assert.assertEquals("modified", disconnectPacket.getReasonString());
    }

}