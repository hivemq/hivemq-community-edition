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

import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import io.netty.channel.Channel;
import io.netty.handler.ssl.SslHandler;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.*;

/**
 * @author Christoph Schäbel
 */
public class SslInitializer {

    private final @NotNull SslHandler sslHandler;
    private final @NotNull Tls tls;
    private final @NotNull MqttServerDisconnector mqttServerDisconnector;
    private final @NotNull SslParameterHandler sslParameterHandler;

    public SslInitializer(final @NotNull SslHandler sslHandler,
                          final @NotNull Tls tls,
                          final @NotNull MqttServerDisconnector mqttServerDisconnector,
                          final @NotNull SslParameterHandler sslParameterHandler) {
        this.sslHandler = sslHandler;
        this.tls = tls;
        this.mqttServerDisconnector = mqttServerDisconnector;
        this.sslParameterHandler = sslParameterHandler;
    }

    public void addHandlers(final Channel ch) {

        ch.pipeline().addFirst(SSL_HANDLER, sslHandler);
        ch.pipeline().addAfter(SSL_HANDLER, SSL_EXCEPTION_HANDLER, new SslExceptionHandler(mqttServerDisconnector));
        ch.pipeline().addAfter(SSL_EXCEPTION_HANDLER, SSL_PARAMETER_HANDLER, sslParameterHandler);

        if (!Tls.ClientAuthMode.NONE.equals(tls.getClientAuthMode())) {
            ch.pipeline().addAfter(SSL_PARAMETER_HANDLER, SSL_CLIENT_CERTIFICATE_HANDLER, new SslClientCertificateHandler(tls, mqttServerDisconnector));
        }

    }


}
