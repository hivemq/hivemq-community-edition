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
package com.hivemq.codec.encoder.mqtt3;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.codec.encoder.MqttEncoder;
import com.hivemq.codec.encoder.mqtt5.MqttMessageEncoderUtil;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.Message;
import io.netty.buffer.ByteBuf;

/**
 * An abstract encoder for MQTT messages which have a variable payload length and
 * need a remaining length header for that
 *
 * @author Dominik Obermaier
 */
public abstract class AbstractVariableHeaderLengthEncoder<T extends Message> implements MqttEncoder<T> {

    protected static void createRemainingLength(final int messageLength, final @NotNull ByteBuf buffer) {
        int val = messageLength;

        do {
            byte b = (byte) (val % 128);
            val = val / 128;
            if (val > 0) {
                b = (byte) (b | (byte) 128);
            }
            buffer.writeByte(b);
        } while (val > 0);
    }

    @Override
    public int bufferSize(final @NotNull ClientConnection clientConnection, final @NotNull T msg) {

        final int remainingLength = remainingLength(msg);
        final int encodedLengthWithHeader = MqttMessageEncoderUtil.encodedPacketLength(remainingLength);

        msg.setRemainingLength(remainingLength);
        msg.setEncodedLength(encodedLengthWithHeader);

        return encodedLengthWithHeader;
    }

    protected abstract int remainingLength(@NotNull T msg);
}
