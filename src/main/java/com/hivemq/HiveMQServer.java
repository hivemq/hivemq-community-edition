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
package com.hivemq;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Injector;
import com.hivemq.bootstrap.HiveMQExceptionHandlerBootstrap;
import com.hivemq.bootstrap.LoggingBootstrap;
import com.hivemq.bootstrap.ioc.GuiceBootstrap;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.ConfigurationBootstrap;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.embedded.EmbeddedExtension;
import com.hivemq.exceptions.StartAbortedException;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.lifecycle.LifecycleModule;
import com.hivemq.metrics.MetricRegistryLogger;
import com.hivemq.migration.MigrationUnit;
import com.hivemq.migration.Migrations;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.statistics.UsageStatistics;
import com.hivemq.util.TemporaryFileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.hivemq.configuration.service.PersistenceConfigurationService.PersistenceMode;

public class HiveMQServer {

    private static final Logger log = LoggerFactory.getLogger(HiveMQServer.class);

    private final @NotNull HivemqId hivemqId;
    private final @NotNull LifecycleModule lifecycleModule;
    private final @NotNull DataLock dataLock;
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull MetricRegistry metricRegistry;
    private final boolean migrate;

    private @Nullable Injector injector;
    private @Nullable FullConfigurationService configService;

    public HiveMQServer() {
        this(
                new SystemInformationImpl(true),
                new MetricRegistry(),
                null,
                true
        );
    }

    public HiveMQServer(
            final @NotNull SystemInformation systemInformation,
            final @Nullable MetricRegistry metricRegistry,
            final @Nullable FullConfigurationService configService,
            final boolean migrate) {
        hivemqId = new HivemqId();
        lifecycleModule = new LifecycleModule();
        dataLock = new DataLock();
        this.systemInformation = systemInformation;
        this.metricRegistry = metricRegistry;
        this.configService = configService;
        this.migrate = migrate;
    }

    public static void main(final String @NotNull [] args) throws Exception {
        final HiveMQServer server = new HiveMQServer();
        try {
            server.start();
        } catch (final StartAbortedException e) {
            log.info("HiveMQ start was cancelled. {}", e.getMessage());
        }
    }

    public void bootstrap() throws Exception {
        // Already bootstrapped.
        if (injector != null) {
            return;
        }

        metricRegistry.addListener(new MetricRegistryLogger());

        LoggingBootstrap.prepareLogging();

        log.info("Starting HiveMQ Community Edition Server");

        // Embedded has already called init as it is required to read the config file.
        if (!systemInformation.isEmbedded()) {
            log.trace("Initializing HiveMQ home directory");
            //Create SystemInformation this early because logging depends on it
            systemInformation.init();
        }

        log.trace("Initializing Logging");
        LoggingBootstrap.initLogging(systemInformation.getConfigFolder());

        log.trace("Initializing Exception handlers");
        HiveMQExceptionHandlerBootstrap.addUnrecoverableExceptionHandler();

        if (configService == null) {
            log.trace("Initializing configuration");
            configService = ConfigurationBootstrap.bootstrapConfig(systemInformation);
        }

        if (configService.persistenceConfigurationService().getMode() == PersistenceMode.FILE) {
            log.trace("Locking data folder.");
            dataLock.lock(systemInformation.getDataFolder().toPath());
        }

        log.info("This HiveMQ ID is {}", hivemqId.get());

        //ungraceful shutdown does not delete tmp folders, so we clean them up on broker start
        log.trace("Cleaning up temporary folders");
        TemporaryFileUtils.deleteTmpFolder(systemInformation.getDataFolder());

        //must happen before persistence injector bootstrap as it creates the persistence folder.
        log.trace("Checking for migrations");
        final Map<MigrationUnit, PersistenceType> migrations = Migrations.checkForTypeMigration(systemInformation);
        final Set<MigrationUnit> valueMigrations = Migrations.checkForValueMigration(systemInformation);

        log.trace("Initializing persistences");
        final Injector persistenceInjector =
                GuiceBootstrap.persistenceInjector(systemInformation, metricRegistry, hivemqId, configService,
                        lifecycleModule);
        //blocks until all persistences started
        persistenceInjector.getInstance(PersistenceStartup.class).finish();

        if (persistenceInjector.getInstance(ShutdownHooks.class).isShuttingDown()) {
            throw new StartAbortedException("User aborted.");
        }

        if (migrate && configService.persistenceConfigurationService().getMode() != PersistenceMode.IN_MEMORY) {

            if (migrations.size() + valueMigrations.size() > 0) {
                if (migrations.isEmpty()) {
                    log.info("Persistence values has been changed, migrating persistent data.");
                } else {
                    log.info("Persistence types has been changed, migrating persistent data.");
                }
                for (final MigrationUnit migrationUnit : migrations.keySet()) {
                    log.debug("{} needs to be migrated.", StringUtils.capitalize(migrationUnit.toString()));
                }
                for (final MigrationUnit migrationUnit : valueMigrations) {
                    log.debug("{} needs to be migrated.", StringUtils.capitalize(migrationUnit.toString()));
                }
                Migrations.migrate(persistenceInjector, migrations, valueMigrations);
            }

            Migrations.afterMigration(systemInformation);
        }

        if (configService.persistenceConfigurationService().getMode().equals(PersistenceMode.FILE)) {
            log.info("Starting with file persistence mode.");
        } else {
            log.info("Starting with in-memory persistence mode.");
        }

        log.trace("Initializing Guice");
        injector = GuiceBootstrap.bootstrapInjector(systemInformation, metricRegistry, hivemqId,
                configService, persistenceInjector, lifecycleModule);
    }

