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
import com.hivemq.extensions.auth.parameter.PublishAuthorizerOutputImpl;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Christoph Schäbel
 */
public class PublishAuthorizerContext extends PluginInOutTaskContext<PublishAuthorizerOutputImpl> {

    private final @NotNull PublishAuthorizerOutputImpl output;
    private final @NotNull SettableFuture<PublishAuthorizerOutputImpl> authorizeFuture;
    private final int authorizerCount;
    private final @NotNull ChannelHandlerContext ctx;
    private final @NotNull AtomicInteger counter;

    public PublishAuthorizerContext(
            final @NotNull String identifier,
            final @NotNull PublishAuthorizerOutputImpl output,
            final @NotNull SettableFuture<PublishAuthorizerOutputImpl> authorizeFuture,
            final int authorizerCount,
            final @NotNull ChannelHandlerContext ctx) {

        super(identifier);
        this.output = output;
        this.authorizeFuture = authorizeFuture;
        this.authorizerCount = authorizerCount;
        this.ctx = ctx;
        this.counter = new AtomicInteger(0);
    }

    @Override
    public void pluginPost(final @NotNull PublishAuthorizerOutputImpl pluginOutput) {

        if (pluginOutput.isAsync() && pluginOutput.isTimedOut() && pluginOutput.getTimeoutFallback() == TimeoutFallback.FAILURE) {
            //Timeout fallback failure means publish delivery prevention
            pluginOutput.forceFailedAuthorization();
        }

        if (pluginOutput.getAuthorizationState() == PublishAuthorizerOutputImpl.AuthorizationState.FAIL
                || pluginOutput.getAuthorizationState() == PublishAuthorizerOutputImpl.AuthorizationState.DISCONNECT) {
            ctx.channel().attr(ChannelAttributes.INCOMING_PUBLISHES_SKIP_REST).set(true);
        }


        //the publish is done if any authorizer sets the outcome
        if (pluginOutput.isCompleted()) {
            authorizeFuture.set(pluginOutput);
            return;
        }

        if (counter.incrementAndGet() == authorizerCount) {
            authorizeFuture.set(pluginOutput);
        }
    }

    public void increment() {
        //we must set the future when no more authorizers are registered
        if (counter.incrementAndGet() == authorizerCount) {
            authorizeFuture.set(output);
        }
    }
}
