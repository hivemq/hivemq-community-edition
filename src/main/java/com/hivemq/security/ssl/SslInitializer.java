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

package com.hivemq.security.ssl;

import com.hivemq.bootstrap.netty.initializer.AbstractChannelInitializer;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.logging.EventLog;
import io.netty.channel.Channel;
import io.netty.handler.ssl.SslHandler;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.*;

/**
 * @author Christoph Schäbel
 */
public class SslInitializer {

    private final SslHandler sslHandler;
    private final Tls tls;
    private final EventLog eventLog;
    private final SslParameterHandler sslParameterHandler;

    public SslInitializer(final SslHandler sslHandler, final Tls tls, final EventLog eventLog, final SslParameterHandler sslParameterHandler) {
        this.sslHandler = sslHandler;
        this.tls = tls;
        this.eventLog = eventLog;
        this.sslParameterHandler = sslParameterHandler;
    }

    public void addHandlers(final Channel ch) {

        ch.pipeline().addBefore(AbstractChannelInitializer.FIRST_ABSTRACT_HANDLER, SSL_HANDLER, sslHandler);
        ch.pipeline().addAfter(SSL_HANDLER, SSL_EXCEPTION_HANDLER, new SslExceptionHandler(eventLog));
        ch.pipeline().addAfter(SSL_EXCEPTION_HANDLER, SSL_PARAMETER_HANDLER, sslParameterHandler);

        if (!Tls.ClientAuthMode.NONE.equals(tls.getClientAuthMode())) {
            ch.pipeline().addAfter(SSL_PARAMETER_HANDLER, SSL_CLIENT_CERTIFICATE_HANDLER, new SslClientCertificateHandler(tls, eventLog));
        }

    }


}
