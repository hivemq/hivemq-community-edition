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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.inject.Inject;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.security.exception.SslException;
import com.hivemq.security.ioc.Security;
import io.netty.handler.ssl.SslContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.hivemq.configuration.service.InternalConfigurations.SSL_RELOAD_ENABLED;
import static com.hivemq.configuration.service.InternalConfigurations.SSL_RELOAD_INTERVAL;

/**
 * @author Christoph Schäbel
 */
public class SslContextStore {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SslContextStore.class);

    private static final int BUF_LEN = 1024;
    final @NotNull Map<Tls, SslContext> sslContextMap;
    final @NotNull Map<Tls, HashCode> checksumMap;
    final @NotNull ScheduledExecutorService executorService;
    final @NotNull SslUtil sslUtil;

    @Inject
    public SslContextStore(
            @Security final @NotNull ScheduledExecutorService executorService, final @NotNull SslUtil sslUtil) {
        this.executorService = executorService;
        this.sslUtil = sslUtil;
        sslContextMap = new ConcurrentHashMap<>();
        checksumMap = new ConcurrentHashMap<>();
    }

    public boolean contains(final @NotNull Tls tls) {
        return sslContextMap.containsKey(tls);
    }

    public boolean contains(final @NotNull SslContext sslContext) {
        return sslContextMap.containsValue(sslContext);
    }

    public SslContext get(final @NotNull Tls tls) {
        return sslContextMap.get(tls);
    }

    public void put(final @NotNull Tls tls, final @NotNull SslContext sslContext) {
        sslContextMap.put(tls, sslContext);
        if (SSL_RELOAD_ENABLED) {
            executorService.schedule(new SslContextFirstTimeRunnable(tls,
                            sslContextMap,
                            checksumMap,
                            SSL_RELOAD_INTERVAL,
                            executorService,
                            sslUtil),
                    0,
                    TimeUnit.SECONDS);
        }
    }

    public void remove(final @NotNull Tls tls) {
        sslContextMap.remove(tls);
    }

    public void putAtStart(final @NotNull Tls tls, final @NotNull SslContext sslContext) {
        sslContextMap.put(tls, sslContext);
        if (SSL_RELOAD_ENABLED) {
            new SslContextFirstTimeRunnable(tls,
                    sslContextMap,
                    checksumMap,
                    SSL_RELOAD_INTERVAL,
                    executorService,
                    sslUtil).run();
        }
    }

    @VisibleForTesting
    static HashCode hashTrustAndKeyStore(final @NotNull Tls tls) throws IOException {
        final Hasher hasher = Hashing.md5().newHasher();
        final File keyStore = new File(tls.getKeystorePath());
        hashStore(keyStore, hasher);

        if (tls.getTruststorePath() != null && !StringUtils.isBlank(tls.getTruststorePath())) {
            final File trustStore = new File(tls.getTruststorePath());
            hashStore(trustStore, hasher);

        }
        return hasher.hash();
    }

    @VisibleForTesting
    static void hashStore(final @NotNull File keyStore, final @NotNull Hasher hasher) throws IOException {
        try (final FileInputStream fileInputStream = new FileInputStream(keyStore)) {
            final byte[] buffer = new byte[BUF_LEN];
            while (fileInputStream.read(buffer) != -1) {
                hasher.putBytes(buffer);
                Arrays.fill(buffer, (byte) 0);
            }
        }
    }

    @VisibleForTesting
    static class SslContextFirstTimeRunnable implements Runnable {

        private final @NotNull Tls tls;
        private final @NotNull Map<Tls, SslContext> sslContextMap;
        private final @NotNull Map<Tls, HashCode> checksumMap;
        private final int interval;
        private final @NotNull ScheduledExecutorService executorService;
        private final @NotNull SslUtil sslUtil;

        SslContextFirstTimeRunnable(
                final @NotNull Tls tls,
                final @NotNull Map<Tls, SslContext> sslContextMap,
                final @NotNull Map<Tls, HashCode> checksumMap,
                final int interval,
                final @NotNull ScheduledExecutorService executorService,
                final @NotNull SslUtil sslUtil) {
            this.tls = tls;
            this.sslContextMap = sslContextMap;
            this.checksumMap = checksumMap;
            this.interval = interval;
            this.executorService = executorService;
            this.sslUtil = sslUtil;
        }

        @Override
        public void run() {
            try {
                final HashCode hash = hashTrustAndKeyStore(tls);
                checksumMap.put(tls, hash);
            } catch (final IOException e) {
                log.error("Could not generate initial hash of KeyStore and TrustStore", e);
                throw new UnrecoverableException();
            }
            //only start scheduled execution if first hash went through
            executorService.schedule(new SslContextScheduledRunnable(tls,
                    sslContextMap,
                    checksumMap,
                    interval,
                    executorService,
                    sslUtil), interval, TimeUnit.SECONDS);
        }
    }

    @VisibleForTesting
    static class SslContextScheduledRunnable implements Runnable {

        private final @NotNull Tls tls;
        private final @NotNull Map<Tls, SslContext> sslContextMap;
        private final @NotNull Map<Tls, HashCode> checksumMap;
        private final int interval;
        private final @NotNull ScheduledExecutorService executorService;
        private final @NotNull SslUtil sslUtil;

        SslContextScheduledRunnable(
                final @NotNull Tls tls,
                final @NotNull Map<Tls, SslContext> sslContextMap,
                final @NotNull Map<Tls, HashCode> checksumMap,
                final int interval,
                final @NotNull ScheduledExecutorService executorService,
                final @NotNull SslUtil sslUtil) {
            this.tls = tls;
            this.sslContextMap = sslContextMap;
            this.checksumMap = checksumMap;
            this.interval = interval;
            this.executorService = executorService;
            this.sslUtil = sslUtil;
        }

        @Override
        public void run() {
            try {
                final HashCode hash = hashTrustAndKeyStore(tls);
                final HashCode oldHash = checksumMap.get(tls);
                if (!hash.equals(oldHash)) {
                    final KeyManagerFactory kmf = sslUtil.createKeyManagerFactory(tls.getKeystoreType(),
                            tls.getKeystorePath(),
                            tls.getKeystorePassword(),
                            tls.getPrivateKeyPassword());
                    final TrustManagerFactory tmf;
                    if (tls.getTruststorePath() != null && !StringUtils.isBlank(tls.getTruststorePath())) {
                        tmf = sslUtil.createTrustManagerFactory(tls.getTruststoreType(),
                                tls.getTruststorePath(),
                                tls.getTruststorePassword());
                    } else {
                        tmf = null;
                    }

                    final SslContext context =
                            sslUtil.createSslServerContext(kmf, tmf, tls.getCipherSuites(), tls.getProtocols());
                    sslContextMap.put(tls, context);
                    checksumMap.put(tls, hash);
                    log.info("Successfully updated changed SSL Context");
                }

            } catch (final FileNotFoundException e) {
                log.warn("Could not find keystore or truststore file", e);
            } catch (final SSLException | SslException e) {
                log.warn("Could not parse new SSL Context from changed keystore or truststore", e);
            } catch (final Exception e) {
                log.warn("Scheduled SSL Context check failed", e);
            } finally {
                executorService.schedule(new SslContextScheduledRunnable(tls,
                                sslContextMap,
                                checksumMap,
                                interval,
                                executorService,
                                sslUtil),
                        interval,
                        TimeUnit.SECONDS);
            }
        }
    }

}
