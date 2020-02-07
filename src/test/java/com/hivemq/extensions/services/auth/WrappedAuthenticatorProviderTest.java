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

import com.hivemq.extension.sdk.api.auth.Authenticator;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthenticatorProvider;
import com.hivemq.extension.sdk.api.services.auth.provider.EnhancedAuthenticatorProvider;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URL;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Georg Held
 */
@SuppressWarnings("NullabilityAnnotations")
public class WrappedAuthenticatorProviderTest {

    @Mock
    private EnhancedAuthenticator enhancedAuthenticator;

    @Mock
    private SimpleAuthenticator simpleAuthenticator;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatorProviderInput input;
    private IsolatedPluginClassloader isolatedPluginClassloader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        isolatedPluginClassloader = new IsolatedPluginClassloader(new URL[]{}, Thread.currentThread().getContextClassLoader());
    }

    @Test(timeout = 5000)
    public void test_null_provider_returns_null() {
        final WrappedAuthenticatorProvider wrapped = new WrappedAuthenticatorProvider((AuthenticatorProvider) i -> null, isolatedPluginClassloader);
        wrapped.setCheckThreading(false);
        assertNull(wrapped.getAuthenticator(input));
    }

    @Test(timeout = 5000)
    public void test_simple_provider_returns_simple() {
        final WrappedAuthenticatorProvider wrappedAuthenticatorProvider = new WrappedAuthenticatorProvider((AuthenticatorProvider) i -> simpleAuthenticator, isolatedPluginClassloader);
        wrappedAuthenticatorProvider.setCheckThreading(false);
        assertSame(simpleAuthenticator, wrappedAuthenticatorProvider.getAuthenticator(input));
        assertNull(wrappedAuthenticatorProvider.getEnhancedAuthenticator(input));
    }

    @Test(timeout = 5000)
    public void test_other_provider_returns_null() {
        final WrappedAuthenticatorProvider wrappedAuthenticatorProvider = new WrappedAuthenticatorProvider((AuthenticatorProvider) i -> authenticator, isolatedPluginClassloader);
        wrappedAuthenticatorProvider.setCheckThreading(false);
        assertNull(wrappedAuthenticatorProvider.getAuthenticator(input));
    }

    @Test(timeout = 5000)
    public void test_enhanced_null_provider_returns_null() {
        final WrappedAuthenticatorProvider wrapped = new WrappedAuthenticatorProvider((EnhancedAuthenticatorProvider) i -> null, isolatedPluginClassloader);
        wrapped.setCheckThreading(false);
        assertNull(wrapped.getEnhancedAuthenticator(input));
    }

    @Test(timeout = 5000)
    public void test_enhanced_provider_returns_simple() {
        final WrappedAuthenticatorProvider wrappedAuthenticatorProvider = new WrappedAuthenticatorProvider((EnhancedAuthenticatorProvider) i -> enhancedAuthenticator, isolatedPluginClassloader);
        wrappedAuthenticatorProvider.setCheckThreading(false);
        assertSame(enhancedAuthenticator, wrappedAuthenticatorProvider.getEnhancedAuthenticator(input));
        assertNull(wrappedAuthenticatorProvider.getAuthenticator(input));
    }

    @Test(timeout = 5000)
    public void test_enhanced_provider_bad_threading() {
        final WrappedAuthenticatorProvider wrappedAuthenticatorProvider = new WrappedAuthenticatorProvider((EnhancedAuthenticatorProvider) i -> enhancedAuthenticator, isolatedPluginClassloader);
        wrappedAuthenticatorProvider.setCheckThreading(true);
        assertNull(wrappedAuthenticatorProvider.getEnhancedAuthenticator(input));
    }

    @Test(timeout = 5000)
    public void test_simple_provider_bad_threading() {
        final WrappedAuthenticatorProvider wrappedAuthenticatorProvider = new WrappedAuthenticatorProvider((AuthenticatorProvider) i -> simpleAuthenticator, isolatedPluginClassloader);
        wrappedAuthenticatorProvider.setCheckThreading(true);
        assertNull(wrappedAuthenticatorProvider.getAuthenticator(input));
    }

}