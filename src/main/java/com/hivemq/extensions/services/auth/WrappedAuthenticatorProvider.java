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

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.Authenticator;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthenticatorProvider;
import com.hivemq.extension.sdk.api.services.auth.provider.EnhancedAuthenticatorProvider;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Georg Held
 */
public class WrappedAuthenticatorProvider {

    private static final Logger log = LoggerFactory.getLogger(WrappedAuthenticatorProvider.class);
    private static final String WRONG_CLASS_LOG_STATEMENT = "An extension provided an Authenticator instance of {} that was " +
            "neither an implementation of SimpleAuthenticator " +
            "nor EnhancedAuthenticator. The authenticator will be ignored.";
    private static final String UNCAUGHT_EXCEPTION_LOG_STATEMENT = "Uncaught exception was thrown in " +
            "AuthenticatorProvider from extension. Extensions are responsible on their own to handle exceptions.";

    @Nullable
    private final AuthenticatorProvider simpleAuthenticatorProvider;
    @Nullable
    private final EnhancedAuthenticatorProvider enhancedAuthenticatorProvider;
    @NotNull
    private final IsolatedPluginClassloader classLoader;

    private final AtomicBoolean checkThreading = new AtomicBoolean(true);

    public WrappedAuthenticatorProvider(@NotNull final AuthenticatorProvider simpleAuthenticatorProvider, @NotNull final IsolatedPluginClassloader classLoader) {
        this.simpleAuthenticatorProvider = simpleAuthenticatorProvider;
        this.classLoader = classLoader;
        this.enhancedAuthenticatorProvider = null;
    }

    public WrappedAuthenticatorProvider(@NotNull final EnhancedAuthenticatorProvider simpleAuthenticatorProvider, @NotNull final IsolatedPluginClassloader classLoader) {
        this.enhancedAuthenticatorProvider = simpleAuthenticatorProvider;
        this.classLoader = classLoader;
        this.simpleAuthenticatorProvider = null;
    }

    public @NotNull IsolatedPluginClassloader getClassLoader() {
        return classLoader;
    }

    @Nullable
    public SimpleAuthenticator getAuthenticator(@NotNull final AuthenticatorProviderInput authenticatorProviderInput) {

        if(enhancedAuthenticatorProvider != null){
            return null;
        }

        try {
            if(checkThreading.get()) {
                Preconditions.checkArgument(Thread.currentThread().getContextClassLoader() instanceof IsolatedPluginClassloader);
            }
            final Authenticator authenticator = Objects.requireNonNull(simpleAuthenticatorProvider).getAuthenticator(authenticatorProviderInput);

            if (authenticator == null) {
                return null;
            }

            if (authenticator instanceof SimpleAuthenticator) {
                return (SimpleAuthenticator) authenticator;
            }

            log.warn(WRONG_CLASS_LOG_STATEMENT, authenticator.getClass());
            return null;
        } catch (final Throwable throwable) {
            Exceptions.rethrowError(UNCAUGHT_EXCEPTION_LOG_STATEMENT, throwable);
            return null;
        }
    }

    @Nullable
    public EnhancedAuthenticator getEnhancedAuthenticator(@NotNull final AuthenticatorProviderInput authenticatorProviderInput) {

        if(enhancedAuthenticatorProvider == null){
            return null;
        }

        try {
            if(checkThreading.get()) {
                Preconditions.checkArgument(Thread.currentThread().getContextClassLoader() instanceof IsolatedPluginClassloader);
            }
            return Objects.requireNonNull(enhancedAuthenticatorProvider).getEnhancedAuthenticator(authenticatorProviderInput);
        } catch (final Throwable throwable) {
            Exceptions.rethrowError(UNCAUGHT_EXCEPTION_LOG_STATEMENT, throwable);
            return null;
        }
    }

    public boolean isEnhanced() {
        return enhancedAuthenticatorProvider != null;
    }

    public void setCheckThreading(final boolean check){
        checkThreading.set(check);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final WrappedAuthenticatorProvider that = (WrappedAuthenticatorProvider) o;
        return Objects.equals(classLoader, that.classLoader);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classLoader);
    }
}
