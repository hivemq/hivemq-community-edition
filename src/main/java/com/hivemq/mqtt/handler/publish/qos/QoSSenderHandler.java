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

package com.hivemq.mqtt.handler.publish.qos;

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 */
public class QoSSenderHandler extends ChannelDuplexHandler {

    @NotNull
    private static final Logger log = LoggerFactory.getLogger(QoSSenderHandler.class);

    private final @NotNull PublishPollService publishPollService;


    @Inject
    QoSSenderHandler(@NotNull final PublishPollService publishPollService) {
        this.publishPollService = publishPollService;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, @NotNull final Object msg) throws Exception {

        final String client = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();

        if (msg instanceof CONNECT) {

            //devour message

        } else if (msg instanceof PUBACK) {

            //QoS1
            handlePuback((PUBACK) msg, client);

        } else if (msg instanceof PUBREC) {

            //QoS2
            handlePubrec(ctx, (PUBREC) msg, client);

        } else if (msg instanceof PUBCOMP) {
            //QoS2
            handlePubcomp((PUBCOMP) msg, client);

        } else {
            super.channelRead(ctx, msg);

        }
    }

    private void handlePubcomp(@NotNull final PUBCOMP msg, @NotNull final String client) {
        log.trace("Client {}: Received PUBCOMP", client);

        if (log.isTraceEnabled()) {
            log.trace("Client {}: Received PUBCOMP remove message id:[{}]", client, msg.getPacketIdentifier());
        }
    }

    private void handlePuback(final PUBACK msg, @NotNull final String client) {
        log.trace("Client {}: Received PUBACK", client);
        final int messageId = msg.getPacketIdentifier();

        if (log.isTraceEnabled()) {
            log.trace("Client {}: Received PUBACK remove message id:[{}] ", client, messageId);
        }
    }

    private void handlePubrec(@NotNull final ChannelHandlerContext ctx, @NotNull final PUBREC msg, @NotNull final String client) {
        log.trace("Client {}: Received pubrec", client);

        final ListenableFuture<Void> future = publishPollService.putPubrelInQueue(client, msg.getPacketIdentifier());
        FutureUtils.addExceptionLogger(future);
        if (log.isTraceEnabled()) {
            log.trace("Client {}: Received PUBREC remove message id:[{}]", client, msg.getPacketIdentifier());
        }
        //We send it with channel instead of context because otherwise we can't intercept the write in this handler
        ctx.channel().writeAndFlush(new PUBREL(msg.getPacketIdentifier()));
    }

}