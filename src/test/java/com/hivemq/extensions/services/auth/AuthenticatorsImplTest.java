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
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthenticatorProvider;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import org.junit.Before;
import org.junit.Test;
import util.IsolatedExtensionClassloaderUtil;

import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthenticatorsImplTest {

    private final @NotNull SimpleAuthenticator simpleAuthenticator1 = mock(SimpleAuthenticator.class);
    private final @NotNull SimpleAuthenticator simpleAuthenticator2 = mock(SimpleAuthenticator.class);
    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull HiveMQExtension extension1 = mock(HiveMQExtension.class);
    private final @NotNull HiveMQExtension extension2 = mock(HiveMQExtension.class);

    private @NotNull AuthenticatorsImpl authenticators;

    @Before
    public void setUp() {
        final IsolatedExtensionClassloader isolatedExtensionClassloader1 =
                IsolatedExtensionClassloaderUtil.buildClassLoader();
        final IsolatedExtensionClassloader isolatedExtensionClassloader2 =
                IsolatedExtensionClassloaderUtil.buildClassLoader();

        when(hiveMQExtensions.getExtensionForClassloader(isolatedExtensionClassloader1)).thenReturn(extension1);
        when(hiveMQExtensions.getExtensionForClassloader(isolatedExtensionClassloader2)).thenReturn(extension2);

        when(hiveMQExtensions.getExtension("extension1")).thenReturn(extension1);
        when(hiveMQExtensions.getExtension("extension2")).thenReturn(extension2);

        when(extension1.getPriority()).thenReturn(11);
        when(extension2.getPriority()).thenReturn(10);

        when(extension1.getId()).thenReturn("extension1");
        when(extension2.getId()).thenReturn("extension2");

        final WrappedAuthenticatorProvider simpleProvider1 =
                new WrappedAuthenticatorProvider((AuthenticatorProvider) i -> simpleAuthenticator1,
                        isolatedExtensionClassloader1);
        final WrappedAuthenticatorProvider simpleProvider2 =
                new WrappedAuthenticatorProvider((AuthenticatorProvider) i -> simpleAuthenticator2,
                        isolatedExtensionClassloader2);

        authenticators = new AuthenticatorsImpl(hiveMQExtensions);
        authenticators.registerAuthenticatorProvider(simpleProvider1);
        authenticators.registerAuthenticatorProvider(simpleProvider2);
    }

    @Test(timeout = 5000)
    public void test_registered_authenticators_are_ordered() {
        final Map<String, WrappedAuthenticatorProvider> registeredAuthenticators =
                authenticators.getAuthenticatorProviderMap();

        assertEquals(2, registeredAuthenticators.size());

        final Iterator<WrappedAuthenticatorProvider> iterator = registeredAuthenticators.values().iterator();

        assertSame(simpleAuthenticator1, iterator.next().getAuthenticator(mock(AuthenticatorProviderInput.class)));
        assertSame(simpleAuthenticator2, iterator.next().getAuthenticator(mock(AuthenticatorProviderInput.class)));
    }
}
