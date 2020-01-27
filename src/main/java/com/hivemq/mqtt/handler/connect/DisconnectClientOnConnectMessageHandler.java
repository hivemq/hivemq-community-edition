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

package com.hivemq.mqtt.handler.connect;

import com.google.inject.Inject;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.connect.CONNECT;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

import static com.hivemq.util.ChannelUtils.getChannelIP;

/**
 * This handler disconnects the client if it sends a MQTT CONNECT message was sent
 * <p>
 * This is typically added to the pipeline after the first MQTT CONNECT handler was added
 * to the pipeline because only one MQTT CONNECT message is allowed
 *
 * @author Dominik Obermaier
 */
@ChannelHandler.Sharable
@Singleton
public class DisconnectClientOnConnectMessageHandler extends SimpleChannelInboundHandler<CONNECT> {
    @NotNull
    private static final Logger log = LoggerFactory.getLogger(DisconnectClientOnConnectMessageHandler.class);

    @NotNull
    private final EventLog eventLog;

    @Inject
    DisconnectClientOnConnectMessageHandler(@NotNull final EventLog eventLog) {
        this.eventLog = eventLog;
    }


    @Override
    protected void channelRead0(@NotNull final ChannelHandlerContext ctx, @NotNull final CONNECT msg) throws Exception {

        if (log.isDebugEnabled()) {
            log.debug("The client with id {} and IP {} sent a second MQTT CONNECT message. This is not allowed. Disconnecting client", msg.getClientIdentifier(), getChannelIP(ctx.channel()).or("UNKNOWN"));
        }
        eventLog.clientWasDisconnected(ctx.channel(), "Sent second CONNECT message");
        ctx.channel().close();
    }
}
