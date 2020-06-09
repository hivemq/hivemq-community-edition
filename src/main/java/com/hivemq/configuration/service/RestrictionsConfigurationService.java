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
package com.hivemq.configuration.service;

import com.hivemq.codec.encoder.mqtt5.UnsignedDataTypes;

/**
 * A Configuration service which allows to get information about the current restrictions configuration
 *
 * @author Dominik Obermaier
 * @author Florian Limpöck
 *
 * @since 4.0
 */
public interface RestrictionsConfigurationService {

    /**
     * UNLIMITED
     */
    int UNLIMITED_CONNECTIONS = -1;
    int UNLIMITED_BANDWIDTH = 0;

    /**
     * DEFAULT VALUES
     */
    long MAX_CONNECTIONS_DEFAULT = UNLIMITED_CONNECTIONS;
    int MAX_CLIENT_ID_LENGTH_DEFAULT = 65535;
    long NO_CONNECT_IDLE_TIMEOUT_DEFAULT = 10000;
    long INCOMING_BANDWIDTH_THROTTLING_DEFAULT = UNLIMITED_BANDWIDTH;
    int MAX_TOPIC_LENGTH_DEFAULT = UnsignedDataTypes.UNSIGNED_SHORT_MAX_VALUE;

    /**
     * BOUNDARY VALUES
     */
    long MAX_CONNECTIONS_MINIMUM = 0;
    int MAX_CLIENT_ID_LENGTH_MINIMUM = 1;
    int MAX_CLIENT_ID_LENGTH_MAXIMUM = UnsignedDataTypes.UNSIGNED_SHORT_MAX_VALUE;
    long NO_CONNECT_IDLE_TIMEOUT_MINIMUM = 1;
    long INCOMING_BANDWIDTH_THROTTLING_MINIMUM = 0;
    int MAX_TOPIC_LENGTH_MINIMUM = 1;
    int MAX_TOPIC_LENGTH_MAXIMUM = UnsignedDataTypes.UNSIGNED_SHORT_MAX_VALUE;


    /**
     * Returns the maximum allowed connections.
     * <p>
     * <b>This method only returns the configuration of maximum concurrent MQTT connections, not the value
     * your HiveMQ license is limited to.</b>
     *
     * @return the maximum allowed connections.
     */
    long maxConnections();

    /**
     * @return the global maximum allowed client identifier length
     */
    int maxClientIdLength();

    /**
     * @return the global maximum timeout for idle TCP connections of clients which didn't send a CONNECT message.
     */
    long noConnectIdleTimeout();

    /**
     * @return the incoming bandwidth limit in bytes
     */
    long incomingLimit();

    /**
     * @return the global maximum allowed topic length
     */
    int maxTopicLength();

    void setMaxConnections(long maxConnections);

    void setMaxClientIdLength(int maxClientIdLength);

    void setNoConnectIdleTimeout(final long noConnectPacketIdleTimeout);

    void setIncomingLimit(long incomingLimit);

    void setMaxTopicLength(int maxTopicLength);

}
