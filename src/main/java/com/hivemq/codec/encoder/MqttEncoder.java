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
package com.hivemq.codec.encoder;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.Message;
import io.netty.buffer.ByteBuf;

/**
 * The Encoder is used to encode mqtt messages.
 *
 * @author Waldemar Ruck
 * @since 4.0
 */
public interface MqttEncoder<T extends Message> {

    /**
     * Encode a mqtt message into a {@link ByteBuf}. This method will be called for each written message that can be
     * handled by this encoder.
     *
     * @param clientConnection the {@link ClientConnection} of the client
     * @param msg              the message to encode
     * @param out              the {@link ByteBuf} into which the encoded message will be written
     */
    void encode(@NotNull ClientConnection clientConnection, @NotNull T msg, @NotNull ByteBuf out);

    /**
     * Calculate the buffer size for an mqtt message. This method will be called everytime
     * before {@link #encode} is called.
     *
     * @param clientConnection the {@link ClientConnection} of the client
     * @param msg              the message for which the buffer size should be calculated.
     * @return the required buffer size for the {@code msg}.
     */
    int bufferSize(@NotNull ClientConnection clientConnection, @NotNull T msg);
}
