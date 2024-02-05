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

import com.hivemq.bootstrap.ClientConnectionContext;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.connect.NoTlsHandshakeIdleHandler;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.NEW_CONNECTION_IDLE_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.NO_TLS_HANDSHAKE_IDLE_EVENT_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.SSL_HANDLER;

public class SslSniHandler extends SniHandler {

    private static final Logger log = LoggerFactory.getLogger(SslSniHandler.class);
    private final @NotNull Tls tls;
    private final @NotNull Channel ch;
    private final Consumer<Channel> idleHandlerFunction;
    private final @NotNull SslFactory sslFactory;
    private final @NotNull HashMap<String, SslHandler> aliasSslHandlerMap = new HashMap<>();

    private final IdleStateHandler idleStateHandler;
    private final NoTlsHandshakeIdleHandler noTlsHandshakeIdleHandler;

    public SslSniHandler(
            final @NotNull SslContext sslContext, final @NotNull SslFactory sslFactory, final MqttServerDisconnector mqttServerDisconnector,
            final @NotNull Channel ch, final Tls tls,
            final Consumer<Channel> idleHandlerFunction) {
        super((input, promise) -> {
            //This could be used to return a different SslContext depending on the provided hostname
            //For now the same SslContext is returned independent of the provided hostname

            log.trace("SSLContext with input {}, cipherSuites {} and attributes {}", input, sslContext.cipherSuites(), sslContext.attributes());
            promise.setSuccess(sslContext);
            return promise;
        });

        this.tls = tls;
        this.ch = ch;
        this.idleHandlerFunction = idleHandlerFunction;
        this.sslFactory = sslFactory;

        final int handshakeTimeout = tls.getHandshakeTimeout();
        idleStateHandler = new IdleStateHandler(handshakeTimeout, 0, 0, TimeUnit.MILLISECONDS);
        noTlsHandshakeIdleHandler = new NoTlsHandshakeIdleHandler(mqttServerDisconnector);

        if (handshakeTimeout > 0) {
            ch.pipeline().addLast(NEW_CONNECTION_IDLE_HANDLER, idleStateHandler);
            ch.pipeline().addLast(NO_TLS_HANDSHAKE_IDLE_EVENT_HANDLER, noTlsHandshakeIdleHandler);
        }
    }

    @Override
    protected void replaceHandler(
            final @NotNull ChannelHandlerContext ctx,
            final @Nullable String hostname,
            final @NotNull SslContext sslContext) {

        if (hostname != null) {
            final ClientConnectionContext clientConnectionContext = ClientConnectionContext.of(ctx.channel());
            clientConnectionContext.setAuthSniHostname(hostname);
            if (log.isTraceEnabled()) {
                log.trace("Client with IP '{}' sent SNI hostname '{}'",
                        clientConnectionContext.getChannelIP().orElse("UNKNOWN"),
                        hostname);
            }
        }

        SslHandler sslHandlerInstance = null;
        try {
            final int port = ClientConnectionContext.of(ctx.channel()).getConnectedListener().getPort();
            log.trace("Replace ssl handler for hostname: {} and port: {}", hostname, port);
            if (!aliasSslHandlerMap.containsKey(hostname)) {
                aliasSslHandlerMap.put(hostname, sslFactory.getSslHandler(ch, tls, sslContext, hostname, port));
            }
            sslHandlerInstance = aliasSslHandlerMap.get(hostname);

            sslHandlerInstance.handshakeFuture().addListener(future -> {
                if (tls.getHandshakeTimeout() > 0) {
                    ch.pipeline().remove(idleStateHandler);
                    ch.pipeline().remove(noTlsHandshakeIdleHandler);
                }
                idleHandlerFunction.accept(ch);
            });

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
