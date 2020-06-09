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
package com.hivemq.migration.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.hivemq.migration.Migrations;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.LogbackCapturingAppender;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class PayloadExceptionLoggingTest {

    private PayloadExceptionLogging payloadExceptionLogging;
    private Logger migrationLogger;
    private LogbackCapturingAppender capturingAppender;

    @Before
    public void setUp() throws Exception {
        payloadExceptionLogging = new PayloadExceptionLogging();
        migrationLogger = LoggerFactory.getLogger(Migrations.MIGRATION_LOGGER_NAME);
        capturingAppender = LogbackCapturingAppender.Factory.weaveInto(migrationLogger);

    }

    @Test(timeout = 5000)
    public void test_message_logging() throws Exception {

        payloadExceptionLogging.addLogging(1, null, null);
        payloadExceptionLogging.addLogging(1, null, null);
        payloadExceptionLogging.addLogging(1, null, null);
        payloadExceptionLogging.addLogging(1, null, null);
        payloadExceptionLogging.addLogging(1, null, null);
        payloadExceptionLogging.addLogging(1, null, null);
        payloadExceptionLogging.addLogging(1, null, null);
        payloadExceptionLogging.addLogging(1, null, null);
        payloadExceptionLogging.addLogging(1, true, "topic1");
        payloadExceptionLogging.addLogging(2, true, "topic2");
        payloadExceptionLogging.addLogging(3, true, "topic3");
        payloadExceptionLogging.addLogging(4, true, "topic4");

        final Map<Long, PayloadExceptionLogging.MissingMessageInformation> map = payloadExceptionLogging.getMap();

        assertEquals(4, map.size());
        for (long i = 1; i < 5; i++) {
            final PayloadExceptionLogging.MissingMessageInformation information = map.get(i);
            assertNotNull(information);
            assertEquals(i, information.getPayloadId());
            assertTrue(information.isRetained());
            assertEquals("topic" + i, information.getTopic());
        }

        payloadExceptionLogging.logAndClear();
    }

    @Test(timeout = 5000)
    public void test_message_logging_gets_cleared() throws Exception {
        payloadExceptionLogging.addLogging(1, null, "topic");

        final Map<Long, PayloadExceptionLogging.MissingMessageInformation> map1 = payloadExceptionLogging.getMap();
        assertFalse(map1.isEmpty());

        payloadExceptionLogging.logAndClear();

        final Map<Long, PayloadExceptionLogging.MissingMessageInformation> map2 = payloadExceptionLogging.getMap();
        assertTrue(map2.isEmpty());

        assertTrue(capturingAppender.isLogCaptured());
        final ILoggingEvent lastCapturedLog = capturingAppender.getLastCapturedLog();
        assertEquals(Level.WARN, lastCapturedLog.getLevel());
    }

    @Test(timeout = 5000)
    public void test_nothing_gets_logged_if_empty() throws Exception {
        payloadExceptionLogging.logAndClear();
        assertFalse(capturingAppender.isLogCaptured());
    }

}