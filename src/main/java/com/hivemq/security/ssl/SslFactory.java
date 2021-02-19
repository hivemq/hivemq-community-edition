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
import javax.net.ssl.SSLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Christoph Schäbel
 */
public class SslFactory {

    private final @NotNull SslContextStore sslContextStore;
    private final @NotNull SslContextFactory sslContextFactory;


    private static final Logger log = LoggerFactory.getLogger(SslFactory.class);

    @Inject
    public SslFactory(@NotNull final SslContextStore sslContextStore,
                      @NotNull final SslContextFactory sslContextFactory) {
        this.sslContextStore = sslContextStore;
        this.sslContextFactory = sslContextFactory;
    }

    @NotNull
    public SslHandler getSslHandler(@NotNull final Channel ch, @NotNull final Tls tls, final @NotNull SslContext sslContext) throws SslException {

        final SslHandler sslHandler = new SslHandler(getSslEngine(ch, tls, sslContext));

        sslHandler.setHandshakeTimeoutMillis(tls.getHandshakeTimeout());

        return sslHandler;
    }

    @NotNull
    protected SSLEngine getSslEngine(@NotNull final Channel ch, @NotNull final Tls tls, @NotNull final SslContext sslContext) throws SslException {

        final SSLEngine sslEngine = sslContext.newEngine(ch.alloc());

        //set chosen protocols if available
        enableProtocols(sslEngine, tls.getProtocols());

        //it's a server so we do not use client mode
        sslEngine.setUseClientMode(false);

        //cert auth
        if (Tls.ClientAuthMode.REQUIRED.equals(tls.getClientAuthMode())) {
            sslEngine.setNeedClientAuth(true);
        }

        if (Tls.ClientAuthMode.OPTIONAL.equals(tls.getClientAuthMode())) {
            sslEngine.setWantClientAuth(true);
        }

        return sslEngine;
    }

    @NotNull
    public SslContext getSslContext(@NotNull final Tls tls) throws SslException {

        try {
            final SslContext sslContextFromCache = sslContextStore.get(tls);
            if (sslContextFromCache != null) {
                return sslContextFromCache;
            }

            final SslContext sslContext = sslContextFactory.createSslContext(tls);
            sslContextStore.put(tls, sslContext);
            return sslContext;

        } catch (final SSLException e) {
            throw new SslException("Not able to create SSL server context", e);
        }
    }

    public void verifySslAtBootstrap(@NotNull final Listener listener, @NotNull final Tls tls) {
        try {
            if (!sslContextStore.contains(tls)) {
                final SslContext sslContext = sslContextFactory.createSslContext(tls);
                sslContextStore.putAtStart(tls, sslContext);

                final SSLEngine sslEngine = sslContext.newEngine(new PooledByteBufAllocator());
                enableProtocols(sslEngine, tls.getProtocols());
                log.info("Enabled protocols for {} at address {} and port {}: {}", listener.readableName(), listener.getBindAddress(), listener.getPort(), Arrays.toString(sslEngine.getEnabledProtocols()));
                final String[] enabledCipherSuites = sslEngine.getEnabledCipherSuites();
                log.info("Enabled cipher suites for {} at address {} and port {}: {}", listener.readableName(), listener.getBindAddress(), listener.getPort(), Arrays.toString(enabledCipherSuites));

                final List<String> cipherSuites = tls.getCipherSuites();
                if (cipherSuites.size() > 0) {
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
                        unknownCipherSuitesSet = Sets.difference(ImmutableSet.copyOf(cipherSuites), ImmutableSet.copyOf(enabledCipherSuites));
                    }

                    if (unknownCipherSuitesSet.size() > 0) {
                        log.warn("Unknown cipher suites for {} at address {} and port {}: {}", listener.readableName(), listener.getBindAddress(), listener.getPort(), unknownCipherSuitesSet);
                    }
                }
            }
        } catch (final Exception e) {
            log.error("Not able to create SSL server context. Reason: {}", e.getMessage());
            log.debug("Original exception", e);
            throw new UnrecoverableException(false);
        }
    }


    private void enableProtocols(@NotNull final SSLEngine sslEngine, @NotNull final List<String> protocolList) {
        if (protocolList.size() > 0) {
            final String[] protocols = protocolList.toArray(new String[protocolList.size()]);
            sslEngine.setEnabledProtocols(protocols);
        }
    }


}
