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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.security.exception.SslException;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.handler.ssl.OpenSslServerContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@LazySingleton
public class SslFactory {
    private static final @NotNull Logger log = LoggerFactory.getLogger(SslFactory.class);

    private final @NotNull SslContextStore sslContextStore;

    @Inject
    public SslFactory(final @NotNull SslContextStore sslContextStore) {
        this.sslContextStore = sslContextStore;
    }

    public @NotNull SslHandler getSslHandler(
            final @NotNull Channel ch, final @NotNull Tls tls, final @NotNull SslContext sslContext)
            throws SslException {
        final SSLEngine sslEngine = sslContext.newEngine(ch.alloc());
        return getSslHandler(sslEngine, tls);
    }

    public @NotNull SslHandler getSslHandler(
            final @NotNull Channel ch, final @NotNull Tls tls, final @NotNull SslContext sslContext, final String hostname, final int port)
            throws SslException {
        final SSLEngine sslEngine = sslContext.newEngine(ch.alloc(), hostname, port);
        return getSslHandler(sslEngine, tls);
    }

    public @NotNull SslHandler getSslHandler(
            final @NotNull SSLEngine sslEngine, final @NotNull Tls tls)
            throws SslException {
        // if prefer server suites is null -> use default of the engine
        final Boolean preferServerCipherSuites = tls.isPreferServerCipherSuites();
        if (preferServerCipherSuites != null) {
            final SSLParameters params = sslEngine.getSSLParameters();
            params.setUseCipherSuitesOrder(preferServerCipherSuites);
            sslEngine.setSSLParameters(params);
        }

        final SslHandler sslHandler = new SslHandler(sslEngine);
        sslHandler.setHandshakeTimeoutMillis(tls.getHandshakeTimeout());
        return sslHandler;
    }

    public @NotNull SslContext getSslContext(final @NotNull Tls tls) throws SslException {
        return sslContextStore.getAndInitAsync(tls);
    }

    public void verifySslAtBootstrap(final @NotNull Listener listener, final @NotNull Tls tls) {
        try {
            sslContextStore.createAndInitIfAbsent(tls, sslContext -> {
                final SSLEngine sslEngine = sslContext.newEngine(new PooledByteBufAllocator());
                log.info("Enabled protocols for {} at address {} and port {}: {}",
                        listener.readableName(),
                        listener.getBindAddress(),
                        listener.getPort(),
                        Arrays.toString(sslEngine.getEnabledProtocols()));
                final String[] enabledCipherSuites = sslEngine.getEnabledCipherSuites();
                log.info("Enabled cipher suites for {} at address {} and port {}: {}",
                        listener.readableName(),
                        listener.getBindAddress(),
                        listener.getPort(),
                        Arrays.toString(enabledCipherSuites));

                final List<String> cipherSuites = tls.getCipherSuites();
                if (!cipherSuites.isEmpty()) {
                    final Set<String> unknownCipherSuitesSet;

                    if (sslContext instanceof OpenSslServerContext) {
                        // the prefixes TLS_ and SSL_ are ignored by OpenSSL
                        final Set<String> enabledCipherSuitesSet = new HashSet<>();
                        for (final String enabledCipherSuite : enabledCipherSuites) {
                            enabledCipherSuitesSet.add(enabledCipherSuite.substring(4));
                        }
                        unknownCipherSuitesSet = new HashSet<>();
                        for (final String cipherSuite : cipherSuites) {

                            if (cipherSuite == null) {
                                continue;
                            }

                            if (!enabledCipherSuitesSet.contains(cipherSuite.substring(4))) {
                                unknownCipherSuitesSet.add(cipherSuite);
                            }
                        }
                    } else {
                        unknownCipherSuitesSet = Sets.difference(ImmutableSet.copyOf(cipherSuites),
                                ImmutableSet.copyOf(enabledCipherSuites));
                    }

                    if (!unknownCipherSuitesSet.isEmpty()) {
                        log.warn("Unknown cipher suites for {} at address {} and port {}: {}",
                                listener.readableName(),
                                listener.getBindAddress(),
                                listener.getPort(),
                                unknownCipherSuitesSet);
                    }
                }
            });
        } catch (final Exception e) {
            log.error("Not able to create SSL server context. Reason: {}", e.getMessage());
            log.debug("Original exception", e);
            throw new UnrecoverableException(false);
        }
    }
}
