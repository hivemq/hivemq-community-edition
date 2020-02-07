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

package com.hivemq.metrics.handler;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.publish.PUBLISH;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * This handler gathers statistics about MQTT messages.
 *
 * @author Dominik Obermaier
 * @author Florian Limpöck
 */
@ChannelHandler.Sharable
@Singleton
public class GlobalMQTTMessageCounter extends ChannelDuplexHandler {

    private final @NotNull MetricsHolder metricsHolder;

    @Inject
    public GlobalMQTTMessageCounter(final @NotNull MetricsHolder metricsHolder) {
        this.metricsHolder = metricsHolder;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final @NotNull Object msg) throws Exception {

        if (msg instanceof Message) {

            metricsHolder.getIncomingMessageCounter().inc();

            if (msg instanceof CONNECT) {
                metricsHolder.getIncomingConnectCounter().inc();
            }

            if (msg instanceof PUBLISH) {
                metricsHolder.getIncomingPublishCounter().inc();
            }
        }

        super.channelRead(ctx, msg);
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final @NotNull Object msg, final @NotNull ChannelPromise promise) throws Exception {

        if (msg instanceof Message) {

            metricsHolder.getOutgoingMessageCounter().inc();

            if (msg instanceof PUBLISH) {
                metricsHolder.getOutgoingPublishCounter().inc();
            }
        }

        super.write(ctx, msg, promise);
    }

}
