package com.hivemq.extensions.handler.testextensions;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.Authorizer;
import com.hivemq.extension.sdk.api.auth.PublishAuthorizer;
import com.hivemq.extension.sdk.api.auth.parameter.AuthorizerProviderInput;
import com.hivemq.extension.sdk.api.auth.parameter.PublishAuthorizerInput;
import com.hivemq.extension.sdk.api.auth.parameter.PublishAuthorizerOutput;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthorizerProvider;

import java.util.concurrent.CountDownLatch;

public final class TestPubAuthorizerNextProvider implements AuthorizerProvider {

    private final CountDownLatch countDownLatch;

    public TestPubAuthorizerNextProvider(final CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public @Nullable Authorizer getAuthorizer(@NotNull final AuthorizerProviderInput authorizerProviderInput) {
        return new PublishAuthorizer() {
            @Override
            public void authorizePublish(final PublishAuthorizerInput input, final PublishAuthorizerOutput output) {
                System.out.println("authorize");
                output.nextExtensionOrDefault();
                countDownLatch.countDown();
            }
        };
    }
}