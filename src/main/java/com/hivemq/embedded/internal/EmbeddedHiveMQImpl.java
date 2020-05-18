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
import com.google.common.annotations.VisibleForTesting;
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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.util.ThreadFactoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Georg Held
 */
class EmbeddedHiveMQImpl implements EmbeddedHiveMQ {

    private static final @NotNull Logger log = LoggerFactory.getLogger(EmbeddedHiveMQImpl.class);

    private final @NotNull SystemInformationImpl systemInformation;
    private final @NotNull MetricRegistry metricRegistry;
    @VisibleForTesting
    final @NotNull ExecutorService stateChangeExecutor;
    private @Nullable FullConfigurationService configurationService;
    private @Nullable Injector injector;

    private boolean closed = false;
    private @NotNull State currentState = State.STOPPED;
    private @NotNull State desiredState = State.STOPPED;
    private @Nullable Exception failedException;

    private @NotNull LinkedList<CompletableFuture<Void>> startFutures = new LinkedList<>();
    private @NotNull LinkedList<CompletableFuture<Void>> stopFutures = new LinkedList<>();

    EmbeddedHiveMQImpl(
            final @Nullable File conf, final @Nullable File data, final @Nullable File extensions) {

        systemInformation = new SystemInformationImpl(true, true, conf, data, extensions);
        // we create the metric registry here to make it accessible before start
        metricRegistry = new MetricRegistry();

        // Once the EmbeddedHiveMQ gets garbage collected this gets automatically shut down
        stateChangeExecutor =
                Executors.newSingleThreadExecutor(ThreadFactoryUtil.create("embedded-hivemq-state-change-executor"));
    }

    @Override
    public void close() throws ExecutionException, InterruptedException {
        final Future<?> shutDownFuture;
        synchronized (this) {
            closed = true;
            this.desiredState = State.STOPPED;
            stateChangeExecutor.submit(this::stateChange);
            shutDownFuture = stateChangeExecutor.submit(stateChangeExecutor::shutdown);
        }
        shutDownFuture.get();
    }

    private enum State {

        RUNNING,
        STOPPED,
        FAILED;
    }

    private void stateChange() {
        final List<CompletableFuture<Void>> localStartFutures;
        final List<CompletableFuture<Void>> localStopFutures;
        final State localDesiredState;

        synchronized (this) {
            localStartFutures = startFutures;
            localStopFutures = stopFutures;
            localDesiredState = desiredState;
            startFutures = new LinkedList<>();
            stopFutures = new LinkedList<>();
        }

        // ignoring failed state during close and desired state stopped
        if (currentState == State.FAILED && !(closed && desiredState == State.STOPPED)) {
            if (failedException != null) {
                failFutureLists(failedException, localStartFutures, localStopFutures);
                return;
            } else {
                log.error("Encountered a FAILED EmbeddedHiveMQ state without a reason present.");
                failFutureLists(
                        new IllegalStateException("FAILED EmbeddedHiveMQ state without a reason present"),
                        localStartFutures,
                        localStopFutures);
            }
        } else if (currentState == State.STOPPED) {
            if (localDesiredState == State.STOPPED) {
                failFutureList(new AbortedStateChangeException("EmbeddedHiveMQ was stopped"), localStartFutures);
                succeedFutureList(localStopFutures);
            } else if (localDesiredState == State.RUNNING) {
                log.info("Starting EmbeddedHiveMQ.");
                try {
                    configurationService = ConfigurationBootstrap.bootstrapConfig(systemInformation);

                    bootstrapInjector();
                    final HiveMQServer hiveMQServer = injector.getInstance(HiveMQServer.class);
                    hiveMQServer.start();

                    failFutureList(new AbortedStateChangeException("EmbeddedHiveMQ was started"), localStopFutures);
                    succeedFutureList(localStartFutures);
                    currentState = State.RUNNING;
                } catch (final Exception ex) {
                    currentState = State.FAILED;
                    failedException = ex;
                    failFutureLists(ex, localStartFutures, localStopFutures);
                }
            }
        } else if (currentState == State.RUNNING) {
            if (localDesiredState == State.RUNNING) {
                failFutureList(new AbortedStateChangeException("EmbeddedHiveMQ was started"), localStopFutures);
                succeedFutureList(localStartFutures);
            } else if (localDesiredState == State.STOPPED) {
                log.info("Stopping EmbeddedHiveMQ.");

                try {

                    final ShutdownHooks shutdownHooks = injector.getInstance(ShutdownHooks.class);

                    for (final HiveMQShutdownHook hiveMQShutdownHook : shutdownHooks.getRegistry().values()) {
                        try {
                            // We call run, as we want to execute the hooks now, in this thread
                            //noinspection CallToThreadRun
                            hiveMQShutdownHook.run();
                        } catch (final Exception ex) {
                            if (closed) {
                                // during close we try to shut down as much as possible
                                log.error("Exception during shutdown hook \"{}\".", hiveMQShutdownHook.name());
                            } else {
                                throw ex;
                            }
                        }
                    }

                    for (final HiveMQShutdownHook hiveMQShutdownHook : shutdownHooks.getAsyncShutdownHooks()) {
                        try {
                            // We call run, as we want to execute the hooks now, in this thread
                            //noinspection CallToThreadRun
                            hiveMQShutdownHook.run();
                        } catch (final Exception ex) {
                            if (closed) {
                                // during close we try to shut down as much as possible
                                log.error("Exception during shutdown hook \"{}\".", hiveMQShutdownHook.name());
                            } else {
                                throw ex;
                            }
                        }
                    }

                    shutdownHooks.clearRuntime();
                    metricRegistry.removeMatching(MetricFilter.ALL);
                    injector = null;
                    failFutureList(new AbortedStateChangeException("EmbeddedHiveMQ was stopped"), localStartFutures);
                    succeedFutureList(localStopFutures);
                    currentState = State.STOPPED;
                } catch (final Exception ex) {
                    currentState = State.FAILED;
                    failedException = ex;
                    failFutureLists(ex, localStartFutures, localStopFutures);
                }
            }
        }
    }

