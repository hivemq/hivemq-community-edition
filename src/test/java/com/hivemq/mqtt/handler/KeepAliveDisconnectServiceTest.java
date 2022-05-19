package com.hivemq.mqtt.handler;

import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class KeepAliveDisconnectServiceTest {

    private final @NotNull ShutdownHooks shutdownHooks = new ShutdownHooks();
    private final @NotNull ArgumentCaptor<Channel> channelArgumentCaptor = ArgumentCaptor.forClass(Channel.class);

    private final @NotNull MqttServerDisconnector mqttServerDisconnector = mock(MqttServerDisconnector.class);

    private @NotNull KeepAliveDisconnectService keepAliveDisconnectService;


    @Before
    public void setUp() {
        keepAliveDisconnectService = new KeepAliveDisconnectService(mqttServerDisconnector, shutdownHooks);
        doAnswer(invocation -> null).when(mqttServerDisconnector).disconnect(channelArgumentCaptor.capture(), any(), any(), any(), any());
    }

    @After
    public void tearDown() {
        shutdownHooks.runShutdownHooks();
    }

    @Test
    public void test_scheduleTaskOnFirstSubmit() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        keepAliveDisconnectService.submitKeepAliveDisconnect(channel);
        await().until(() -> {
            channel.runPendingTasks();
            return channelArgumentCaptor.getAllValues().size() == 1;
        });
    }

    @Test
    public void test_whenTheFirstTaskIsSubmitted_thenTheTaskIsScheduledAndExecuted() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        keepAliveDisconnectService.submitKeepAliveDisconnect(channel);
        await().until(() -> {
            channel.runPendingTasks();
            return channelArgumentCaptor.getAllValues().size() == 1;
        });
    }

    @Test
    public void test_whenMoreThanOneBatchIsSubmitted_thenTheTaskIsScheduledAndExecuted() {
        final Channel channel = mock(Channel.class);
        final EventLoop eventLoop = mock(EventLoop.class);
        when(channel.eventLoop()).thenReturn(eventLoop);
        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        doAnswer(invocation -> {
            final Runnable runnable = invocation.getArgument(0);
            executorService.execute(runnable);
            return null;
        }).when(eventLoop).execute(any());

        for (int i = 0; i < 10000; i++) {
            keepAliveDisconnectService.submitKeepAliveDisconnect(channel);
        }

        try {
            await().pollInterval(1, TimeUnit.MILLISECONDS).timeout(30, TimeUnit.SECONDS)
                    .until(() -> channelArgumentCaptor.getAllValues().size() == 2000);
        } finally {
            executorService.shutdown();
        }
    }

    @Test
    public void test_whenTasksAreSlowlySubmitted_thenAllTasksWillBeExecuted() {
        // TestChannel behaves odd sometimes, this is safe
        final Channel channel = mock(Channel.class);
        final EventLoop eventLoop = mock(EventLoop.class);
        when(channel.eventLoop()).thenReturn(eventLoop);
        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        doAnswer(invocation -> {
            final Runnable runnable = invocation.getArgument(0);
            executorService.execute(runnable);
            return null;
        }).when(eventLoop).execute(any());

        new Thread(() -> {
            for (int i = 0; i < 2000; i++) {
                keepAliveDisconnectService.submitKeepAliveDisconnect(channel);
                try {
                    Thread.sleep(1);
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        try {
            await().pollInterval(1, TimeUnit.MILLISECONDS).timeout(30, TimeUnit.SECONDS)
                    .until(() -> channelArgumentCaptor.getAllValues().size() == 2000);
        } finally {
            executorService.shutdown();
        }
    }
}