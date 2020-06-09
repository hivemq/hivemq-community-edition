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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * @author Dominik Obermaier
 */
class DiagnosticLogging {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(DiagnosticLogging.class);


    /**
     * Sets the trace log to the root logger. Also adds filter, to make sure that
     * the appender which are already defined for HiveMQ are not affected by this logging
     * level change.
     * <p>
     * <b>This will significantly slow down HiveMQ, since the root level loggers Level is changed
     * to the finest logging level!</b>
     *
     * @param filePath the file path
     */
    static void setTraceLog(final String filePath) {

        log.info("Creating trace log {}", filePath);

        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        final Level originalLoggingLevel = logger.getLevel();
        final Iterator<Appender<ILoggingEvent>> appenderIterator = logger.iteratorForAppenders();
        while (appenderIterator.hasNext()) {
            final Appender<ILoggingEvent> next = appenderIterator.next();
            next.addFilter(new PreserveOriginalLoggingLevelFilter(originalLoggingLevel));
        }

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
        ple.setContext(lc);
        ple.start();

        final FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setFile(filePath);
        fileAppender.setEncoder(ple);
        fileAppender.setContext(lc);
        fileAppender.start();

        logger.addAppender(fileAppender);
        logger.setLevel(Level.ALL);
        logger.setAdditive(false);
    }

    /**
     * A filter which preserves the original logging level
     */
    private static class PreserveOriginalLoggingLevelFilter extends Filter<ILoggingEvent> {

        private final Level originalLevel;

        PreserveOriginalLoggingLevelFilter(final Level originalLevel) {
            this.originalLevel = originalLevel;
        }

        @Override
        public FilterReply decide(final ILoggingEvent event) {
            if (event.getLevel().toInt() < originalLevel.toInt()) {
                return FilterReply.DENY;
            } else {
                return FilterReply.ACCEPT;
            }
        }
    }
}
