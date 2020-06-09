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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lukas Brandl
 */
public class ThreadPreConditions {

    public static final String SINGLE_WRITER_THREAD_PREFIX = "single-writer";

    private static final Logger log = LoggerFactory.getLogger(ThreadPreConditions.class);

    private static boolean enabled = false;

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
        if (!Thread.currentThread().getName().startsWith(prefix)) {
            log.error("Thread name doesn't start with {}", prefix);
            throw new ThreadPreConditionException();
        }
    }

    public static class ThreadPreConditionException extends RuntimeException {

    }
}
