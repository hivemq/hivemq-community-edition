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
package com.hivemq.extensions.auth.parameter;

import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.hivemq.extensions.auth.parameter.SubscriptionAuthorizerOutputImpl.AuthorizationState.CONTINUE;
import static com.hivemq.extensions.auth.parameter.SubscriptionAuthorizerOutputImpl.AuthorizationState.DISCONNECT;
import static com.hivemq.extensions.auth.parameter.SubscriptionAuthorizerOutputImpl.AuthorizationState.FAIL;
import static com.hivemq.extensions.auth.parameter.SubscriptionAuthorizerOutputImpl.AuthorizationState.SUCCESS;
import static com.hivemq.extensions.auth.parameter.SubscriptionAuthorizerOutputImpl.AuthorizationState.UNDECIDED;
import static org.junit.Assert.assertEquals;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("NullabilityAnnotations")
public class SubscriptionAuthorizerOutputImplTest {

    @Mock
    private PluginOutPutAsyncer asyncer;

    private SubscriptionAuthorizerOutputImpl output;
    private AutoCloseable closeable;

    @Before
    public void before() {
        closeable = MockitoAnnotations.openMocks(this);
        output = new SubscriptionAuthorizerOutputImpl(asyncer);
    }

    @After
    public void releaseMocks() throws Exception {
        closeable. close();
    }

    @Test
    public void test_output_success() {
        output.authorizeSuccessfully();
        assertEquals(SUCCESS, output.getAuthorizationState());
    }

    @Test
    public void test_output_none() {
        assertEquals(UNDECIDED, output.getAuthorizationState());
    }

    @Test
    public void test_output_continue() {
        output.nextExtensionOrDefault();
        assertEquals(CONTINUE, output.getAuthorizationState());
        assertEquals(false, output.isCompleted());
    }

    @Test
    public void test_output_fail() {
        output.failAuthorization();
        assertEquals(FAIL, output.getAuthorizationState());
        assertEquals(true, output.isCompleted());
    }

    @Test
    public void test_output_force_fail() {
        output.authorizeSuccessfully();
        output.forceFailedAuthorization();
        assertEquals(FAIL, output.getAuthorizationState());
        assertEquals(true, output.isCompleted());
    }

    @Test
    public void test_output_fail_code() {
        output.failAuthorization(SubackReasonCode.QUOTA_EXCEEDED);
        assertEquals(FAIL, output.getAuthorizationState());
        assertEquals(SubackReasonCode.QUOTA_EXCEEDED, output.getSubackReasonCode());
        assertEquals(true, output.isCompleted());
    }

    @Test
    public void test_output_fail_code_string() {
        output.failAuthorization(SubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, "test-string");
        assertEquals(FAIL, output.getAuthorizationState());
        assertEquals(SubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, output.getSubackReasonCode());
        assertEquals("test-string", output.getReasonString());
        assertEquals(true, output.isCompleted());
    }

    @Test
    public void test_output_disconnect() {
        output.disconnectClient();
        assertEquals(DISCONNECT, output.getAuthorizationState());
        assertEquals(true, output.isCompleted());
    }

    @Test
    public void test_output_disconnect_code() {
        output.disconnectClient(DisconnectReasonCode.CONNECTION_RATE_EXCEEDED);
        assertEquals(DISCONNECT, output.getAuthorizationState());
        assertEquals(DisconnectReasonCode.CONNECTION_RATE_EXCEEDED, output.getDisconnectReasonCode());
        assertEquals(true, output.isCompleted());
    }

    @Test
    public void test_output_disconnect_code_string() {
        output.disconnectClient(DisconnectReasonCode.CONNECTION_RATE_EXCEEDED, "test-string");
        assertEquals(DISCONNECT, output.getAuthorizationState());
        assertEquals(DisconnectReasonCode.CONNECTION_RATE_EXCEEDED, output.getDisconnectReasonCode());
        assertEquals("test-string", output.getReasonString());
        assertEquals(true, output.isCompleted());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_exception_multiple_result_next() {
        output.authorizeSuccessfully();
        output.nextExtensionOrDefault();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_exception_multiple_result_success() {
        output.authorizeSuccessfully();
        output.authorizeSuccessfully();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_exception_multiple_result_fail() {
        output.authorizeSuccessfully();
        output.failAuthorization();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_exception_multiple_result_disconnect() {
        output.authorizeSuccessfully();
        output.disconnectClient();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_fail_sucess_code() {
        output.failAuthorization(SubackReasonCode.GRANTED_QOS_0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_fail_string_sucess_code() {
        output.failAuthorization(SubackReasonCode.GRANTED_QOS_1, "test-string");
    }
}
