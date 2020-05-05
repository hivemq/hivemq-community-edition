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

package com.hivemq.embedded.internal;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Injector;
import com.hivemq.HiveMQServer;
import com.hivemq.bootstrap.ioc.GuiceBootstrap;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.ConfigurationBootstrap;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.embedded.EmbeddedHiveMQ;
import com.hivemq.embedded.EmbeddedHiveMQBuilder;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.persistence.PersistenceStartup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * @author Georg Held
 */
public class EmbeddedHiveMQImpl implements EmbeddedHiveMQ {

    private static final @NotNull Logger log = LoggerFactory.getLogger(EmbeddedHiveMQImpl.class);

    private final @Nullable Path conf;
    private final @NotNull Path extensions;
    private final @NotNull Path data;
    private final @NotNull SystemInformationImpl systemInformation;
    private final @NotNull MetricRegistry metricRegistry;
    private @Nullable FullConfigurationService configurationService;
    private @Nullable Injector injector;

    private volatile @NotNull State currentState;
    private volatile @Nullable CompletableFuture<Void> startFuture;
    private volatile @Nullable CompletableFuture<Void> stopFuture;

    EmbeddedHiveMQImpl(final @Nullable Path conf, final @NotNull Path extensions, final @NotNull Path data) {

        this.conf = conf;
        this.extensions = extensions;
        this.data = data;

        currentState = State.NOT_STARTED;
        systemInformation = new SystemInformationImpl(true, true);
        // we create the metric registry here to make it accessible before start
        metricRegistry = new MetricRegistry();
    }

    private enum State {

        NOT_STARTED,
        STARTING,
        RUNNING,
        STOPPING,
        STOPPED;

        private boolean isHigher(final @NotNull State other) {
            return this.ordinal() > other.ordinal();
        }

        private boolean isLower(final @NotNull State other) {
            return this.ordinal() < other.ordinal();
        }
    }

    private synchronized void advanceState(final @NotNull State newState) {
        if ((currentState == State.STOPPED && newState == State.NOT_STARTED)
                || newState.isHigher(currentState)) {
            log.debug("Advancing EmbeddedHiveMQState from \"{}\" to \"{}\"", currentState, newState);
            currentState = newState;
        }
    }

    @Override
    public synchronized @NotNull CompletableFuture<Void> start() {
        advanceState(State.NOT_STARTED);
        if (currentState == State.NOT_STARTED) {
            advanceState(State.STARTING);
            log.info("Starting EmbeddedHiveMQ.");

            startFuture = new CompletableFuture<>();
            CompletableFuture.runAsync(() -> {

                configurationService = ConfigurationBootstrap.bootstrapConfig(systemInformation);

                bootstrapInjector();
                final HiveMQServer hiveMQServer = injector.getInstance(HiveMQServer.class);
                try {
                    hiveMQServer.start();
                } catch (final Exception e) {
                    throw new HiveMQServerException(e);
                }
                advanceState(State.RUNNING);
            }).whenComplete(HiveMQServerException.handler(startFuture));
        } else {
            log.warn("Could not start EmbeddedHiveMQ. Reason: \"Already started\"");
        }
        return startFuture;
    }

    private void bootstrapInjector() {
        if (injector == null) {
            final HivemqId hiveMQId = new HivemqId();
            final Injector persistenceInjector =
                    GuiceBootstrap.persistenceInjector(
                            systemInformation, metricRegistry, hiveMQId, configurationService);

            try {
                persistenceInjector.getInstance(PersistenceStartup.class).finish();
            } catch (final InterruptedException e) {
                log.error("EmbeddedHiveMQ persistence Startup interrupted.");
            }

            injector =
                    GuiceBootstrap.bootstrapInjector(systemInformation, metricRegistry, hiveMQId, configurationService,
                            persistenceInjector);
        }
    }

    @Override
    public synchronized @NotNull CompletableFuture<Void> stop() {
        if (currentState == State.NOT_STARTED) {
            log.warn("Could not stop EmbeddedHiveMQ. Reason: \"Not started\"");
            stopFuture = CompletableFuture.completedFuture(null);

        } else if (currentState.isLower(State.STOPPING)) {
            advanceState(State.STOPPING);
            log.info("Stopping EmbeddedHiveMQ.");

            stopFuture = startFuture.thenRunAsync(() -> {
                final ShutdownHooks shutdownHooks = injector.getInstance(ShutdownHooks.class);

                for (final HiveMQShutdownHook hiveMQShutdownHook : shutdownHooks.getRegistry().values()) {
                    // We call run, as we want to execute the hooks now, in this thread
                    //noinspection CallToThreadRun
                    hiveMQShutdownHook.run();
                }
                for (final HiveMQShutdownHook hiveMQShutdownHook : shutdownHooks.getAsyncShutdownHooks()) {
                    // We call run, as we want to execute the hooks now, in this thread
                    //noinspection CallToThreadRun
                    hiveMQShutdownHook.run();
                }

                shutdownHooks.clearRuntime();
                //TODO Why is this necessary again?
                metricRegistry.removeMatching(MetricFilter.ALL);
                injector = null;
                advanceState(State.STOPPED);
            });
        } else {
            log.warn("Could not stop EmbeddedHiveMQ. Reason: \"Already stopping\"");
        }
        return stopFuture;
    }

    @Override
    public @Nullable MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

//    @SuppressWarnings("ResultOfMethodCallIgnored")
//    private File createHomeFolder() {
//        try {
//            final File createdFolder = File.createTempFile("junit", "");
//            createdFolder.delete();
//            createdFolder.mkdir();
//            System.setProperty(SystemProperties.HIVEMQ_HOME, createdFolder.getAbsolutePath());
//            return createdFolder;
//        } catch (final IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    @SuppressWarnings("ResultOfMethodCallIgnored")
//    private void createConfigFile(final String configXML) {
//        try {
//            final File configFolder = new File(tempFolder, "conf");
//            configFolder.mkdir();
//            final File file = new File(configFolder, "config.xml");
//            file.createNewFile();
//            Files.write(configXML.getBytes(UTF_8), file);
//        } catch (final IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @SuppressWarnings("ResultOfMethodCallIgnored")
//    private void recursiveDelete(final File file) {
//        final File[] files = file.listFiles();
//        if (files != null) {
//            for (final File each : files) {
//                recursiveDelete(each);
//            }
//        }
//        file.delete();
//    }

    private static class HiveMQServerException extends RuntimeException {

        private HiveMQServerException(final Throwable cause) {
            super(cause);
        }

        private static <V, T extends Throwable> @NotNull BiConsumer<V, T> handler(
                final @NotNull CompletableFuture<V> result) {
            return (value, throwable) -> {
                if (throwable != null) {
                    if (throwable instanceof HiveMQServerException) {
                        result.completeExceptionally(throwable.getCause());
                        return;
                    }
                    result.completeExceptionally(throwable);
                }
                result.complete(value);
            };
        }
    }
}
