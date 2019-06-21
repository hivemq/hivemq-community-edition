/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.util;

import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.persistence.util.BatchedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Collection;

/**
 * Various utilities for dealing with Exceptions
 *
 * @author Dominik Obermaier
 */
public class Exceptions {

    private static final Logger log = LoggerFactory.getLogger(Exceptions.class);

    /**
     * Throwables are rethrown if they are an error. Otherwise they will just be logged.
     *
     * @param throwable the throwable to guard HiveMQ from
     */
    public static void rethrowError(final String text, final Throwable throwable) {
        if (throwable instanceof Error) {
            throw (Error) throwable;
        } else {
            log.error(text, throwable);
        }
    }

    /**
     * Checks if somewhere in the chain of causes in a Throwable lies an UnrecoverableException. If so it re-throws the
     * causing Exception.
     * <p>
     * <p>Does nothing if the throwable itself is an UnrecoverableException</p>
     *
     * @param throwable the to be checked Throwable
     */
    public static void rethrowUnrecoverableCause(final Throwable throwable) {
        if (throwable == null) {
            return;
        }
        Throwable t = throwable;
        Throwable cause = throwable.getCause();
        while (cause != null && t != cause) {
            if (cause instanceof UnrecoverableException) {
                throw (UnrecoverableException) cause;
            }
            t = cause;
            cause = t.getCause();
        }
    }

    public static void rethrowError(final Throwable throwable) {
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
    }

    /**
     * Checks if a throwable is an IOException or a BatchedException that contains ClosedChannelException only.
     *
     * @param throwable to check
     * @return true if the exception is an IOException
     */
    public static boolean isConnectionClosedException(final Throwable throwable) {
        if (throwable instanceof IOException) {
            return true;
        }
        if (throwable instanceof BatchedException) {
            final Collection<Throwable> throwables = ((BatchedException) throwable).getThrowables();
            for (final Throwable inner : throwables) {
                if (!(inner instanceof ClosedChannelException)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
