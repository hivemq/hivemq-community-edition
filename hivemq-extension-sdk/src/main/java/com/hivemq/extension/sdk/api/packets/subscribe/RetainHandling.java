/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extension.sdk.api.packets.subscribe;

import com.hivemq.extension.sdk.api.annotations.Nullable;

/**
 * The retain handling of a subscription.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
public enum RetainHandling {

    /**
     * Send every retained message for any subscription.
     *
     * @since 4.0.0
     */
    SEND(0),

    /**
     * Send every retained message for new subscriptions only.
     *
     * @since 4.0.0
     */
    SEND_IF_NEW_SUBSCRIPTION(1),

    /**
     * Never send a retained message for the subscription.
     *
     * @since 4.0.0
     */
    DO_NOT_SEND(2);

    /**
     * Retain handling as integer representation.
     *
     * @since 4.0.0
     */
    final int code;

    RetainHandling(final int code) {
        this.code = code;
    }

    /**
     * Get the retain handling as integer.
     *
     * @return The retain handling.
     * @since 4.0.0
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the Retain Handling belonging to the given byte code.
     *
     * @param code The byte code.
     * @return The Retain Handling belonging to the byte code or <code>null</code> if the byte code is not a valid
     * Retain Handling.
     * @since 4.0.0
     */
    public static @Nullable RetainHandling fromCode(final int code) {
        final RetainHandling[] values = values();
        if (code < 0 || code >= values.length) {
            return null;
        }
        return values[code];
    }
}
