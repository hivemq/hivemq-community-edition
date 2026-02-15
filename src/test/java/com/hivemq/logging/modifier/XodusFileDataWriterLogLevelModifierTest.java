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
package com.hivemq.logging.modifier;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.hivemq.bootstrap.LoggingBootstrap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.LogLevelModifierTurboFilter;
import jetbrains.exodus.io.FileDataWriter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;


public class XodusFileDataWriterLogLevelModifierTest {

    private ch.qos.logback.classic.Logger rootLogger;
    private Level level;
    private LoggerContext context;

    @Before
    public void setUp() throws Exception {

        context = (LoggerContext) LoggerFactory.getILoggerFactory();

        rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

        final LogLevelModifierTurboFilter logLevelModifierTurboFilter = new LogLevelModifierTurboFilter();
        logLevelModifierTurboFilter.registerLogLevelModifier(new XodusFileDataWriterLogLevelModifier());
        level = rootLogger.getLevel();
        rootLogger.setLevel(Level.DEBUG);
        context.addTurboFilter(logLevelModifierTurboFilter);
        context.getLogger("jetbrains.exodus").setLevel(Level.DEBUG);
    }

    @After
    public void tearDown() throws Exception {
        rootLogger.setLevel(level);
        context.resetTurboFilterList();
    }

    @AfterClass
    public static void afterClass() {
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();
        LoggingBootstrap.prepareLogging();
    }

    @Test
    public void test_first_time_gets_modified_to_debug() {
        final String msg = "Can't open directory channel. Log directory fsync won't be performed.";

        final AtomicInteger loggedCounter = new AtomicInteger();
        final Iterator<Appender<ILoggingEvent>> appenderIterator = rootLogger.iteratorForAppenders();

        while (appenderIterator.hasNext()) {
            appenderIterator.next().addFilter(createFilter(loggedCounter, msg));
        }
        context.getLogger(FileDataWriter.class).warn(msg);

        assertEquals(1, loggedCounter.get());
    }

    private @NotNull Filter<ILoggingEvent> createFilter(
            final @NotNull AtomicInteger loggedCounter,
            final @NotNull String text) {

        return new Filter<>() {
            @Override
            public FilterReply decide(final ILoggingEvent event) {
                if (event.getLevel().equals(Level.DEBUG)) {
                    if (event.getFormattedMessage().equals(text)) {
                        loggedCounter.getAndIncrement();
                        return FilterReply.NEUTRAL;
                    }
                }
                return FilterReply.NEUTRAL;
            }
        };
    }
}
