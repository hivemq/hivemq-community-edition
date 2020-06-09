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
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.google.common.annotations.VisibleForTesting;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.EnvironmentImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

/**
 * This Log Level Modificator is used to modify the log level of Xodus logs.
 *
 * @author Dominik Obermaier
 */
public class XodusEnvironmentImplLogLevelModificator extends TurboFilter {

    @VisibleForTesting
    final static Logger environmentalLogger = LoggerFactory.getLogger(EnvironmentImpl.class);

    @Override
    public FilterReply decide(final Marker marker, final ch.qos.logback.classic.Logger logger, final Level level,
                              final String format, final Object[] params, final Throwable t) {
        if (level.isGreaterOrEqual(Level.INFO)) {

            if (logger.getName().equals(environmentalLogger.getName())) {

                if (format != null) {

                    if (format.contains("transaction(s) not finished")) {
                        logger.trace(marker, format, params);
                        return FilterReply.DENY;
                    } else if (format.contains("Transactions stack traces are not available")) {
                        logger.trace(marker, format, params);
                        return FilterReply.DENY;
                    }
                }
            }
        }


        if (level.isGreaterOrEqual(Level.ERROR) && t != null && t.getMessage() != null && t instanceof ExodusException) {
            if (t.getMessage().contains("cleanFile") || t.getMessage().contains("There is no file by address")) {
                logger.trace(marker, "Xodus background job unable to cleanup stale data just now, trying again later");
                return FilterReply.DENY;
            }
        }

        // Let other filters decide
        return FilterReply.NEUTRAL;
    }
}


