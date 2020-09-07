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

import com.google.inject.Inject;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ChannelUtils;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.NotSslRecordException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

/**
 * This Exception handler is responsible for handling SSLExceptions and all other
 * SSL related exceptions.
 * <p>
 * SSLExceptions are fatal most of the time (for a client which wants to connect with SSL :-) ),
 * so we typically can only log.
 *
 * @author Christoph Schäbel
 * @author Dominik Obermaier
 */
public class SslExceptionHandler extends ChannelHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(SslExceptionHandler.class);
    private final @NotNull MqttServerDisconnector mqttServerDisconnector;

    @Inject
    public SslExceptionHandler(final @NotNull MqttServerDisconnector mqttServerDisconnector) {
        this.mqttServerDisconnector = mqttServerDisconnector;
    }

    @Override
    public void exceptionCaught(final @NotNull ChannelHandlerContext ctx, final @NotNull Throwable cause) {

        if (ignorableException(cause, ctx)) {
            return;
        }

        //SslHandshakeExceptions are wrapped so we check the cause instead
        if (cause.getCause() != null) {
            if (cause.getCause() instanceof SSLHandshakeException) {
                logSSLHandshakeException(ctx, cause);
                //Just in case the channel wasn't closed already
                mqttServerDisconnector.logAndClose(ctx.channel(),
                        null, //already logged
                        "SSL handshake failed");
                return;

            } else if (cause.getCause() instanceof SSLException) {
                logSSLException(ctx, cause);
                mqttServerDisconnector.logAndClose(ctx.channel(),
                        null, //already logged
                        "SSL message transmission failed");
                return;
            }
        }

        //Rethrow Exception, we can only handle SSL Exceptions
        ctx.fireExceptionCaught(cause);
    }


    private void logSSLException(final @NotNull ChannelHandlerContext ctx, final @NotNull Throwable cause) {
        if (log.isDebugEnabled()) {

            final Throwable rootCause = ExceptionUtils.getRootCause(cause);

            final String clientId = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();
            if (clientId != null) {
                log.debug("SSL message transmission for client {} failed: {}", clientId, rootCause.getMessage());
            } else {
                log.debug("SSL message transmission failed for client with IP {}: {}", ChannelUtils.getChannelIP(ctx.channel()).or("UNKNOWN"), rootCause.getMessage());
            }
            log.trace("Original Exception", rootCause);
        }
    }

    private void logSSLHandshakeException(final @NotNull ChannelHandlerContext ctx, final @NotNull Throwable cause) {
        if (log.isDebugEnabled()) {

            final Throwable rootCause = ExceptionUtils.getRootCause(cause);

            final String clientId = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();
            if (clientId != null) {
                log.debug("SSL Handshake for client {} failed: {}", clientId, rootCause.getMessage());
            } else {
                log.debug("SSL Handshake failed for client with IP {}: {}", ChannelUtils.getChannelIP(ctx.channel()).or("UNKNOWN"), rootCause.getMessage());
            }
            log.trace("Original Exception", rootCause);
        }
    }


    private boolean ignorableException(final @NotNull Throwable cause, final @NotNull ChannelHandlerContext ctx) {

        if (cause instanceof NotSslRecordException) {
            if (log.isDebugEnabled()) {
                log.debug("Client {} sent data which is not SSL/TLS to a SSL/TLS listener. Disconnecting client.", ChannelUtils.getChannelIP(ctx.channel()).or("UNKNOWN"));
                log.trace("Original Exception:", cause);
            }
            //Just in case the client wasn't disconnected already
            mqttServerDisconnector.logAndClose(ctx.channel(),
                    null, //already logged
                    "SSL handshake failed");
            return true;
        }
        return false;
    }
}
