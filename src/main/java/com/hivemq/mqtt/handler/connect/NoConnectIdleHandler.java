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
package com.hivemq.mqtt.handler.connect;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.util.ChannelUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.NEW_CONNECTION_IDLE_HANDLER;

/**
 * @author Christoph Schäbel
 * @author Silvio Giebl
 */
@Singleton
@ChannelHandler.Sharable
public class NoConnectIdleHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(NoConnectIdleHandler.class);
    private final @NotNull EventLog eventLog;

    @Inject
    public NoConnectIdleHandler(final @NotNull EventLog eventLog) {
        this.eventLog = eventLog;
    }

    @Override
    public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) {
        if (msg instanceof CONNECT) {
            try {
                ctx.pipeline().remove(NEW_CONNECTION_IDLE_HANDLER);
                ctx.pipeline().remove(this);
            } catch (final NoSuchElementException ex) {
                //no problem, because if these handlers are not in the pipeline anyway, we still get the expected result here
                log.trace("Not able to remove no connect idle handler");
            }
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void userEventTriggered(final @NotNull ChannelHandlerContext ctx, final @NotNull Object evt) {

        if (evt instanceof IdleStateEvent) {

            if (((IdleStateEvent) evt).state() == IdleState.READER_IDLE) {
                if (log.isDebugEnabled()) {

                    log.debug("Client with IP {} disconnected. The client was idle for too long without sending a MQTT CONNECT packet",
                            ChannelUtils.getChannelIP(ctx.channel()).or("UNKNOWN"));
                }
                eventLog.clientWasDisconnected(ctx.channel(), "No CONNECT sent in time");
                ctx.close();
                return;
            }
        }
        ctx.fireUserEventTriggered(evt);
    }
}