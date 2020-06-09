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
 * @author Dominik Obermaier
 */
public class XodusEnvironmentImplLogLevelModificatorTest {

    private ch.qos.logback.classic.Logger rootLogger;
    private Level level;

    @Before
    public void setUp() throws Exception {

        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

        level = rootLogger.getLevel();
        rootLogger.setLevel(Level.TRACE);
        context.addTurboFilter(new XodusEnvironmentImplLogLevelModificator());
        context.getLogger("jetbrains.exodus").setLevel(Level.TRACE);
    }

    @After
    public void tearDown() throws Exception {
        rootLogger.setLevel(level);
    }

    @Test
    public void test_xodus_error_message_transactions_not_finished() throws Exception {
        final String msg = "Environment[/var/folders/fp/dfyv187j4h5g6cn6xs49qpqw0000gn/T/junit5059509984233082259/junit231924022829037059] is active: 1 transaction(s) not finished";

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final Iterator<Appender<ILoggingEvent>> appenderIterator = rootLogger.iteratorForAppenders();

        while (appenderIterator.hasNext()) {

            appenderIterator.next().addFilter(createFilter(countDownLatch, msg));
        }
        XodusEnvironmentImplLogLevelModificator.environmentalLogger.error(msg);

        assertEquals(true, countDownLatch.await(3, TimeUnit.SECONDS));
    }

    @Test
    public void test_xodus_error_message_transaction_stack_traces_not_available() throws Exception {
        final String msg = "Transactions stack traces are not available, set 'exodus.env.monitorTxns.timeout > 0'";

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final Iterator<Appender<ILoggingEvent>> appenderIterator = rootLogger.iteratorForAppenders();
        while (appenderIterator.hasNext()) {

            appenderIterator.next().addFilter(createFilter(countDownLatch, msg));
        }
        XodusEnvironmentImplLogLevelModificator.environmentalLogger.error(msg);

        assertEquals(true, countDownLatch.await(3, TimeUnit.SECONDS));
    }

    @NotNull
    private Filter<ILoggingEvent> createFilter(final CountDownLatch countDownLatch, final String text) {
        return new Filter<ILoggingEvent>() {
            @Override
            public FilterReply decide(final ILoggingEvent event) {
                if (event.getLevel().equals(Level.TRACE)) {
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