package com.hivemq.mqtt.handler;

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.annotations.ExecuteInEventloop;
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
    private static final long MIN_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

    private final long readerIdleTimeNanos;

    private @Nullable Future<?> timeoutTaskFuture;
    private long lastReadTime;

    private byte state; // 0 - none, 1 - initialized, 2 - destroyed
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
    public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
            // channelActive() event has been fired already, which means this.channelActive() will
            // not be invoked. We have to initialize here instead.
            initialize(ctx.channel());
        }
    }

    @Override
    public void handlerRemoved(final @NotNull ChannelHandlerContext ctx) throws Exception {
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
    public void channelReadComplete(final @NotNull ChannelHandlerContext ctx) throws Exception {
        if ((readerIdleTimeNanos > 0) && reading) {
            lastReadTime = ticksInNanos();
            reading = false;
        }
        ctx.fireChannelReadComplete();
    }

    @VisibleForTesting
    void initialize(final @NotNull Channel channel) {
        //only initialize of it's not initialized
        if (state == 0) {
            state = 1;
            lastReadTime = ticksInNanos();
            if (readerIdleTimeNanos > 0) {
                timeoutTaskFuture = channel.eventLoop().schedule(new ReaderIdleTimeoutTask(channel), readerIdleTimeNanos, TimeUnit.NANOSECONDS);
            }
        }
    }

    @VisibleForTesting
    long ticksInNanos() {
        return System.nanoTime();
    }


    private void destroy() {
        state = 2;
        if (timeoutTaskFuture != null) {
            timeoutTaskFuture.cancel(false);
            timeoutTaskFuture = null;
        }
    }

    @ExecuteInEventloop
    public void pauseKeepAliveDisconnect() {
        if (timeoutTaskFuture != null) {
            timeoutTaskFuture.cancel(false);
            timeoutTaskFuture = null;
        }
    }

    @ExecuteInEventloop
    public void resumeKeepAliveDisconnect(final @NotNull Channel channel) {
        if (timeoutTaskFuture == null) {
            timeoutTaskFuture = channel.eventLoop().schedule(new ReaderIdleTimeoutTask(channel), readerIdleTimeNanos, TimeUnit.NANOSECONDS);
        }
    }

    class ReaderIdleTimeoutTask implements Runnable {

        private final @NotNull Channel channel;

        ReaderIdleTimeoutTask(final @NotNull Channel channel) {
            this.channel = channel;
        }

        @Override
        public void run() {
            if (!channel.isOpen()) {
                return;
            }
            long nextDelay = readerIdleTimeNanos;
            if (!reading) {
                nextDelay -= ticksInNanos() - lastReadTime;
            }

            if (nextDelay <= 0) {
                keepAliveDisconnectService.submitKeepAliveDisconnect(channel);
            } else {
                // Read occurred before the timeout - set a new timeout with shorter delay.
                timeoutTaskFuture = channel.eventLoop().schedule(this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }

    }

    @VisibleForTesting
    public byte getState() {
        return state;
    }

    @VisibleForTesting
    public boolean isReading() {
        return reading;
    }

    @VisibleForTesting
    public long getReaderIdleTimeNanos() {
        return readerIdleTimeNanos;
    }
}
