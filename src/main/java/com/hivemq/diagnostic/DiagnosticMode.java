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
package com.hivemq.diagnostic;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.diagnostic.data.DiagnosticData;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.util.ThreadFactoryUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class DiagnosticMode {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticMode.class);

    private static final String THREAD_NAME_FORMAT = "diagnostic-mode-%d";

    public static final String FILE_NAME_METRIC_LOG = "metric.log";
    public static final String FILE_NAME_TRACE_LOG = "tracelog.log";
    public static final String FILE_NAME_DIAGNOSTICS_FILE = "diagnostics.txt";
    public static final String FILE_NAME_DIAGNOSTICS_FOLDER = "diagnostics";
    public static final String FILE_NAME_MIGRATION_LOG = "migration.log";

    private final @NotNull DiagnosticData diagnosticData;
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull ShutdownHooks shutdownHooks;
    private final @NotNull ScheduledExecutorService executor;

    private @Nullable ConsoleReporter metricReporter;

    @Inject
    DiagnosticMode(final @NotNull DiagnosticData diagnosticData,
            final @NotNull SystemInformation systemInformation,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ShutdownHooks shutdownHooks) {
        this(diagnosticData, systemInformation, metricRegistry, shutdownHooks, Executors.newSingleThreadScheduledExecutor(ThreadFactoryUtil.create(THREAD_NAME_FORMAT)));
    }

    /**
     * @param executor This instance adopts ownership of the passed executor.
     *                 Used only for testing purposes, as the otherwise internally created executor of ConsoleReporter
     *                 can't be accessed.
     */
    @VisibleForTesting
    DiagnosticMode(final @NotNull DiagnosticData diagnosticData,
            final @NotNull SystemInformation systemInformation,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull ScheduledExecutorService executor) {
        this.diagnosticData = diagnosticData;
        this.systemInformation = systemInformation;
        this.metricRegistry = metricRegistry;
        this.shutdownHooks = shutdownHooks;
        this.executor = executor;
    }

    @PostConstruct
    public void init() {

        final Optional<File> diagnosticsFolder = createDiagnosticsFolder();

        if (diagnosticsFolder.isPresent()) {
            createDiagnosticsFile(diagnosticsFolder.get());

            DiagnosticLogging.setTraceLog(new File(diagnosticsFolder.get(), FILE_NAME_TRACE_LOG).getAbsolutePath());

            copyMigrationLog(diagnosticsFolder);
            startLoggingMetrics(diagnosticsFolder.get());
        }
    }

    private void startLoggingMetrics(final File diagnosticFolder) {
        final File metricLog = new File(diagnosticFolder, FILE_NAME_METRIC_LOG);

        try {
            final PrintStream logStream = new PrintStream(metricLog, Charset.defaultCharset().name());
            metricReporter = ConsoleReporter.forRegistry(metricRegistry)
                    .scheduleOn(executor)
                    // Shut this executor down on stop. We can configure this here because we own it.
                    .shutdownExecutorOnStop(true)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .outputTo(logStream)
                    .build();
            metricReporter.start(1, TimeUnit.SECONDS);

            shutdownHooks.add(new HiveMQShutdownHook() {
                @Override
                public @NotNull String name() {
                    return "HiveMQ Diagnostic Mode Shutdown";
                }

                @Override
                public @NotNull Priority priority() {
                    // DiagnosticMode must not access other JNI components that might be shut down before it by trying to
                    // retrieve current metrics from them. These components may dispose their internal state outside the JVM
                    // which might lead to SIGSEGV upon access.
                    // Since DiagnosticMode itself isn't expected to be accessed by other components, shut it down ASAP for
                    // simplicity.
                    // No interference is expected with other shutdown hooks of the same priority.
                    return Priority.FIRST;
                }

                @Override
                public void run() {
                    metricReporter.stop();
                }
            });

        } catch (final IOException e) {
            log.error("Not able to create metric.log, for {}", e.getCause());
        }
    }

    private void copyMigrationLog(final @NotNull Optional<File> diagnosticsFolder) {

        //copy migration log if available
        final File migrationLog = new File(systemInformation.getLogFolder(), FILE_NAME_MIGRATION_LOG);

        if (migrationLog.exists()) {
            try {
                FileUtils.copyFileToDirectory(migrationLog, diagnosticsFolder.get());
            } catch (final IOException e) {
                log.error("Not able to copy migration log to diagnostics folder", e);
            }
        }
    }

    private void createDiagnosticsFile(final @NotNull File diagnosticsFolder) {
        final File diagnosticsFile = new File(diagnosticsFolder, FILE_NAME_DIAGNOSTICS_FILE);

        try {
            log.info("Creating Diagnostics file: {}", diagnosticsFile.getAbsolutePath());
            diagnosticsFile.createNewFile();

            Files.write(diagnosticData.get(), diagnosticsFile, Charsets.UTF_8);

        } catch (final IOException e) {
            log.error("Could not create the diagnostics.txt file. Stopping Diagnostic Mode");
        }
    }

    private @NotNull Optional<File> createDiagnosticsFolder() {
        final File hiveMQHomeFolder = systemInformation.getHiveMQHomeFolder();

        final File diagnosticsFolder = new File(hiveMQHomeFolder, FILE_NAME_DIAGNOSTICS_FOLDER);

        if (diagnosticsFolder.exists()) {
            try {
                log.warn("Diagnostics folder already exists, deleting old folder");
                FileUtils.forceDelete(diagnosticsFolder);
            } catch (final IOException e) {
                log.error("Could not delete diagnostics folder. Stopping Diagnostic Mode");
                return Optional.empty();
            }
        }

        try {
            log.info("Creating 'diagnostics' folder in HiveMQ home folder: {}", hiveMQHomeFolder.getAbsolutePath());
            FileUtils.forceMkdir(diagnosticsFolder);
        } catch (final IOException e) {
            log.error("Could not create diagnostics folder. Stopping Diagnostic Mode");
            return Optional.empty();
        }

        return Optional.of(diagnosticsFolder);
    }
}
