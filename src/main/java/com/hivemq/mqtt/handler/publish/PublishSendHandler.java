package com.hivemq.mqtt.handler.publish;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Daniel Krüger
 */
public class PublishSendHandler extends ChannelInboundHandlerAdapter {

    private @Nullable ChannelHandlerContext ctx;
    private final @NotNull LinkedList<PublishWithFuture> messagesToWrite = new LinkedList<>();

    @Override
    public void handlerAdded(final @NotNull ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void channelWritabilityChanged(final @NotNull ChannelHandlerContext ctx) {
        final Channel channel = ctx.channel();
        if (channel.isWritable()) {
            channel.eventLoop().execute(this::consumeQueue);
        }
        ctx.fireChannelWritabilityChanged();
    }

    public void sendPublish(final @NotNull List<PublishWithFuture> publishes) {
        assert ctx != null : "Context must not be null";
        messagesToWrite.addAll(publishes);
        consumeQueue();
    }

    public void consumeQueue() {
        assert ctx != null : "Context must not be null";
        int written = 0;
        while (ctx.channel().isWritable() && !messagesToWrite.isEmpty()) {
            final PublishWithFuture publishFuture = messagesToWrite.poll();
            ctx.write(publishFuture).addListener(new PublishWriteFailedListener(publishFuture.getFuture()));
            written++;
        }
        if (written > 0) {
            ctx.flush();
        }
    }

}
