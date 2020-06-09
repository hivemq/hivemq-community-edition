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

/**
 * The MessageDroppedService is used to centralize the update of dropped message metrics. The corresponding method
 * should be called, whenever a message is dropped.
 *
 * @author Lukas Brandl
 */
public interface MessageDroppedService {

    /**
     * Update the metrics if a qos 0 message was dropped because the queue for the client was not yet empty
     */
    void qos0MemoryExceeded(final String clientId, final String topic, final int qos, final long currentMemory, final long maxMemory);

    /**
     * Update the metrics if a message was dropped because the client message queue was full
     */
    void queueFull(final String clientId, final String topic, final int qos);

    /**
     * Update the metrics if a message was dropped because the shared subscription message queue was full
     */
    void queueFullShared(final String sharedId, final String topic, final int qos);

    /**
     * Update the metrics if a qos 0 message was dropped because the client socket was not writable
     */
    void notWritable(final String clientId, final String topic, final int qos);

    /**
     * Update the metrics if a PUBLISH was dropped because an extension prevented onward delivery.
     */
    void extensionPrevented(final String clientId, final String topic, final int qos);

    /**
     * Update the metrics if a message was dropped because of an internal error
     */
    void failed(final String clientId, final String topic, final int qos);

    /**
     * Update the metrics if a PUBLISH was dropped because the packet size exceeded.
     */
    void publishMaxPacketSizeExceeded(final String clientId, final String topic, final int qos, final long maximumPacketSize, final long packetSize);

    /**
     * Update the metrics if any mqtt message but PUBLISH was dropped because the packet size exceeded.
     */
    void messageMaxPacketSizeExceeded(final String clientId, final String messageType, final long maximumPacketSize, final long packetSize);

    void failedShared(final String group, final String topic, final int qos);

    void qos0MemoryExceededShared(final String clientId, final String topic, final int qos, final long currentMemory, final long maxMemory);

}
