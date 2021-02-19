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
import com.hivemq.configuration.service.entity.TlsWebsocketListener;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.security.exception.SslException;
import com.hivemq.security.ssl.SslFactory;
import com.hivemq.websocket.WebSocketInitializer;
import io.netty.channel.Channel;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.SSL_CLIENT_CERTIFICATE_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.SSL_PARAMETER_HANDLER;

/**
 * @author Christoph Schäbel
 */
public class TlsWebsocketChannelInitializer extends AbstractTlsChannelInitializer {

    @NotNull
    private final TlsWebsocketListener tlsWebsocketListener;

    public TlsWebsocketChannelInitializer(@NotNull final ChannelDependencies channelDependencies,
                                          @NotNull final TlsWebsocketListener tlsWebsocketListener,
                                          @NotNull final SslFactory sslFactory) {

        super(channelDependencies, tlsWebsocketListener, sslFactory);
        this.tlsWebsocketListener = tlsWebsocketListener;
    }

    @Override
    protected void addSpecialHandlers(@NotNull final Channel ch) throws SslException {
        super.addSpecialHandlers(ch);

        final Tls.ClientAuthMode authMode = tlsWebsocketListener.getTls().getClientAuthMode();
        final String handlerName = !Tls.ClientAuthMode.NONE.equals(authMode) ? SSL_CLIENT_CERTIFICATE_HANDLER : SSL_PARAMETER_HANDLER;
        new WebSocketInitializer(tlsWebsocketListener).addHandlers(ch, handlerName);
    }
}
