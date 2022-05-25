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
package com.hivemq.mqtt.handler.publish;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Christoph Schäbel
 */
public enum PublishStatus {

    /**
     * message has been delivered
     */
    DELIVERED(1),

    /**
     * client is not connected
     */
    NOT_CONNECTED(2),

    /**
     * the message could not be delivered
     */
    FAILED(3),

    /**
     * the message is not yet delivered
     */
    IN_PROGRESS(4),

    /**
     * client socket is not writable (QoS 0 only)
     */
    CHANNEL_NOT_WRITABLE(5);

    private static final @NotNull PublishStatus @NotNull [] VALUES = values();

    private final int id;

    PublishStatus(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static @NotNull PublishStatus valueOf(final int i) {
        try {
            return VALUES[i - 1];
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("No state found for the given value : " + i + ".", e);
        }
    }
}
