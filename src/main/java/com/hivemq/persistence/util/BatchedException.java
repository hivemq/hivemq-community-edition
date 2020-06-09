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
package com.hivemq.persistence.util;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Georg Held
 */
public class BatchedException extends Exception {
    final Collection<Throwable> throwables;

    public BatchedException(final Collection<Throwable> throwables) {
        super();

        checkArgument(throwables.size() > 1, "throwables.size() must be greater than 1");
        this.throwables = throwables;
    }

    public Collection<Throwable> getThrowables() {
        return this.throwables;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    @Override
    public void printStackTrace() {
        for (final Throwable throwable : throwables) {
            throwable.printStackTrace();
        }
    }

    @Override
    public void printStackTrace(final PrintStream s) {
        for (final Throwable throwable : throwables) {
            throwable.printStackTrace(s);
        }
    }

    @Override
    public void printStackTrace(final PrintWriter s) {
        for (final Throwable throwable : throwables) {
            throwable.printStackTrace(s);
        }
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        int size = 0;
        for (final Throwable throwable : throwables) {
            size += throwable.getStackTrace().length;
        }
        final StackTraceElement[] stackTraceElements = new StackTraceElement[size];
        int i = 0;

        for (final Throwable throwable : throwables) {
            for (final StackTraceElement stackTraceElement : throwable.getStackTrace()) {
                stackTraceElements[i] = stackTraceElement;
                i++;
            }
        }
        return stackTraceElements;
    }

    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.toString());
        for (final Throwable throwable : throwables) {
            stringBuilder.append(", ").append(throwable);
        }
        return stringBuilder.toString();
    }
}
