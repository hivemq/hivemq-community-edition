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
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.util.StatusPrinter;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.NettyLogLevelModifier;
import com.hivemq.logging.XodusEnvironmentImplLogLevelModificator;
import com.hivemq.logging.XodusFileDataWriterLogLevelModificator;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is responsible for all logging bootstrapping. This is only
 * needed at the very beginning of HiveMQs lifecycle and before bootstrapping other
 * resources
 *
 * @author Dominik Obermaier
 */
public class LoggingBootstrap {

    private static @NotNull ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

    private static final Logger log = LoggerFactory.getLogger(LoggingBootstrap.class);

    private static final XodusFileDataWriterLogLevelModificator xodusFileDataWriterLogLevelModificator =
            new XodusFileDataWriterLogLevelModificator();
    private static final NettyLogLevelModifier nettyLogLevelModifier = new NettyLogLevelModifier();
    private static final XodusEnvironmentImplLogLevelModificator xodusEnvironmentImplLogLevelModificator =
            new XodusEnvironmentImplLogLevelModificator();

    private static final List<Appender<ILoggingEvent>> defaultAppenders = new LinkedList<>();

    /**
     * Prepares the logging. This method must be called before any logging occurs
     */
    public static void prepareLogging() {

        final ch.qos.logback.classic.Logger logger = getRootLogger();


        final Iterator<Appender<ILoggingEvent>> iterator = logger.iteratorForAppenders();
        while (iterator.hasNext()) {
            final Appender<ILoggingEvent> next = iterator.next();
            //We remove the appender for the moment
            logger.detachAppender(next);
            defaultAppenders.add(next);
        }


        //This appender just adds entries to an Array List so we can queue the log statements for later
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

    }

    /**
     * Initializes all Logging for HiveMQ. Call this method only once
     * at the very beginning of the HiveMQ lifecycle
     */
    public static void initLogging(final @NotNull File configFolder) {

        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        final ch.qos.logback.classic.Logger logger = getRootLogger();

        context.addListener(new LogbackChangeListener(logger));

        final boolean overridden = overrideLogbackXml(configFolder);

        if (!overridden) {
            reEnableDefaultAppenders();
        }
        redirectJULToSLF4J();
        logQueuedEntries();

        reset();

        // must be added here, as addLoglevelModifiers() is much to late
        if (SystemUtils.IS_OS_WINDOWS) {
            context.addTurboFilter(xodusFileDataWriterLogLevelModificator);
            log.trace("Added Xodus log level modifier for FileDataWriter.class");
        }

        context.addTurboFilter(nettyLogLevelModifier);
        log.trace("Added Netty log level modifier");
    }

    /**
     * Re-enables all default appenders that were removed from the root logger for startup
     */
    private static void reEnableDefaultAppenders() {

        final ch.qos.logback.classic.Logger logger = getRootLogger();

        for (final Appender<ILoggingEvent> defaultAppender : defaultAppenders) {
            logger.addAppender(defaultAppender);
        }
    }

    /**
     * Logs all queued Entries to the logger. It is assumed that the logger is fully initialized at this point
     */
    private static void logQueuedEntries() {

        final ch.qos.logback.classic.Logger logger = getRootLogger();

        listAppender.stop();
        //Now we need to detach the appender (if needed) so it isn't used anymore
        logger.detachAppender(listAppender);
        for (final ILoggingEvent loggingEvent : listAppender.list) {
            logger.callAppenders(loggingEvent);
        }
    }

    private static @NotNull ch.qos.logback.classic.Logger getRootLogger() {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        return context.getLogger(Logger.ROOT_LOGGER_NAME);
    }

    /**
     * Redirects all logging statements from java.util.logging to SLF4J
     * <p>
     * This is needed because we may have many dependencies which rely on JUL.
     */
    private static void redirectJULToSLF4J() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    /**
     * Overrides the standard Logging configuration delivered with HiveMQ with
     * a logback.xml from the config folder.
     *
     * @return If the default configuration was overridden
     */
    private static boolean overrideLogbackXml(final @NotNull File configFolder) {
        final File file = new File(configFolder, "logback.xml");
        if (file.canRead()) {
            log.info("Log Configuration was overridden by {}", file.getAbsolutePath());
            final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            try {
                context.reset();

                final JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                configurator.doConfigure(file);

                context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(listAppender);
                return true;
            } catch (final JoranException je) {
                // StatusPrinter will handle this
            } catch (final Exception ex) {
                // Just in case, so we see a stacktrace if the logger could not be initialized
                ex.printStackTrace();
            } finally {
                StatusPrinter.printInCaseOfErrorsOrWarnings(context);
            }
            // Print internal status data in case of warnings or errors.
            return false;
        } else {
            log.warn(
                    "The logging configuration file {} does not exist. Using HiveMQ default logging configuration.",
                    file.getAbsolutePath());
            return false;
        }
    }

    public static void addLoglevelModifiers() {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        context.addTurboFilter(xodusEnvironmentImplLogLevelModificator);
        log.trace("Added Xodus log level modifier for EnvironmentImpl.class");
    }

    /**
     * Resets everything to the initial state
     */
    private static void reset() {
        defaultAppenders.clear();
        listAppender.list.clear();
    }

    private static class LogbackChangeListener implements LoggerContextListener {

        private final @NotNull ch.qos.logback.classic.Logger logger;

        private LogbackChangeListener(
                final @NotNull ch.qos.logback.classic.Logger logger) {
            this.logger = logger;
        }

        @Override
        public boolean isResetResistant() {
            return true;
        }

        @Override
        public void onStart(final @NotNull LoggerContext context) {
            //noop
        }

        /**
         * filters and appender must be re added after logback.xml change
         * <p>
         * as reset if logger context removes everything.
         *
         * @see LoggerContext#reset()
         */
        @Override
        public void onReset(final @NotNull LoggerContext context) {
            log.trace("logback.xml was changed");
            addTurboFilters(context);
        }

        @Override
        public void onStop(final @NotNull LoggerContext context) {
            //noop
        }

        @Override
        public void onLevelChange(final @NotNull ch.qos.logback.classic.Logger logger, final @NotNull Level level) {
            //noop
        }

        private void addTurboFilters(final @NotNull LoggerContext context) {

            context.addTurboFilter(xodusFileDataWriterLogLevelModificator);
            context.addTurboFilter(nettyLogLevelModifier);
            context.addTurboFilter(xodusEnvironmentImplLogLevelModificator);
        }
    }
}
