package com.hivemq.extensions.handler.testextensions;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.auth.Authorizer;
import com.hivemq.extension.sdk.api.auth.SubscriptionAuthorizer;
import com.hivemq.extension.sdk.api.auth.parameter.AuthorizerProviderInput;
import com.hivemq.extension.sdk.api.auth.parameter.SubscriptionAuthorizerInput;
import com.hivemq.extension.sdk.api.auth.parameter.SubscriptionAuthorizerOutput;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthorizerProvider;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

public final class TestTimeoutAuthorizerProvider implements AuthorizerProvider {

    private final CountDownLatch countDownLatch;

    public TestTimeoutAuthorizerProvider(final CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public @Nullable Authorizer getAuthorizer(@NotNull final AuthorizerProviderInput authorizerProviderInput) {
        return new SubscriptionAuthorizer() {
            @Override
            public void authorizeSubscribe(
                    @NotNull final SubscriptionAuthorizerInput subscriptionAuthorizerInput,
                    @NotNull final SubscriptionAuthorizerOutput subscriptionAuthorizerOutput) {
                subscriptionAuthorizerOutput.async(Duration.ofMillis(1), TimeoutFallback.FAILURE);

                System.out.println("authorize async");

                try {
                    Thread.sleep(100);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }

                countDownLatch.countDown();
            }
        };
    }
}