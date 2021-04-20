package com.hivemq.codec.decoder;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import util.TestMqttDecoder;

import static org.junit.Assert.*;

public class Mqtt3UnsubscribeDecoderTest {

    private @NotNull EmbeddedChannel embeddedChannel;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        embeddedChannel = new EmbeddedChannel(TestMqttDecoder.create());
        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).set(new ClientConnection());
        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
    }

    @Test
    public void test_unsubscribe_received() {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1010_0010);
        //Remaining length
        buf.writeByte(12);

        //MessageID
        buf.writeShort(55555);

        // "a/b"
        buf.writeBytes(new byte[]{0, 3, 0x61, 0x2F, 0x62});

        // "c/d"
        buf.writeBytes(new byte[]{0, 3, 0x63, 0x2F, 0x64});


        embeddedChannel.writeInbound(buf);

        final UNSUBSCRIBE subscribe = embeddedChannel.readInbound();

        assertNotNull(subscribe);

        assertEquals(55555, subscribe.getPacketIdentifier());
        assertEquals(2, subscribe.getTopics().size());

        assertEquals("a/b", subscribe.getTopics().get(0));

        assertEquals("c/d", subscribe.getTopics().get(1));

        assertTrue(embeddedChannel.isActive());
    }

    @Test
    public void test_empty_unsubscribe() {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1010_0010);
        //Remaining length
        buf.writeByte(2);

        //MessageID
        buf.writeShort(55555);

        embeddedChannel.writeInbound(buf);

        final UNSUBSCRIBE unsubscribe = embeddedChannel.readInbound();

        assertNull(unsubscribe);


        //Client got disconnected
        assertFalse(embeddedChannel.isActive());
    }

    @Test
    public void test_unsubscribe_has_empty_topic() {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1010_0010);
        //Remaining length
        buf.writeByte(4);

        //MessageID
        buf.writeShort(55555);

        // Empty
        buf.writeBytes(new byte[]{0, 0});


        embeddedChannel.writeInbound(buf);


        final UNSUBSCRIBE unsubscribe = embeddedChannel.readInbound();

        assertNull(unsubscribe);

        //Client got disconnected
        assertFalse(embeddedChannel.isActive());
    }

    @Test
    public void test_unsubscribe_invalid_header_mqtt_311() {

        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).set(new ClientConnection());
        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1010_0100);
        //Remaining length
        buf.writeByte(12);

        //MessageID
        buf.writeShort(55555);

        // "a/b"
        buf.writeBytes(new byte[]{0, 3, 0x61, 0x2F, 0x62});

        // "c/d"
        buf.writeBytes(new byte[]{0, 3, 0x63, 0x2F, 0x64});

        embeddedChannel.writeInbound(buf);


        //The client needs to get disconnected
        assertFalse(embeddedChannel.isActive());
    }

    @Test
    public void test_unsubscribe_invalid_header_mqtt_31() {

        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).set(new ClientConnection());
        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv3_1);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1010_0100);
        //Remaining length
        buf.writeByte(12);

        //MessageID
        buf.writeShort(55555);

        // "a/b"
        buf.writeBytes(new byte[]{0, 3, 0x61, 0x2F, 0x62});

        // "c/d"
        buf.writeBytes(new byte[]{0, 3, 0x63, 0x2F, 0x64});

        embeddedChannel.writeInbound(buf);


        //The client needs to get disconnected
        assertFalse(embeddedChannel.isActive());
    }

}