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
package com.hivemq.mqtt.message.dropping;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * @author Georg Held
 */
public class MessageDroppedServiceImpl implements MessageDroppedService {

    private static final NumberFormat FORMAT = NumberFormat.getInstance(Locale.US);

    private final @NotNull MetricsHolder metricsHolder;
    private final @NotNull EventLog eventLog;

    MessageDroppedServiceImpl(final @NotNull MetricsHolder metricsHolder, final @NotNull EventLog eventLog) {
        this.metricsHolder = metricsHolder;
        this.eventLog = eventLog;
    }

    /**
     * Update the metrics if a message was dropped because the client message queue was full
     */
    @Override
    public void queueFull(final @NotNull String clientId, final @NotNull String topic, final int qos) {
        metricsHolder.getDroppedMessageCounter().inc();
        eventLog.messageDropped(clientId, topic, qos, "The client message queue is full");
    }

    /**
     * Update the metrics if a message was dropped because the shared subscription message queue was full
     */
    @Override
    public void queueFullShared(final @NotNull String sharedSubscription, final @NotNull String topic, final int qos) {
        metricsHolder.getDroppedMessageCounter().inc();
        eventLog.sharedSubscriptionMessageDropped(sharedSubscription, topic, qos, "The shared subscription message queue is full");
    }

    /**
     * Update the metrics if a qos 0 message was dropped because the queue for the client was not yet empty
     */
    @Override
    public void qos0MemoryExceeded(final @NotNull String clientId, final @NotNull String topic, final int qos, final long currentMemory, final long maxMemory) {
        metricsHolder.getDroppedMessageCounter().inc();

        final String reason = "The QoS 0 memory limit exceeded, size: " + FORMAT.format(currentMemory) + " bytes, max: " + FORMAT.format(maxMemory) + " bytes";

        eventLog.messageDropped(clientId, topic, qos, reason);
    }

    /**
     * Update the metrics if a qos 0 message was dropped because the client socket was not writable
     */
    @Override
    public void notWritable(final @NotNull String clientId, final @NotNull String topic, final int qos) {
        metricsHolder.getDroppedMessageCounter().inc();
        eventLog.messageDropped(clientId, topic, qos, "The tcp socket was not writable");
    }

    /**
     * @inheritDoc
     */
    @Override
    public void extensionPrevented(final @NotNull String clientId, final @NotNull String topic, final int qos) {
        metricsHolder.getDroppedMessageCounter().inc();
        eventLog.messageDropped(clientId, topic, qos, "Extension prevented onward delivery");
    }

    /**
     * Update the metrics if a message was dropped because of an internal error
     */
    @Override
    public void failed(final @NotNull String clientId, final @NotNull String topic, final int qos) {
        metricsHolder.getDroppedMessageCounter().inc();
        eventLog.messageDropped(clientId, topic, qos, "Internal error");
    }

    @Override
    public void failedShared(final @NotNull String group, final @NotNull String topic, final int qos) {
        metricsHolder.getDroppedMessageCounter().inc();
        eventLog.sharedSubscriptionMessageDropped(group, topic, qos, "Internal error");
    }

    @Override
    public void qos0MemoryExceededShared(final @NotNull String group, final @NotNull String topic, final int qos, final long currentMemory, final long maxMemory) {
        metricsHolder.getDroppedMessageCounter().inc();

        final String reason = "The QoS 0 memory limit exceeded, size: " + FORMAT.format(currentMemory) + " bytes, max: " + FORMAT.format(maxMemory) + " bytes";

        eventLog.sharedSubscriptionMessageDropped(group, topic, qos, reason);
    }

    @Override
    public void publishMaxPacketSizeExceeded(final @NotNull String clientId, final @NotNull String topic, final int qos, final long maximumPacketSize, final long packetSize) {
        metricsHolder.getDroppedMessageCounter().inc();

        final String reason = "Maximum packet size exceeded, size: " + FORMAT.format(packetSize) + " bytes, max: " + FORMAT.format(maximumPacketSize) + " bytes";

        eventLog.messageDropped(clientId, topic, qos, reason);
    }

    @Override
    public void messageMaxPacketSizeExceeded(final @NotNull String clientId, final @NotNull String messageType, final long maximumPacketSize, final long packetSize) {
        final String reason = "Maximum packet size exceeded, size: " + FORMAT.format(packetSize) + " bytes, max: " + FORMAT.format(maximumPacketSize) + " bytes";

        eventLog.mqttMessageDropped(clientId, messageType, reason);
    }

}
