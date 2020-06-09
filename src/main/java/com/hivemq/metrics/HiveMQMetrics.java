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
import com.codahale.metrics.Gauge;

/**
 * This class holds a constant {@link HiveMQMetric} for every metric which is provided by HiveMQ
 *
 * @author Christoph Schäbel
 */
public class HiveMQMetrics {

    /**
     * represents a {@link Counter}, which counts every incoming MQTT message
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> INCOMING_MESSAGE_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.total.count", Counter.class);

    /**
     * represents a {@link Counter}, which counts every outgoing MQTT message
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> OUTGOING_MESSAGE_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.total.count", Counter.class);

    /**
     * represents a {@link Counter}, which counts every incoming MQTT CONNECT messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> INCOMING_CONNECT_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.connect.count", Counter.class);

    /**
     * represents a {@link Counter}, which counts every incoming MQTT PUBLISH messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> INCOMING_PUBLISH_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.publish.count", Counter.class);

    /**
     * represents a {@link Counter}, which counts every outgoing MQTT PUBLISH messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> OUTGOING_PUBLISH_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.publish.count", Counter.class);

    /**
     * represents a {@link Counter}, which counts every dropped PUBLISH messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> DROPPED_MESSAGE_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.dropped.count", Counter.class);

    /**
     * represents a {@link Gauge}, which holds the current amount of retained messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Gauge<Number>> RETAINED_MESSAGES_CURRENT =
            HiveMQMetric.gaugeValue("com.hivemq.messages.retained.current");

    /**
     * represents a {@link Gauge}, which holds the total amount of read bytes
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Gauge<Number>> BYTES_READ_TOTAL =
            HiveMQMetric.gaugeValue("com.hivemq.networking.bytes.read.total");

    /**
     * represents a {@link Gauge}, which holds total of written bytes
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Gauge<Number>> BYTES_WRITE_TOTAL =
            HiveMQMetric.gaugeValue("com.hivemq.networking.bytes.write.total");

    /**
     * represents a {@link Gauge}, which holds the current total number of connections
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Gauge<Number>> CONNECTIONS_OVERALL_CURRENT =
            HiveMQMetric.gaugeValue("com.hivemq.networking.connections.current");

    /**
     * represents a {@link Counter}, which is increased every time a network connection is closed
     *
     * @since 3.4
     */
    public static final HiveMQMetric<Counter> CONNECTIONS_CLOSED_COUNT =
            HiveMQMetric.valueOf("com.hivemq.networking.connections-closed.total.count", Counter.class);

    /**
     * represents a {@link Counter}, which measures the current count of subscriptions
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> SUBSCRIPTIONS_CURRENT =
            HiveMQMetric.valueOf("com.hivemq.subscriptions.overall.current", Counter.class);

    /**
     * represents a {@link Gauge}, which measures the current count of stored sessions
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Gauge<Number>> CLIENT_SESSIONS_CURRENT =
            HiveMQMetric.gaugeValue("com.hivemq.sessions.overall.current");

    /**
     * represents a {@link Gauge}, which measures the approximate memory usage of the retained message persistence if
     * the memory persistence is used.
     */
    public static final HiveMQMetric<Gauge<Number>> RETAINED_MESSAGES_MEMORY_PERSISTENCE_TOTAL_SIZE =
            HiveMQMetric.gaugeValue("com.hivemq.persistence.retained-messages.in-memory.total-size");

    /**
     * represents a {@link Gauge}, which measures the approximate memory usage of the payload persistence if
     * the memory persistence is used.
     */
    public static final HiveMQMetric<Gauge<Number>> PAYLOAD_MEMORY_PERSISTENCE_TOTAL_SIZE =
            HiveMQMetric.gaugeValue("com.hivemq.persistence.payload.in-memory.total-size");

    /**
     * represents a {@link Gauge}, which measures the approximate memory usage of the subscription persistence if
     * the memory persistence is used.
     */
    public static final HiveMQMetric<Gauge<Number>> CLIENT_SESSION_SUBSCRIPTIONS_MEMORY_PERSISTENCE_TOTAL_SIZE =
            HiveMQMetric.gaugeValue("com.hivemq.persistence.client-session.subscriptions.in-memory.total-size");

    /**
     * represents a {@link Gauge}, which measures the approximate memory usage of the client session persistence if
     * the memory persistence is used.
     */
    public static final HiveMQMetric<Gauge<Number>> CLIENT_SESSIONS_MEMORY_PERSISTENCE_TOTAL_SIZE =
            HiveMQMetric.gaugeValue("com.hivemq.persistence.client-sessions.in-memory.total-size");

    /**
     * represents a {@link Gauge}, which measures the approximate memory usage of the queued message persistence if
     * the memory persistence is used.
     */
    public static final HiveMQMetric<Gauge<Number>> QUEUED_MESSAGES_MEMORY_PERSISTENCE_TOTAL_SIZE =
            HiveMQMetric.gaugeValue("com.hivemq.persistence.queued-messages.in-memory.total-size");
}

