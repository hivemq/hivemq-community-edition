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
 * @author Florian Limpöck
 * @since 4.0.0
 */
public enum PublishReturnCode {

    /**
     * the publish was delivered.
     */
    DELIVERED(0),

    /**
     * the publish has no matching subscribers.
     */
    NO_MATCHING_SUBSCRIBERS(1),

    /**
     * the publish deliveration failed
     */
    FAILED(2);

    private static final @NotNull PublishReturnCode @NotNull [] VALUES = values();

    private final int id;

    PublishReturnCode(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static @NotNull PublishReturnCode valueOf(final int i) {
        try {
            return VALUES[i];
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("No publish return code found for the given value : " + i + ".", e);
        }
    }
}
