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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.hivemq.configuration.service.PersistenceConfigurationService.PersistenceMode;

/**
 * @author Dominik Obermaier
 * @author Florian Limpöck
 */
public class HiveMQServer {

    private static final Logger log = LoggerFactory.getLogger(HiveMQServer.class);

    private final @NotNull HivemqId hivemqId;
    private final @NotNull LifecycleModule lifecycleModule;
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
        this.systemInformation = systemInformation;
        this.metricRegistry = metricRegistry;
        this.configService = configService;
        this.migrate = migrate;
    }

    public static void main(final String @NotNull [] args) throws Exception {
        final HiveMQServer server = new HiveMQServer();
        try {
            server.start();
        } catch (final StartAbortedException ignored) {
            // This exception is ignored as we throw it ourselves when HiveMQ shutdown is initiated during the startup.
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
            throw new StartAbortedException();
        }

        if (migrate && configService.persistenceConfigurationService().getMode() != PersistenceMode.IN_MEMORY) {

            if (migrations.size() + valueMigrations.size() > 0) {
                if (migrations.size() > 0) {
                    log.info("Persistence types has been changed, migrating persistent data.");
                } else {
                    log.info("Persistence values has been changed, migrating persistent data.");
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
            throw new StartAbortedException();
        }

        final HiveMQInstance instance = injector.getInstance(HiveMQInstance.class);

        if (InternalConfigurations.GC_AFTER_STARTUP) {
            log.trace("Starting initial garbage collection after startup");
            final long start = System.currentTimeMillis();
            //Start garbage collection of objects we don't need anymore after starting up
            System.gc();
            log.trace("Finished initial garbage collection after startup in {}ms", System.currentTimeMillis() - start);
        }

        if (shutdownHooks.isShuttingDown()) {
            throw new StartAbortedException();
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
            throw new StartAbortedException();
        }

        final UsageStatistics usageStatistics = injector.getInstance(UsageStatistics.class);
        usageStatistics.start();
    }

    public void start() throws Exception {

        final long startTime = System.nanoTime();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stop();
        }, "shutdown-thread," + hivemqId.get()));

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
    }

    public @Nullable Injector getInjector() {
        return injector;
    }
}
