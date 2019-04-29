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

import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Georg Held
 */
@SuppressWarnings("NullabilityAnnotations")
public class AuthenticatorsImplTest {

    @Mock
    private SimpleAuthenticator simpleAuthenticator1;
    @Mock
    private SimpleAuthenticator simpleAuthenticator2;
    @Mock
    private AuthenticatorProviderInput authenticatorProviderInput;

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private HiveMQExtension extension1;

    @Mock
    private HiveMQExtension extension2;

    private WrappedAuthenticatorProvider simpleProvider1;
    private WrappedAuthenticatorProvider simpleProvider2;

    private AuthenticatorsImpl authenticators;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);


        final IsolatedPluginClassloader isolatedPluginClassloader1 = new IsolatedPluginClassloader(new URL[]{}, Thread.currentThread().getContextClassLoader());
        final IsolatedPluginClassloader isolatedPluginClassloader2 = new IsolatedPluginClassloader(new URL[]{}, Thread.currentThread().getContextClassLoader());

        when(hiveMQExtensions.getExtensionForClassloader(isolatedPluginClassloader1)).thenReturn(extension1);
        when(hiveMQExtensions.getExtensionForClassloader(isolatedPluginClassloader2)).thenReturn(extension2);

        when(hiveMQExtensions.getExtension("extension1")).thenReturn(extension1);
        when(hiveMQExtensions.getExtension("extension2")).thenReturn(extension2);

        when(extension1.getPriority()).thenReturn(11);
        when(extension2.getPriority()).thenReturn(10);

        when(extension1.getId()).thenReturn("extension1");
        when(extension2.getId()).thenReturn("extension2");

        simpleProvider1 = new WrappedAuthenticatorProvider((i) -> simpleAuthenticator1, isolatedPluginClassloader1);
        simpleProvider2 = new WrappedAuthenticatorProvider((i) -> simpleAuthenticator2, isolatedPluginClassloader2);
        authenticators = new AuthenticatorsImpl(hiveMQExtensions);
        authenticators.registerAuthenticatorProvider(simpleProvider1);
        authenticators.registerAuthenticatorProvider(simpleProvider2);
    }

    @Test(timeout = 5000)
    public void test_registered_authenticators_are_ordered() {

        final Map<String, WrappedAuthenticatorProvider> registeredAuthenticators = authenticators.getAuthenticatorProviderMap();

        assertEquals(2, registeredAuthenticators.size());

        final Iterator<WrappedAuthenticatorProvider> iterator = registeredAuthenticators.values().iterator();

        assertSame(simpleAuthenticator1, iterator.next().getAuthenticator(null));
        assertSame(simpleAuthenticator2, iterator.next().getAuthenticator(null));
    }

    @Test(timeout = 5000)
    public void test_are_authenticators_available_shows_correct_state() {
        assertTrue(authenticators.areAuthenticatorsAvailable());
    }

}