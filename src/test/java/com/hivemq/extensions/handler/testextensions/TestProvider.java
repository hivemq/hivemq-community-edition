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
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListener;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListenerProvider;
import com.hivemq.extension.sdk.api.events.client.parameters.*;

import java.util.concurrent.CountDownLatch;

/**
 * Test extension used in ClientLifecycleEventHandlerTest
 */
public final class TestProvider implements ClientLifecycleEventListenerProvider {

    private final @NotNull CountDownLatch onMqttConnectionStartLatch;
    private final @NotNull CountDownLatch onAuthenticationFailedDisconnectLatch;
    private final @NotNull CountDownLatch onConnectionLostLatch;
    private final @NotNull CountDownLatch onClientInitiatedDisconnectLatch;
    private final @NotNull CountDownLatch onServerInitiatedDisconnectLatch;
    private final @NotNull CountDownLatch onAuthenticationSuccessfulLatch;
    private final @NotNull CountDownLatch onDisconnectLatch;

    public TestProvider(final @NotNull CountDownLatch countDownLatch) {
        this.onMqttConnectionStartLatch = countDownLatch;
        this.onAuthenticationFailedDisconnectLatch = countDownLatch;
        this.onConnectionLostLatch = countDownLatch;
        this.onClientInitiatedDisconnectLatch = countDownLatch;
        this.onServerInitiatedDisconnectLatch = countDownLatch;
        this.onAuthenticationSuccessfulLatch = countDownLatch;
        this.onDisconnectLatch = countDownLatch;
    }

    @Override
    public @NotNull ClientLifecycleEventListener getClientLifecycleEventListener(final @NotNull ClientLifecycleEventListenerProviderInput input) {
        return new ClientLifecycleEventListener() {
            @Override
            public void onMqttConnectionStart(final @NotNull ConnectionStartInput input) {
                onMqttConnectionStartLatch.countDown();
                System.out.println("connect");
            }

            @Override
            public void onAuthenticationFailedDisconnect(final @NotNull AuthenticationFailedInput input) {
                onAuthenticationFailedDisconnectLatch.countDown();
                System.out.println("auth failed");
            }

            @Override
            public void onConnectionLost(final @NotNull ConnectionLostInput input) {
                onConnectionLostLatch.countDown();
                System.out.println("connection lost");
            }

            @Override
            public void onClientInitiatedDisconnect(final @NotNull ClientInitiatedDisconnectInput input) {
                onClientInitiatedDisconnectLatch.countDown();
                System.out.println("client disconnect");
            }

            @Override
            public void onServerInitiatedDisconnect(final @NotNull ServerInitiatedDisconnectInput input) {
                onServerInitiatedDisconnectLatch.countDown();
                System.out.println("server disconnect");
            }

            @Override
            public void onAuthenticationSuccessful(final @NotNull AuthenticationSuccessfulInput input) {
                onAuthenticationSuccessfulLatch.countDown();
                System.out.println("auth success");
            }

            @Override
            public void onDisconnect(final @NotNull DisconnectEventInput input) {
                onDisconnectLatch.countDown();
                System.out.println("disconnect");
            }
        };
    }
}
