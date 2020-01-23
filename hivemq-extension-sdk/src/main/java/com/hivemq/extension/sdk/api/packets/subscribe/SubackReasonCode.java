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

/**
 * The SUBACK reason codes for MQTT 5.
 * <p>
 * MQTT 3.1 and MQTT 3.1.1 supports only:
 * <ul>
 * <li>{@link #GRANTED_QOS_0}</li>
 * <li>{@link #GRANTED_QOS_1}</li>
 * <li>{@link #GRANTED_QOS_2}</li>
 * <li>{@link #UNSPECIFIED_ERROR} (in the MQTT 3 specification known as FAILURE)</li>
 * </ul>
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
public enum SubackReasonCode {

    /**
     * This is a success code.
     *
     * @since 4.0.0
     */
    GRANTED_QOS_0,
    /**
     * This is a success code.
     *
     * @since 4.0.0
     */
    GRANTED_QOS_1,
    /**
     * This is a success code.
     *
     * @since 4.0.0
     */
    GRANTED_QOS_2,
    /**
     * This is an unsuccessful code.
     * <p>
     * For an MQTT 3 SUBACK this translates to the return code FAILURE.
     *
     * @since 4.0.0
     */
    UNSPECIFIED_ERROR,
    /**
     * This is an unsuccessful code.
     * <p>
     * For an MQTT 3 SUBACK this translates to the return code FAILURE.
     *
     * @since 4.0.0
     */
    IMPLEMENTATION_SPECIFIC_ERROR,
    /**
     * This is an unsuccessful code.
     * <p>
     * For an MQTT 3 SUBACK this translates to the return code FAILURE.
     *
     * @since 4.0.0
     */
    NOT_AUTHORIZED,
    /**
     * This is an unsuccessful code.
     * <p>
     * For an MQTT 3 SUBACK this translates to the return code FAILURE.
     *
     * @since 4.0.0
     */
    TOPIC_FILTER_INVALID,
    /**
     * This is an unsuccessful code.
     * <p>
     * For an MQTT 3 SUBACK this translates to the return code FAILURE.
     *
     * @since 4.0.0
     */
    PACKET_IDENTIFIER_IN_USE,
    /**
     * This is an unsuccessful code.
     * <p>
     * For an MQTT 3 SUBACK this translates to the return code FAILURE.
     *
     * @since 4.0.0
     */
    QUOTA_EXCEEDED,
    /**
     * This is an unsuccessful code.
     * <p>
     * For an MQTT 3 SUBACK this translates to the return code FAILURE.
     *
     * @since 4.0.0
     */
    SHARED_SUBSCRIPTION_NOT_SUPPORTED,
    /**
     * This is an unsuccessful code.
     * <p>
     * For an MQTT 3 SUBACK this translates to the return code FAILURE.
     *
     * @since 4.0.0
     */
    SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED,
    /**
     * This is an unsuccessful code.
     * <p>
     * For an MQTT 3 SUBACK this translates to the return code FAILURE.
     *
     * @since 4.0.0
     */
    WILDCARD_SUBSCRIPTION_NOT_SUPPORTED
}