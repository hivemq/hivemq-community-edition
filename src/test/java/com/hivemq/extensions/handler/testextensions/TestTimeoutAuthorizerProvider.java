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

package com.hivemq.extensions.handler.testextensions;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.auth.Authorizer;
import com.hivemq.extension.sdk.api.auth.SubscriptionAuthorizer;
import com.hivemq.extension.sdk.api.auth.parameter.AuthorizerProviderInput;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthorizerProvider;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

/**
 * Test extension used in PluginAuthorizerServiceImplTest
 */
public final class TestTimeoutAuthorizerProvider implements AuthorizerProvider {

    private final @NotNull CountDownLatch countDownLatch;

    public TestTimeoutAuthorizerProvider(final @NotNull CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public @NotNull Authorizer getAuthorizer(final @NotNull AuthorizerProviderInput authorizerProviderInput) {
        return (SubscriptionAuthorizer) (subscriptionAuthorizerInput, subscriptionAuthorizerOutput) -> {
            subscriptionAuthorizerOutput.async(Duration.ofMillis(1), TimeoutFallback.FAILURE);

            System.out.println("authorize async");

            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }

            countDownLatch.countDown();
        };
    }
}
