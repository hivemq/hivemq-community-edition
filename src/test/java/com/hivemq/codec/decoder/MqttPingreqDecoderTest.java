package com.hivemq.codec.decoder;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import util.TestMqttDecoder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Florian Limpöck
 */
public class MqttPingreqDecoderTest {

    private @NotNull EmbeddedChannel embeddedChannel;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        embeddedChannel = new EmbeddedChannel(TestMqttDecoder.create());
    }

    @Test
    public void test_ping_request_received_mqtt_311() {

        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).set(new ClientConnection());
        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1100_0000);
        buf.writeByte(0b0000_0000);
        embeddedChannel.writeInbound(buf);

        final Object pingreq = embeddedChannel.readInbound();

        assertTrue(pingreq instanceof PINGREQ);

        assertTrue(embeddedChannel.isActive());
    }

    @Test
    public void test_ping_request_received_mqtt_5() {

        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).set(new ClientConnection());
        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv5);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1100_0000);
        buf.writeByte(0b0000_0000);
        embeddedChannel.writeInbound(buf);

        final Object pingreq = embeddedChannel.readInbound();

        assertTrue(pingreq instanceof PINGREQ);

        assertTrue(embeddedChannel.isActive());
    }

    @Test
    public void test_ping_request_invalid_header_mqtt_311() {

        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).set(new ClientConnection());
        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1100_0001);
        buf.writeByte(0b0000_0000);
        embeddedChannel.writeInbound(buf);


        //The client needs to get disconnected
        assertFalse(embeddedChannel.isActive());
    }

    @Test
    public void test_ping_request_invalid_header_mqtt_5() {

        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).set(new ClientConnection());
        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv5);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1100_0001);
        buf.writeByte(0b0000_0000);
        embeddedChannel.writeInbound(buf);


        //The client needs to get disconnected
        assertFalse(embeddedChannel.isActive());
    }

    @Test
    public void test_ping_request_invalid_header_ignored_mqtt_31() {

        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).set(new ClientConnection());
        embeddedChannel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv3_1);
        //In this test we check that additional headers are ignored in MQTT 3.1 if they're invalid

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1100_0001);
        buf.writeByte(0b0000_0000);
        embeddedChannel.writeInbound(buf);

        final Object pingreq = embeddedChannel.readInbound();

        assertTrue(pingreq instanceof PINGREQ);

        assertTrue(embeddedChannel.isActive());
    }

}