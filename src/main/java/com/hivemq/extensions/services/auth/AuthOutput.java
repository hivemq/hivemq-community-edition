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

package com.hivemq.extensions.services.auth;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;

/**
 * @author Florian Limpöck
*/
public interface AuthOutput {

    /**
     * @return is the output timed out.
     */
    boolean isTimedOut();

    /**
     * @return is the output object async.
     */
    boolean isAsync();

    /**
     * @return the fallback for timeouts.
     */
    @NotNull
    TimeoutFallback getTimeoutFallback();

    /**
     * @return the current state of the output.
     */
    @NotNull
    AuthenticationState getAuthenticationState();

    /**
     * @return is an authenticator present and used?
     */
    boolean isAuthenticatorPresent();

    /**
     * let the authentication fail.
     */
    void failAuthentication();

    /**
     * let the authentication fail by timeout.
     */
    void failByTimeout();

    /**
     * let the authentication be decided by next extension or default config.
     */
    void nextByTimeout();

}
