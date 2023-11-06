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
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.logging.modifier.LogLevelModifier;
import org.slf4j.Marker;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LogLevelModifierTurboFilter extends TurboFilter {

    private final @NotNull List<LogLevelModifier> logLevelModifiers = new CopyOnWriteArrayList<>();

    @Override
    public @NotNull FilterReply decide(
            final @Nullable Marker marker,
            final @NotNull Logger logger,
            final @NotNull Level level,
            final @Nullable String format,
            final @Nullable Object @Nullable [] params,
            final @Nullable Throwable t) {

        FilterReply filterReply = FilterReply.NEUTRAL;

        if (format == null || level == Level.OFF) {
            // format is the log message
            return filterReply;
        }

        for (final LogLevelModifier logLevelModifier : logLevelModifiers) {
            filterReply = logLevelModifier.decide(marker, logger, level, format, params, t);
            if (filterReply != FilterReply.NEUTRAL) {
                return filterReply;
            }
        }
        return filterReply;
    }

    public void registerLogLevelModifier(final @NotNull LogLevelModifier logLevelModifier) {
        logLevelModifiers.add(logLevelModifier);
    }
}
