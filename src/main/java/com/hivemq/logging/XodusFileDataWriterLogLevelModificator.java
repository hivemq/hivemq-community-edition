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
import jetbrains.exodus.io.FileDataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Georg Held
 */
public class XodusFileDataWriterLogLevelModificator extends TurboFilter {

    @VisibleForTesting
    static final Logger fileDataWriterLogger = LoggerFactory.getLogger(FileDataWriter.class);
    private static final Logger log = LoggerFactory.getLogger(XodusFileDataWriterLogLevelModificator.class);

    private final AtomicBoolean first = new AtomicBoolean(true);

    @Override
    public FilterReply decide(final Marker marker, final ch.qos.logback.classic.Logger logger, final Level level, final String format, final Object[] params, final Throwable t) {

        if (level.isGreaterOrEqual(Level.WARN)) {
            if (logger.getName().equals(fileDataWriterLogger.getName())) {
                if (format != null) {
                    if (format.startsWith("Can't open directory channel. Log directory fsync won't be performed.")) {
                        if (first.getAndSet(false)) {
                            log.debug("Can't open directory channel. Log directory fsync won't be performed.");
                        }
                        return FilterReply.DENY;
                    }
                }
            }

        }
        return FilterReply.NEUTRAL;
    }
}
