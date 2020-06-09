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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.entity.Tls;
import io.netty.handler.ssl.SslContext;

import javax.inject.Inject;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

public class SslContextFactoryImpl implements SslContextFactory {


    @NotNull
    private final SslUtil sslUtil;

    @Inject
    public SslContextFactoryImpl(@NotNull final SslUtil sslUtil) {
        this.sslUtil = sslUtil;
    }

    @NotNull
    public SslContext createSslContext(@NotNull final Tls tls) throws SSLException {
        final KeyManagerFactory kmf = sslUtil.getKeyManagerFactory(tls);
        final TrustManagerFactory tmFactory = sslUtil.getTrustManagerFactory(tls);
        return sslUtil.createSslServerContext(kmf, tmFactory, tls.getCipherSuites(), tls.getProtocols());
    }


}
