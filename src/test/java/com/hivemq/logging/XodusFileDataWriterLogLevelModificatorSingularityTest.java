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
package com.hivemq.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author Georg Held
 */
public class XodusFileDataWriterLogLevelModificatorSingularityTest {

    private ch.qos.logback.classic.Logger rootLogger;
    private Level level;
    private LoggerContext context;

    @Before
    public void setUp() throws Exception {

        context = (LoggerContext) LoggerFactory.getILoggerFactory();

        rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

        level = rootLogger.getLevel();
        rootLogger.setLevel(Level.DEBUG);
        context.addTurboFilter(new XodusFileDataWriterLogLevelModificator());
        context.getLogger("jetbrains.exodus").setLevel(Level.DEBUG);
    }

    @After
    public void tearDown() throws Exception {
        rootLogger.setLevel(level);
        context.resetTurboFilterList();
    }


    @Test(timeout = 5000)
    public void test_get_only_logged_once() throws Exception {
        final String msg = "Can't open directory channel. Log directory fsync won't be performed.";

        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final Iterator<Appender<ILoggingEvent>> appenderIterator = rootLogger.iteratorForAppenders();

        final Appender<ILoggingEvent> next = appenderIterator.next();
        next.addFilter(createFilter(countDownLatch, msg));

        XodusFileDataWriterLogLevelModificator.fileDataWriterLogger.warn(msg);
        XodusFileDataWriterLogLevelModificator.fileDataWriterLogger.warn(msg);
        XodusFileDataWriterLogLevelModificator.fileDataWriterLogger.warn(msg);
        XodusFileDataWriterLogLevelModificator.fileDataWriterLogger.warn(msg);
        XodusFileDataWriterLogLevelModificator.fileDataWriterLogger.warn(msg);

        assertEquals(false, countDownLatch.await(3, TimeUnit.SECONDS));
    }


    @NotNull
    private Filter<ILoggingEvent> createFilter(final CountDownLatch countDownLatch, final String text) {
        return new Filter<ILoggingEvent>() {
            @Override
            public FilterReply decide(final ILoggingEvent event) {
                if (event.getLevel().equals(Level.DEBUG)) {
                    if (event.getFormattedMessage().equals(text)) {
                        countDownLatch.countDown();
                        return FilterReply.NEUTRAL;
                    }
                }
                return FilterReply.NEUTRAL;
            }
        };
    }
}
