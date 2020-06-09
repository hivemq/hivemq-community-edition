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
package com.hivemq.persistence;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hivemq.configuration.service.InternalConfigurations.PERSISTENCE_STARTUP_SHUTDOWN_TIMEOUT;
import static com.hivemq.configuration.service.InternalConfigurations.PERSISTENCE_STARTUP_THREAD_POOL_SIZE;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@Singleton
public class PersistenceStartup extends HiveMQShutdownHook {

    private static final Logger log = LoggerFactory.getLogger(PersistenceStartup.class);

    private static final int FILE_PERSISTENCE_COUNT = 5;

    private final @NotNull ExecutorService persistenceStartExecutor;
    private final @NotNull ExecutorService environmentCreateExecutor;

    private final @NotNull List<FilePersistence> filePersistenceList;

    private final long start;

    public PersistenceStartup() {
        persistenceStartExecutor = Executors.newFixedThreadPool(FILE_PERSISTENCE_COUNT);
        environmentCreateExecutor = Executors.newFixedThreadPool(PERSISTENCE_STARTUP_THREAD_POOL_SIZE.get());
        filePersistenceList = new ArrayList<>(FILE_PERSISTENCE_COUNT);
        start = System.currentTimeMillis();
    }

    public void submitEnvironmentCreate(@NotNull final Runnable createTask) {
        environmentCreateExecutor.submit(createTask);
    }

    public void submitPersistenceStart(@NotNull final FilePersistence filePersistence) {
        filePersistenceList.add(filePersistence);
        persistenceStartExecutor.submit(filePersistence::start);
    }

    public void finish() throws InterruptedException {

        log.trace("Waiting for persistence start execution");
        persistenceStartExecutor.shutdown();
        while (!persistenceStartExecutor.awaitTermination(20, TimeUnit.SECONDS)) {
            log.trace("Waiting for persistence start execution");
        }

        log.trace("Waiting for environment create execution");
        environmentCreateExecutor.shutdown();
        while (!environmentCreateExecutor.awaitTermination(20, TimeUnit.SECONDS)) {
            log.trace("Waiting for environment create execution");
        }

        log.trace("Initialized persistences in {}ms", System.currentTimeMillis() - start);
    }

    public @NotNull String name() {
        return "PersistenceStartupShutdownHook";
    }

    public @NotNull Priority priority() {
        return Priority.DOES_NOT_MATTER;
    }

    public boolean isAsynchronous() {
        return false;
    }

    public void run() {

        log.trace("Shutting down persistence startup executors");
        persistenceStartExecutor.shutdown();
        environmentCreateExecutor.shutdown();

        try {
            if (!persistenceStartExecutor.awaitTermination(PERSISTENCE_STARTUP_SHUTDOWN_TIMEOUT.get(), TimeUnit.SECONDS)) {
                persistenceStartExecutor.shutdownNow();
            }
            if (!environmentCreateExecutor.awaitTermination(PERSISTENCE_STARTUP_SHUTDOWN_TIMEOUT.get(), TimeUnit.SECONDS)) {
                environmentCreateExecutor.shutdownNow();
            }
        } catch (final InterruptedException e) {
            persistenceStartExecutor.shutdownNow();
            environmentCreateExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        try {
            log.debug("Closing file persistences");
            for (final FilePersistence filePersistence : filePersistenceList) {
                filePersistence.stop();
            }
        } catch (final Throwable e) {
            log.error("Closing file persistence failed", e);
        }


    }
}
