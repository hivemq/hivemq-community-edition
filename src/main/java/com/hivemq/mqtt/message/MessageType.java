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

    private final int type;

    MessageType(final int type) {

        this.type = type;
    }

    public static MessageType valueOf(final int i) {

        for (final MessageType messageType : MessageType.values()) {
            if (messageType.type == i) {
                return messageType;
            }
        }
        throw new IllegalArgumentException("Invalid Message Type: " + i);
    }

    public int getType() {
        return type;
    }
}