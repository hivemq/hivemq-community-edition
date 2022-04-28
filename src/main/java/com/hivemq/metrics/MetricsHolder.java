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
package com.hivemq.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.inject.Singleton;

import static com.hivemq.metrics.HiveMQMetrics.*;

/**
 * @author Christoph Schäbel
 */
@Singleton
public class MetricsHolder {

    private final @NotNull MetricRegistry metricRegistry;

    private final @NotNull Counter incomingMessageCounter;
    private final @NotNull Counter outgoingMessageCounter;

    private final @NotNull Counter incomingConnectCounter;

    private final @NotNull Counter incomingPublishCounter;
    private final @NotNull Counter outgoingPublishCounter;

    private final @NotNull Counter droppedMessageCounter;

    private final @NotNull Counter subscriptionCounter;

    private final @NotNull Counter closedConnectionsCounter;

    private final @NotNull Counter channelNotWritableCounter;

    private final @NotNull Counter storedWillMessagesCount;
    private final @NotNull Counter publishedWillMessagesCount;

    public MetricsHolder(final @NotNull MetricRegistry metricRegistry) {

        this.metricRegistry = metricRegistry;

        incomingMessageCounter = metricRegistry.counter(INCOMING_MESSAGE_COUNT.name());
        outgoingMessageCounter = metricRegistry.counter(OUTGOING_MESSAGE_COUNT.name());

        incomingConnectCounter = metricRegistry.counter(INCOMING_CONNECT_COUNT.name());

        incomingPublishCounter = metricRegistry.counter(INCOMING_PUBLISH_COUNT.name());
        outgoingPublishCounter = metricRegistry.counter(OUTGOING_PUBLISH_COUNT.name());

        droppedMessageCounter = metricRegistry.counter(DROPPED_MESSAGE_COUNT.name());

        closedConnectionsCounter = metricRegistry.counter(CONNECTIONS_CLOSED_COUNT.name());

        subscriptionCounter = metricRegistry.counter(SUBSCRIPTIONS_CURRENT.name());

        channelNotWritableCounter = metricRegistry.counter(MQTT_CONNECTION_NOT_WRITABLE_CURRENT.name());

        storedWillMessagesCount = metricRegistry.counter(WILL_MESSAGE_COUNT.name());
        publishedWillMessagesCount = metricRegistry.counter(WILL_MESSAGE_PUBLISHED_COUNT_TOTAL.name());
    }

    public @NotNull MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    public @NotNull Counter getIncomingMessageCounter() {
        return incomingMessageCounter;
    }

    public @NotNull Counter getOutgoingMessageCounter() {
        return outgoingMessageCounter;
    }

    public @NotNull Counter getIncomingConnectCounter() {
        return incomingConnectCounter;
    }

    public @NotNull Counter getIncomingPublishCounter() {
        return incomingPublishCounter;
    }

    public @NotNull Counter getOutgoingPublishCounter() {
        return outgoingPublishCounter;
    }

    public @NotNull Counter getDroppedMessageCounter() {
        return droppedMessageCounter;
    }

    public @NotNull Counter getSubscriptionCounter() {
        return subscriptionCounter;
    }

    public @NotNull Counter getClosedConnectionsCounter() {
        return closedConnectionsCounter;
    }

    public @NotNull Counter getChannelNotWritableCounter() {
        return channelNotWritableCounter;
    }

    public @NotNull Counter getStoredWillMessagesCount() {
        return storedWillMessagesCount;
    }

    public @NotNull Counter getPublishedWillMessagesCount() {
        return publishedWillMessagesCount;
    }
}
