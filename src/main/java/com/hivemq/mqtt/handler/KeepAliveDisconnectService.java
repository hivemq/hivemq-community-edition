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
package com.hivemq.mqtt.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.Channel;
import org.jctools.queues.MpscLinkedQueue;

import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class KeepAliveDisconnectService {

    private final @NotNull MqttServerDisconnector mqttServerDisconnector;
    private final @NotNull MpscLinkedQueue<Channel> disconnectQueue = new MpscLinkedQueue<>();
    private final @NotNull ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private final long disconnectBatch;
    private final AtomicInteger submittedTasks = new AtomicInteger();

    @Inject
    public KeepAliveDisconnectService(final @NotNull MqttServerDisconnector mqttServerDisconnector,
                                      final @NotNull ShutdownHooks shutdownHooks) {
        this.mqttServerDisconnector = mqttServerDisconnector;
        this.disconnectBatch = InternalConfigurations.DISCONNECT_KEEP_ALIVE_BATCH;
        shutdownHooks.add(new HiveMQShutdownHook() {
            @Override
            public @NotNull String name() {
                return "KeepAliveDisconnectService shutdown";
            }

            @Override
            public void run() {
                scheduledExecutorService.shutdown();
            }
        });
    }

    public void submitKeepAliveDisconnect(final @NotNull Channel channel) {
        disconnectQueue.offer(channel);
        if (submittedTasks.getAndIncrement() == 0) {
            try {
                scheduledExecutorService.schedule(new DisconnectorTask(), 100, TimeUnit.MILLISECONDS);
            } catch (final RejectedExecutionException rejectedExecutionException) {
                // can be ignored, can happen during Shutdown
            }
        }
    }

    public class DisconnectorTask implements Runnable {
        @Override
        public void run() {
            int i = 0;
            try {
                while (i < disconnectBatch) {
                    final Channel channel = disconnectQueue.relaxedPoll();
                    if (channel == null) {
                        break;
                    }
                    i++;
                    channel.eventLoop().execute(() -> {
                        mqttServerDisconnector.disconnect(channel,
                                "Client with ID {} and IP {} disconnected. The client was idle for too long without sending an MQTT control packet",
                                "Client was idle for too long",
                                Mqtt5DisconnectReasonCode.KEEP_ALIVE_TIMEOUT,
                                ReasonStrings.DISCONNECT_KEEP_ALIVE_TIMEOUT);
                    });
                }
            } finally {
                // always reschedule even if an exception happens within the while loop
                if (submittedTasks.addAndGet(-i) > 0) {
                    try {
                        scheduledExecutorService.schedule(this, 100, TimeUnit.MILLISECONDS);
                    } catch (final RejectedExecutionException rejectedExecutionException) {
                        // can be ignored, can happen during Shutdown
                    }
                }
            }
        }
    }
}
