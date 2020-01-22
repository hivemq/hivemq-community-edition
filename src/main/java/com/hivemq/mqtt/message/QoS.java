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

package com.hivemq.mqtt.message;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * The Quality of Service level
 *
 * @author Dominik Obermaier
 * @since 1.4
 */
public enum QoS {

    /**
     * At most once delivery. The message will be delivered once or never (best effort delivery)
     */
    AT_MOST_ONCE(0),
    /**
     * At least once delivery. The message will be delivered once or multiple times
     */
    AT_LEAST_ONCE(1),
    /**
     * At exactly once delivery. The message will be delivered once and only once
     */
    EXACTLY_ONCE(2);

    private final int qosNumber;

    QoS(final int qosNumber) {

        this.qosNumber = qosNumber;
    }

    /**
     * @return the integer value of the QoS. Can be 0, 1 or 2
     */
    public int getQosNumber() {
        return qosNumber;
    }

    /**
     * Creates a QoS level enum from an integer
     *
     * @param qosNumber the QoS level as integer (0,1,2)
     * @return the QoS level or <code>null</code> if an invalid QoS level was passed
     */
    @NotNull
    public static QoS valueOf(final int qosNumber) {

        for (final QoS qoS : QoS.values()) {
            if (qoS.getQosNumber() == qosNumber) {
                return qoS;
            }
        }
        throw new IllegalArgumentException("QoS not found for number: " + qosNumber);
    }
}
