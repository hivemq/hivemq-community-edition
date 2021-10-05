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
package com.hivemq.util;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Lukas Brandl
 */
public class ThreadPreConditions {

    public static final String SINGLE_WRITER_THREAD_PREFIX = "single-writer";
    public static final String NETTY_NATIVE_CHILD_EVENTLOOP = "hivemq-native-eventloop-child";
    public static final String NETTY_CHILD_EVENTLOOP = "hivemq-eventloop-child";

    private static boolean enabled = false;

    static {
        final String enableThreadPreconditionString = System.getProperty("TEST_ENABLE_THREAD_PRECONDITION");
        if (enableThreadPreconditionString != null) {
            enabled = Boolean.parseBoolean(enableThreadPreconditionString);
        }
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void enable() {
        enabled = true;
    }

    public static void disable() {
        enabled = false;
    }

    public static void startsWith(@NotNull final String prefix) {
        if (!enabled) {
            return;
        }
        final String name = Thread.currentThread().getName();
        if (!name.startsWith(prefix)) {
            throw new ThreadPreConditionException("Thread name doesn't start with '" + prefix + "' as expected. Thread name: '" + name + "'.");
        }
    }

    public static void inNettyChildEventloop() {
        if (!enabled) {
            return;
        }
        final String name = Thread.currentThread().getName();
        if (!name.startsWith(NETTY_NATIVE_CHILD_EVENTLOOP) && !name.startsWith(NETTY_CHILD_EVENTLOOP)) {
            throw new ThreadPreConditionException("Thread name doesn't start with '" + NETTY_NATIVE_CHILD_EVENTLOOP + "' as expected. Thread name: '" + name + "'.");
        }
    }

    public static class ThreadPreConditionException extends RuntimeException {

        public ThreadPreConditionException(final @NotNull String message) {
            super(message);
        }
    }
}
