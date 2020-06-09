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
package com.hivemq.extensions.auth;

/**
 * State of the authentication set by any extension for CONNECT and AUTH.
 *
 * @author Daniel Krüger
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
enum AuthenticationState {
    /**
     * No extension has set any outcome.
     */
    UNDECIDED,

    /**
     * An extension has set the outcome to successful.
     */
    SUCCESS,

    /**
     * An extension has set the outcome to failed.
     */
    FAILED,

    /**
     * An extension has set the outcome to continue.
     */
    CONTINUE,

    /**
     * An extension has set the outcome to next extension or default.
     */
    NEXT_EXTENSION_OR_DEFAULT;

    boolean isFinal() {
        return (this != UNDECIDED) && (this != NEXT_EXTENSION_OR_DEFAULT);
    }
}