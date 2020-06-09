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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.diagnostic.data.DiagnosticData;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class DiagnosticModeTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Mock
    DiagnosticData diagnosticData;

    @Mock
    SystemInformation systemInformation;

    File hivemqHomeFolder;

    MetricRegistry metricRegistry;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        hivemqHomeFolder = temporaryFolder.newFolder();

        when(diagnosticData.get()).thenReturn("value");
        when(systemInformation.getLogFolder()).thenReturn(temporaryFolder.newFolder());
        when(systemInformation.getHiveMQHomeFolder()).thenReturn(hivemqHomeFolder);

        metricRegistry = new MetricRegistry();
        System.setProperty("hivemq.home", hivemqHomeFolder.getAbsolutePath());

    }

    @Test(timeout = 5000)
    public void test_metric_logging() throws Exception {
        new DiagnosticMode(diagnosticData, systemInformation, metricRegistry).init();
        final File diagnosticsFolder = new File(hivemqHomeFolder, DiagnosticMode.FILE_NAME_DIAGNOSTICS_FOLDER);
        assertTrue(diagnosticsFolder.isDirectory());

        final Meter meter = metricRegistry.meter("request");
        meter.mark();

        final File metricFile = new File(diagnosticsFolder, DiagnosticMode.FILE_NAME_METRIC_LOG);
        assertTrue(metricFile.exists());

        while (FileUtils.readFileToString(metricFile, Charset.defaultCharset()).isEmpty()) {
            Thread.sleep(10);
        }
    }


    @Test
    public void test_diagnostic_mode_creates_files() throws Exception {
        new DiagnosticMode(diagnosticData, systemInformation, metricRegistry).init();

        final File diagnosticsFolder = new File(hivemqHomeFolder, DiagnosticMode.FILE_NAME_DIAGNOSTICS_FOLDER);
        assertTrue(diagnosticsFolder.isDirectory());

        final File diagnosticsFile = new File(diagnosticsFolder, DiagnosticMode.FILE_NAME_DIAGNOSTICS_FILE);
        assertTrue(diagnosticsFile.exists());
        assertEquals("value", FileUtils.readFileToString(diagnosticsFile, StandardCharsets.UTF_8));


        final File tracelogFile = new File(diagnosticsFolder, DiagnosticMode.FILE_NAME_TRACE_LOG);
        assertTrue(tracelogFile.exists());
    }

    @Test
    public void test_diagnostic_file_exists_instead_of_folder() throws Exception {

        final File diagnosticsFolder = new File(hivemqHomeFolder, DiagnosticMode.FILE_NAME_DIAGNOSTICS_FOLDER);
        assertTrue(diagnosticsFolder.createNewFile());
        assertTrue(diagnosticsFolder.isFile());

        new DiagnosticMode(diagnosticData, systemInformation, metricRegistry).init();

        //We're making sure that the file was deleted and a folder is now there instead
        assertTrue(diagnosticsFolder.isDirectory());
    }

    @Test
    public void test_can_not_create_diagnostic_folder() throws Exception {
        hivemqHomeFolder.setWritable(false);

        new DiagnosticMode(diagnosticData, systemInformation, metricRegistry).init();

        //No Exception, this is good!
        assertTrue(hivemqHomeFolder.listFiles().length == 0);
    }

    @Test
    public void test_diagnostic_folder_not_deletable() throws Exception {

        final File diagnosticsFolder = new File(hivemqHomeFolder, DiagnosticMode.FILE_NAME_DIAGNOSTICS_FOLDER);
        assertTrue(diagnosticsFolder.createNewFile());
        hivemqHomeFolder.setWritable(false);

        new DiagnosticMode(diagnosticData, systemInformation, metricRegistry).init();

        //No Exception, this is good!
        assertNull(diagnosticsFolder.listFiles());
    }

    @Test
    public void test_migration_log_copied() throws Exception {

        final File logFolder = new File(hivemqHomeFolder, "log");
        logFolder.mkdirs();
        when(systemInformation.getLogFolder()).thenReturn(logFolder);

        final File migrationLogFile = new File(logFolder, DiagnosticMode.FILE_NAME_MIGRATION_LOG);
        assertTrue(migrationLogFile.createNewFile());

        new DiagnosticMode(diagnosticData, systemInformation, metricRegistry).init();

        final File diagnosticsFolder = new File(hivemqHomeFolder, DiagnosticMode.FILE_NAME_DIAGNOSTICS_FOLDER);
        final File file = new File(diagnosticsFolder, DiagnosticMode.FILE_NAME_MIGRATION_LOG);
        assertTrue(file.exists());

    }
}