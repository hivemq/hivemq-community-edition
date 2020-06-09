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
package com.hivemq.extensions.handler.tasks;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extensions.auth.parameter.SubscriptionAuthorizerOutputImpl;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Christoph Schäbel
 */
public class SubscriptionAuthorizerContext extends PluginInOutTaskContext<SubscriptionAuthorizerOutputImpl> {

    private final @NotNull SubscriptionAuthorizerOutputImpl output;
    private final @NotNull SettableFuture<SubscriptionAuthorizerOutputImpl> authorizeFuture;
    private final int authorizerCount;
    private final @NotNull AtomicInteger counter;

    public SubscriptionAuthorizerContext(
            final @NotNull String identifier,
            final @NotNull SubscriptionAuthorizerOutputImpl output,
            final @NotNull SettableFuture<SubscriptionAuthorizerOutputImpl> authorizeFuture,
            final int authorizerCount) {

        super(identifier);
        this.output = output;
        this.authorizeFuture = authorizeFuture;
        this.authorizerCount = authorizerCount;
        this.counter = new AtomicInteger(0);
    }

    @Override
    public void pluginPost(final @NotNull SubscriptionAuthorizerOutputImpl pluginOutput) {

        if (pluginOutput.isAsync() && pluginOutput.isTimedOut() && pluginOutput.getTimeoutFallback() == TimeoutFallback.FAILURE) {
            //Timeout fallback failure means publish delivery prevention
            pluginOutput.forceFailedAuthorization();
        }

        //the topic is done if any authorizer sets the outcome
        if (pluginOutput.isCompleted()) {
            authorizeFuture.set(pluginOutput);
            return;
        }

        if (counter.incrementAndGet() == authorizerCount) {
            authorizeFuture.set(pluginOutput);
        }
    }

    public void increment() {
        //we must set the future when no more interceptors are registered
        if (counter.incrementAndGet() == authorizerCount) {
            authorizeFuture.set(output);
        }
    }
}
