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
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.Authenticator;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.*;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthenticatorProvider;
import com.hivemq.extension.sdk.api.services.auth.provider.EnhancedAuthenticatorProvider;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.IsolatedExtensionClassloaderUtil;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecurityRegistryImplTest {

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull AuthenticatorProviderInput input = mock(AuthenticatorProviderInput.class);

    private @NotNull Authenticators authenticators;
    private @NotNull SecurityRegistryImpl securityRegistry;

    private @NotNull AuthenticatorProvider provider1;
    private @NotNull AuthenticatorProvider provider2;
    private @NotNull Authenticator authenticator1;
    private @NotNull Authenticator authenticator2;

    private @NotNull EnhancedAuthenticatorProvider enhancedProvider1;
    private @NotNull EnhancedAuthenticatorProvider enhancedProvider2;
    private @NotNull EnhancedAuthenticator enhancedAuthenticator1;
    private @NotNull EnhancedAuthenticator enhancedAuthenticator2;

    @Before
    public void setUp() throws Exception {
        try (final IsolatedExtensionClassloader cl = IsolatedExtensionClassloaderUtil.buildClassLoader(temporaryFolder.getRoot()
                .toPath(), new Class[]{
                TestProvider1.class, TestProvider2.class, TestSimpleAuthenticator.class, EnhancedTestProvider1.class,
                EnhancedTestProvider2.class, TestEnhancedAuthenticator.class
        })) {
            final HiveMQExtension hiveMQExtension = mock(HiveMQExtension.class);
            when(hiveMQExtension.getId()).thenReturn("extension");
            when(hiveMQExtension.getPriority()).thenReturn(1);

            final HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
            when(hiveMQExtensions.getExtensionForClassloader(any(ClassLoader.class))).thenReturn(hiveMQExtension);
            when(hiveMQExtensions.getExtension(anyString())).thenReturn(hiveMQExtension);
            when(hiveMQExtension.getExtensionClassloader()).thenReturn(cl);

            final Authorizers authorizers = new AuthorizersImpl(hiveMQExtensions);
            authenticators = new AuthenticatorsImpl(hiveMQExtensions);
            securityRegistry = new SecurityRegistryImpl(authenticators, authorizers, hiveMQExtensions);

            provider1 = IsolatedExtensionClassloaderUtil.loadInstance(cl, TestProvider1.class);
            provider2 = IsolatedExtensionClassloaderUtil.loadInstance(cl, TestProvider2.class);
            final Authenticator auth1 = provider1.getAuthenticator(input);
            assertNotNull(auth1);
            authenticator1 = auth1;
            final Authenticator auth2 = provider2.getAuthenticator(input);
            assertNotNull(auth2);
            authenticator2 = auth2;

            enhancedProvider1 = IsolatedExtensionClassloaderUtil.loadInstance(cl, EnhancedTestProvider1.class);
            enhancedProvider2 = IsolatedExtensionClassloaderUtil.loadInstance(cl, EnhancedTestProvider2.class);
            final EnhancedAuthenticator enhancedAuth1 = enhancedProvider1.getEnhancedAuthenticator(input);
            assertNotNull(enhancedAuth1);
            enhancedAuthenticator1 = enhancedAuth1;
            final EnhancedAuthenticator enhancedAuth2 = enhancedProvider2.getEnhancedAuthenticator(input);
            assertNotNull(enhancedAuth2);
            enhancedAuthenticator2 = enhancedAuth2;
        }
    }

    @Test(timeout = 5000)
    public void test_set_authenticator_provider() {
        securityRegistry.setAuthenticatorProvider(provider1);

        final Map<String, WrappedAuthenticatorProvider> registeredAuthenticators =
                authenticators.getAuthenticatorProviderMap();

        assertEquals(1, registeredAuthenticators.size());
        assertSame(authenticator1, registeredAuthenticators.values().iterator().next().getAuthenticator(input));
    }

    @Test(timeout = 5000)
    public void test_set_second_authenticator_provider_from_same_classloader() {
        securityRegistry.setAuthenticatorProvider(provider1);
        Map<String, WrappedAuthenticatorProvider> registeredAuthenticators =
                authenticators.getAuthenticatorProviderMap();
        assertEquals(1, registeredAuthenticators.size());
        assertSame(authenticator1, registeredAuthenticators.values().iterator().next().getAuthenticator(input));

        // replace authenticator
        securityRegistry.setAuthenticatorProvider(provider2);
        registeredAuthenticators = authenticators.getAuthenticatorProviderMap();
        assertEquals(1, registeredAuthenticators.size());
        assertSame(authenticator2, registeredAuthenticators.values().iterator().next().getAuthenticator(input));

    }

    @Test(timeout = 5000)
    public void test_set_enhanced_authenticator_provider() {
        securityRegistry.setEnhancedAuthenticatorProvider(enhancedProvider1);

        final Map<String, WrappedAuthenticatorProvider> registeredAuthenticators =
                authenticators.getAuthenticatorProviderMap();

        assertEquals(1, registeredAuthenticators.size());
        assertSame(enhancedAuthenticator1,
                registeredAuthenticators.values().iterator().next().getEnhancedAuthenticator(input));
    }

    @Test(timeout = 5000)
    public void test_set_second_enhanced_authenticator_provider_from_same_classloader() {
        securityRegistry.setEnhancedAuthenticatorProvider(enhancedProvider1);
        Map<String, WrappedAuthenticatorProvider> registeredAuthenticators =
                authenticators.getAuthenticatorProviderMap();
        assertEquals(1, registeredAuthenticators.size());
        assertSame(enhancedAuthenticator1,
                registeredAuthenticators.values().iterator().next().getEnhancedAuthenticator(input));

        // replace authenticator
        securityRegistry.setEnhancedAuthenticatorProvider(enhancedProvider2);
        registeredAuthenticators = authenticators.getAuthenticatorProviderMap();
        assertEquals(1, registeredAuthenticators.size());
        assertSame(enhancedAuthenticator2,
                registeredAuthenticators.values().iterator().next().getEnhancedAuthenticator(input));

    }

    @Test(timeout = 5000, expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_set_null_authenticator_provider() {
        securityRegistry.setAuthenticatorProvider(null);
    }

    public static class TestProvider1 implements AuthenticatorProvider {

        private final @NotNull Authenticator authenticator;

        public TestProvider1() {
            authenticator = new TestSimpleAuthenticator();
        }

        @Override
        public @Nullable Authenticator getAuthenticator(
                final @NotNull AuthenticatorProviderInput authenticatorProviderInput) {
            return authenticator;
        }
    }

    public static class TestProvider2 implements AuthenticatorProvider {

        private final @NotNull Authenticator authenticator;

        public TestProvider2() {
            authenticator = new TestSimpleAuthenticator();
        }

        @Override
        public @Nullable Authenticator getAuthenticator(
                final @NotNull AuthenticatorProviderInput authenticatorProviderInput) {
            return authenticator;
        }
    }

    public static class EnhancedTestProvider1 implements EnhancedAuthenticatorProvider {

        private final @NotNull EnhancedAuthenticator authenticator;

        public EnhancedTestProvider1() {
            authenticator = new TestEnhancedAuthenticator();
        }

        @Override
        public @Nullable EnhancedAuthenticator getEnhancedAuthenticator(
                final @NotNull AuthenticatorProviderInput authenticatorProviderInput) {
            return authenticator;
        }
    }

    public static class EnhancedTestProvider2 implements EnhancedAuthenticatorProvider {

        private final @NotNull EnhancedAuthenticator authenticator;

        public EnhancedTestProvider2() {
            authenticator = new TestEnhancedAuthenticator();
        }

        @Override
        public @Nullable EnhancedAuthenticator getEnhancedAuthenticator(
                final @NotNull AuthenticatorProviderInput authenticatorProviderInput) {
            return authenticator;
        }
    }

    public static class TestSimpleAuthenticator implements SimpleAuthenticator {

        @Override
        public void onConnect(
                final @NotNull SimpleAuthInput input, final @NotNull SimpleAuthOutput output) {
        }
    }

    public static class TestEnhancedAuthenticator implements EnhancedAuthenticator {

        @Override
        public void onConnect(final @NotNull EnhancedAuthConnectInput input, final @NotNull EnhancedAuthOutput output) {
        }

        @Override
        public void onAuth(final @NotNull EnhancedAuthInput input, final @NotNull EnhancedAuthOutput output) {
        }
    }
}
