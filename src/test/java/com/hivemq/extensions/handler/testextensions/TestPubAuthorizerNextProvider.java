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
import com.hivemq.extension.sdk.api.auth.Authorizer;
import com.hivemq.extension.sdk.api.auth.PublishAuthorizer;
import com.hivemq.extension.sdk.api.auth.parameter.AuthorizerProviderInput;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthorizerProvider;

import java.util.concurrent.CountDownLatch;

/**
 * Test extension used in PluginAuthorizerServiceImplTest
 */
public final class TestPubAuthorizerNextProvider implements AuthorizerProvider {

    private final @NotNull CountDownLatch countDownLatch;

    public TestPubAuthorizerNextProvider(final @NotNull CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public @NotNull Authorizer getAuthorizer(final @NotNull AuthorizerProviderInput authorizerProviderInput) {
        return (PublishAuthorizer) (input, output) -> {
            System.out.println("authorize");
            output.nextExtensionOrDefault();
            countDownLatch.countDown();
        };
    }
}
