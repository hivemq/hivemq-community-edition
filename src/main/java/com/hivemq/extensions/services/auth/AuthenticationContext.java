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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Daniel Krüger
*/
public class AuthenticationContext {

    private final @NotNull AtomicInteger index;
    private @NotNull AuthenticationState authenticationState;

    public AuthenticationContext() {
        this.authenticationState = AuthenticationState.UNDECIDED;
        index = new AtomicInteger(0);
    }

    @NotNull
    AuthenticationState getAuthenticationState() {
        return authenticationState;
    }

    void setAuthenticationState(final @NotNull AuthenticationState authenticationState) {
        this.authenticationState = authenticationState;
    }

    public void increment() {
        index.incrementAndGet();
    }

    @NotNull
    public AtomicInteger getIndex() {
        return index;
    }
}