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
import com.hivemq.extension.sdk.api.services.auth.SecurityRegistry;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthenticatorProvider;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthorizerProvider;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Georg Held
 */
@Singleton
public class SecurityRegistryImpl implements SecurityRegistry {

    @NotNull
    private final Authenticators authenticators;

    @NotNull
    private final Authorizers authorizers;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    @Inject
    public SecurityRegistryImpl(
            @NotNull final Authenticators authenticators,
            @NotNull final Authorizers authorizers,
            @NotNull final HiveMQExtensions hiveMQExtensions) {
        this.authenticators = authenticators;
        this.authorizers = authorizers;
        this.hiveMQExtensions = hiveMQExtensions;
    }

    @Override
    public void setAuthenticatorProvider(@NotNull final AuthenticatorProvider authenticatorProvider) {
        checkNotNull(authenticatorProvider, "authenticatorProvider must not be null");

        final @NotNull IsolatedPluginClassloader classLoader =
                (IsolatedPluginClassloader) authenticatorProvider.getClass().getClassLoader();
        final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(classLoader);

        checkNotNull(plugin, "Extension classloader must be known, before an extension can add an AuthenticatorProvider.");

        final WrappedAuthenticatorProvider wrapped = new WrappedAuthenticatorProvider(authenticatorProvider, classLoader);
        authenticators.registerAuthenticatorProvider(wrapped);

    }

    @Override
    public void setAuthorizerProvider(@NotNull final AuthorizerProvider authorizerProvider) {
        checkNotNull(authorizerProvider, "authorizerProvider must not be null");
        authorizers.addAuthorizerProvider(authorizerProvider);
    }
}
