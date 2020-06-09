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
package com.hivemq.configuration.service.impl;

import com.hivemq.configuration.service.RestrictionsConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Dominik Obermaier
 */
@Singleton
public class RestrictionsConfigurationServiceImpl implements RestrictionsConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(RestrictionsConfigurationServiceImpl.class);

    private final AtomicLong maxConnections = new AtomicLong(MAX_CONNECTIONS_DEFAULT);
    private final AtomicInteger maxClientIdLength = new AtomicInteger(MAX_CLIENT_ID_LENGTH_DEFAULT);
    private final AtomicLong noConnectIdleTimeout = new AtomicLong(NO_CONNECT_IDLE_TIMEOUT_DEFAULT);
    private final AtomicLong incomingLimit = new AtomicLong(INCOMING_BANDWIDTH_THROTTLING_DEFAULT);
    private final AtomicInteger maxTopicLength = new AtomicInteger(MAX_TOPIC_LENGTH_DEFAULT);

    @Override
    public long maxConnections() {
        return maxConnections.get();
    }

    @Override
    public int maxClientIdLength() {
        return maxClientIdLength.get();
    }

    @Override
    public long noConnectIdleTimeout() {
        return noConnectIdleTimeout.get();
    }

    @Override
    public long incomingLimit() {
        return incomingLimit.get();
    }

    @Override
    public int maxTopicLength() {
        return maxTopicLength.get();
    }

    @Override
    public void setMaxConnections(final long maxConnections) {
        log.debug("Setting global maximum allowed connections to {}", maxConnections);
        this.maxConnections.set(maxConnections);
    }

    @Override
    public void setMaxClientIdLength(final int maxClientIdLength) {
        log.debug("Setting the maximum client id length to {}", maxClientIdLength);
        this.maxClientIdLength.set(maxClientIdLength);
    }

    @Override
    public void setNoConnectIdleTimeout(final long noConnectIdleTimeout) {
        log.debug("Setting the timeout for disconnecting idle tcp connections before a connect message was received to {} milliseconds", noConnectIdleTimeout);
        this.noConnectIdleTimeout.set(noConnectIdleTimeout);
    }

    @Override
    public void setIncomingLimit(final long incomingLimit) {
        log.debug("Throttling the global incoming traffic limit {} bytes/second", incomingLimit);
        this.incomingLimit.set(incomingLimit);
    }

    @Override
    public void setMaxTopicLength(final int maxTopicLength) {
        log.debug("Setting the maximum topic length to {}", maxTopicLength);
        this.maxTopicLength.set(maxTopicLength);
    }

}
