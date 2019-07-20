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
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Georg Held
 */
@SuppressWarnings("NullabilityAnnotations")
public class ConnectAuthTaskOutputTest {

    @Mock
    private PluginOutPutAsyncer asyncer;

    private ConnectAuthTaskOutput connectAuthTaskOutput;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final CONNECT connect = new CONNECT.Mqtt5Builder().withClientIdentifier("unneeded")
                .withMqtt5UserProperties(
                        Mqtt5UserProperties.of(new MqttUserProperty("one", "one"), new MqttUserProperty("two", "two")))
                .build();
        connectAuthTaskOutput = new ConnectAuthTaskOutput(asyncer, true, new ModifiableClientSettingsImpl(65535));
    }

    @Test(timeout = 5000)
    public void test_user_properties_of_connect_are_not_present() {
        final List<String> one = connectAuthTaskOutput.getOutboundUserProperties().getAllForName("one");
        final List<String> two = connectAuthTaskOutput.getOutboundUserProperties().getAllForName("two");

        assertEquals(0, one.size());
        assertEquals(0, two.size());
    }

    @Test(timeout = 5000)
    public void test_copy_constructor_receives_user_properties() {

        connectAuthTaskOutput.getOutboundUserProperties().addUserProperty("testA", "valueA");

        connectAuthTaskOutput = new ConnectAuthTaskOutput(connectAuthTaskOutput);
        connectAuthTaskOutput.getOutboundUserProperties().addUserProperty("testB", "valueB");

        connectAuthTaskOutput = new ConnectAuthTaskOutput(connectAuthTaskOutput);
        connectAuthTaskOutput.getOutboundUserProperties().addUserProperty("testC", "valueC");

        connectAuthTaskOutput = new ConnectAuthTaskOutput(connectAuthTaskOutput);

        assertEquals(3, connectAuthTaskOutput.getChangedUserProperties().asImmutableList().size());

    }

    @Test(timeout = 5000)
    public void test_fail_sets_decided() {
        connectAuthTaskOutput.failAuthentication();
        assertEquals(ConnectAuthTaskOutput.AuthenticationState.FAILED, connectAuthTaskOutput.getAuthenticationState());
        assertEquals(ConnackReasonCode.NOT_AUTHORIZED, connectAuthTaskOutput.getConnackReasonCode());
        assertEquals("Authentication failed by extension", connectAuthTaskOutput.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_success_sets_decided() {
        connectAuthTaskOutput.authenticateSuccessfully();
        assertEquals(ConnectAuthTaskOutput.AuthenticationState.SUCCESS, connectAuthTaskOutput.getAuthenticationState());
    }

    @Test(timeout = 5000)
    public void test_continue_does_not_decide() {
        connectAuthTaskOutput.nextExtensionOrDefault();
        assertEquals(
                ConnectAuthTaskOutput.AuthenticationState.CONTINUE, connectAuthTaskOutput.getAuthenticationState());
    }

    @Test(timeout = 5000)
    public void test_throwable_fails_authentication() {
        connectAuthTaskOutput.setThrowable(new RuntimeException());
        assertEquals(ConnectAuthTaskOutput.AuthenticationState.FAILED, connectAuthTaskOutput.getAuthenticationState());
        assertEquals(ConnackReasonCode.UNSPECIFIED_ERROR, connectAuthTaskOutput.getConnackReasonCode());
        assertEquals("Unhandled exception in authentication extension", connectAuthTaskOutput.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_fail_with_success_reason_code_does_not_fail_authentication() {

        Exception expected = null;
        try {
            connectAuthTaskOutput.failAuthentication(ConnackReasonCode.SUCCESS, "foo");
        } catch (final IllegalArgumentException ex) {
            expected = ex;
        }

        assertNotNull(expected);
        assertNotEquals(
                ConnectAuthTaskOutput.AuthenticationState.FAILED, connectAuthTaskOutput.getAuthenticationState());
    }

    @Test
    public void test_async_duration() {

        connectAuthTaskOutput.async(Duration.ofSeconds(10));

        assertEquals(ConnackReasonCode.NOT_AUTHORIZED, connectAuthTaskOutput.getConnackReasonCode());
        assertEquals("Authentication failed by timeout", connectAuthTaskOutput.getReasonString());

    }

    @Test(expected = NullPointerException.class)
    public void test_async_null() {

        connectAuthTaskOutput.async(null);

    }

    @Test
    public void test_async_duration_fallback() {

        connectAuthTaskOutput.async(Duration.ofSeconds(10), TimeoutFallback.SUCCESS);

        assertEquals(ConnackReasonCode.NOT_AUTHORIZED, connectAuthTaskOutput.getConnackReasonCode());
        assertEquals("Authentication failed by timeout", connectAuthTaskOutput.getReasonString());

    }

    @Test
    public void test_async_duration_fallback_code() {

        connectAuthTaskOutput.async(
                Duration.ofSeconds(10), TimeoutFallback.SUCCESS, ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD);

        assertEquals(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD, connectAuthTaskOutput.getConnackReasonCode());
        assertEquals("Authentication failed by timeout", connectAuthTaskOutput.getReasonString());

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_async_success_code() {

        connectAuthTaskOutput.async(Duration.ofSeconds(10), TimeoutFallback.SUCCESS, ConnackReasonCode.SUCCESS);

    }

    @Test
    public void test_async_duration_fallback_string() {

        connectAuthTaskOutput.async(Duration.ofSeconds(10), TimeoutFallback.SUCCESS, "Failed by me");

        assertEquals(ConnackReasonCode.NOT_AUTHORIZED, connectAuthTaskOutput.getConnackReasonCode());
        assertEquals("Failed by me", connectAuthTaskOutput.getReasonString());

    }

    @Test
    public void test_async_duration_fallback_code_string() {

        connectAuthTaskOutput.async(
                Duration.ofSeconds(10), TimeoutFallback.SUCCESS, ConnackReasonCode.SERVER_BUSY, "Failed by me");

        assertEquals(ConnackReasonCode.SERVER_BUSY, connectAuthTaskOutput.getConnackReasonCode());
        assertEquals("Failed by me", connectAuthTaskOutput.getReasonString());

    }
}