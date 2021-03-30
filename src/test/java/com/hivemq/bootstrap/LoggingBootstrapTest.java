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
package com.hivemq.bootstrap;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;
import util.LogbackCapturingAppender;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * @author Dominik Obermaier
 */
public class LoggingBootstrapTest {


    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private Level level;


    @Before
    public void setUp() throws Exception {
        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        level = logger.getLevel();

        logger.setLevel(Level.INFO);

    }

    @After
    public void tearDown() throws Exception {
        LogbackCapturingAppender.Factory.cleanUp();
        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        logger.setLevel(level);
        resetLogToOriginal();
    }


    @Test
    public void test_override_standard_logback() throws Exception {

        final String overridenContents = "" +
                "<configuration>\n" +
                "\n" +
                "    <appender name=\"STDOUT\" class=\"ch.qos.logback.core.ConsoleAppender\">\n" +
                "        <!-- encoders are assigned the type\n" +
                "             ch.qos.logback.classic.encoder.PatternLayoutEncoder by default -->\n" +
                "        <encoder>\n" +
                "            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>\n" +
                "        </encoder>\n" +
                "    </appender>\n" +
                "\n" +
                "    <root level=\"trace\">\n" +
                "        <appender-ref ref=\"STDOUT\"/>\n" +
                "    </root>\n" +
                "\n" +
                "</configuration>";

        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);


        assertEquals(false, logger.isTraceEnabled());

        try {

            final File configFolder = temporaryFolder.newFolder();

            Files.write(overridenContents, new File(configFolder, "logback.xml"), StandardCharsets.UTF_8);

            LoggingBootstrap.initLogging(configFolder);

            assertEquals(true, logger.isTraceEnabled());
        } finally {
            //Set back to the original level, otherwise we interfere with other tests
            resetLogToOriginal();
        }
    }

    @Test
    public void test_dont_override_standard_logback_no_file_in_folder() throws Exception {


        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);


        assertEquals(false, logger.isTraceEnabled());

        try {
            final File configFolder = temporaryFolder.newFolder();

            LoggingBootstrap.prepareLogging();
            //No file was written
            LoggingBootstrap.initLogging(configFolder);

            assertEquals(false, logger.isTraceEnabled());
        } finally {
            //Set back to the original level, otherwise we interfere with other tests
            resetLogToOriginal();
        }
    }

    @Test
    public void test_logger_prepare_holds_back_logger_until_init_logging() throws Exception {

        try {

            final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

            final LogbackCapturingAppender testAppender = LogbackCapturingAppender.Factory.weaveInto(logger);
            LoggingBootstrap.prepareLogging();

            logger.info("testlog");

            //The logging statement is cached and is not available yet
            assertFalse(testAppender.isLogCaptured());

            //This "resets" to the original logger
            LoggingBootstrap.initLogging(temporaryFolder.newFolder());


            assertTrue(testAppender.isLogCaptured());

            final ImmutableList<Appender<ILoggingEvent>> appenders = ImmutableList.copyOf(logger.iteratorForAppenders());


            for (final Appender<ILoggingEvent> appender : appenders) {
                if (appender instanceof ListAppender) {
                    fail();
                }
            }

            LogbackCapturingAppender.Factory.cleanUp();
        } finally {
            resetLogToOriginal();
        }
    }

    @Test
    public void test_log_file_overriden() throws Exception {

        final String overridenContents = "" +
                "<configuration>\n" +
                "\n" +
                "    <appender name=\"APP\" class=\"ch.qos.logback.core.ConsoleAppender\">\n" +
                "        <!-- encoders are assigned the type\n" +
                "             ch.qos.logback.classic.encoder.PatternLayoutEncoder by default -->\n" +
                "        <encoder>\n" +
                "            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>\n" +
                "        </encoder>\n" +
                "    </appender>\n" +
                "\n" +
                "    <root level=\"trace\">\n" +
                "        <appender-ref ref=\"APP\"/>\n" +
                "    </root>\n" +
                "\n" +
                "</configuration>";

        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        try {

            LoggingBootstrap.prepareLogging();
            final File configFolder = temporaryFolder.newFolder();

            Files.write(overridenContents, new File(configFolder, "logback.xml"), StandardCharsets.UTF_8);

            LoggingBootstrap.initLogging(configFolder);

            final ImmutableList<Appender<ILoggingEvent>> appenders = ImmutableList.copyOf(logger.iteratorForAppenders());

            //We expect only 2 Appenders: The Instrumented Appender and a Console Appender we created above

            for (final Appender<ILoggingEvent> appender : appenders) {
                assertTrue(appender + " was not expected", appender.getName().equals("com.hivemq.logging") || appender.getName().equals("APP"));
            }

        } finally {
            //Set back to the original level, otherwise we interfere with other tests
            resetLogToOriginal();
        }
    }


    public void resetLogToOriginal() throws Exception {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();

        final JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(this.getClass().getClassLoader().getResource("logback-test.xml"));
    }

}