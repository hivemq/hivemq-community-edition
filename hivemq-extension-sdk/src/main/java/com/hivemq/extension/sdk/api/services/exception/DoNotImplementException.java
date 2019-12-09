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
package com.hivemq.extension.sdk.api.services.exception;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * This exception is used to signal that a given interface,
 * annotated with {@link DoNotImplement} was falsely implemented by an extension.
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class DoNotImplementException extends RuntimeException {

    /**
     * The name of the class that was implemented.
     * @since 4.0.0
     */
    private final @NotNull String implementedClass;

    /**
     * Flag that if <b>true</b> will also fill in the stack trace for the exception.
     * @since 4.0.0
     */
    private final boolean fillInStacktrace;

    /**
     * Creates a new DoNotImplementException.
     *
     * @param implementedClass The name of the implemented class.
     * @param fillInStacktrace Whether the created exception should fill in a stacktrace.
     * @since 4.0.0
     */
    public DoNotImplementException(final @NotNull String implementedClass, final boolean fillInStacktrace) {
        this.implementedClass = implementedClass;
        this.fillInStacktrace = fillInStacktrace;
    }

    /**
     * Creates a new DoNotImplementException that will not contain a stacktrace.
     *
     * @param implementedClass The name of the implemented class.
     * @since 4.0.0
     */
    public DoNotImplementException(final @NotNull String implementedClass) {
        this(implementedClass, false);
    }

    @Override
    public synchronized @NotNull Throwable fillInStackTrace() {
        if (fillInStacktrace) {
            return super.fillInStackTrace();
        }
        return this;
    }

    /**
     * Returns the name of the implemented class.
     *
     * @return The name of the implemented class.
     * @since 4.0.0
     */
    public @NotNull String getImplementedClass() {
        return implementedClass;
    }
}
