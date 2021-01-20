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
package com.hivemq.metrics.handler;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import javax.inject.Inject;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.ALL_CHANNELS_GROUP_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.GLOBAL_TRAFFIC_COUNTER;

/**
 * The statistics initializer which is responsible for adding
 * all statistic handlers to the channel pipeline
 *
 * @author Dominik Obermaier
 */
public class MetricsInitializer extends ChannelHandlerAdapter {


    private final GlobalTrafficCounter globalTrafficCounter;

    @Inject
    MetricsInitializer(final GlobalTrafficCounter globalTrafficCounter) {
        this.globalTrafficCounter = globalTrafficCounter;
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {

        ctx.pipeline().addAfter(ALL_CHANNELS_GROUP_HANDLER, GLOBAL_TRAFFIC_COUNTER, globalTrafficCounter);

        //We're removing ourselves after the statistic handlers were added
        ctx.pipeline().remove(this);
    }
}
