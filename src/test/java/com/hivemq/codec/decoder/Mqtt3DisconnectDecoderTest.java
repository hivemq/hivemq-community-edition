package com.hivemq.codec.decoder;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import util.TestMqttDecoder;

import static org.junit.Assert.*;

public class Mqtt3DisconnectDecoderTest {

    private @NotNull EmbeddedChannel embeddedChannel;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        embeddedChannel = new EmbeddedChannel(TestMqttDecoder.create());
        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).set(new ClientConnection());
        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
    }

    @Test
    public void test_disconnect_received() {
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1110_0000);
        buf.writeByte(0b0000_0000);
        embeddedChannel.writeInbound(buf);

        final DISCONNECT disconnect = embeddedChannel.readInbound();

        assertNotNull(disconnect);

        assertTrue(embeddedChannel.isActive());
    }

    @Test
    public void test_disconnect_invalid_header_mqtt_311() {
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1110_0010);
        buf.writeByte(0b0000_0000);
        embeddedChannel.writeInbound(buf);


        //The client needs to get disconnected
        assertFalse(embeddedChannel.isActive());
    }

    @Test
    public void test_disconnect_invalid_header_mqtt_31() {

        //In this test we check that additional headers are ignored in MQTT 3.1 if they're invalid
        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv3_1);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1110_0010);
        buf.writeByte(0b0000_0000);
        embeddedChannel.writeInbound(buf);

        final DISCONNECT disconnect = embeddedChannel.readInbound();

        assertNotNull(disconnect);

        assertTrue(embeddedChannel.isActive());
    }

}