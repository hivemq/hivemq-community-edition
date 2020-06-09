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

import com.codahale.metrics.MetricRegistry;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Georg Held
 */
public class MessageDroppedServiceImplTest {

    private final String clientId = "clientId";
    private final String topic = "topic";
    private final int qos = 1;

    @Mock
    private EventLog eventLog;

    private MessageDroppedService messageDroppedService;


    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        messageDroppedService = new MessageDroppedServiceImpl(new MetricsHolder(new MetricRegistry()), eventLog);
    }

    @Test
    public void test_queue_full_is_responsible() {
        messageDroppedService.queueFull(clientId, topic, qos);
        verify(eventLog, times(1)).messageDropped(clientId, topic, qos, "The client message queue is full");
    }

    @Test
    public void test_publish_inbound_intercepted() {
        messageDroppedService.extensionPrevented(clientId, topic, qos);
        verify(eventLog, times(1)).messageDropped(clientId, topic, qos, "Extension prevented onward delivery");

    }

    @Test
    public void test_qos0_memory_exceeded() {
        messageDroppedService.qos0MemoryExceeded(clientId, topic, qos, 1111, 1000);
        verify(eventLog, times(1)).messageDropped(clientId, topic, qos, "The QoS 0 memory limit exceeded, size: 1,111 bytes, max: 1,000 bytes");

    }

    @Test
    public void test_qos0_memory_exceeded_shared() {
        messageDroppedService.qos0MemoryExceededShared(clientId, topic, qos, 1111, 1000);
        verify(eventLog, times(1)).sharedSubscriptionMessageDropped(clientId, topic, qos, "The QoS 0 memory limit exceeded, size: 1,111 bytes, max: 1,000 bytes");

    }
}