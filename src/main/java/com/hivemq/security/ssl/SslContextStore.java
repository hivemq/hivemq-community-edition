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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.PrimitiveSink;
import com.google.inject.Inject;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.security.exception.SslException;
import com.hivemq.security.ioc.Security;
import io.netty.handler.ssl.SslContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.hivemq.configuration.service.InternalConfigurations.SSL_RELOAD_ENABLED;
import static com.hivemq.configuration.service.InternalConfigurations.SSL_RELOAD_INTERVAL_SEC;

@LazySingleton
public class SslContextStore {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SslContextStore.class);

    private final @NotNull ScheduledExecutorService executorService;
    private final @NotNull SslContextFactory sslContextFactory;
    private final @NotNull ConcurrentMap<Tls, SslContext> sslContextMap;
    private final @NotNull ConcurrentMap<Tls, HashCode> checksumMap;

    @Inject
    public SslContextStore(
            final @Security @NotNull ScheduledExecutorService executorService,
            final @NotNull SslContextFactory sslContextFactory) {
        this.executorService = executorService;
        this.sslContextFactory = sslContextFactory;
        this.sslContextMap = new ConcurrentHashMap<>();
        this.checksumMap = new ConcurrentHashMap<>();
    }

    public @NotNull SslContext getAndInitAsync(final @NotNull Tls tls) {
        return getAndInit(tls, executorService, sslContext -> {});
    }

    public void createAndInitIfAbsent(final @NotNull Tls tls, final @NotNull Consumer<SslContext> onCreate) {
        getAndInit(tls, Runnable::run, onCreate);
    }

    private @NotNull SslContext getAndInit(
            final @NotNull Tls tls,
            final @NotNull Executor initExecutor,
            final @NotNull Consumer<SslContext> onCreate) {
        return sslContextMap.computeIfAbsent(tls, key -> {
            final SslContext sslContext = sslContextFactory.createSslContext(key);
            initExecutor.execute(new SslContextFirstTimeRunnable(key));
            onCreate.accept(sslContext);
            return sslContext;
        });
    }

    @VisibleForTesting
    static @NotNull HashCode hashKeystoreAndTruststore(final @NotNull Tls tls) throws IOException {
        try {
            //noinspection UnstableApiUsage,deprecation
            return Hashing.md5().hashObject(tls, KeystoreAndTruststoreHashFunnel.INSTANCE);
        } catch (final UncheckedIOException e) {
            throw e.getCause();
        }
    }

    @VisibleForTesting
    final class SslContextFirstTimeRunnable implements Runnable {

        private final @NotNull Tls tls;

        private SslContextFirstTimeRunnable(final @NotNull Tls tls) {
            this.tls = tls;
        }

        @Override
        public void run() {
            if (SSL_RELOAD_ENABLED) {
                try {
                    final HashCode hash = hashKeystoreAndTruststore(tls);
                    checksumMap.put(tls, hash);

                } catch (final Exception e) {
                    log.error("Could not generate initial hash of KeyStore and TrustStore", e);
                    throw new UnrecoverableException();
                }
                //only start scheduled execution if first hash went through
                executorService.scheduleAtFixedRate(new SslContextScheduledRunnable(tls),
                        SSL_RELOAD_INTERVAL_SEC,
                        SSL_RELOAD_INTERVAL_SEC,
                        TimeUnit.SECONDS);
            }
        }
    }

    @VisibleForTesting
    final class SslContextScheduledRunnable implements Runnable {

        private final @NotNull Tls tls;

        private SslContextScheduledRunnable(final @NotNull Tls tls) {
            this.tls = tls;
        }

        @Override
        public void run() {
            try {
                final HashCode hash = hashKeystoreAndTruststore(tls);
                final HashCode oldHash = checksumMap.get(tls);
                if (!hash.equals(oldHash)) {
                    final SslContext context = sslContextFactory.createSslContext(tls);
                    sslContextMap.put(tls, context);
                    checksumMap.put(tls, hash);
                    log.info("Successfully updated changed SSL Context");
                }

            } catch (final FileNotFoundException e) {
                log.warn("Could not find keystore or truststore file", e);
            } catch (final SslException e) {
                log.warn("Could not parse new SSL Context from changed keystore or truststore", e);
            } catch (final Exception e) {
                log.warn("Scheduled SSL Context check failed", e);
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private enum KeystoreAndTruststoreHashFunnel implements Funnel<Tls> {
        INSTANCE;

        @Override
        public void funnel(final @NotNull Tls tls, final @NotNull PrimitiveSink sink) {
            funnelFile(tls.getKeystorePath(), sink);

            if (StringUtils.isNotBlank(tls.getTruststorePath())) {
                funnelFile(tls.getTruststorePath(), sink);
            }
        }

        private static void funnelFile(final @NotNull String fileName, final @NotNull PrimitiveSink sink) {
            try (final FileInputStream fileInputStream = new FileInputStream(fileName)) {
                fileInputStream.transferTo(Funnels.asOutputStream(sink));
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

}
