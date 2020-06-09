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

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Dominik Obermaier
 * @since 1.4
 */
public abstract class MessageWithID implements Message {

    protected int packetIdentifier;

    public MessageWithID() {
    }

    public MessageWithID(final int packetIdentifier) {
        this.packetIdentifier = packetIdentifier;
    }

    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    public void setPacketIdentifier(final int packetIdentifier) {

        checkArgument(packetIdentifier <= 65535, "Message id %s is invalid. Max message id is 65535.", packetIdentifier);

        // -1 for QoS 0 publishes. (the only message with optional packet identifier)
        checkArgument(packetIdentifier >= -1, "Message id %s is invalid. Min message id is -1.", packetIdentifier);

        this.packetIdentifier = packetIdentifier;
    }
}
