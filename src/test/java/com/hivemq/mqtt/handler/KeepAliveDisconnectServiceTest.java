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

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    public void test_whenTasksAreSlowlySubmittedAndThrowExceptions_thenAllTasksWillBeExecuted() {
        // TestChannel behaves odd sometimes, this is safe
        final Channel channel = mock(Channel.class);
        final EventLoop eventLoop = mock(EventLoop.class);
        when(channel.eventLoop()).thenReturn(eventLoop);
        final Random random = new Random();
        final AtomicInteger withoutException = new AtomicInteger(0);
        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        doAnswer(invocation -> {
            final Runnable runnable = invocation.getArgument(0);
            if (random.nextInt(10) == 7) {
                throw new RuntimeException();
            } else {
                withoutException.incrementAndGet();
            }

            executorService.execute(runnable);
            return null;
        }).when(eventLoop).execute(any());

        new Thread(() -> {
            while (withoutException.get() <= 100) {
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
                    .until(() -> channelArgumentCaptor.getAllValues().size() >= 100);
        } finally {
            executorService.shutdown();
        }
    }
}