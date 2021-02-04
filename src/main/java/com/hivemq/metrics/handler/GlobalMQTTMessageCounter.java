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
package com.hivemq.metrics.handler;

import com.codahale.metrics.Gauge;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.publish.PUBLISH;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.atomic.LongAdder;

import static com.hivemq.metrics.HiveMQMetrics.BYTES_READ_TOTAL;
import static com.hivemq.metrics.HiveMQMetrics.BYTES_WRITE_TOTAL;

/**
 * Gathers statistics about inbound and outbound MQTT messages.
 *
 * @author Dominik Obermaier
 * @author Florian Limpöck
 */
@Singleton
public class GlobalMQTTMessageCounter {

    private final @NotNull MetricsHolder metricsHolder;

    private final @NotNull LongAdder bytesReadTotal;
    private final @NotNull LongAdder bytesWrittenTotal;

    @Inject
    public GlobalMQTTMessageCounter(final @NotNull MetricsHolder metricsHolder) {
        this.metricsHolder = metricsHolder;
        this.bytesReadTotal = new LongAdder();
        this.bytesWrittenTotal = new LongAdder();

        metricsHolder.getMetricRegistry().register(BYTES_READ_TOTAL.name(), (Gauge<Long>) bytesReadTotal::longValue);
        metricsHolder.getMetricRegistry().register(BYTES_WRITE_TOTAL.name(), (Gauge<Long>) bytesWrittenTotal::longValue);
    }

    public void countInbound(final @NotNull Message message) {
        metricsHolder.getIncomingMessageCounter().inc();
        if (message instanceof CONNECT) {
            metricsHolder.getIncomingConnectCounter().inc();
        }
        if (message instanceof PUBLISH) {
            metricsHolder.getIncomingPublishCounter().inc();
        }
    }

    public void countInboundTraffic(final int bytes) {
        bytesReadTotal.add(bytes);
    }

    public void countOutbound(final @NotNull Message message) {
        metricsHolder.getOutgoingMessageCounter().inc();
        if (message instanceof PUBLISH) {
            metricsHolder.getOutgoingPublishCounter().inc();
        }
    }

    public void countOutboundTraffic(final int bytes) {
        bytesWrittenTotal.add(bytes);
    }


}
