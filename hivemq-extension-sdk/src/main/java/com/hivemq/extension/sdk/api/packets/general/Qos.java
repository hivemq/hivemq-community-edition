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
package com.hivemq.extension.sdk.api.packets.general;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * The quality of service level (QOS) of a PUBLISH or subscription.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
public enum Qos {
    /**
     * At most once delivery. The message will be delivered once or never (best effort delivery).
     *
     * @since 4.0.0
     */
    AT_MOST_ONCE(0),
    /**
     * At least once delivery. The message will be delivered once or multiple times.
     *
     * @since 4.0.0
     */
    AT_LEAST_ONCE(1),
    /**
     * At exactly once delivery. The message will be delivered once and only once.
     *
     * @since 4.0.0
     */
    EXACTLY_ONCE(2);

    /**
     * Quality service level as integer representation.
     *
     * @since 4.0.0
     */
    private final int qosNumber;

    Qos(final int qosNumber) {
        this.qosNumber = qosNumber;
    }

    /**
     * Get the quality service level as integer.
     *
     * @return The quality of service.
     * @since 4.0.0
     */
    public int getQosNumber() {
        return qosNumber;
    }

    /**
     * Creates a Qos level enum from an integer.
     *
     * @param code the Qos level as integer code(0,1,2).
     * @return The Qos level.
     * @throws IllegalArgumentException when the parameter is an invalid quality of service level.
     * @since 4.0.0
     */
    @NotNull
    public static Qos valueOf(final int code) {

        for (final Qos qoS : Qos.values()) {
            if (qoS.getQosNumber() == code) {
                return qoS;
            }
        }
        throw new IllegalArgumentException("Qos not found for number: " + code);
    }
}
