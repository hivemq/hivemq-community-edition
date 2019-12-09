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

import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.List;

import static com.hivemq.extensions.services.auth.AuthenticationState.*;
import static org.junit.Assert.*;

/**
 * @author Georg Held
 */
@SuppressWarnings("NullabilityAnnotations")
public class ConnectSimpleAuthTaskOutputTest {

    @Mock
    private PluginOutPutAsyncer asyncer;

    private ConnectSimpleAuthTaskOutput connectSimpleAuthTaskOutput;
    private AuthenticationContext authenticationContext;

    private final ModifiableDefaultPermissions permissions = new ModifiableDefaultPermissionsImpl();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        authenticationContext = new AuthenticationContext();
        connectSimpleAuthTaskOutput = new ConnectSimpleAuthTaskOutput(asyncer, new ModifiableClientSettingsImpl(65535), permissions, authenticationContext, true);
    }

    @Test(timeout = 5000)
    public void test_user_properties_of_connect_are_not_present() {
        final List<String> one = connectSimpleAuthTaskOutput.getOutboundUserProperties().getAllForName("one");
        final List<String> two = connectSimpleAuthTaskOutput.getOutboundUserProperties().getAllForName("two");

        assertEquals(0, one.size());
        assertEquals(0, two.size());
    }

    @Test(timeout = 5000)
    public void test_copy_constructor_receives_user_properties() {

        connectSimpleAuthTaskOutput.getOutboundUserProperties().addUserProperty("testA", "valueA");

        connectSimpleAuthTaskOutput = new ConnectSimpleAuthTaskOutput(connectSimpleAuthTaskOutput);
        connectSimpleAuthTaskOutput.getOutboundUserProperties().addUserProperty("testB", "valueB");

        connectSimpleAuthTaskOutput = new ConnectSimpleAuthTaskOutput(connectSimpleAuthTaskOutput);
        connectSimpleAuthTaskOutput.getOutboundUserProperties().addUserProperty("testC", "valueC");

        connectSimpleAuthTaskOutput = new ConnectSimpleAuthTaskOutput(connectSimpleAuthTaskOutput);

        assertEquals(3, connectSimpleAuthTaskOutput.getChangedUserProperties().asImmutableList().size());

    }

    @Test(timeout = 5000)
    public void test_fail_sets_decided() {
        connectSimpleAuthTaskOutput.failAuthentication();
        assertEquals(FAILED, connectSimpleAuthTaskOutput.getAuthenticationState());
        assertEquals(ConnackReasonCode.NOT_AUTHORIZED, connectSimpleAuthTaskOutput.getConnackReasonCode());
        assertEquals("Authentication failed by extension", connectSimpleAuthTaskOutput.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_success_sets_decided() {
        connectSimpleAuthTaskOutput.authenticateSuccessfully();
        assertEquals(SUCCESS, connectSimpleAuthTaskOutput.getAuthenticationState());
    }

    @Test(timeout = 5000)
    public void test_continue_does_not_decide() {
        connectSimpleAuthTaskOutput.nextExtensionOrDefault();
        assertEquals(NEXT_EXTENSION_OR_DEFAULT, connectSimpleAuthTaskOutput.getAuthenticationState());
    }

    @Test(timeout = 5000)
    public void test_throwable_fails_authentication() {
        connectSimpleAuthTaskOutput.setThrowable(new RuntimeException());
        assertEquals(FAILED, connectSimpleAuthTaskOutput.getAuthenticationState());
        assertEquals(ConnackReasonCode.UNSPECIFIED_ERROR, connectSimpleAuthTaskOutput.getConnackReasonCode());
        assertEquals("Unhandled exception in authentication extension", connectSimpleAuthTaskOutput.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_fail_with_success_reason_code_does_not_fail_authentication() {

        Exception expected = null;
        try {
            connectSimpleAuthTaskOutput.failAuthentication(ConnackReasonCode.SUCCESS, "foo");
        } catch (final IllegalArgumentException ex) {
            expected = ex;
        }

        assertNotNull(expected);
        assertNotEquals(
                FAILED, connectSimpleAuthTaskOutput.getAuthenticationState());
    }

    @Test
    public void test_async_duration() {

        connectSimpleAuthTaskOutput.async(Duration.ofSeconds(10));

        assertEquals(ConnackReasonCode.NOT_AUTHORIZED, connectSimpleAuthTaskOutput.getConnackReasonCode());
        assertEquals("Authentication failed by timeout", connectSimpleAuthTaskOutput.getReasonString());

    }

    @Test(expected = NullPointerException.class)
    public void test_async_null() {

        connectSimpleAuthTaskOutput.async(null);

    }

    @Test
    public void test_async_duration_fallback() {

        connectSimpleAuthTaskOutput.async(Duration.ofSeconds(10), TimeoutFallback.SUCCESS);

        assertEquals(ConnackReasonCode.NOT_AUTHORIZED, connectSimpleAuthTaskOutput.getConnackReasonCode());
        assertEquals("Authentication failed by timeout", connectSimpleAuthTaskOutput.getReasonString());

    }

    @Test
    public void test_async_duration_fallback_code() {

        connectSimpleAuthTaskOutput.async(
                Duration.ofSeconds(10), TimeoutFallback.SUCCESS, ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD);

        assertEquals(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD, connectSimpleAuthTaskOutput.getConnackReasonCode());
        assertEquals("Authentication failed by timeout", connectSimpleAuthTaskOutput.getReasonString());

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_async_success_code() {

        connectSimpleAuthTaskOutput.async(Duration.ofSeconds(10), TimeoutFallback.SUCCESS, ConnackReasonCode.SUCCESS);

    }

    @Test
    public void test_async_duration_fallback_string() {

        connectSimpleAuthTaskOutput.async(Duration.ofSeconds(10), TimeoutFallback.SUCCESS, "Failed by me");

        assertEquals(ConnackReasonCode.NOT_AUTHORIZED, connectSimpleAuthTaskOutput.getConnackReasonCode());
        assertEquals("Failed by me", connectSimpleAuthTaskOutput.getReasonString());

    }

    @Test
    public void test_async_duration_fallback_code_string() {

        connectSimpleAuthTaskOutput.async(
                Duration.ofSeconds(10), TimeoutFallback.SUCCESS, ConnackReasonCode.SERVER_BUSY, "Failed by me");

        assertEquals(ConnackReasonCode.SERVER_BUSY, connectSimpleAuthTaskOutput.getConnackReasonCode());
        assertEquals("Failed by me", connectSimpleAuthTaskOutput.getReasonString());

    }
}