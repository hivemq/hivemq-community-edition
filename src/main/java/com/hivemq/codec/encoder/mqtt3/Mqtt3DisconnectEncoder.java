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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import io.netty.buffer.ByteBuf;

/**
 * @author Lukas Brandl
 */
public class Mqtt3DisconnectEncoder implements MqttEncoder<DISCONNECT> {

    public static final int ENCODED_DISCONNECT_SIZE = 2;
    private static final byte DISCONNECT_FIXED_HEADER = (byte) 0b1110_0000;
    private static final byte DISCONNECT_REMAINING_LENGTH = 0b0000_0000;

    @Override
    public void encode(
            final @NotNull ClientConnection clientConnection,
            final @NotNull DISCONNECT msg,
            final @NotNull ByteBuf out) {

        out.writeByte(DISCONNECT_FIXED_HEADER);
        out.writeByte(DISCONNECT_REMAINING_LENGTH);
    }

    @Override
    public int bufferSize(final @NotNull ClientConnection clientConnection, final @NotNull DISCONNECT msg) {
        return ENCODED_DISCONNECT_SIZE;
    }
}
