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

package com.hivemq.mqtt.handler.publish;

import com.hivemq.mqtt.message.MessageIDPools;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.pool.MessageIDPool;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * This handler is responsible for returning message ids to the message pool
 * if a QoS 1 and 2 acknowledgement was sent
 *
 * @author Dominik Obermaier
 */
@Singleton
@ChannelHandler.Sharable
public class ReturnMessageIdToPoolHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ReturnMessageIdToPoolHandler.class);

    private final MessageIDPools messageIDPools;

    @Inject
    ReturnMessageIdToPoolHandler(final MessageIDPools messageIDPools) {
        this.messageIDPools = messageIDPools;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        super.channelRead(ctx, msg);

        //QoS 1 || 2 finished
        if (msg instanceof PUBACK || msg instanceof PUBCOMP) {

            //Both acks ar a subclass of MessageWithID
            final int messageId = ((MessageWithID) msg).getPacketIdentifier();

            //Such a message ID must never be null, but better be safe than sorry
            if (messageId > 0) {

                final String client = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();

                if (client != null) {

                    final MessageIDPool messageIDPool = messageIDPools.forClientOrNull(client);
                    if (messageIDPool != null) {
                        messageIDPool.returnId(messageId);
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Returning Message ID {} for client {} because of a {} message was received", messageId, client, msg.getClass().getSimpleName());
                    }
                }
                //Should never happen
                else {
                    log.warn("Could not return message id {} to the pool because there was an empty client id", messageId);
                }
            }
        }
    }


}
