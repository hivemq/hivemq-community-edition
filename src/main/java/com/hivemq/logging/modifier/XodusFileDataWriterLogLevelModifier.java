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
import jetbrains.exodus.io.FileDataWriter;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.concurrent.atomic.AtomicBoolean;

public class XodusFileDataWriterLogLevelModifier implements LogLevelModifier {

    private final @NotNull AtomicBoolean first = new AtomicBoolean(true);
    private final @NotNull Logger fileDataWriterLogger;

    public XodusFileDataWriterLogLevelModifier() {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        fileDataWriterLogger = context.getLogger(FileDataWriter.class);
    }

    @Override
    public @NotNull FilterReply decide(
            final @Nullable Marker marker,
            final @NotNull ch.qos.logback.classic.Logger logger,
            final @NotNull Level level,
            final @NotNull String format,
            final @Nullable Object @Nullable [] params,
            final @Nullable Throwable t) {

        if (level.isGreaterOrEqual(Level.WARN)) {
            if (logger.equals(fileDataWriterLogger)) {
                if (format.startsWith("Can't open directory channel. Log directory fsync won't be performed.")) {
                    if (first.getAndSet(false)) {
                        logger.debug("Can't open directory channel. Log directory fsync won't be performed.");
                    }
                    return FilterReply.DENY;
                }
            }
        }
        return FilterReply.NEUTRAL;
    }
}
