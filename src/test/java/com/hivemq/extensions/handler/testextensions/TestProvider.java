package com.hivemq.extensions.handler.testextensions;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListener;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListenerProvider;
import com.hivemq.extension.sdk.api.events.client.parameters.*;

import java.util.concurrent.CountDownLatch;

public final class TestProvider implements ClientLifecycleEventListenerProvider {

    private final CountDownLatch onMqttConnectionStartLatch;
    private final CountDownLatch onAuthenticationFailedDisconnectLatch;
    private final CountDownLatch onConnectionLostLatch;
    private final CountDownLatch onClientInitiatedDisconnectLatch;
    private final CountDownLatch onServerInitiatedDisconnectLatch;
    private final CountDownLatch onAuthenticationSuccessfulLatch;
    private final CountDownLatch onDisconnectLatch;

    public TestProvider(final CountDownLatch countDownLatch) {
        this.onMqttConnectionStartLatch = countDownLatch;
        this.onAuthenticationFailedDisconnectLatch = countDownLatch;
        this.onConnectionLostLatch = countDownLatch;
        this.onClientInitiatedDisconnectLatch = countDownLatch;
        this.onServerInitiatedDisconnectLatch = countDownLatch;
        this.onAuthenticationSuccessfulLatch = countDownLatch;
        this.onDisconnectLatch = countDownLatch;
    }

    @Override
    public @Nullable ClientLifecycleEventListener getClientLifecycleEventListener(@NotNull ClientLifecycleEventListenerProviderInput input) {

        return new ClientLifecycleEventListener() {
            @Override
            public void onMqttConnectionStart(@NotNull ConnectionStartInput input) {
                onMqttConnectionStartLatch.countDown();
                System.out.println("connect");
            }

            @Override
            public void onAuthenticationFailedDisconnect(@NotNull AuthenticationFailedInput input) {
                onAuthenticationFailedDisconnectLatch.countDown();
                System.out.println("auth failed");
            }

            @Override
            public void onConnectionLost(@NotNull ConnectionLostInput input) {
                onConnectionLostLatch.countDown();
                System.out.println("connection lost");
            }

            @Override
            public void onClientInitiatedDisconnect(@NotNull ClientInitiatedDisconnectInput input) {
                onClientInitiatedDisconnectLatch.countDown();
                System.out.println("client disconnect");
            }

            @Override
            public void onServerInitiatedDisconnect(@NotNull ServerInitiatedDisconnectInput input) {
                onServerInitiatedDisconnectLatch.countDown();
                System.out.println("server disconnect");
            }

            @Override
            public void onAuthenticationSuccessful(@NotNull AuthenticationSuccessfulInput input) {
                onAuthenticationSuccessfulLatch.countDown();
                System.out.println("auth success");
            }

            @Override
            public void onDisconnect(@NotNull DisconnectEventInput input) {
                onDisconnectLatch.countDown();
                System.out.println("disconnect");
            }
        };
    }
}