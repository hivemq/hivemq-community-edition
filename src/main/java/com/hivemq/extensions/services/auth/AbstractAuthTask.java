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
import com.hivemq.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extensions.client.ClientAuthenticators;

/**
 * @author Florian Limpöck
*/
public abstract class AbstractAuthTask {

    static final String LOG_STATEMENT = "Uncaught exception was thrown in EnhancedAuthenticator from extension." +
            " Extensions are responsible on their own to handle exceptions.";

    private final @NotNull WrappedAuthenticatorProvider wrappedAuthenticatorProvider;
    private final @NotNull AuthenticatorProviderInput authenticatorProviderInput;
    private final @NotNull String extensionId;
    private final @NotNull ClientAuthenticators clientAuthenticators;

    AbstractAuthTask(final @NotNull WrappedAuthenticatorProvider wrappedAuthenticatorProvider,
                     final @NotNull AuthenticatorProviderInput authenticatorProviderInput,
                     final @NotNull String extensionId,
                     final @NotNull ClientAuthenticators clientAuthenticators) {
        this.wrappedAuthenticatorProvider = wrappedAuthenticatorProvider;
        this.authenticatorProviderInput = authenticatorProviderInput;
        this.extensionId = extensionId;
        this.clientAuthenticators = clientAuthenticators;
    }

    boolean decided(final @NotNull AuthenticationState currentState) {
        return currentState != AuthenticationState.NEXT_EXTENSION_OR_DEFAULT &&
                currentState != AuthenticationState.UNDECIDED;
    }

    @Nullable EnhancedAuthenticator updateAndGetAuthenticator() {

        final EnhancedAuthenticator authenticatorFromClient = clientAuthenticators.getAuthenticatorMap().get(extensionId);
        if (authenticatorFromClient != null && authenticatorFromClient.getClass().getClassLoader().equals(wrappedAuthenticatorProvider.getClassLoader())) {
            return authenticatorFromClient;
        }
        final EnhancedAuthenticator authenticatorProvided = wrappedAuthenticatorProvider.getEnhancedAuthenticator(authenticatorProviderInput);
        if (authenticatorProvided != null) {
            clientAuthenticators.put(extensionId, authenticatorProvided);
            return authenticatorProvided;
        }
        return null;
    }

    public @NotNull ClassLoader getPluginClassLoader() {
        return wrappedAuthenticatorProvider.getClassLoader();
    }

}
