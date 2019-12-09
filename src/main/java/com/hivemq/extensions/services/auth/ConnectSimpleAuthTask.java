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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.util.Exceptions;

/**
 * @author Georg Held
 * @author Florian Limpöck
 */
public class ConnectSimpleAuthTask implements PluginInOutTask<ConnectSimpleAuthTaskInput, ConnectSimpleAuthTaskOutput> {

    private static final String LOG_STATEMENT = "Uncaught exception was thrown in SimpleAuthenticator from extension." +
            " Extensions are responsible on their own to handle exceptions.";

    private final @NotNull WrappedAuthenticatorProvider wrappedAuthenticatorProvider;
    private final @NotNull AuthenticatorProviderInput authenticatorProviderInput;

    public ConnectSimpleAuthTask(final @NotNull WrappedAuthenticatorProvider provider, final @NotNull AuthenticatorProviderInput authenticatorProviderInput) {
        this.wrappedAuthenticatorProvider = provider;
        this.authenticatorProviderInput = authenticatorProviderInput;
    }

    @Override
    public @NotNull ConnectSimpleAuthTaskOutput apply(
            final @NotNull ConnectSimpleAuthTaskInput connectSimpleAuthTaskInput,
            final @NotNull ConnectSimpleAuthTaskOutput connectSimpleAuthTaskOutput) {

        if (connectSimpleAuthTaskOutput.getAuthenticationState() != AuthenticationState.UNDECIDED &&
                connectSimpleAuthTaskOutput.getAuthenticationState() != AuthenticationState.NEXT_EXTENSION_OR_DEFAULT) {
            return connectSimpleAuthTaskOutput;
        }
        try {
            final SimpleAuthenticator authenticator = wrappedAuthenticatorProvider.getAuthenticator(authenticatorProviderInput);
            if (authenticator != null) {
                authenticator.onConnect(connectSimpleAuthTaskInput, connectSimpleAuthTaskOutput);
                connectSimpleAuthTaskOutput.authenticatorPresent();
            }
        } catch (final Throwable throwable) {
            Exceptions.rethrowError(LOG_STATEMENT, throwable);
            connectSimpleAuthTaskOutput.setThrowable(throwable);
        }
        return connectSimpleAuthTaskOutput;
    }

    @Override
    public @NotNull ClassLoader getPluginClassLoader() {
        return wrappedAuthenticatorProvider.getClassLoader();
    }
}
