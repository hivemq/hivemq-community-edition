package util.encoder;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.codec.encoder.MqttEncoder;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.PINGREQ;
import io.netty.buffer.ByteBuf;

/**
 * @author Abdullah Imal
 */
public class PingreqEncoder implements MqttEncoder<PINGREQ> {

    private static final byte PINGREQ_FIXED_HEADER = (byte) 0b1100_0000;
    private static final byte PINGREQ_REMAINING_LENGTH = (byte) 0b0000_0000;
    private static final int ENCODED_PINGREQ_SIZE = 2;

    @Override
    public void encode(
            final @NotNull ClientConnection clientConnection,
            final @NotNull PINGREQ msg,
            final @NotNull ByteBuf out) {

        out.writeByte(PINGREQ_FIXED_HEADER);
        out.writeByte(PINGREQ_REMAINING_LENGTH);
    }

    @Override
    public int bufferSize(final @NotNull ClientConnection clientConnection, final @NotNull PINGREQ msg) {
        return ENCODED_PINGREQ_SIZE;
    }
}
