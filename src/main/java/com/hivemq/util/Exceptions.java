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
import com.hivemq.persistence.util.BatchedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

/**
 * Various utilities for dealing with exceptions.
 */
public class Exceptions {

    private static final Logger log = LoggerFactory.getLogger(Exceptions.class);

    /**
     * {@link Throwable} instances are rethrown if they are an {@link Error}.
     * Otherwise, they will just be logged.
     *
     * @param throwable the throwable to guard HiveMQ from
     */
    public static void rethrowError(final @NotNull String text, final @NotNull Throwable throwable) {
        if (throwable instanceof Error) {
            throw (Error) throwable;
        } else {
            log.error(text, throwable);
        }
    }

    /**
     * {@link Throwable} instances are rethrown if they are an {@link Error}.
     * Otherwise, this is a noop.
     */
    public static void rethrowError(final @NotNull Throwable throwable) {
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
    }

    /**
     * Checks if a throwable is an IOException.
     *
     * @param throwable to check
     * @return true if the exception is an IOException
     */
    public static boolean isConnectionClosedException(final @NotNull Throwable throwable) {
        if (throwable instanceof IOException) {
            return true;
        }
        if (throwable instanceof BatchedException) {
            final BatchedException batchedException = (BatchedException) throwable;
            for (final Throwable inner : batchedException.getThrowables()) {
                if (!(inner instanceof ClosedChannelException)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
