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
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Files;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.diagnostic.data.DiagnosticData;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * @author Dominik Obermaier
 */
public class DiagnosticMode {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(DiagnosticMode.class);
    public static final String FILE_NAME_METRIC_LOG = "metric.log";
    public static final String FILE_NAME_TRACE_LOG = "tracelog.log";
    public static final String FILE_NAME_DIAGNOSTICS_FILE = "diagnostics.txt";
    public static final String FILE_NAME_DIAGNOSTICS_FOLDER = "diagnostics";
    public static final String FILE_NAME_MIGRATION_LOG = "migration.log";
    private final DiagnosticData diagnosticData;
    private final SystemInformation systemInformation;
    private final MetricRegistry metricRegistry;


    @Inject
    DiagnosticMode(final DiagnosticData diagnosticData,
                   final SystemInformation systemInformation,
                   final MetricRegistry metricRegistry) {
        this.diagnosticData = diagnosticData;
        this.systemInformation = systemInformation;
        this.metricRegistry = metricRegistry;
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
            final ConsoleReporter metricReporter = ConsoleReporter.forRegistry(metricRegistry)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .outputTo(logStream)
                    .build();
            metricReporter.start(1, TimeUnit.SECONDS);

        } catch (final IOException e) {
            log.error("Not able to create metric.log, for {}", e.getCause());
        }
    }

    private void copyMigrationLog(final Optional<File> diagnosticsFolder) {

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

    private void createDiagnosticsFile(final File diagnosticsFolder) {
        final File diagnosticsFile = new File(diagnosticsFolder, FILE_NAME_DIAGNOSTICS_FILE);

        try {
            log.info("Creating Diagnostics file: {}", diagnosticsFile.getAbsolutePath());
            diagnosticsFile.createNewFile();

            Files.write(diagnosticData.get(), diagnosticsFile, Charsets.UTF_8);

        } catch (final IOException e) {
            log.error("Could not create the diagnostics.txt file. Stopping Diagnostic Mode");
        }
    }

    private Optional<File> createDiagnosticsFolder() {
        final File hiveMQHomeFolder = systemInformation.getHiveMQHomeFolder();

        final File diagnosticsFolder = new File(hiveMQHomeFolder, FILE_NAME_DIAGNOSTICS_FOLDER);

        if (diagnosticsFolder.exists()) {
            try {
                log.warn("Diagnostics folder already exists, deleting old folder");
                FileUtils.forceDelete(diagnosticsFolder);
            } catch (final IOException e) {
                log.error("Could not delete diagnostics folder. Stopping Diagnostic Mode");
                return Optional.absent();
            }
        }

        try {
            log.info("Creating 'diagnostics' folder in HiveMQ home folder: {}", hiveMQHomeFolder.getAbsolutePath());
            FileUtils.forceMkdir(diagnosticsFolder);
        } catch (final IOException e) {
            log.error("Could not create diagnostics folder. Stopping Diagnostic Mode");
            return Optional.absent();
        }

        return Optional.of(diagnosticsFolder);

    }
}
