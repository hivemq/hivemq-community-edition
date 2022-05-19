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
package com.hivemq.mqtt.message;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Dominik Obermaier
 * @since 1.4
 */
public enum MessageType {
    RESERVED_ZERO(0),
    CONNECT(1),
    CONNACK(2),
    PUBLISH(3),
    PUBACK(4),
    PUBREC(5),
    PUBREL(6),
    PUBCOMP(7),
    SUBSCRIBE(8),
    SUBACK(9),
    UNSUBSCRIBE(10),
    UNSUBACK(11),
    PINGREQ(12),
    PINGRESP(13),
    DISCONNECT(14),
    AUTH(15);

    private static final @NotNull MessageType @NotNull [] VALUES = values();

    private final int type;

    MessageType(final int type) {

        this.type = type;
    }

    public static @NotNull MessageType valueOf(final int i) {
        try {
            return VALUES[i];
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid Message Type: " + i, e);
        }
    }

    public int getType() {
        return type;
    }
}