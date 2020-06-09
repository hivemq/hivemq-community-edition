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
package com.hivemq.mqtt.handler.ping;

import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.PINGRESP;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Christoph Schäbel
 */
@Singleton
@ChannelHandler.Sharable
public class PingRequestHandler extends SimpleChannelInboundHandler<PINGREQ> {

    private final Logger log = LoggerFactory.getLogger(PingRequestHandler.class);

    private static final PINGRESP PING_RESPONSE = new PINGRESP();

    @Inject
    PingRequestHandler() {
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final PINGREQ msg) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("PingReq received for client {}.", ctx.channel().attr(ChannelAttributes.CLIENT_ID).get());
        }
        ctx.writeAndFlush(PING_RESPONSE);
        if (log.isTraceEnabled()) {
            log.trace("PingResp sent for client {}.", ctx.channel().attr(ChannelAttributes.CLIENT_ID).get());
        }
    }

}
