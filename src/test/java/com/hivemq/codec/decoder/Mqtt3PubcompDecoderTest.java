package com.hivemq.codec.decoder;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import util.TestMqttDecoder;

import static org.junit.Assert.*;

public class Mqtt3PubcompDecoderTest {

    private @NotNull EmbeddedChannel embeddedChannel;
    private @NotNull ClientConnection clientConnection;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        embeddedChannel = new EmbeddedChannel(TestMqttDecoder.create());
        clientConnection = new ClientConnection();
        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).set(clientConnection);
    }

    @Test
    public void test_pubcomp_received() {
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0111_0000);
        buf.writeByte(0b0000_0010);
        buf.writeShort(55555);
        embeddedChannel.writeInbound(buf);

        final PUBCOMP pubcomp = embeddedChannel.readInbound();

        assertEquals(55555, pubcomp.getPacketIdentifier());

        assertTrue(embeddedChannel.isActive());
    }

    @Test
    public void test_pubcomp_invalid_header_mqtt_311() {
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0111_0010);
        buf.writeByte(0b0000_0010);
        buf.writeShort(55555);
        embeddedChannel.writeInbound(buf);


        //The client needs to get disconnected
        assertFalse(embeddedChannel.isActive());
    }

    @Test
    public void test_pubcomp_invalid_header_mqtt_31() {

        //In this test we check that additional headers are ignored in MQTT 3.1 if they're invalid
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0111_0010);
        buf.writeByte(0b0000_0010);
        buf.writeShort(55555);
        embeddedChannel.writeInbound(buf);

        final PUBCOMP pubcomp = embeddedChannel.readInbound();

        assertEquals(55555, pubcomp.getPacketIdentifier());

        assertTrue(embeddedChannel.isActive());
    }

}