    private void failFutureLists(
            final @NotNull Exception exception,
            final @NotNull List<CompletableFuture<Void>> startFutures,
            final @NotNull List<CompletableFuture<Void>> stopFutures) {
        failFutureList(exception, startFutures);
        failFutureList(exception, stopFutures);
    }

    private void failFutureList(
            final @NotNull Exception exception, final @NotNull List<CompletableFuture<Void>> futures) {
        for (final CompletableFuture<Void> future : futures) {
            future.completeExceptionally(exception);
        }
    }

    private void succeedFutureList(final @NotNull List<CompletableFuture<Void>> futures) {
        for (final CompletableFuture<Void> future : futures) {
            future.complete(null);
        }
    }

    private void bootstrapInjector() {
        if (injector == null) {
            final HivemqId hiveMQId = new HivemqId();
            final Injector persistenceInjector = GuiceBootstrap.persistenceInjector(systemInformation,
                    metricRegistry,
                    hiveMQId,
                    configurationService);

            try {
                persistenceInjector.getInstance(PersistenceStartup.class).finish();
            } catch (final InterruptedException e) {
                log.error("EmbeddedHiveMQ persistence Startup interrupted.");
            }

            injector = GuiceBootstrap.bootstrapInjector(systemInformation,
                    metricRegistry,
                    hiveMQId,
                    configurationService,
                    persistenceInjector);
        }
    }

    @Override
    public @NotNull CompletableFuture<Void> start() {
        synchronized (this) {
            if (closed) {
                return CompletableFuture.failedFuture(new IllegalStateException("EmbeddedHiveMQ was already closed"));
            }
            desiredState = State.RUNNING;
            final CompletableFuture<Void> future = new CompletableFuture<>();
            startFutures.add(future);
            stateChangeExecutor.execute(this::stateChange);
            return future;
        }
    }

    @Override
    public @NotNull CompletableFuture<Void> stop() {
        synchronized (this) {
            if (closed) {
                return CompletableFuture.failedFuture(new IllegalStateException("EmbeddedHiveMQ was already closed"));
            }
            desiredState = State.STOPPED;
            final CompletableFuture<Void> future = new CompletableFuture<>();
            stopFutures.add(future);
            stateChangeExecutor.execute(this::stateChange);
            return future;
        }
    }

    @Override
    public @NotNull MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    @VisibleForTesting
    @Nullable Injector getInjector() {
        return injector;
    }

    private static class AbortedStateChangeException extends Exception {

        public AbortedStateChangeException(final @NotNull String message) {
            super(message);
        }
    }
}
