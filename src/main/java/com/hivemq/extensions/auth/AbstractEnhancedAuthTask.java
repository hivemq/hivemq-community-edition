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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extensions.client.ClientAuthenticators;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.services.auth.WrappedAuthenticatorProvider;

/**
 * @author Silvio Giebl
 */
abstract class AbstractEnhancedAuthTask<I extends PluginTaskInput, O extends AuthOutput<?>>
        extends AbstractAuthTask<I, O> {

    private final @NotNull ClientAuthenticators clientAuthenticators;

    AbstractEnhancedAuthTask(
            final @NotNull WrappedAuthenticatorProvider wrappedAuthenticatorProvider,
            final @NotNull AuthenticatorProviderInput authenticatorProviderInput,
            final @NotNull String extensionId,
            final @NotNull ClientAuthenticators clientAuthenticators) {

        super(wrappedAuthenticatorProvider, authenticatorProviderInput, extensionId);
        this.clientAuthenticators = clientAuthenticators;
    }

    @Override
    void call(final @NotNull I input, final @NotNull O output) {
        final EnhancedAuthenticator authenticator = updateAndGetAuthenticator();
        if (authenticator != null) {
            output.setAuthenticatorPresent();
            call(authenticator, input, output);
        }
    }

    abstract void call(@NotNull EnhancedAuthenticator authenticator, @NotNull I input, @NotNull O output);

    @Nullable EnhancedAuthenticator updateAndGetAuthenticator() {
        final EnhancedAuthenticator authenticator = clientAuthenticators.getAuthenticatorMap().get(extensionId);
        if ((authenticator != null) &&
                authenticator.getClass().getClassLoader().equals(wrappedAuthenticatorProvider.getClassLoader())) {
            return authenticator;
        }
        final EnhancedAuthenticator newAuthenticator =
                wrappedAuthenticatorProvider.getEnhancedAuthenticator(authenticatorProviderInput);
        if (newAuthenticator != null) {
            clientAuthenticators.put(extensionId, newAuthenticator);
            return newAuthenticator;
        }
        return null;
    }
}