    public void startInstance(final @Nullable EmbeddedExtension embeddedExtension) throws Exception {
        if (injector == null) {
            throw new UnrecoverableException(true);
        }
        final ShutdownHooks shutdownHooks = injector.getInstance(ShutdownHooks.class);
        if (shutdownHooks.isShuttingDown()) {
            throw new StartAbortedException("User aborted.");
        }

        final HiveMQInstance instance = injector.getInstance(HiveMQInstance.class);

        if (InternalConfigurations.GC_AFTER_STARTUP_ENABLED) {
            log.trace("Starting initial garbage collection after startup");
            final long start = System.currentTimeMillis();
            //Start garbage collection of objects we don't need anymore after starting up
            System.gc();
            log.trace("Finished initial garbage collection after startup in {}ms", System.currentTimeMillis() - start);
        }

        if (shutdownHooks.isShuttingDown()) {
            throw new StartAbortedException("User aborted.");
        }

        /* It's important that we are modifying the log levels after Guice is initialized,
        otherwise this somehow interferes with Singleton creation */
        LoggingBootstrap.addLoglevelModifiers();
        instance.start(embeddedExtension);
    }

    public void afterStart() {
        if (injector == null) {
            return;
        }
        final ShutdownHooks shutdownHooks = injector.getInstance(ShutdownHooks.class);
        if (shutdownHooks.isShuttingDown()) {
            throw new StartAbortedException("User aborted.");
        }

        final UsageStatistics usageStatistics = injector.getInstance(UsageStatistics.class);
        usageStatistics.start();
    }

    public void start() throws Exception {

        final long startTime = System.nanoTime();

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "shutdown-thread," + hivemqId.get()));

        bootstrap();
        startInstance(null);

        log.info("Started HiveMQ in {}ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));

        afterStart();
    }

    public void stop() {
        if (injector == null) {
            return;
        }
        final ShutdownHooks shutdownHooks = injector.getInstance(ShutdownHooks.class);
        // Already shutdown.
        if (shutdownHooks.isShuttingDown()) {
            return;
        }

        shutdownHooks.runShutdownHooks();

        if (configService.persistenceConfigurationService().getMode() == PersistenceMode.FILE) {
            dataLock.unlock();
        }
    }

    public @Nullable Injector getInjector() {
        return injector;
    }

    /**
     * Create a lock file in the data folder of HiveMQ and hold the lock until released in order to avoid a second
     * HiveMQ instance starting with the same data folder.
     */
    private static final class DataLock {

        private @Nullable FileChannel channel;
        private @Nullable FileLock fileLock;

        public void lock(final @NotNull Path dataPath) {
            final Path lockFile = dataPath.resolve("data.lock");
            try {
                channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            } catch (final Throwable e) {
                log.error("Could not open data lock file.", e);
                throw new StartAbortedException("An error occurred while opening the persistence. Is another HiveMQ instance running?");
            }
            try {
                fileLock = channel.tryLock();
            } catch (final Throwable ignored) {
            }
            if (fileLock == null) {
                throw new StartAbortedException("An error occurred while opening the persistence. Is another HiveMQ instance running?");
            }
        }

        public void unlock() {
            try {
                if (fileLock != null && fileLock.isValid()) {
                    fileLock.release();
                }
            } catch (final IOException e) {
                log.error("An error occurred while releasing lock of data folder.");
            }
            try {
                if (channel != null) {
                    channel.close();
                }
            } catch (final IOException e) {
                log.error("An error occurred while closing lock file in data folder.");
            }
        }
    }
}
