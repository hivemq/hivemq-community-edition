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
package com.hivemq.bootstrap.netty.initializer;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;

/**
 * @author Christoph Schäbel
 */
public class ChannelGroupHandler extends ChannelInboundHandlerAdapter {

    private final @NotNull ChannelGroup channelGroup;

    public ChannelGroupHandler(final @NotNull ChannelGroup channelGroup) {
        this.channelGroup = channelGroup;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        channelGroup.add(ctx.channel());
        ctx.fireChannelActive();
        ctx.pipeline().remove(this);
    }

}
