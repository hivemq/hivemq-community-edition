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

import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.security.exception.SslException;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;

import javax.net.ssl.SSLException;
import java.util.List;

@LazySingleton
public class SslContextFactory {

    /**
     * Creates a new {@link SslContext} according to the information stored in the {@link Tls} object
     *
     * @param tls the Tls object with the information for the Tls connection
     * @return the {@link SslContext}
     * @throws SslException thrown if the SslContext could not be created
     */
    public @NotNull SslContext createSslContext(final @NotNull Tls tls) {
        try {
            final SslContextBuilder builder = SslContextBuilder.forServer(SslUtil.getKeyManagerFactory(tls))
                    .sslProvider(SslProvider.JDK)
                    .trustManager(SslUtil.getTrustManagerFactory(tls))
                    .clientAuth(toClientAuth(tls.getClientAuthMode()));

            if (!tls.getProtocols().isEmpty()) {
                builder.protocols(tls.getProtocols());
            }

            //set chosen cipher suites if available or the default one
            final List<String> cipherSuites = tls.getCipherSuites();
            if (cipherSuites != null && !cipherSuites.isEmpty()) {
                builder.ciphers(cipherSuites, SupportedCipherSuiteFilter.INSTANCE);
            } else {
                builder.ciphers(null, SupportedCipherSuiteFilter.INSTANCE);
            }

            return builder.build();
        } catch (final SSLException e) {
            throw new SslException("Not able to create SSL server context", e);
        }
    }

    private static @NotNull ClientAuth toClientAuth(final @NotNull Tls.ClientAuthMode clientAuthMode) {
        switch (clientAuthMode) {
            case NONE:
                return ClientAuth.NONE;
            case OPTIONAL:
                return ClientAuth.OPTIONAL;
            case REQUIRED:
                return ClientAuth.REQUIRE;
        }

        throw new SslException("Invalid auth mode: " + clientAuthMode);
    }
}
