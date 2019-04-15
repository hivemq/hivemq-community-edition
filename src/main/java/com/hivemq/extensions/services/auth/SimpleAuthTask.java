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
import com.hivemq.extension.sdk.api.auth.Authenticator;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.util.Exceptions;

/**
 * @author Georg Held
 */
public class SimpleAuthTask implements PluginInOutTask<ConnectAuthTaskInput, ConnectAuthTaskOutput> {

    private static final String LOG_STATEMENT = "Uncaught exception was thrown in SimpleAuthenticator from extension." +
            " Extensions are responsible on their own to handle exceptions.";

    private final @NotNull WrappedAuthenticatorProvider wrappedAuthenticatorProvider;
    private final @NotNull AuthenticatorProviderInput authenticatorProviderInput;

    public SimpleAuthTask(final @NotNull WrappedAuthenticatorProvider provider, final @NotNull AuthenticatorProviderInput authenticatorProviderInput) {
        this.wrappedAuthenticatorProvider = provider;
        this.authenticatorProviderInput = authenticatorProviderInput;
    }

    @Override
    public @NotNull ConnectAuthTaskOutput apply(
            final @NotNull ConnectAuthTaskInput connectAuthTaskInput,
            final @NotNull ConnectAuthTaskOutput connectAuthTaskOutput) {

        if (connectAuthTaskOutput.getAuthenticationState() != ConnectAuthTaskOutput.AuthenticationState.UNDECIDED &&
                connectAuthTaskOutput.getAuthenticationState() != ConnectAuthTaskOutput.AuthenticationState.CONTINUE) {
            return connectAuthTaskOutput;
        }
        try {
            final Authenticator authenticator = wrappedAuthenticatorProvider.getAuthenticator(authenticatorProviderInput);
            if (authenticator instanceof SimpleAuthenticator) {
                ((SimpleAuthenticator) authenticator).onConnect(connectAuthTaskInput, connectAuthTaskOutput);
                connectAuthTaskOutput.authenticatorPresent();
            }
        } catch (final Throwable throwable) {
            Exceptions.rethrowError(LOG_STATEMENT, throwable);
            connectAuthTaskOutput.setThrowable(throwable);
        }
        return connectAuthTaskOutput;
    }
}
