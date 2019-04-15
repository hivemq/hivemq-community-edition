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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

/**
 * @author Georg Held
 */
@SuppressWarnings("NullabilityAnnotations")
public class SimpleAuthTaskTest {

    @Mock
    private WrappedAuthenticatorProvider wrappedAuthenticatorProvider;

    @Mock
    private SimpleAuthenticator simpleAuth;

    @Mock
    private ConnectAuthTaskInput connectAuthTaskInput;

    @Mock
    private AuthenticatorProviderInput authenticatorProviderInput;

    @Mock
    private ConnectAuthTaskOutput connectAuthTaskOutput;

    private SimpleAuthTask simpleAuthTask;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(connectAuthTaskOutput.getAuthenticationState()).thenReturn(
                ConnectAuthTaskOutput.AuthenticationState.CONTINUE);

        when(wrappedAuthenticatorProvider.getAuthenticator(authenticatorProviderInput)).thenReturn(simpleAuth);

        simpleAuthTask = new SimpleAuthTask(wrappedAuthenticatorProvider, authenticatorProviderInput);
    }

    @Test(timeout = 5000)
    public void test_task_is_performed_when_undecided() {

        simpleAuthTask.apply(connectAuthTaskInput, connectAuthTaskOutput);

        verify(simpleAuth, times(1)).onConnect(same(connectAuthTaskInput), same(connectAuthTaskOutput));
    }

    @Test(timeout = 5000)
    public void test_task_is_performed_when_continuing() {

        when(connectAuthTaskOutput.getAuthenticationState()).thenReturn(
                ConnectAuthTaskOutput.AuthenticationState.CONTINUE);

        simpleAuthTask.apply(connectAuthTaskInput, connectAuthTaskOutput);

        verify(simpleAuth, times(1)).onConnect(same(connectAuthTaskInput), same(connectAuthTaskOutput));
    }

    @Test(timeout = 5000)
    public void test_task_is_not_performed_when_decided() {
        when(connectAuthTaskOutput.getAuthenticationState()).thenReturn(
                ConnectAuthTaskOutput.AuthenticationState.SUCCESS);

        simpleAuthTask.apply(connectAuthTaskInput, connectAuthTaskOutput);

        verify(simpleAuth, never()).onConnect(any(ConnectAuthTaskInput.class), any(ConnectAuthTaskOutput.class));
    }

    @Test(timeout = 5000)
    public void test_output_is_failed_when_by_exception() {
        final RuntimeException toBeThrown = new RuntimeException();
        doThrow(toBeThrown).when(simpleAuth).onConnect(any(), any());

        simpleAuthTask.apply(connectAuthTaskInput, connectAuthTaskOutput);

        verify(connectAuthTaskOutput, times(1)).setThrowable(same(toBeThrown));
    }

    @Test(timeout = 5000, expected = Error.class)
    public void test_error_is_rethrown() {
        final Error toBeThrown = new Error();
        doThrow(toBeThrown).when(simpleAuth).onConnect(any(), any());

        simpleAuthTask.apply(connectAuthTaskInput, connectAuthTaskOutput);
    }
}