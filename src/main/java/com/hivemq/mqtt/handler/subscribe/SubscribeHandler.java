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
package com.hivemq.mqtt.handler.subscribe;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.handler.IncomingSubscribeHandler;
import com.hivemq.mqtt.handler.connect.SubscribeMessageBarrier;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Florian Limpöck
 */
@Singleton
@ChannelHandler.Sharable
public class SubscribeHandler extends SimpleChannelInboundHandler<SUBSCRIBE> {

    private final @NotNull IncomingSubscribeHandler incomingSubscribeHandler;

    @Inject
    public SubscribeHandler(final @NotNull IncomingSubscribeHandler incomingSubscribeHandler) {
        this.incomingSubscribeHandler = incomingSubscribeHandler;
    }

    @Override
    protected void channelRead0(@NotNull final ChannelHandlerContext ctx, @NotNull final SUBSCRIBE msg) throws Exception {
        SubscribeMessageBarrier.addToPipeline(ctx);
        incomingSubscribeHandler.interceptOrDelegate(ctx, msg);
    }
}
