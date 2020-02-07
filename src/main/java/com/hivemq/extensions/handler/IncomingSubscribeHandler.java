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

package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.subscribe.parameter.SubscribeInboundInputImpl;
import com.hivemq.extensions.interceptor.subscribe.parameter.SubscribeInboundOutputImpl;
import com.hivemq.extensions.packets.subscribe.SubscribePacketImpl;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Florian Limpöck
 * @since 4.1.0
 */
@ChannelHandler.Sharable
@Singleton
public class IncomingSubscribeHandler extends SimpleChannelInboundHandler<SUBSCRIBE> {

    private static final Logger log = LoggerFactory.getLogger(IncomingSubscribeHandler.class);

    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull PluginAuthorizerService pluginAuthorizerService;
    private final @NotNull FullConfigurationService fullConfigurationService;

    @Inject
    public IncomingSubscribeHandler(
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull PluginAuthorizerService pluginAuthorizerService,
            final @NotNull FullConfigurationService fullConfigurationService) {

        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.pluginAuthorizerService = pluginAuthorizerService;
        this.fullConfigurationService = fullConfigurationService;
    }

    @Override
    public void channelRead0(final @NotNull ChannelHandlerContext ctx, final @NotNull SUBSCRIBE msg) {
        interceptOrDelegate(ctx, msg);
    }

    /**
     * intercepts the subscribe message when the channel is active, the client id is set and interceptors are available,
     * otherwise delegates to authorizer
     *
     * @param ctx       the context of the channel handler
     * @param subscribe the subscribe to process
     */
    private void interceptOrDelegate(final @NotNull ChannelHandlerContext ctx, final @NotNull SUBSCRIBE subscribe) {

        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null || clientContext.getSubscribeInboundInterceptors().isEmpty()) {
            pluginAuthorizerService.authorizeSubscriptions(ctx, subscribe);
            return;
        }

        final List<SubscribeInboundInterceptor> subscribeInboundInterceptors =
                clientContext.getSubscribeInboundInterceptors();

        final SubscribeInboundOutputImpl inboundOutput =
                new SubscribeInboundOutputImpl(fullConfigurationService, asyncer, subscribe);
        final SubscribeInboundInputImpl inboundInput =
                new SubscribeInboundInputImpl(new SubscribePacketImpl(subscribe), clientId, channel);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final SubscribeInboundInterceptorContext interceptorContext = new SubscribeInboundInterceptorContext(
                clientId, inboundOutput, inboundInput, interceptorFuture, subscribeInboundInterceptors.size());

        for (final SubscribeInboundInterceptor interceptor : subscribeInboundInterceptors) {

            //we can stop running interceptors if delivery is prevented.
            if (inboundOutput.deliveryPrevented()) {
                //we do not know if it is already set by an async task so we check it
                if (!interceptorFuture.isDone()) {
                    interceptorFuture.set(null);
                }
                break;
            }

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            //disabled extension would be null
            if (plugin == null) {
                interceptorContext.increment();
                continue;
            }

            final SubscribeInboundInterceptorTask interceptorTask =
                    new SubscribeInboundInterceptorTask(interceptor, plugin.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, inboundInput, inboundOutput, interceptorTask);
        }

        final InterceptorFutureCallback callback =
                new InterceptorFutureCallback(inboundOutput, subscribe, ctx, pluginAuthorizerService);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
    }

    private static class InterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull SubscribeInboundOutputImpl inboundOutput;
        private final @NotNull SUBSCRIBE subscribe;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull PluginAuthorizerService pluginAuthorizerService;

        InterceptorFutureCallback(
                final @NotNull SubscribeInboundOutputImpl inboundOutput,
                final @NotNull SUBSCRIBE subscribe,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull PluginAuthorizerService pluginAuthorizerService) {

            this.inboundOutput = inboundOutput;
            this.subscribe = subscribe;
            this.ctx = ctx;
            this.pluginAuthorizerService = pluginAuthorizerService;
        }

        @Override
        public void onSuccess(final @Nullable Void result) {
            if (inboundOutput.deliveryPrevented()) {
                preventSubscribe();
            } else {
                final SUBSCRIBE finalSubscribe = SUBSCRIBE.from(inboundOutput.getSubscribePacket());
                pluginAuthorizerService.authorizeSubscriptions(ctx, finalSubscribe);
            }
        }

        private void preventSubscribe() {
            final List<Mqtt5SubAckReasonCode> ackReasonCodes = new ArrayList<>(subscribe.getTopics().size());
            for (int i = 0; i < subscribe.getTopics().size(); i++) {
                ackReasonCodes.add(Mqtt5SubAckReasonCode.UNSPECIFIED_ERROR);
            }
            // no need to check mqtt version since the mqtt 3 encoder will just not encode reason string and properties.
            ctx.writeAndFlush(new SUBACK(subscribe.getPacketIdentifier(), ackReasonCodes,
                    ReasonStrings.SUBACK_EXTENSION_PREVENTED));
        }

        @Override
        public void onFailure(final @NotNull Throwable t) {
            //should never happen, since the settable future never sets an exception
            pluginAuthorizerService.authorizeSubscriptions(ctx, subscribe);
        }
    }

    private class SubscribeInboundInterceptorContext extends PluginInOutTaskContext<SubscribeInboundOutputImpl> {

        private final @NotNull SubscribeInboundOutputImpl output;
        private final @NotNull SubscribeInboundInputImpl input;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        SubscribeInboundInterceptorContext(
                final @NotNull String identifier,
                final @NotNull SubscribeInboundOutputImpl output,
                final @NotNull SubscribeInboundInputImpl input,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final int interceptorCount) {

            super(identifier);
            this.output = output;
            this.input = input;
            this.interceptorFuture = interceptorFuture;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(@NotNull final SubscribeInboundOutputImpl pluginOutput) {

            if (pluginOutput.isAsync() && pluginOutput.isTimedOut() &&
                    pluginOutput.getTimeoutFallback() == TimeoutFallback.FAILURE) {
                //Timeout fallback failure means publish delivery prevention
                pluginOutput.forciblyPreventSubscribeDelivery();
            }

            if (output.getSubscribePacket().isModified()) {
                input.updateSubscribe(output.getSubscribePacket());
            }

            if (counter.incrementAndGet() == interceptorCount || pluginOutput.deliveryPrevented()) {
                interceptorFuture.set(null);
            }
        }

        public void increment() {
            //we must set the future when no more interceptors are registered
            if (counter.incrementAndGet() == interceptorCount) {
                interceptorFuture.set(null);
            }
        }
    }

    private class SubscribeInboundInterceptorTask
            implements PluginInOutTask<SubscribeInboundInputImpl, SubscribeInboundOutputImpl> {

        private final @NotNull SubscribeInboundInterceptor interceptor;
        private final @NotNull String extensionId;

        private SubscribeInboundInterceptorTask(
                final @NotNull SubscribeInboundInterceptor interceptor,
                final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull SubscribeInboundOutputImpl apply(
                final @NotNull SubscribeInboundInputImpl subscribeInboundInput,
                final @NotNull SubscribeInboundOutputImpl subscribeInboundOutput) {

            if (subscribeInboundOutput.deliveryPrevented()) {
                //it's already prevented so no further interceptors must be called.
                return subscribeInboundOutput;
            }
            try {
                interceptor.onInboundSubscribe(subscribeInboundInput, subscribeInboundOutput);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound subscribe interception. Extensions are responsible on their own to handle exceptions.",
                        extensionId);
                subscribeInboundOutput.forciblyPreventSubscribeDelivery();
                Exceptions.rethrowError(e);
            }
            return subscribeInboundOutput;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }
}
