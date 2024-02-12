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
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.security.exception.SslException;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class SslContextFactory {
    private static final Logger log = LoggerFactory.getLogger(SslContextFactory.class);

    public static final int DNS_NAME_TYPE = 2;

    /**
     * Creates a new {@link SslContext} according to the information stored in the {@link Tls} object
     *
     * @param tls the Tls object with the information for the Tls connection
     * @return the {@link SslContext}
     * @throws SslException thrown if the SslContext could not be created
     */
    public @NotNull SslContext createSslContext(final @NotNull Tls tls) {
        try {
            final KeyManagerFactory keyManagerFactory = SslUtil.getKeyManagerFactory(tls);
            final X509KeyManager origKm = (X509KeyManager) keyManagerFactory.getKeyManagers()[0];

            final DnsResolver dnsResolver = new DnsResolver(createDnsHostnameMap(tls.getKeystorePath(), tls.getKeystorePassword()));

            final X509ExtendedKeyManager customKeyManager = new X509ExtendedKeyManager() {

                @Override
                public String[] getClientAliases(final String keyType, final Principal[] issuers) {
                    return origKm.getClientAliases(keyType, issuers);
                }

                @Override
                public String chooseClientAlias(
                        final String[] keyType, final Principal[] issuers, final Socket socket) {
                    return origKm.chooseClientAlias(keyType, issuers, socket);
                }

                @Override
                public String[] getServerAliases(final String keyType, final Principal[] issuers) {
                    return origKm.getServerAliases(keyType, issuers);
                }

                @Override
                public String chooseServerAlias(final String keyType, final Principal[] issuers, final Socket socket) {
                    return origKm.chooseServerAlias(keyType, issuers, socket);
                }

                @Override
                public X509Certificate[] getCertificateChain(final String alias) {
                    return origKm.getCertificateChain(alias);
                }

                @Override
                public PrivateKey getPrivateKey(final String alias) {
                    return origKm.getPrivateKey(alias);
                }

                @Override
                public String chooseEngineServerAlias(
                        final String keyType, final Principal[] issuers, final SSLEngine engine) {
                    final String hostname = engine.getPeerHost();
                    final String certificateAlias;
                    if (hostname == null) {
                        // Without SNI activated the hostname is null, so we use the default alias
                        certificateAlias = tls.getDefaultKeystoreAlias();
                        log.debug("No SNI hostname given, using default alias: {}", certificateAlias);
                    } else {
                        certificateAlias = dnsResolver.resolve(hostname);
                    }
                    log.trace("Choose engine server alias for host: {} found alias: {}", hostname,
                            certificateAlias);
                    return certificateAlias;
                }
            };

            final SslContextBuilder builder = SslContextBuilder.forServer(customKeyManager)
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

    private static Map<String, String> createDnsHostnameMap(
            final @NotNull String keystorePath, final @NotNull String keystorePassword) {
        final KeyStore keystore;
        final Enumeration<String> aliases;

        try (final FileInputStream fis = new FileInputStream(keystorePath)) {
            keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(fis, keystorePassword.toCharArray());

            aliases = keystore.aliases();
        } catch (final IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException e) {
            log.warn("Failed to open keystore for certificate processing, the message is: {}", e.getMessage());

            return Collections.emptyMap();
        }

        if (aliases == null || !aliases.hasMoreElements()) {
            return Collections.emptyMap();
        }

        final Map<String, String> dnsHostnameMap = new HashMap<>();

        for (final String alias : Collections.list(aliases)) {
            final Certificate[] certificateChain;

            try {
                certificateChain = keystore.getCertificateChain(alias);
            } catch (final KeyStoreException e) {
                log.warn("Failed to get certificate with alias {} from keystore, the message is: {}", alias, e.getMessage());
                continue;
            }

            for (final Certificate certificateChainItem : certificateChain) {
                if (!(certificateChainItem instanceof X509Certificate)) {
                    continue;
                }

                final X509Certificate x509Cert = (X509Certificate) certificateChainItem;

                Set<String> dnsHostnames = null;

                try {
                    dnsHostnames = getDnsHostnamesFromCertificate(x509Cert);
                } catch (final CertificateParsingException e) {
                    log.warn("Failed to parse certificate for alias: {}, the message is: {}", alias, e.getMessage());
                }

                if (dnsHostnames == null) {
                    continue;
                }

                dnsHostnames.forEach(dnsHostname -> dnsHostnameMap.put(dnsHostname, alias));
            }
        }

        log.info("Parsed hostNames: {}",
                dnsHostnameMap.isEmpty() ? "no hostnames available" : String.join(", ", dnsHostnameMap.values()));

        return dnsHostnameMap;
    }

    private static Set<String> getDnsHostnamesFromCertificate(final X509Certificate x509Cert)
            throws CertificateParsingException {
        final Collection<List<?>> altNames = x509Cert.getSubjectAlternativeNames();

        if (altNames == null) {
            return null;
        }

        return altNames.stream().map(altName -> {
            if (altName.size() < 2) {
                return null;
            }

            if ((Integer) altName.get(0) != DNS_NAME_TYPE) {
                return null;
            }

            final Object data = altName.get(1);
            if (data instanceof String) {
                return (String) data;
            }

            return null;
        }).filter(Objects::nonNull).collect(Collectors.toSet());
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
