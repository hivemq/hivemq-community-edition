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
package com.hivemq.configuration.entity;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static com.hivemq.configuration.service.RestrictionsConfigurationService.*;

/**
 * @author Dominik Obermaier
 * @author Florian Limpöck
 */
@XmlRootElement(name = "restrictions")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class RestrictionsEntity {

    @XmlElement(name = "max-connections", defaultValue = "-1")
    private @NotNull Long maxConnections = MAX_CONNECTIONS_DEFAULT;

    @XmlElement(name = "max-client-id-length", defaultValue = "65535")
    private @NotNull Integer maxClientIdLength = MAX_CLIENT_ID_LENGTH_DEFAULT;

    @XmlElement(name = "max-topic-length", defaultValue = "65535")
    private @NotNull Integer maxTopicLength = MAX_TOPIC_LENGTH_DEFAULT;

    @XmlElement(name = "no-connect-idle-timeout", defaultValue = "10000")
    private @NotNull Long noConnectIdleTimeout = NO_CONNECT_IDLE_TIMEOUT_DEFAULT;

    @XmlElement(name = "incoming-bandwidth-throttling", defaultValue = "0")
    private @NotNull Long incomingBandwidthThrottling = INCOMING_BANDWIDTH_THROTTLING_DEFAULT;

    public long getMaxConnections() {
        return maxConnections;
    }

    public long getIncomingBandwidthThrottling() {
        return incomingBandwidthThrottling;
    }

    public int getMaxClientIdLength() {
        return maxClientIdLength;
    }

    public int getMaxTopicLength() {
        return maxTopicLength;
    }

    public long getNoConnectIdleTimeout() {
        return noConnectIdleTimeout;
    }
}
