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

package com.hivemq.util;

import com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;

/**
 * Various utilities for dealing with Publishes or data from Publishes
 *
 * @author Dominik Obermaier
 */
public class PublishUtil {


    /**
     * Returns the minimum QoS of both passed QoS
     *
     * @param subscribedQoS
     * @param actualQoS
     * @return the minimum of both passed QoS
     */
    public static QoS getMinQoS(final QoS subscribedQoS, final QoS actualQoS) {
        QoS qosToReturn = actualQoS;
        if (subscribedQoS.getQosNumber() < actualQoS.getQosNumber()) {
            qosToReturn = subscribedQoS;
        }
        return qosToReturn;
    }

    /**
     * Check if the expiry interval for the given publish is expired
     *
     * @param publish to check
     * @return true if the ttl of the publish is expired
     */
    public static boolean isExpired(final PUBLISH publish) {
        return isExpired(publish.getTimestamp(), publish.getMessageExpiryInterval());
    }

    /**
     * Check if the ttl for the given timestamp is expired
     *
     * @param timestamp of the publish creation
     * @param ttl       The time to live in seconds
     * @return true if the ttl of the publish is expired
     */
    public static boolean isExpired(final long timestamp, final long ttl) {

        if (ttl == MqttConfigurationDefaults.TTL_DISABLED || ttl == PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET) {
            return false;
        }

        final long timeToLiveInMilliseconds = ttl * 1000L;

        //prevent accidental overflow
        if (timeToLiveInMilliseconds < 0) {
            return false;
        }

        return timestamp + timeToLiveInMilliseconds <= System.currentTimeMillis();
    }
}
