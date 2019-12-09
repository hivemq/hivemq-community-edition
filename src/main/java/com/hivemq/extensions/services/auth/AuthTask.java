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
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extension.sdk.api.packets.auth.AuthReasonCode;
import com.hivemq.extensions.client.ClientAuthenticators;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.util.Exceptions;

/**
 * @author Daniel Krüger
 */
public class AuthTask extends AbstractAuthTask implements PluginInOutTask<AuthTaskInput, AuthTaskOutput> {

    private static final String LOG_STATEMENT = "Uncaught exception was thrown in EnhancedAuthenticator from extension." +
            " Extensions are responsible on their own to handle exceptions.";

    public AuthTask(final @NotNull WrappedAuthenticatorProvider wrappedAuthenticatorProvider,
                    final @NotNull AuthenticatorProviderInput authenticatorProviderInput,
                    final @NotNull String extensionId,
                    final @NotNull ClientAuthenticators clientAuthenticators) {
        super(wrappedAuthenticatorProvider, authenticatorProviderInput, extensionId, clientAuthenticators);
    }

    @Override
    public @NotNull AuthTaskOutput apply(
            final @NotNull AuthTaskInput authTaskInput,
            final @NotNull AuthTaskOutput authTaskOutput) {

        if (decided(authTaskOutput.getAuthenticationState())) {
            return authTaskOutput;
        }

        final EnhancedAuthenticator authenticator = super.updateAndGetAuthenticator();
        if (authenticator == null) {
            return authTaskOutput;
        }

        authTaskOutput.authenticatorPresent();

        try {
            if (authTaskInput.getAuthPacket().getReasonCode() == AuthReasonCode.REAUTHENTICATE) {
                authenticator.onReAuth(authTaskInput, authTaskOutput);
            } else {
                authenticator.onAuth(authTaskInput, authTaskOutput);
            }
        } catch (final Throwable throwable) {
            authTaskOutput.setThrowable(throwable);
            Exceptions.rethrowError(LOG_STATEMENT, throwable);
        }
        return authTaskOutput;
    }
}