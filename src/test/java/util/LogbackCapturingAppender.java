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
package util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is a appender which can be used in Unit tests to see what was actually log. Usage:
 * <p>
 * <code>
 * <p>
 * final LogbackCapturingAppender capturing = LogbackCapturingAppender.Factory.weaveInto(LicensingConnectionLimiter.log);<br/>
 * <br/>
 * .... fancy test logic here .... <br/>
 * <br/>
 * assertEquals(true, capturing.isLogCaptured()); <br/>
 * assertEquals(capturing.getLastCapturedLog().getLevel(), Level.WARN); <br/>
 * assertEquals(capturing.getLastCapturedLog().getFormattedMessage(), expectedLogMessage);  <br/>
 * <p>
 * </code>
 *
 * @author Dominik Obermaier
 */
public final class LogbackCapturingAppender extends AppenderBase<ILoggingEvent> {
    public static class Factory {

        private static final List<LogbackCapturingAppender> ALL = new ArrayList<LogbackCapturingAppender>();

        private Factory() {
        }

        public static LogbackCapturingAppender weaveInto(final org.slf4j.Logger sl4jLogger) {
            final LogbackCapturingAppender appender = new LogbackCapturingAppender(sl4jLogger);
            ALL.add(appender);
            return appender;
        }


        public static void cleanUp() {
            for (final LogbackCapturingAppender appender : ALL) {
                appender.cleanUp();
            }
        }
    }

    private final Logger log;
    private ILoggingEvent captured;

    private final List<ILoggingEvent> allCaptured = new ArrayList<ILoggingEvent>();

    public LogbackCapturingAppender(final org.slf4j.Logger sl4jLogger) {
        log = (Logger) sl4jLogger;
        addAppender(log);
        detachDefaultConsoleAppender();
    }

    private void detachDefaultConsoleAppender() {
        final Logger rootLogger = getRootLogger();
        final Appender<ILoggingEvent> consoleAppender = rootLogger.getAppender("console");
        rootLogger.detachAppender(consoleAppender);
    }

    private Logger getRootLogger() {
        return log.getLoggerContext().getLogger("ROOT");
    }

    private void addAppender(final Logger logger) {
        logger.setLevel(Level.ALL);
        logger.addAppender(this);
        start();
    }

    public ILoggingEvent getLastCapturedLog() {
        return captured;
    }

    public List<ILoggingEvent> getCapturedLogs() {
        return Collections.unmodifiableList(allCaptured);
    }

    public boolean isLogCaptured() {
        return captured != null;
    }

    @Override
    protected void append(final ILoggingEvent iLoggingEvent) {
        allCaptured.add(iLoggingEvent);
        captured = iLoggingEvent;
    }

    private void cleanUp() {
        log.detachAppender(this);

    }
}
