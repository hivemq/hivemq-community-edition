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
package com.hivemq.mqtt.handler.publish;

import com.codahale.metrics.Counter;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Daniel Krüger
 */
public class PublishSendHandler extends ChannelInboundHandlerAdapter implements Runnable {

    private @Nullable ChannelHandlerContext ctx;
    private final @NotNull LinkedList<PublishWithFuture> messagesToWrite = new LinkedList<>();
    private final @NotNull Counter channelNotWritable;
    private boolean wasWritable = true;

    public PublishSendHandler(final @NotNull MetricsHolder metricsHolder) {
        channelNotWritable = metricsHolder.getChannelNotWritableCounter();
    }

    @Override
    public void handlerAdded(final @NotNull ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void channelWritabilityChanged(final @NotNull ChannelHandlerContext ctx) {
        final Channel channel = ctx.channel();
        if (channel.isWritable() && !wasWritable) {
            wasWritable = true;
            channelNotWritable.dec();

            channel.eventLoop().execute(this);
        }
        ctx.fireChannelWritabilityChanged();
    }

    public void sendPublishes(final @NotNull List<PublishWithFuture> publishes) {
        assert ctx != null : "ctx can not be null because sendPublishes is called after handlerAdded";
        ctx.channel().eventLoop().execute(() -> {
            messagesToWrite.addAll(publishes);
            consumeQueue();
        });
    }

    @Override
    public void run() {
        consumeQueue();
    }

    private void consumeQueue() {
        assert ctx != null : "ctx can not be null because consumeQueue is called after handlerAdded";
        int written = 0;
        while (!messagesToWrite.isEmpty()) {
            if (!ctx.channel().isWritable()) {
                if (wasWritable) {
                    wasWritable = false;
                    channelNotWritable.inc();
                }
                break;
            }
            final PublishWithFuture publish = messagesToWrite.poll();
            ctx.write(publish).addListener(new PublishWriteFailedListener(publish.getFuture()));
            written++;
            if (written >= InternalConfigurations.MAX_PUBLISHES_BEFORE_FLUSH.get()) {
                ctx.flush();
                written = 0;
            }
        }

        if (written > 0) {
            ctx.flush();
        }
    }

}
