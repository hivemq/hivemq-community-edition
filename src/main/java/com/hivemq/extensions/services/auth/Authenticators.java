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
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthenticatorProvider;
import com.hivemq.extension.sdk.api.services.auth.provider.EnhancedAuthenticatorProvider;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;

import java.util.Map;

/**
 * Internal interface for holding the {@link AuthenticatorProvider}s from
 * the extension system.
 *
 * @author Georg Held
 */
@ThreadSafe
public interface Authenticators {

    @NotNull Map<@NotNull String, @NotNull WrappedAuthenticatorProvider> getAuthenticatorProviderMap();

    /**
     * Register a {@link WrappedAuthenticatorProvider}. Will replace an other WrappedAuthenticatorProvider with the same
     * {@link IsolatedPluginClassloader}.
     *
     * @param provider a wrapped {@link AuthenticatorProvider}
     */
    void registerAuthenticatorProvider(@NotNull WrappedAuthenticatorProvider provider);

    /**
     * @return true if any {@link AuthenticatorProvider} have been
     * registered, false otherwise.
     */
    boolean areAuthenticatorsAvailable();

    /**
     * checks if at least one {@link EnhancedAuthenticatorProvider} is registered.
     *
     * @return true if at least one is registered, false otherwise.
     */
    boolean isEnhancedAvailable();

    /**
     * @return the amount of registered {@link EnhancedAuthenticator}
     */
    int getEnhancedAuthenticatorCount();

    /**
     * @return the amount of registered {@link SimpleAuthenticator}
     */
    int getSimpleAuthenticatorCount();
}
