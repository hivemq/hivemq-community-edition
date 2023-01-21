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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;

public class ThreadFactoryUtil {

    /**
     * Creates a {@link ThreadFactory} with given nameFormat and an {@link UncaughtExceptionHandler} to log every
     * uncaught exception.
     *
     * @param nameFormat the format of the name
     */
    public static @NotNull ThreadFactory create(final @NotNull String nameFormat) {
        return new ThreadFactoryBuilder().setNameFormat(nameFormat)
                .setUncaughtExceptionHandler(new UncaughtExceptionHandler())
                .build();
    }

    private static class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(UncaughtExceptionHandler.class);

        @Override
        public void uncaughtException(final @NotNull Thread thread, final @NotNull Throwable throwable) {
            log.error("Uncaught exception in thread '{}'.", thread.getName(), throwable);
        }
    }
}
