/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.mqtt.handler.publish;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 * This handler is responsible for handling PUBLISH events which were sent from other handlers internally.
 * So essentially this handler is used for republishing the internal PUBLISH event to the channel this handler
 * is added to.
 *
 * @author Dominik Obermaier
 */
public class PublishUserEventReceivedHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {

        final PUBLISH publish;
        final SettableFuture<PublishStatus> statusFuture;
        if (evt instanceof PublishWithFuture) {
            publish = ((PublishWithFuture) evt);
            statusFuture = ((PublishWithFuture) evt).getFuture();
            writePublish(ctx, publish, statusFuture);
        } else if (evt instanceof PUBLISH) {
            publish = (PUBLISH) evt;
            writePublish(ctx, publish, null);
        } else {
            super.userEventTriggered(ctx, evt);
        }

    }

    private void writePublish(final ChannelHandlerContext ctx, final PUBLISH publish, @Nullable final SettableFuture<PublishStatus> statusFuture) {

        if (statusFuture == null) {
            ctx.writeAndFlush(publish);
        } else {
            final ChannelPromise channelPromise = ctx.channel().newPromise();
            ctx.writeAndFlush(publish, channelPromise);

            channelPromise.addListener(new PublishWritePromiseListener(statusFuture));
        }

    }
}
