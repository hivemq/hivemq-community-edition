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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.embedded.EmbeddedChannel;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class KeepAliveDisconnectHandlerTest {

    public static final long READER_IDLE_TIME = KeepAliveDisconnectHandler.MIN_TIMEOUT_NANOS * 2;
    private final @NotNull ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
    private final @NotNull Channel channel = mock(Channel.class);
    private final @NotNull EventLoop executor = mock(EventLoop.class);
    private final @NotNull KeepAliveDisconnectService keepAliveDisconnectService = mock(KeepAliveDisconnectService.class);

    @Before
    public void setUp() {
        when(ctx.channel()).thenReturn(channel);
        when(channel.eventLoop()).thenReturn(executor);
    }

    @Test
    public void test_handlerAdded_whenNotInitialized_thenInitialize() {
        final KeepAliveDisconnectHandler keepAliveDisconnectHandler = new KeepAliveDisconnectHandler(READER_IDLE_TIME, TimeUnit.NANOSECONDS, keepAliveDisconnectService);
        when(channel.isActive()).thenReturn(true);
        when(channel.isRegistered()).thenReturn(true);
        keepAliveDisconnectHandler.handlerAdded(ctx);
        verify(executor, times(1)).schedule(any(KeepAliveDisconnectHandler.ReaderIdleTimeoutTask.class), eq(READER_IDLE_TIME), eq(TimeUnit.NANOSECONDS));
        assertEquals(1, keepAliveDisconnectHandler.getState());
    }

    @Test
    public void test_handlerRemoved_stateIsDestroyed() {
        final KeepAliveDisconnectHandler keepAliveDisconnectHandler = new KeepAliveDisconnectHandler(READER_IDLE_TIME, TimeUnit.NANOSECONDS, keepAliveDisconnectService);
        keepAliveDisconnectHandler.handlerRemoved(ctx);
        assertEquals(2, keepAliveDisconnectHandler.getState());
    }

    @Test
    public void test_channelRegistered_whenNotInitialized_thenInitialize() throws Exception {
        final KeepAliveDisconnectHandler keepAliveDisconnectHandler = new KeepAliveDisconnectHandler(READER_IDLE_TIME, TimeUnit.NANOSECONDS, keepAliveDisconnectService);
        when(channel.isActive()).thenReturn(true);
        when(channel.isRegistered()).thenReturn(true);
        keepAliveDisconnectHandler.channelRegistered(ctx);
        verify(executor, times(1)).schedule(any(KeepAliveDisconnectHandler.ReaderIdleTimeoutTask.class), eq(READER_IDLE_TIME), eq(TimeUnit.NANOSECONDS));
        assertEquals(1, keepAliveDisconnectHandler.getState());
    }

    @Test
    public void test_channelActive_whenNotInitialized_thenInitialize() throws Exception {
        final KeepAliveDisconnectHandler keepAliveDisconnectHandler = new KeepAliveDisconnectHandler(READER_IDLE_TIME, TimeUnit.NANOSECONDS, keepAliveDisconnectService);
        when(channel.isActive()).thenReturn(true);
        when(channel.isRegistered()).thenReturn(true);
        keepAliveDisconnectHandler.channelActive(ctx);
        verify(executor, times(1)).schedule(any(KeepAliveDisconnectHandler.ReaderIdleTimeoutTask.class), eq(READER_IDLE_TIME), eq(TimeUnit.NANOSECONDS));
        assertEquals(1, keepAliveDisconnectHandler.getState());
    }

    @Test
    public void test_channelInactive_stateIsDestroyed() throws Exception {
        final KeepAliveDisconnectHandler keepAliveDisconnectHandler = new KeepAliveDisconnectHandler(READER_IDLE_TIME, TimeUnit.NANOSECONDS, keepAliveDisconnectService);
        keepAliveDisconnectHandler.channelInactive(ctx);
        assertEquals(2, keepAliveDisconnectHandler.getState());
    }


    @Test
    public void test_channelRead_whileReading_thenReadingIsSetToTrue() throws Exception {
        final KeepAliveDisconnectHandler keepAliveDisconnectHandler = new KeepAliveDisconnectHandler(READER_IDLE_TIME, TimeUnit.NANOSECONDS, keepAliveDisconnectService);
        when(channel.isActive()).thenReturn(true);
        when(channel.isRegistered()).thenReturn(true);
        keepAliveDisconnectHandler.channelRead(ctx, new byte[12]);
        assertTrue(keepAliveDisconnectHandler.isReading());
    }


    @Test
    public void test_channelReadComplete() throws Exception {
        final KeepAliveDisconnectHandler keepAliveDisconnectHandler = new KeepAliveDisconnectHandler(READER_IDLE_TIME, TimeUnit.NANOSECONDS, keepAliveDisconnectService);
        when(channel.isActive()).thenReturn(true);
        when(channel.isRegistered()).thenReturn(true);
        keepAliveDisconnectHandler.channelRead(ctx, new byte[12]);
        assertTrue(keepAliveDisconnectHandler.isReading());
        keepAliveDisconnectHandler.channelReadComplete(ctx);
        assertFalse(keepAliveDisconnectHandler.isReading());
    }

    @Test
    public void test_initialize_whenNotInitialize_thenInitialize() {
        final KeepAliveDisconnectHandler keepAliveDisconnectHandler = new KeepAliveDisconnectHandler(READER_IDLE_TIME, TimeUnit.NANOSECONDS, keepAliveDisconnectService);
        when(channel.isActive()).thenReturn(true);
        when(channel.isRegistered()).thenReturn(true);
        keepAliveDisconnectHandler.initialize(channel);
        verify(executor, times(1)).schedule(any(KeepAliveDisconnectHandler.ReaderIdleTimeoutTask.class), eq(READER_IDLE_TIME), eq(TimeUnit.NANOSECONDS));
        assertEquals(1, keepAliveDisconnectHandler.getState());
    }

    @Test
    public void test_initialize_whenAlreadyInitialized_thenDontInitialize() {
        final KeepAliveDisconnectHandler keepAliveDisconnectHandler = new KeepAliveDisconnectHandler(READER_IDLE_TIME, TimeUnit.NANOSECONDS, keepAliveDisconnectService);
        when(channel.isActive()).thenReturn(true);
        when(channel.isRegistered()).thenReturn(true);

        keepAliveDisconnectHandler.initialize(channel);
        verify(executor, times(1)).schedule(any(KeepAliveDisconnectHandler.ReaderIdleTimeoutTask.class), eq(READER_IDLE_TIME), eq(TimeUnit.NANOSECONDS));
        assertEquals(1, keepAliveDisconnectHandler.getState());
        keepAliveDisconnectHandler.initialize(channel);
        verify(executor, times(1)).schedule(any(KeepAliveDisconnectHandler.ReaderIdleTimeoutTask.class), eq(READER_IDLE_TIME), eq(TimeUnit.NANOSECONDS));
        assertEquals(1, keepAliveDisconnectHandler.getState());
    }

    @Test
    public void test_initialize_whenDestroyed_thenDontInitialize() {
        final KeepAliveDisconnectHandler keepAliveDisconnectHandler = new KeepAliveDisconnectHandler(READER_IDLE_TIME, TimeUnit.NANOSECONDS, keepAliveDisconnectService);
        when(channel.isActive()).thenReturn(true);
        when(channel.isRegistered()).thenReturn(true);
        keepAliveDisconnectHandler.handlerRemoved(ctx);
        keepAliveDisconnectHandler.initialize(channel);
        verify(executor, never()).schedule(any(KeepAliveDisconnectHandler.ReaderIdleTimeoutTask.class), eq(READER_IDLE_TIME), eq(TimeUnit.NANOSECONDS));
        assertEquals(2, keepAliveDisconnectHandler.getState());
    }

    @Test
    public void test_initialize_whenKeepAliveIs0_thenDontScheduleTask() {
        final KeepAliveDisconnectHandler keepAliveDisconnectHandler = new KeepAliveDisconnectHandler(0, TimeUnit.NANOSECONDS, keepAliveDisconnectService);
        when(channel.isActive()).thenReturn(true);
        when(channel.isRegistered()).thenReturn(true);
        keepAliveDisconnectHandler.initialize(channel);
        verify(executor, never()).schedule(any(KeepAliveDisconnectHandler.ReaderIdleTimeoutTask.class), eq(READER_IDLE_TIME), eq(TimeUnit.NANOSECONDS));
        assertEquals(1, keepAliveDisconnectHandler.getState());
    }



    @Test
    public void test_ReaderIdleTimeoutTask_run_whenReadTimedOut_thenSubmitToKeepAliveDisconnector() {
        // we need to spy to be able to mock the ticksInNanos() method
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        final ArgumentCaptor<Channel> argumentCaptor = ArgumentCaptor.forClass(Channel.class);
        doNothing().when(keepAliveDisconnectService).submitKeepAliveDisconnect(argumentCaptor.capture());
        final KeepAliveDisconnectHandler keepAliveDisconnectHandler = spy(new KeepAliveDisconnectHandler(READER_IDLE_TIME, TimeUnit.NANOSECONDS, keepAliveDisconnectService));
        when(keepAliveDisconnectHandler.ticksInNanos()).thenReturn(0L);
        keepAliveDisconnectHandler.initialize(embeddedChannel);
        when(keepAliveDisconnectHandler.ticksInNanos()).thenReturn(READER_IDLE_TIME * 2);
        await().until(() -> {
            embeddedChannel.runPendingTasks();
            return argumentCaptor.getAllValues().size() == 1;
        });
    }

    @Test
    public void test_ReaderIdleTimeoutTask_run_whileReading_dontDisconnect() throws Exception {
        // we need to spy to be able to mock the ticksInNanos() method
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        when(ctx.channel()).thenReturn(embeddedChannel);
        final ArgumentCaptor<Channel> argumentCaptor = ArgumentCaptor.forClass(Channel.class);
        doNothing().when(keepAliveDisconnectService).submitKeepAliveDisconnect(argumentCaptor.capture());
        final KeepAliveDisconnectHandler keepAliveDisconnectHandler = spy(new KeepAliveDisconnectHandler(READER_IDLE_TIME, TimeUnit.NANOSECONDS, keepAliveDisconnectService));
        when(keepAliveDisconnectHandler.ticksInNanos()).thenReturn(0L);
        keepAliveDisconnectHandler.initialize(embeddedChannel);
        keepAliveDisconnectHandler.channelRead(ctx, new byte[12]);
        when(keepAliveDisconnectHandler.ticksInNanos()).thenReturn(READER_IDLE_TIME * 2);
        assertThrows(ConditionTimeoutException.class, () -> await().timeout(2, TimeUnit.SECONDS).until(() -> {
            embeddedChannel.runPendingTasks();
            return argumentCaptor.getAllValues().size() == 1;
        }));

        // complete the read and test that it now times out correctly
        when(keepAliveDisconnectHandler.ticksInNanos()).thenReturn(0L);
        keepAliveDisconnectHandler.channelReadComplete(ctx);
        when(keepAliveDisconnectHandler.ticksInNanos()).thenReturn(READER_IDLE_TIME * 2);

        await().timeout(2, TimeUnit.SECONDS).until(() -> {
            embeddedChannel.runPendingTasks();
            return argumentCaptor.getAllValues().size() == 1;
        });
    }
}