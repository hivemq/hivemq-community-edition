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

import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.PluginPriorityComparator;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientAuthenticatorsImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static com.hivemq.extensions.services.auth.AuthenticationState.NEXT_EXTENSION_OR_DEFAULT;
import static com.hivemq.extensions.services.auth.AuthenticationState.SUCCESS;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

/**
 * @author Florian Limpöck
*/
public class ConnectEnhancedAuthTaskTest {

    @Mock
    private WrappedAuthenticatorProvider wrappedAuthenticatorProvider;

    @Mock
    private EnhancedAuthenticator enhancedAuthenticator;

    @Mock
    private ConnectEnhancedAuthTaskInput connectEnhancedAuthTaskInput;

    @Mock
    private AuthenticatorProviderInput authenticatorProviderInput;

    @Mock
    private AuthTaskOutput connectEnhancedAuthTaskOutput;

    @Mock
    private HiveMQExtensions extensions;

    private ConnectEnhancedAuthTask connectEnhancedAuthTask;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(connectEnhancedAuthTaskOutput.getAuthenticationState()).thenReturn(
                NEXT_EXTENSION_OR_DEFAULT);

        when(wrappedAuthenticatorProvider.getEnhancedAuthenticator(authenticatorProviderInput)).thenReturn(enhancedAuthenticator);
        when(wrappedAuthenticatorProvider.getClassLoader()).thenReturn(Mockito.mock(IsolatedPluginClassloader.class));

        connectEnhancedAuthTask = new ConnectEnhancedAuthTask(wrappedAuthenticatorProvider, authenticatorProviderInput, "extension1", new ClientAuthenticatorsImpl(new PluginPriorityComparator(extensions)));
        assertNotNull(connectEnhancedAuthTask.getPluginClassLoader());
    }

    @Test(timeout = 5000)
    public void test_task_is_performed_when_undecided() {

        connectEnhancedAuthTask.apply(connectEnhancedAuthTaskInput, connectEnhancedAuthTaskOutput);

        verify(enhancedAuthenticator, times(1)).onConnect(same(connectEnhancedAuthTaskInput), same(connectEnhancedAuthTaskOutput));
    }

    @Test(timeout = 5000)
    public void test_task_is_performed_when_continuing() {

        when(connectEnhancedAuthTaskOutput.getAuthenticationState()).thenReturn(
                NEXT_EXTENSION_OR_DEFAULT);

        connectEnhancedAuthTask.apply(connectEnhancedAuthTaskInput, connectEnhancedAuthTaskOutput);

        verify(enhancedAuthenticator, times(1)).onConnect(same(connectEnhancedAuthTaskInput), same(connectEnhancedAuthTaskOutput));
    }

    @Test(timeout = 5000)
    public void test_task_is_not_performed_when_decided() {
        when(connectEnhancedAuthTaskOutput.getAuthenticationState()).thenReturn(SUCCESS);

        connectEnhancedAuthTask.apply(connectEnhancedAuthTaskInput, connectEnhancedAuthTaskOutput);

        verify(enhancedAuthenticator, never()).onConnect(any(ConnectEnhancedAuthTaskInput.class), any(AuthTaskOutput.class));
    }

    @Test(timeout = 5000)
    public void test_output_is_failed_when_by_exception() {
        final RuntimeException toBeThrown = new RuntimeException();
        doThrow(toBeThrown).when(enhancedAuthenticator).onConnect(any(), any());

        connectEnhancedAuthTask.apply(connectEnhancedAuthTaskInput, connectEnhancedAuthTaskOutput);

        verify(connectEnhancedAuthTaskOutput, times(1)).setThrowable(same(toBeThrown));
    }

    @Test(timeout = 5000, expected = Error.class)
    public void test_error_is_rethrown() {
        final Error toBeThrown = new Error();
        doThrow(toBeThrown).when(enhancedAuthenticator).onConnect(any(), any());

        connectEnhancedAuthTask.apply(connectEnhancedAuthTaskInput, connectEnhancedAuthTaskOutput);
    }

}