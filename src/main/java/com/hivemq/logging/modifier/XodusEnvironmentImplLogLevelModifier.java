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
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.spi.FilterReply;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.EnvironmentImpl;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

public class XodusEnvironmentImplLogLevelModifier implements LogLevelModifier {

    private final @NotNull Logger environmentalLogger;

    public XodusEnvironmentImplLogLevelModifier() {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        environmentalLogger = context.getLogger(EnvironmentImpl.class);
    }

    @Override
    public @NotNull FilterReply decide(
            final @Nullable Marker marker,
            final @NotNull Logger logger,
            final @NotNull Level level,
            final @NotNull String format,
            final @Nullable Object @Nullable [] params,
            final @Nullable Throwable t) {

        if (level.isGreaterOrEqual(Level.INFO)) {
            if (logger.equals(environmentalLogger)) {
                if (format.contains("transaction(s) not finished")) {
                    logger.trace(marker, format, params);
                    return FilterReply.DENY;
                }
                if (format.contains("Transactions stack traces are not available")) {
                    logger.trace(marker, format, params);
                    return FilterReply.DENY;
                }
            }
            if (level == Level.ERROR && t instanceof ExodusException) {
                if (t.getMessage().contains("cleanFile") || t.getMessage().contains("There is no file by address")) {
                    logger.trace(marker, "Xodus background job unable to cleanup stale data just now, trying again later");
                    return FilterReply.DENY;
                }
            }
        }
        return FilterReply.NEUTRAL;
    }
}
