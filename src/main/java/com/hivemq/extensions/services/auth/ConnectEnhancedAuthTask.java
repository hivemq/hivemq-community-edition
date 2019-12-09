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
import com.hivemq.extensions.client.ClientAuthenticators;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.util.Exceptions;

/**
 * @author Daniel Krüger
 */

public class ConnectEnhancedAuthTask extends AbstractAuthTask implements PluginInOutTask<ConnectEnhancedAuthTaskInput, AuthTaskOutput> {

    public ConnectEnhancedAuthTask(final @NotNull WrappedAuthenticatorProvider wrappedAuthenticatorProvider,
                                   final @NotNull AuthenticatorProviderInput authenticatorProviderInput,
                                   final @NotNull String extensionId,
                                   final @NotNull ClientAuthenticators clientAuthenticators) {
        super(wrappedAuthenticatorProvider, authenticatorProviderInput, extensionId, clientAuthenticators);
    }

    @Override
    public @NotNull AuthTaskOutput apply(
            final @NotNull ConnectEnhancedAuthTaskInput connectAuthTaskInput,
            final @NotNull AuthTaskOutput connectAuthTaskOutput) {


        if (decided(connectAuthTaskOutput.getAuthenticationState())) {
            return connectAuthTaskOutput;
        }

        final EnhancedAuthenticator authenticator = super.updateAndGetAuthenticator();
        if(authenticator == null){
            return connectAuthTaskOutput;
        }

        connectAuthTaskOutput.authenticatorPresent();

        try {
            authenticator.onConnect(connectAuthTaskInput, connectAuthTaskOutput);
        } catch (final Throwable throwable) {
            connectAuthTaskOutput.setThrowable(throwable);
            Exceptions.rethrowError(LOG_STATEMENT, throwable);
        }
        return connectAuthTaskOutput;
    }
}
