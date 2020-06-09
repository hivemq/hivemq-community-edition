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
import org.slf4j.Marker;

/**
 * @author Lukas Brandl
 */
public class NettyLogLevelModifier extends TurboFilter {

    @Override
    public FilterReply decide(final Marker marker, final Logger logger, final Level level, final String format, final Object[] params, final Throwable t) {

        if (format == null || logger == null) {
            return FilterReply.NEUTRAL;
        }

        if (logger.getName() == null) {
            return FilterReply.NEUTRAL;
        }

        if (level == Level.DEBUG) {

            if (logger.getName().startsWith("io.netty.handler.traffic.")) {
                return FilterReply.DENY;
            } else if (logger.getName().contains("io.netty.util.internal.NativeLibraryLoader")) {
                if (t instanceof UnsatisfiedLinkError) {
                    return FilterReply.DENY;
                }
                if (params == null) {
                    logger.trace(marker, format, params);
                    return FilterReply.DENY;
                }
                final Object[] paramList = params;
                for (final Object param : paramList) {
                    if (param instanceof UnsatisfiedLinkError) {
                        return FilterReply.DENY;
                    }
                }
                logger.trace(marker, format, params);
                return FilterReply.DENY;
            } else if (logger.getName().startsWith("io.netty")) {
                return traceAndSortOutUnsupportedOperationException(marker, logger, format, params, t);
            }

        } else if (level == Level.TRACE) {
            if (logger.getName().contains("io.netty.channel.nio.NioEventLoop")) {
                return sortOutUnsupportedOperationException(params, t);
            }
        }

        return FilterReply.NEUTRAL;
    }

    @NotNull
    private FilterReply traceAndSortOutUnsupportedOperationException(final Marker marker, final Logger logger,
                                                                     final String format, final Object[] params, final Throwable t) {
        if (t instanceof UnsupportedOperationException) {
            return FilterReply.DENY;
        }
        if (params == null) {
            logger.trace(marker, format, params);
            return FilterReply.DENY;
        }
        final Object[] paramList = params;
        for (final Object param : paramList) {
            if (param instanceof UnsupportedOperationException) {
                return FilterReply.DENY;
            }
        }
        logger.trace(marker, format, params);
        return FilterReply.DENY;
    }

    @NotNull
    private FilterReply sortOutUnsupportedOperationException(final Object[] params, final Throwable t) {
        if (t instanceof UnsupportedOperationException) {
            return FilterReply.DENY;
        }
        if (params == null) {
            return FilterReply.NEUTRAL;
        }
        final Object[] paramList = params;
        for (final Object param : paramList) {
            if (param instanceof UnsupportedOperationException) {
                return FilterReply.DENY;
            }
        }
        return FilterReply.NEUTRAL;
    }

}
