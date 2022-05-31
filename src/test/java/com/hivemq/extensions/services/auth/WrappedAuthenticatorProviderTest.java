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

package com.hivemq.extensions.services.auth;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.Authenticator;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthenticatorProvider;
import com.hivemq.extension.sdk.api.services.auth.provider.EnhancedAuthenticatorProvider;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import org.junit.Test;
import util.IsolatedExtensionClassloaderUtil;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

public class WrappedAuthenticatorProviderTest {

    private final @NotNull EnhancedAuthenticator enhancedAuthenticator = mock(EnhancedAuthenticator.class);
    private final @NotNull SimpleAuthenticator simpleAuthenticator = mock(SimpleAuthenticator.class);
    private final @NotNull Authenticator authenticator = mock(Authenticator.class);
    private final @NotNull AuthenticatorProviderInput input = mock(AuthenticatorProviderInput.class);

    private final @NotNull IsolatedExtensionClassloader isolatedExtensionClassloader =
            IsolatedExtensionClassloaderUtil.buildClassLoader();

    @Test(timeout = 5000)
    public void test_null_provider_returns_null() {
        final WrappedAuthenticatorProvider wrapped =
                new WrappedAuthenticatorProvider((AuthenticatorProvider) i -> null, isolatedExtensionClassloader);
        assertNull(wrapped.getAuthenticator(input));
    }

    @Test(timeout = 5000)
    public void test_simple_provider_returns_simple() {
        final WrappedAuthenticatorProvider wrappedAuthenticatorProvider =
                new WrappedAuthenticatorProvider((AuthenticatorProvider) i -> simpleAuthenticator,
                        isolatedExtensionClassloader);
        assertSame(simpleAuthenticator, wrappedAuthenticatorProvider.getAuthenticator(input));
        assertNull(wrappedAuthenticatorProvider.getEnhancedAuthenticator(input));
    }

    @Test(timeout = 5000)
    public void test_other_provider_returns_null() {
        final WrappedAuthenticatorProvider wrappedAuthenticatorProvider =
                new WrappedAuthenticatorProvider((AuthenticatorProvider) i -> authenticator,
                        isolatedExtensionClassloader);
        assertNull(wrappedAuthenticatorProvider.getAuthenticator(input));
    }

    @Test(timeout = 5000)
    public void test_enhanced_null_provider_returns_null() {
        final WrappedAuthenticatorProvider wrapped =
                new WrappedAuthenticatorProvider((EnhancedAuthenticatorProvider) i -> null,
                        isolatedExtensionClassloader);
        assertNull(wrapped.getEnhancedAuthenticator(input));
    }

    @Test(timeout = 5000)
    public void test_enhanced_provider_returns_simple() {
        final WrappedAuthenticatorProvider wrappedAuthenticatorProvider =
                new WrappedAuthenticatorProvider((EnhancedAuthenticatorProvider) i -> enhancedAuthenticator,
                        isolatedExtensionClassloader);
        assertSame(enhancedAuthenticator, wrappedAuthenticatorProvider.getEnhancedAuthenticator(input));
        assertNull(wrappedAuthenticatorProvider.getAuthenticator(input));
    }
}
