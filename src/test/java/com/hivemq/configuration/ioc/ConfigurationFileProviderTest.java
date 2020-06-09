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
package com.hivemq.configuration.ioc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.hivemq.configuration.SystemProperties;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.reader.ConfigurationFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ClearSystemProperties;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class ConfigurationFileProviderTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public final ClearSystemProperties myPropertyIsCleared = new ClearSystemProperties(SystemProperties.HIVEMQ_HOME);

    @Mock
    private Appender mockAppender;

    @Mock
    private SystemInformation systemInformation;

    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;


    private File confFolder;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.addAppender(mockAppender);

        final File hivemqHomefolder = folder.newFolder();
        confFolder = new File(hivemqHomefolder, "conf");
        assertTrue(confFolder.mkdir());

        when(systemInformation.getHiveMQHomeFolder()).thenReturn(hivemqHomefolder);
        when(systemInformation.getConfigFolder()).thenReturn(confFolder);
    }

    @After
    public void tearDown() throws Exception {

        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        logger.detachAppender(mockAppender);
    }

    @Test
    public void test_conf_file_ok() throws Exception {

        final File config = new File(confFolder, "config.xml");
        assertTrue(config.createNewFile());

        final ConfigurationFile configurationFile = ConfigurationFileProvider.get(systemInformation);

        assertEquals(true, configurationFile.file().isPresent());

        //No warning / error is logged
        verify(mockAppender, never()).doAppend(captorLoggingEvent.capture());
    }

    @Test
    public void test_conf_folder_does_not_exist() throws Exception {

        assertTrue(confFolder.delete());
        final ConfigurationFile configurationFile = ConfigurationFileProvider.get(systemInformation);

        assertEquals(false, configurationFile.file().isPresent());

        verifyLogStatementContains("does not exist");
    }

    @Test
    public void test_conf_folder_is_a_file() throws Exception {

        assertTrue(confFolder.delete());

        assertTrue(confFolder.createNewFile());

        final ConfigurationFile configurationFile = ConfigurationFileProvider.get(systemInformation);

        assertEquals(false, configurationFile.file().isPresent());

        verifyLogStatementContains("is not a folder");
    }

    @Test
    public void test_conf_folder_is_not_readable() throws Exception {

        assertTrue(confFolder.setReadable(false));

        final ConfigurationFile configurationFile = ConfigurationFileProvider.get(systemInformation);

        assertEquals(false, configurationFile.file().isPresent());

        verifyLogStatementContains("cannot be read by HiveMQ");
    }

    @Test
    public void test_conf_file_does_not_exist() throws Exception {

        final ConfigurationFile configurationFile = ConfigurationFileProvider.get(systemInformation);

        assertEquals(false, configurationFile.file().isPresent());

        verifyLogStatementContains("config.xml does not exist");
    }

    @Test
    public void test_conf_file_is_a_folder() throws Exception {

        final File config = new File(confFolder, "config.xml");
        assertTrue(config.mkdir());

        final ConfigurationFile configurationFile = ConfigurationFileProvider.get(systemInformation);

        assertEquals(false, configurationFile.file().isPresent());

        verifyLogStatementContains("config.xml is not file");
    }

    @Test
    public void test_conf_file_is_not_readable() throws Exception {

        final File config = new File(confFolder, "config.xml");
        assertTrue(config.createNewFile());
        assertTrue(config.setReadable(false));

        final ConfigurationFile configurationFile = ConfigurationFileProvider.get(systemInformation);

        assertEquals(false, configurationFile.file().isPresent());

        verifyLogStatementContains("config.xml cannot be read by HiveMQ");
    }

    @Test
    public void test_conf_file_not_writable() throws Exception {

        final File config = new File(confFolder, "config.xml");
        assertTrue(config.createNewFile());
        assertTrue(config.setWritable(false));

        final ConfigurationFile configurationFile = ConfigurationFileProvider.get(systemInformation);
        //It's just a warning when the file is not writable
        assertEquals(true, configurationFile.file().isPresent());

        verifyLogStatementContains("config.xml is read only and cannot be written by HiveMQ");
        assertEquals(Level.WARN, captorLoggingEvent.getValue().getLevel());
    }

    private void verifyLogStatementContains(final String containsString) {
        verify(mockAppender).doAppend(captorLoggingEvent.capture());
        assertTrue(captorLoggingEvent.getValue().getFormattedMessage().contains(containsString));
    }
}