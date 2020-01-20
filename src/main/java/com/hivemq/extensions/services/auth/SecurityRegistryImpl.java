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
import com.hivemq.extension.sdk.api.services.auth.SecurityRegistry;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthenticatorProvider;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthorizerProvider;
import com.hivemq.extension.sdk.api.services.auth.provider.EnhancedAuthenticatorProvider;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Georg Held
 */
@Singleton
public class SecurityRegistryImpl implements SecurityRegistry {

    private final @NotNull Authenticators authenticators;
    private final @NotNull Authorizers authorizers;

    @Inject
    public SecurityRegistryImpl(final @NotNull Authenticators authenticators, final @NotNull Authorizers authorizers) {
        this.authenticators = authenticators;
        this.authorizers = authorizers;
    }

    @Override
    public void setAuthenticatorProvider(final @NotNull AuthenticatorProvider authenticatorProvider) {
        checkNotNull(authenticatorProvider, "authenticatorProvider must not be null");

        final IsolatedPluginClassloader classLoader =
                (IsolatedPluginClassloader) authenticatorProvider.getClass().getClassLoader();

        final WrappedAuthenticatorProvider wrapped =
                new WrappedAuthenticatorProvider(authenticatorProvider, classLoader);
        authenticators.registerAuthenticatorProvider(wrapped);
    }

    @Override
    public void setEnhancedAuthenticatorProvider(
            final @NotNull EnhancedAuthenticatorProvider enhancedAuthenticatorProvider) {

        checkNotNull(enhancedAuthenticatorProvider, "enhancedAuthenticatorProvider must not be null");

        final IsolatedPluginClassloader classLoader =
                (IsolatedPluginClassloader) enhancedAuthenticatorProvider.getClass().getClassLoader();

        final WrappedAuthenticatorProvider wrapped =
                new WrappedAuthenticatorProvider(enhancedAuthenticatorProvider, classLoader);
        authenticators.registerAuthenticatorProvider(wrapped);
    }

    @Override
    public void setAuthorizerProvider(final @NotNull AuthorizerProvider authorizerProvider) {
        checkNotNull(authorizerProvider, "authorizerProvider must not be null");
        authorizers.addAuthorizerProvider(authorizerProvider);
    }
}
