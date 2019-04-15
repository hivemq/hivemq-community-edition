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

import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelUtils;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

/**
 * This handler is responsible for stopping the read operations on the socket as soon as a CONNECT message was sent by a
 * client in order to create backpressure on the MQTT client side. We start re-consuming things from the client as soon
 * as we send a CONNACK. This behaviour is important because if we don't want to give malicious MQTT clients the chance
 * to allow DOS attacks by sending large amounts of data.
 *
 * @author Dominik Obermaier
 */
@ChannelHandler.Sharable
@Singleton
public class StopReadingAfterConnectHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(StopReadingAfterConnectHandler.class);

    /**
     * This listener is stateless, so we can reuse the same listener and reduce garbage
     **/
    private static final ReenableAutoReadListener REENABLE_AUTO_READ_LISTENER = new ReenableAutoReadListener();

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof CONNECT) {
            if (log.isTraceEnabled()) {
                log.trace("Suspending read operations for MQTT client with clientId {} and IP {}", ((CONNECT) msg).getClientIdentifier(), ChannelUtils.getChannelIP(ctx.channel()).or("UNKNOWN"));
            }
            ctx.channel().config().setAutoRead(false);
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        if (msg instanceof CONNACK) {
            //We don't need to start reading anything from a client we don't accept.
            if (((CONNACK) msg).getReasonCode() == Mqtt5ConnAckReasonCode.SUCCESS) {

                promise.addListener(REENABLE_AUTO_READ_LISTENER);
            }
        }
        super.write(ctx, msg, promise);
    }

    /**
     * A listener that reenables the auto reading if the future returns successfully
     */
    private static class ReenableAutoReadListener implements ChannelFutureListener {

        @Override
        public void operationComplete(final ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                if (log.isTraceEnabled()) {
                    log.trace("Restarting read operations for MQTT client with IP {}", ChannelUtils.getChannelIP(future.channel()).or("UNKNOWN"));
                }
                future.channel().config().setAutoRead(true);
            }
        }
    }
}
