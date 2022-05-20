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

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;

import java.util.concurrent.TimeUnit;

/**
 * Basically a {@link IdleStateHandler} where all functions besides the read idle state are removed.
 */
public class KeepAliveDisconnectHandler extends ChannelInboundHandlerAdapter {
    static final long MIN_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(1);
    private static final byte NOT_INITIATED = 0;
    private static final byte INITIATED = 1;
    private static final byte DESTROYED = 2;

    private final long readerIdleTimeNanos;
    private @Nullable Future<?> timeoutTaskFuture;
    private long lastReadTime;
    private byte state = NOT_INITIATED;
    private boolean reading;
    private final @NotNull KeepAliveDisconnectService keepAliveDisconnectService;

    public KeepAliveDisconnectHandler(final long readerIdleTime,
                                      final @NotNull TimeUnit unit,
                                      final @NotNull KeepAliveDisconnectService keepAliveDisconnectService) {
        this.keepAliveDisconnectService = keepAliveDisconnectService;
        if (readerIdleTime <= 0) {
            readerIdleTimeNanos = 0;
        } else {
            readerIdleTimeNanos = Math.max(unit.toNanos(readerIdleTime), MIN_TIMEOUT_NANOS);
        }
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
        if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
            // channelActive() event has been fired already, which means this.channelActive() will
            // not be invoked. We have to initialize here instead.
            initialize(ctx.channel());
        }
    }

    @Override
    public void handlerRemoved(final @NotNull ChannelHandlerContext ctx) {
        destroy();
    }

    @Override
    public void channelRegistered(final ChannelHandlerContext ctx) throws Exception {
        // Initialize early if channel is active already.
        if (ctx.channel().isActive()) {
            initialize(ctx.channel());
        }
        super.channelRegistered(ctx);
    }

    @Override
    public void channelActive(final @NotNull ChannelHandlerContext ctx) throws Exception {
        // This method will be invoked only if this handler was added
        // before channelActive() event is fired.  If a user adds this handler
        // after the channelActive() event, initialize() will be called by beforeAdd().
        initialize(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(final @NotNull ChannelHandlerContext ctx) throws Exception {
        destroy();
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) throws Exception {
        if (readerIdleTimeNanos > 0) {
            reading = true;
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelReadComplete(final @NotNull ChannelHandlerContext ctx) {
        if ((readerIdleTimeNanos > 0) && reading) {
            lastReadTime = ticksInNanos();
            reading = false;
        }
        ctx.fireChannelReadComplete();
    }

    @VisibleForTesting
    void initialize(final @NotNull Channel channel) {
        //only initialize of it's not initialized
        if (state > NOT_INITIATED) {
            return;
        }
        state = INITIATED;
        lastReadTime = ticksInNanos();
        if (readerIdleTimeNanos > 0) {
            timeoutTaskFuture = channel.eventLoop().schedule(new ReaderIdleTimeoutTask(channel), readerIdleTimeNanos, TimeUnit.NANOSECONDS);
        }
    }

    @VisibleForTesting
    long ticksInNanos() {
        return System.nanoTime();
    }


    private void destroy() {
        state = DESTROYED;
        if (timeoutTaskFuture != null) {
            timeoutTaskFuture.cancel(false);
            timeoutTaskFuture = null;
        }
    }

    @VisibleForTesting
    public long getReaderIdleTimeNanos() {
        return readerIdleTimeNanos;
    }

    public int getState() {
        return state;
    }

    public boolean isReading() {
        return reading;
    }

    class ReaderIdleTimeoutTask implements Runnable {

        private final @NotNull Channel channel;

        ReaderIdleTimeoutTask(final @NotNull Channel channel) {
            this.channel = channel;
        }

        @Override
        public void run() {
            long nextDelay = readerIdleTimeNanos;
            try {
                if (!channel.isOpen()) {
                    return;
                }
                if (!reading) {
                    nextDelay -= ticksInNanos() - lastReadTime;
                }
                if (nextDelay <= 0) {
                    keepAliveDisconnectService.submitKeepAliveDisconnect(channel);
                } else {
                    // Read occurred before the timeout - set a new timeout with shorter delay.
                    timeoutTaskFuture = channel.eventLoop().schedule(this, nextDelay, TimeUnit.NANOSECONDS);
                }
            } catch (final Exception e) {
                timeoutTaskFuture = channel.eventLoop().schedule(this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }
}
