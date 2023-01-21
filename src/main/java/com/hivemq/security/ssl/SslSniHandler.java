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

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.SSL_HANDLER;

public class SslSniHandler extends SniHandler {

    private static final Logger log = LoggerFactory.getLogger(SslSniHandler.class);

    private final @NotNull SslHandler sslHandler;

    public SslSniHandler(final @NotNull SslHandler sslHandler, final @NotNull SslContext sslContext) {
        super((input, promise) -> {
            //This could be used to return a different SslContext depending on the provided hostname
            //For now the same SslContext is returned independent of the provided hostname
            promise.setSuccess(sslContext);
            return promise;
        });
        this.sslHandler = sslHandler;
    }

    @Override
    protected void replaceHandler(final @NotNull ChannelHandlerContext ctx, final @Nullable String hostname, final @NotNull SslContext sslContext) throws Exception {

        if (hostname != null) {
            final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
            clientConnection.setAuthSniHostname(hostname);
            if (log.isTraceEnabled()) {
                log.trace("Client with IP '{}' sent SNI hostname '{}'", clientConnection.getChannelIP().orElse("UNKNOWN"), hostname);
            }
        }

        SslHandler sslHandlerInstance = null;
        try {
            sslHandlerInstance = sslHandler;
            ctx.pipeline().replace(this, SSL_HANDLER, sslHandlerInstance);
            sslHandlerInstance = null;
        } catch (final NoSuchElementException ignored) {
            //ignore, happens when channel is already closed
        } finally {
            // Since the SslHandler was not inserted into the pipeline the ownership of the SSLEngine was not
            // transferred to the SslHandler.
            // See https://github.com/netty/netty/issues/5678
            if (sslHandlerInstance != null) {
                ReferenceCountUtil.safeRelease(sslHandlerInstance.engine());
            }
        }
    }
}
