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

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * This exception is used to signal that a given topic filter is invalid.
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class InvalidTopicException extends Exception {

    /**
     * The topic filter that caused the exception.
     *
     * @since 4.0.0
     */
    private final @NotNull String topicFilter;

    /**
     * Flag that if <b>true</b> will also fill in the stack trace for the exception.
     *
     * @since 4.0.0
     */
    private final boolean fillInStacktrace;

    /**
     * Creates a new InvalidTopicException.
     *
     * @param topicFilter      The invalid topic filter.
     * @param fillInStacktrace Whether the created exception should fill in a stacktrace.
     * @since 4.0.0
     */
    public InvalidTopicException(final @NotNull String topicFilter, final boolean fillInStacktrace) {
        this.topicFilter = topicFilter;
        this.fillInStacktrace = fillInStacktrace;
    }

    /**
     * Creates a new InvalidTopicException that will not contain a stacktrace.
     *
     * @param topicFilter The invalid topic filter.
     * @since 4.0.0
     */
    public InvalidTopicException(final @NotNull String topicFilter) {
        this(topicFilter, false);
    }

    @Override
    public synchronized @NotNull Throwable fillInStackTrace() {
        if (fillInStacktrace) {
            return super.fillInStackTrace();
        }
        return this;
    }

    /**
     * Returns the invalid topic filter.
     *
     * @return The invalid topic filter.
     * @since 4.0.0
     */
    public @NotNull String getTopicFilter() {
        return topicFilter;
    }
}
