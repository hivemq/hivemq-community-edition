package com.hivemq.mqtt.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.Channel;
import org.jctools.queues.MpscLinkedQueue;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class KeepAliveDisconnectService {

    private final @NotNull MqttServerDisconnector mqttServerDisconnector;
    private final @NotNull MpscLinkedQueue<Channel> disconnectQueue = new MpscLinkedQueue<>();
    private final @NotNull ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private final long disconnectBatch;

    @Inject
    public KeepAliveDisconnectService(@NotNull final MqttServerDisconnector mqttServerDisconnector,
                                      @NotNull final ShutdownHooks shutdownHooks) {
        this.mqttServerDisconnector = mqttServerDisconnector;
        this.disconnectBatch = InternalConfigurations.DISCONNECT_KEEP_ALIVE_BATCH;
        this.scheduledExecutorService.scheduleWithFixedDelay(new DisconnectorTask(), 100, 100, TimeUnit.MILLISECONDS);
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
    }

    public class DisconnectorTask implements Runnable {
        @Override
        public void run() {
            for (int i = 0; i < disconnectBatch; i++) {
                final Channel channel = disconnectQueue.relaxedPoll();
                if (channel == null) {
                    return;
                }
                channel.eventLoop().submit(() -> {
                    mqttServerDisconnector.disconnect(channel,
                            "Client with ID {} and IP {} disconnected. The client was idle for too long without sending an MQTT control packet",
                            "Client was idle for too long",
                            Mqtt5DisconnectReasonCode.KEEP_ALIVE_TIMEOUT,
                            ReasonStrings.DISCONNECT_KEEP_ALIVE_TIMEOUT);
                });
            }
        }
    }
}
