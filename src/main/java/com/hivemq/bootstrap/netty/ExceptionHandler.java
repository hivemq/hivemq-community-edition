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

package com.hivemq.bootstrap.netty;

import com.hivemq.logging.EventLog;
import com.hivemq.util.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;

/**
 * @author Christoph Schäbel
 */
@Singleton
@ChannelHandler.Sharable
public class ExceptionHandler extends ChannelHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandler.class);

    private final EventLog eventLog;

    @Inject
    public ExceptionHandler(final EventLog eventLog) {
        this.eventLog = eventLog;
    }

    /* java.io.IOException: Connection reset by peer
     * java.io.IOException: Broken pipe
     * java.nio.channels.ClosedChannelException: null
     * javax.net.ssl.SSLException: not an SSL/TLS record (Use http://... URL to connect to HTTPS server)
     * java.lang.IllegalArgumentException: empty text (Use https://... URL to connect to HTTP server)
     */

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {

        final Channel channel = ctx.channel();

        if (cause instanceof SSLException) {
            //We can ignore SSL Exceptions, since the channel gets closed anyway.
            return;

        } else if (cause instanceof ClosedChannelException) {
            //We can ignore this because the channel is already closed
            return;

        } else if (cause instanceof IOException) {

            //We can ignore this because the channel is already closed because of an IO problem
            return;

        } else if (cause instanceof CorruptedFrameException) {

            //We can ignore this because the channel is already closed because of an IO problem
            eventLog.clientWasDisconnected(channel, "Illegal websocket data sent by client: " + cause.getMessage());
            channel.close();
            return;


        } else if (cause instanceof IllegalArgumentException) {

            //do not log IllegalArgumentException as error

        } else {
            log.error("An unexpected error occurred for client with IP {}: {}",
                    ChannelUtils.getChannelIP(channel).or("UNKNOWN"), ExceptionUtils.getStackTrace(cause));
        }

        if (channel != null) {
            eventLog.clientWasDisconnected(channel, "Channel exception: " + cause.getMessage());
            channel.close();
        }
    }
}
