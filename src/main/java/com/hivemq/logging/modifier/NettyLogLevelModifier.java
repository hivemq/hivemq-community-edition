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
import ch.qos.logback.core.spi.FilterReply;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Marker;

public class NettyLogLevelModifier implements LogLevelModifier {

    @Override
    public @NotNull FilterReply decide(
            final @Nullable Marker marker,
            final @NotNull Logger logger,
            final @NotNull Level level,
            final @NotNull String format,
            final @Nullable Object @Nullable [] params,
            final @Nullable Throwable t) {

        if (level == Level.DEBUG) {
            if (logger.getName().startsWith("io.netty")) {
                if (logger.getName().startsWith("io.netty.handler.traffic.")) {
                    return FilterReply.DENY;
                }
                if (logger.getName().startsWith("io.netty.util.internal.NativeLibraryLoader")) {
                    if (t instanceof UnsatisfiedLinkError) {
                        return FilterReply.DENY;
                    }
                    if (params == null) {
                        logger.trace(marker, format, params);
                        return FilterReply.DENY;
                    }
                    for (final Object param : params) {
                        if (param instanceof UnsatisfiedLinkError) {
                            return FilterReply.DENY;
                        }
                    }
                    logger.trace(marker, format, params);
                    return FilterReply.DENY;
                }
                traceAndSortOutUnsupportedOperationException(marker, logger, format, params, t);
                return FilterReply.DENY;
            }
        } else if (level == Level.TRACE) {
            if (logger.getName().startsWith("io.netty.channel.nio.NioEventLoop")) {
                if (t instanceof UnsupportedOperationException) {
                    return FilterReply.DENY;
                }
                if (params != null) {
                    for (final Object param : params) {
                        if (param instanceof UnsupportedOperationException) {
                            return FilterReply.DENY;
                        }
                    }
                }
            }
        }
        return FilterReply.NEUTRAL;
    }

    private static void traceAndSortOutUnsupportedOperationException(
            final @Nullable Marker marker,
            final @NotNull Logger logger,
            final @NotNull String format,
            final @Nullable Object @Nullable [] params,
            final @Nullable Throwable t) {

        if (t instanceof UnsupportedOperationException) {
            return;
        }
        if (params != null) {
            for (final Object param : params) {
                if (param instanceof UnsupportedOperationException) {
                    return;
                }
            }
        }
        logger.trace(marker, format, params);
    }
}
