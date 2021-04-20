package com.hivemq.codec.decoder.mqtt5;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

abstract class AbstractMqtt5DecoderTest extends AbstractMqttDecoderTest {


    @Override
    protected void createChannel() {
        super.createChannel();
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
    }

    void decodeNullExpected(final byte @NotNull [] encoded) {
        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeBytes(encoded);
        channel.writeInbound(byteBuf);

        final Message message = channel.readInbound();
        assertNull(message);
        assertFalse(channel.isOpen());
        assertFalse(channel.isActive());

        createChannel();
    }

}