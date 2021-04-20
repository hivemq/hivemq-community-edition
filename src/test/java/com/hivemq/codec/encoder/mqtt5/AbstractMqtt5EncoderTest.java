package com.hivemq.codec.encoder.mqtt5;

import com.google.common.collect.ImmutableList;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class AbstractMqtt5EncoderTest {

    static final int MAX_PACKET_SIZE = 130;
    protected @NotNull EmbeddedChannel channel;

    protected void setUp(final @NotNull ChannelHandler encoder) throws Exception {

        channel = new EmbeddedChannel(encoder);
        channel.config().setAllocator(new UnpooledByteBufAllocator(false));
        channel.attr(ChannelAttributes.MAX_PACKET_SIZE_SEND).set((long) MAX_PACKET_SIZE);
        channel.attr(ChannelAttributes.REQUEST_PROBLEM_INFORMATION).set(true);
        channel.attr(ChannelAttributes.CLIENT_ID).set("clientId");
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(new ClientConnection());
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv5);

    }

    void encodeTestBufferSize(final byte @NotNull [] expected, final @NotNull MessageWithID message, final int bufferSize) {
        channel.writeOutbound(message);
        final ByteBuf buf = channel.readOutbound();

        assertTrue("buffer size is shorter than the expected length", bufferSize >= expected.length);

        assertEquals(expected.length, bufferSize);

        try {
            assertEquals(expected.length, buf.readableBytes());
            for (int i = 0; i < expected.length; i++) {
                assertEquals("ByteBuf differed at index " + i, expected[i], buf.readByte());
            }
        } finally {
            buf.release();
        }
    }

    private final @NotNull String user = "user";
    private final @NotNull String property = "property";
    final int userPropertyBytes = 1 // identifier
            + 2 // key length
            + 4 // bytes to encode "user"
            + 2 // value length
            + 8; // bytes to encode "property"

    final private @NotNull MqttUserProperty userProperty = new MqttUserProperty(user, property);

    @NotNull Mqtt5UserProperties getUserProperties(final int totalCount) {
        final ImmutableList.Builder<MqttUserProperty> builder = new ImmutableList.Builder<>();
        for (int i = 0; i < totalCount; i++) {
            builder.add(userProperty);
        }
        return Mqtt5UserProperties.of(builder.build());
    }

    @NotNull String getPaddedUtf8String(final int length) {
        final char[] reasonString = new char[length];
        Arrays.fill(reasonString, 'r');
        return new String(reasonString);
    }

    private int getMaxPropertyLength(final int maxPacketSize) {
        return maxPacketSize - 1  // type, reserved
                - 3  // remaining length
                - 1  // session present
                - 1  // reason code
                - 1;  // property length
    }

    class MaximumPacketBuilder {
        int maxUserPropertyCount;

        int remainingPropertyBytes;

        @NotNull MaximumPacketBuilder build(final int maxPacketSize) {
            // MQTT v5.0 Spec §3.4.11
            final int maxPropertyLength = getMaxPropertyLength(maxPacketSize);

            remainingPropertyBytes = maxPropertyLength % userPropertyBytes;

            maxUserPropertyCount = maxPropertyLength / userPropertyBytes;
            final ImmutableList.Builder<MqttUserProperty> userPropertiesBuilder = new ImmutableList.Builder<>();
            final MqttUserProperty userProperty = new MqttUserProperty(user, property);
            for (int i = 0; i < maxUserPropertyCount; i++) {
                userPropertiesBuilder.add(userProperty);
            }

            return this;
        }

        int getMaxUserPropertiesCount() {
            return maxUserPropertyCount;
        }

    }

}
