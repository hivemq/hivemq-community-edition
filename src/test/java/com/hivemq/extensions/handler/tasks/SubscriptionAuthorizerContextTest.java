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

package com.hivemq.extensions.handler.tasks;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;
import com.hivemq.extensions.auth.parameter.SubscriptionAuthorizerOutputImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import static com.hivemq.extensions.auth.parameter.SubscriptionAuthorizerOutputImpl.AuthorizationState.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("NullabilityAnnotations")
public class SubscriptionAuthorizerContextTest {

    private SubscriptionAuthorizerContext context;
    private SettableFuture<SubscriptionAuthorizerOutputImpl> resultFuture;
    private SubscriptionAuthorizerOutputImpl output;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(mock(ShutdownHooks.class));
        resultFuture = SettableFuture.create();
        output = new SubscriptionAuthorizerOutputImpl(asyncer);
        context = new SubscriptionAuthorizerContext("clientid", output, resultFuture, 1);
    }

    @Test(timeout = 5000)
    public void test_async_timeout_fail() throws ExecutionException, InterruptedException {

        output.markAsAsync();
        output.markAsTimedOut();

        context.pluginPost(output);

        final SubscriptionAuthorizerOutputImpl result = resultFuture.get();
        assertEquals(FAIL, result.getAuthorizationState());
        assertEquals(SubackReasonCode.NOT_AUTHORIZED, result.getSubackReasonCode());
        assertEquals(true, result.isCompleted());
    }

    @Test(timeout = 5000)
    public void test_async_timeout_success() throws ExecutionException, InterruptedException {

        output.async(Duration.ofSeconds(10), TimeoutFallback.SUCCESS);
        output.markAsAsync();
        output.markAsTimedOut();

        context.pluginPost(output);

        final SubscriptionAuthorizerOutputImpl result = resultFuture.get();
        assertEquals(UNDECIDED, result.getAuthorizationState());
        assertEquals(false, result.isCompleted());
    }

    @Test(timeout = 5000)
    public void test_success() throws ExecutionException, InterruptedException {

        output.authorizeSuccessfully();

        context.pluginPost(output);

        final SubscriptionAuthorizerOutputImpl result = resultFuture.get();
        assertEquals(SUCCESS, result.getAuthorizationState());
        assertEquals(true, result.isCompleted());
    }

    @Test(timeout = 5000)
    public void test_fail() throws ExecutionException, InterruptedException {

        output.failAuthorization();

        context.pluginPost(output);

        final SubscriptionAuthorizerOutputImpl result = resultFuture.get();
        assertEquals(FAIL, result.getAuthorizationState());
        assertEquals(true, result.isCompleted());
    }

    @Test(timeout = 5000)
    public void test_disconnect() throws ExecutionException, InterruptedException {

        output.disconnectClient();

        context.pluginPost(output);

        final SubscriptionAuthorizerOutputImpl result = resultFuture.get();
        assertEquals(DISCONNECT, result.getAuthorizationState());
        assertEquals(true, result.isCompleted());
    }

    @Test(timeout = 5000)
    public void test_undecided() throws ExecutionException, InterruptedException {

        context.pluginPost(output);

        final SubscriptionAuthorizerOutputImpl result = resultFuture.get();
        assertEquals(UNDECIDED, result.getAuthorizationState());
        assertEquals(false, result.isCompleted());
    }

    @Test(timeout = 5000)
    public void test_increment_future_returns() throws ExecutionException, InterruptedException {

        context.increment();

        final SubscriptionAuthorizerOutputImpl result = resultFuture.get();
        assertEquals(UNDECIDED, result.getAuthorizationState());
        assertEquals(false, result.isCompleted());
    }

}