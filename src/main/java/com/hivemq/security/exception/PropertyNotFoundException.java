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
package com.hivemq.security.exception;

/**
 * A PropertyNotFoundException is an unchecked {@link RuntimeException}.
 * <p>
 * It should be thrown, if getting a ssl certificate property fails.
 *
 * @author Florian Limpöck
 */
public class PropertyNotFoundException extends RuntimeException {

    public PropertyNotFoundException(final String message) {
        super(message);
    }

    public PropertyNotFoundException(final Throwable throwable) {
        super(throwable);
    }

    public PropertyNotFoundException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    public PropertyNotFoundException(final String message,
                                     final Throwable cause,
                                     final boolean enableSuppression,
                                     final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
