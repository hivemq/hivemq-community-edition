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
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NettyLogLevelModifierTest {
    private NettyLogLevelModifier nettyLogLevelModifier;

    private final String format = "Test-String";
    private Logger rootLogger;

    @Before
    public void setUp() throws Exception {

        final LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

        nettyLogLevelModifier = new NettyLogLevelModifier();
    }

    @Test
    public void test_level_trace() {
        final FilterReply decide = nettyLogLevelModifier.decide(null, rootLogger, Level.TRACE, "", null, null);
        assertEquals(FilterReply.NEUTRAL, decide);
    }

    @Test
    public void test_level_all() {
        final FilterReply decide = nettyLogLevelModifier.decide(null, rootLogger, Level.ALL, "", null, null);
        assertEquals(FilterReply.NEUTRAL, decide);
    }

    @Test
    public void test_level_info_format_null() {
        final FilterReply decide = nettyLogLevelModifier.decide(null, rootLogger, Level.INFO, null, null, null);
        assertEquals(FilterReply.NEUTRAL, decide);
    }

    @Test
    public void test_level_info_format_set() {
        final FilterReply decide = nettyLogLevelModifier.decide(null, rootLogger, Level.INFO, format, null, null);
        assertEquals(FilterReply.NEUTRAL, decide);
    }

    @Test
    public void test_level_info_format_denied_throwable() {

        final LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        rootLogger = context.getLogger("io.netty.util.internal.PlatformDependent0");

        final FilterReply decide = nettyLogLevelModifier.decide(null,
                rootLogger,
                Level.DEBUG,
                format,
                null,
                new UnsupportedOperationException());

        assertEquals(FilterReply.DENY, decide);
    }

    @Test
    public void test_level_info_format_neutral_throwable() {

        final LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        rootLogger = context.getLogger("io.netty.channel.nio.NioEventLoop");

        final FilterReply decide =
                nettyLogLevelModifier.decide(null, rootLogger, Level.TRACE, format, null, new RuntimeException());

        assertEquals(FilterReply.NEUTRAL, decide);
    }

    @Test
    public void test_level_trace_format_denied_throwable() {

        final LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        rootLogger = context.getLogger("io.netty.channel.nio.NioEventLoop");

        final FilterReply decide = nettyLogLevelModifier.decide(null,
                rootLogger,
                Level.TRACE,
                format,
                null,
                new UnsupportedOperationException());

        assertEquals(FilterReply.DENY, decide);
    }

    @Test
    public void test_level_info_format_denied_parameter() {

        final LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        rootLogger = context.getLogger("io.netty.util.internal.PlatformDependent0");

        final FilterReply decide = nettyLogLevelModifier.decide(null,
                rootLogger,
                Level.DEBUG,
                format,
                new Object[]{new UnsupportedOperationException()},
                null);

        assertEquals(FilterReply.DENY, decide);
    }

    @Test
    public void test_level_debug_native_denied_parameter() {

        final LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        rootLogger = context.getLogger("io.netty.util.internal.NativeLibraryLoader");

        final FilterReply decide = nettyLogLevelModifier.decide(null,
                rootLogger,
                Level.DEBUG,
                format,
                new Object[]{new UnsupportedOperationException()},
                null);

        assertEquals(FilterReply.DENY, decide);
    }
}
