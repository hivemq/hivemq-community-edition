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
package com.hivemq.bootstrap.netty.initializer;

import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.configuration.service.entity.TlsListener;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.connect.NoTlsHandshakeIdleHandler;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.security.exception.SslException;
import com.hivemq.security.ssl.SslClientCertificateHandler;
import com.hivemq.security.ssl.SslExceptionHandler;
import com.hivemq.security.ssl.SslFactory;
import com.hivemq.security.ssl.SslSniHandler;
import io.netty.channel.Channel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.*;

/**
 * @author Christoph Schäbel
 */
public abstract class AbstractTlsChannelInitializer extends AbstractChannelInitializer {

    private final @NotNull TlsListener tlsListener;
    private final @NotNull SslFactory sslFactory;
    private final @NotNull ChannelDependencies channelDependencies;

    public AbstractTlsChannelInitializer(@NotNull final ChannelDependencies channelDependencies,
                                         @NotNull final TlsListener tlsListener,
                                         @NotNull final SslFactory sslFactory) {

        super(channelDependencies, tlsListener);
        this.tlsListener = tlsListener;
        this.sslFactory = sslFactory;
        this.channelDependencies = channelDependencies;
    }

    @Override
    protected void addNoConnectIdleHandler(@NotNull final Channel ch) {
        // No connect idle handler are added, as soon as the TLS handshake is done.
    }

    protected void addNoConnectIdleHandlerAfterTlsHandshake(@NotNull final Channel ch) {
        super.addNoConnectIdleHandler(ch);
    }

    @Override
    protected void addSpecialHandlers(@NotNull final Channel ch) throws SslException {
        final int handshakeTimeout = tlsListener.getTls().getHandshakeTimeout();

        final IdleStateHandler idleStateHandler = new IdleStateHandler(handshakeTimeout, 0, 0, TimeUnit.MILLISECONDS);
        final MqttServerDisconnector mqttServerDisconnector = channelDependencies.getMqttServerDisconnector();
        final NoTlsHandshakeIdleHandler noTlsHandshakeIdleHandler = new NoTlsHandshakeIdleHandler(mqttServerDisconnector);
        if (handshakeTimeout > 0) {
            ch.pipeline().addLast(NEW_CONNECTION_IDLE_HANDLER, idleStateHandler);
            ch.pipeline().addLast(NO_TLS_HANDSHAKE_IDLE_EVENT_HANDLER, noTlsHandshakeIdleHandler);
        }

        final Tls tls = tlsListener.getTls();
        final SslContext sslContext = sslFactory.getSslContext(tls);
        final SslHandler sslHandler = sslFactory.getSslHandler(ch, tls, sslContext);
        sslHandler.handshakeFuture().addListener(future -> {
            if (handshakeTimeout > 0) {
                ch.pipeline().remove(idleStateHandler);
                ch.pipeline().remove(noTlsHandshakeIdleHandler);
            }
            addNoConnectIdleHandlerAfterTlsHandshake(ch);
        });

        ch.pipeline().addFirst(SSL_HANDLER, new SslSniHandler(sslHandler, sslContext));
        ch.pipeline().addAfter(SSL_HANDLER, SSL_EXCEPTION_HANDLER, new SslExceptionHandler(mqttServerDisconnector));
        ch.pipeline().addAfter(SSL_EXCEPTION_HANDLER, SSL_PARAMETER_HANDLER, channelDependencies.getSslParameterHandler());

        if (!Tls.ClientAuthMode.NONE.equals(tls.getClientAuthMode())) {
            ch.pipeline().addAfter(SSL_PARAMETER_HANDLER, SSL_CLIENT_CERTIFICATE_HANDLER, new SslClientCertificateHandler(tls, mqttServerDisconnector));
        }
    }

}
