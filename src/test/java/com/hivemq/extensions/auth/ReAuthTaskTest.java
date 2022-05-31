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
package com.hivemq.extensions.auth;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extension.sdk.api.auth.parameter.EnhancedAuthConnectInput;
import com.hivemq.extension.sdk.api.auth.parameter.EnhancedAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.EnhancedAuthOutput;
import com.hivemq.extensions.ExtensionPriorityComparator;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import com.hivemq.extensions.client.ClientAuthenticatorsImpl;
import com.hivemq.extensions.packets.auth.AuthPacketImpl;
import com.hivemq.extensions.services.auth.WrappedAuthenticatorProvider;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import util.IsolatedExtensionClassloaderUtil;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NullabilityAnnotations")
public class ReAuthTaskTest {

    public static AtomicBoolean connect;

    @Mock
    private WrappedAuthenticatorProvider wrappedAuthenticatorProvider;
    public static AtomicBoolean auth;

    private EnhancedAuthenticator enhancedAuthenticator;
    public static AtomicBoolean reAuth;
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ReAuthTask authTask;
    @Mock
    private AuthenticatorProviderInput authenticatorProviderInput;
    private IsolatedExtensionClassloader classloader;
    @Mock
    private HiveMQExtensions extensions;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        connect = new AtomicBoolean();
        auth = new AtomicBoolean();
        reAuth = new AtomicBoolean();

        classloader = IsolatedExtensionClassloaderUtil.buildClassLoader(temporaryFolder.getRoot().toPath(), new Class[]{TestAuthenticator.class});
        enhancedAuthenticator = IsolatedExtensionClassloaderUtil.loadInstance(classloader, TestAuthenticator.class);

        when(wrappedAuthenticatorProvider.getEnhancedAuthenticator(authenticatorProviderInput)).thenReturn(enhancedAuthenticator);
        authTask = new ReAuthTask(wrappedAuthenticatorProvider, authenticatorProviderInput, "extension1", new ClientAuthenticatorsImpl(new ExtensionPriorityComparator(extensions)));
    }

    @Test(timeout = 5000)
    public void test_authenticator_is_same() {

        when(wrappedAuthenticatorProvider.getClassLoader()).thenReturn(classloader);

        final EnhancedAuthenticator authenticator1 = authTask.updateAndGetAuthenticator();
        final EnhancedAuthenticator authenticator2 = authTask.updateAndGetAuthenticator();
        assertSame(authenticator1, authenticator2);
        verify(wrappedAuthenticatorProvider, times(1)).getEnhancedAuthenticator(authenticatorProviderInput);
    }

    @Test(timeout = 5000)
    public void test_next_or_default_reauth() {
        final ReAuthOutput output = Mockito.mock(ReAuthOutput.class);
        final AuthInput input = Mockito.mock(AuthInput.class);
        when(output.getAuthenticationState()).thenReturn(AuthenticationState.NEXT_EXTENSION_OR_DEFAULT);
        when(input.getAuthPacket()).thenReturn(new AuthPacketImpl(new AUTH("method", "data".getBytes(), Mqtt5AuthReasonCode.REAUTHENTICATE, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason")));

        authTask.apply(input, output);
        assertTrue(reAuth.get());
    }

    @Test(timeout = 5000)
    public void test_next_or_default_continue() {
        final ReAuthOutput output = Mockito.mock(ReAuthOutput.class);
        final AuthInput input = Mockito.mock(AuthInput.class);
        when(output.getAuthenticationState()).thenReturn(AuthenticationState.NEXT_EXTENSION_OR_DEFAULT);
        when(input.getAuthPacket()).thenReturn(new AuthPacketImpl(new AUTH("method", "data".getBytes(), Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason")));

        authTask.apply(input, output);
        assertTrue(auth.get());
    }

    @Test(timeout = 5000)
    public void test_next_or_default_no_auth_found() {
        final ReAuthOutput output = Mockito.mock(ReAuthOutput.class);
        final AuthInput input = Mockito.mock(AuthInput.class);
        when(output.getAuthenticationState()).thenReturn(AuthenticationState.NEXT_EXTENSION_OR_DEFAULT);
        when(input.getAuthPacket()).thenReturn(new AuthPacketImpl(new AUTH("method", "data".getBytes(), Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason")));
        when(wrappedAuthenticatorProvider.getEnhancedAuthenticator(authenticatorProviderInput)).thenReturn(null);

        authTask.apply(input, output);

        assertFalse(auth.get());
    }

    @Test(timeout = 5000)
    public void test_undecided_reauth() {
        final ReAuthOutput output = Mockito.mock(ReAuthOutput.class);
        final AuthInput input = Mockito.mock(AuthInput.class);
        when(output.getAuthenticationState()).thenReturn(AuthenticationState.UNDECIDED);
        when(input.getAuthPacket()).thenReturn(new AuthPacketImpl(new AUTH("method", "data".getBytes(), Mqtt5AuthReasonCode.REAUTHENTICATE, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason")));

        authTask.apply(input, output);
        assertTrue(reAuth.get());
    }

    @Test(timeout = 5000)
    public void test_undecided_continue() {
        final ReAuthOutput output = Mockito.mock(ReAuthOutput.class);
        final AuthInput input = Mockito.mock(AuthInput.class);
        when(output.getAuthenticationState()).thenReturn(AuthenticationState.UNDECIDED);
        when(input.getAuthPacket()).thenReturn(new AuthPacketImpl(new AUTH("method", "data".getBytes(), Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason")));

        authTask.apply(input, output);
        assertTrue(auth.get());
    }

    @Test(timeout = 5000)
    public void test_undecided_no_auth_found() {
        final ReAuthOutput output = Mockito.mock(ReAuthOutput.class);
        final AuthInput input = Mockito.mock(AuthInput.class);
        when(output.getAuthenticationState()).thenReturn(AuthenticationState.UNDECIDED);
        when(input.getAuthPacket()).thenReturn(new AuthPacketImpl(new AUTH("method", "data".getBytes(), Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason")));
        when(wrappedAuthenticatorProvider.getEnhancedAuthenticator(authenticatorProviderInput)).thenReturn(null);

        authTask.apply(input, output);
        assertFalse(auth.get());
    }


    @Test(timeout = 5000)
    public void test_decided() {
        final ReAuthOutput output = Mockito.mock(ReAuthOutput.class);
        final AuthInput input = Mockito.mock(AuthInput.class);

        when(output.getAuthenticationState()).thenReturn(AuthenticationState.SUCCESS);
        authTask.apply(input, output);

        when(output.getAuthenticationState()).thenReturn(AuthenticationState.FAILED);
        authTask.apply(input, output);

        when(output.getAuthenticationState()).thenReturn(AuthenticationState.CONTINUE);
        authTask.apply(input, output);

        verify(wrappedAuthenticatorProvider, never()).getEnhancedAuthenticator(authenticatorProviderInput);
    }

    public static class TestAuthenticator implements EnhancedAuthenticator {

        @Override
        public void onConnect(@NotNull final EnhancedAuthConnectInput input, @NotNull final EnhancedAuthOutput output) {
            connect.set(true);
        }

        @Override
        public void onAuth(@NotNull final EnhancedAuthInput input, @NotNull final EnhancedAuthOutput output) {
            auth.set(true);
        }

        @Override
        public void onReAuth(@NotNull final EnhancedAuthInput input, @NotNull final EnhancedAuthOutput output) {
            reAuth.set(true);
        }
    }
}
