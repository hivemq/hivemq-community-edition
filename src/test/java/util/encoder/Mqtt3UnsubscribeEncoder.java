/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package util.encoder;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.codec.encoder.mqtt3.AbstractVariableHeaderLengthEncoder;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.util.Strings;
import com.hivemq.util.Utf8Utils;
import io.netty.buffer.ByteBuf;

import java.util.List;

/**
 * @author Lukas Brandl
 */
public class Mqtt3UnsubscribeEncoder extends AbstractVariableHeaderLengthEncoder<UNSUBSCRIBE> {

    private static final byte UNSUBSCRIBE_FIXED_HEADER = (byte) 0b1010_0010;

    @Override
    public void encode(
            final @NotNull ClientConnection clientConnection,
            final @NotNull UNSUBSCRIBE msg,
            final @NotNull ByteBuf out) {

        out.writeByte(UNSUBSCRIBE_FIXED_HEADER);
        createRemainingLength(msg.getRemainingLength(), out);

        out.writeShort(msg.getPacketIdentifier());

        final List<String> topics = msg.getTopics();
        for (final String topic : topics) {
            Strings.createPrefixedBytesFromString(topic, out);
        }
    }

    protected int remainingLength(final @NotNull UNSUBSCRIBE msg) {
        int length = 0;
        length += 2; // message ID
        for (final String topic : msg.getTopics()) {
            length += Utf8Utils.encodedLength(topic) + 2;
        }
        return length;
    }
}
