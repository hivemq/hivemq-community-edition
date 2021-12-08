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
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.services.auth.WrappedAuthenticatorProvider;
import com.hivemq.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
abstract class AbstractAuthTask<I extends PluginTaskInput, O extends AuthOutput<?>> implements PluginInOutTask<I, O> {

    private static final Logger log = LoggerFactory.getLogger(AbstractAuthTask.class);

    final @NotNull WrappedAuthenticatorProvider wrappedAuthenticatorProvider;
    final @NotNull AuthenticatorProviderInput authenticatorProviderInput;
    final @NotNull String extensionId;

    AbstractAuthTask(
            final @NotNull WrappedAuthenticatorProvider wrappedAuthenticatorProvider,
            final @NotNull AuthenticatorProviderInput authenticatorProviderInput,
            final @NotNull String extensionId) {

        this.wrappedAuthenticatorProvider = wrappedAuthenticatorProvider;
        this.authenticatorProviderInput = authenticatorProviderInput;
        this.extensionId = extensionId;
    }

    @Override
    public @NotNull O apply(final @NotNull I input, final @NotNull O output) {
        if (output.getAuthenticationState().isFinal()) {
            return output;
        }
        try {
            call(input, output);
        } catch (final Throwable throwable) {
            output.failByThrowable(throwable);
            Exceptions.rethrowError(throwable);
            log.warn(
                    "Uncaught exception was thrown from extension with id \"{}\" in authenticator. " +
                            "Extensions are responsible for their own exception handling.", extensionId, throwable);
        }
        return output;
    }

    abstract void call(@NotNull I input, @NotNull O output);

    @Override
    public @NotNull ClassLoader getPluginClassLoader() {
        return wrappedAuthenticatorProvider.getClassLoader();
    }
}
