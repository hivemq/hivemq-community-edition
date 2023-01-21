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
package com.hivemq.websocket;

import com.hivemq.bootstrap.ClientConnection;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * A Websocket frame handler for text frames. MQTT does not allow any text frames, so clients
 * sending these frames will be disconnected
 *
 * @author Lukas Brandl
 */
public class WebSocketTextFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final Logger log = LoggerFactory.getLogger(WebSocketTextFrameHandler.class);

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final TextWebSocketFrame msg) throws Exception {
        final Channel channel = ctx.channel();
        channel.disconnect();
        if (log.isDebugEnabled()) {
            final ClientConnection clientConnection = channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
            final Optional<String> channelIP = (clientConnection == null)
                    ? Optional.empty()
                    : clientConnection.getChannelIP();

            log.debug("Sending websocket text frames is illegal, only binary frames are allowed for MQTT over websockets. " +
                    "Disconnecting client with IP{}.", channelIP);
        }
    }
}
