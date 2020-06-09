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
package com.hivemq.security.ssl;

import com.hivemq.bootstrap.netty.ChannelHandlerNames;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;

import javax.inject.Singleton;
import javax.net.ssl.SSLSession;

import static io.netty.channel.ChannelHandler.Sharable;

/**
 * @author Florian Limpöck
 */
@Sharable
@Singleton
public class SslParameterHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {

        if (!(evt instanceof SslHandshakeCompletionEvent)) {
            super.userEventTriggered(ctx, evt);
            return;
        }

        final Channel channel = ctx.channel();
        final SslHandler sslHandler = (SslHandler) channel.pipeline().get(ChannelHandlerNames.SSL_HANDLER);
        final SSLSession session = sslHandler.engine().getSession();
        channel.attr(ChannelAttributes.AUTH_CIPHER_SUITE).set(session.getCipherSuite());
        channel.attr(ChannelAttributes.AUTH_PROTOCOL).set(session.getProtocol());

        channel.pipeline().remove(this);

        super.userEventTriggered(ctx, evt);
    }
}
