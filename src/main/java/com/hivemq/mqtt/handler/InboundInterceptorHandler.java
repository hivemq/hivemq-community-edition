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
package com.hivemq.mqtt.handler;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.handler.*;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Daniel Krüger
 */
@Singleton
@ChannelHandler.Sharable
public class InboundInterceptorHandler extends ChannelInboundHandlerAdapter {

    private final @NotNull ConnectInboundInterceptorHandler connectInboundInterceptorHandler;

    private final @NotNull PubackInterceptorHandler pubackInterceptorHandler;
    private final @NotNull PubrecInterceptorHandler pubrecInterceptorHandler;
    private final @NotNull PubrelInterceptorHandler pubrelInterceptorHandler;
    private final @NotNull PubcompInterceptorHandler pubcompInterceptorHandler;

    private final @NotNull UnsubscribeInboundInterceptorHandler unsubscribeInboundInterceptorHandler;
    private final @NotNull PingInterceptorHandler pingInterceptorHandler;
    private final @NotNull DisconnectInterceptorHandler disconnectInterceptorHandler;

    @Inject
    public InboundInterceptorHandler(
            final @NotNull ConnectInboundInterceptorHandler connectInboundInterceptorHandler,
            final @NotNull PubackInterceptorHandler pubackInterceptorHandler,
            final @NotNull PubrecInterceptorHandler pubrecInterceptorHandler,
            final @NotNull PubrelInterceptorHandler pubrelInterceptorHandler,
            final @NotNull PubcompInterceptorHandler pubcompInterceptorHandler,
            final @NotNull UnsubscribeInboundInterceptorHandler unsubscribeInboundInterceptorHandler,
            final @NotNull PingInterceptorHandler pingInterceptorHandler,
            final @NotNull DisconnectInterceptorHandler disconnectInterceptorHandler) {
        this.connectInboundInterceptorHandler = connectInboundInterceptorHandler;
        this.pubackInterceptorHandler = pubackInterceptorHandler;
        this.pubrecInterceptorHandler = pubrecInterceptorHandler;
        this.pubrelInterceptorHandler = pubrelInterceptorHandler;
        this.pubcompInterceptorHandler = pubcompInterceptorHandler;
        this.unsubscribeInboundInterceptorHandler = unsubscribeInboundInterceptorHandler;
        this.pingInterceptorHandler = pingInterceptorHandler;
        this.disconnectInterceptorHandler = disconnectInterceptorHandler;
    }

    @Override
    public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) {
        //the order is important: it has to be ordered by the expected frequency to avoid instance of checks
        if (msg instanceof PUBACK) {
            pubackInterceptorHandler.handleInboundPuback(ctx, ((PUBACK) msg));
        } else if (msg instanceof PUBREC) {
            pubrecInterceptorHandler.handleInboundPubrec(ctx, ((PUBREC) msg));
        } else if (msg instanceof PUBREL) {
            pubrelInterceptorHandler.handleInboundPubrel(ctx, ((PUBREL) msg));
        } else if (msg instanceof PUBCOMP) {
            pubcompInterceptorHandler.handleInboundPubcomp(ctx, ((PUBCOMP) msg));
        } else if (msg instanceof PINGREQ) {
            pingInterceptorHandler.handleInboundPingReq(ctx, ((PINGREQ) msg));
        } else if (msg instanceof UNSUBSCRIBE) {
            unsubscribeInboundInterceptorHandler.handleInboundUnsubscribe(ctx, (UNSUBSCRIBE) msg);
        } else if (msg instanceof DISCONNECT) {
            disconnectInterceptorHandler.handleInboundDisconnect(ctx, ((DISCONNECT) msg));
        } else if (msg instanceof CONNECT) {
            connectInboundInterceptorHandler.readConnect(ctx, (CONNECT) msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }
}
