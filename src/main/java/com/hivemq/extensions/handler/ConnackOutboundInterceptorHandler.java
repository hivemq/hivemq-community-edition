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

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptorProvider;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.connack.ConnackOutboundInputImpl;
import com.hivemq.extensions.interceptor.connack.ConnackOutboundOutputImpl;
import com.hivemq.extensions.interceptor.connack.ConnackOutboundProviderInputImpl;
import com.hivemq.extensions.packets.connack.ConnackPacketImpl;
import com.hivemq.extensions.services.interceptor.Interceptors;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
@Singleton
@ChannelHandler.Sharable
public class ConnackOutboundInterceptorHandler extends ChannelOutboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ConnackOutboundInterceptorHandler.class);

    @NotNull
    private final FullConfigurationService configurationService;

    @NotNull
    private final PluginOutPutAsyncer asyncer;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    @NotNull
    private final PluginTaskExecutorService pluginTaskExecutorService;

    @NotNull
    private final Interceptors interceptors;

    @NotNull
    private final ServerInformation serverInformation;

    @NotNull
    private final EventLog eventLog;

    @Inject
    public ConnackOutboundInterceptorHandler(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
            final @NotNull Interceptors interceptors,
            final @NotNull ServerInformation serverInformation,
            final @NotNull EventLog eventLog) {

        this.configurationService = configurationService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.interceptors = interceptors;
        this.serverInformation = serverInformation;
        this.eventLog = eventLog;
    }

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull Object msg,
            final @NotNull ChannelPromise promise)
            throws Exception {

        if (!(msg instanceof CONNACK)) {
            super.write(ctx, msg, promise);
            return;
        }

        final CONNACK connack = (CONNACK) msg;

        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ImmutableMap<String, ConnackOutboundInterceptorProvider> connackOutboundInterceptorProviders =
                interceptors.connackOutboundInterceptorProviders();
        if (connackOutboundInterceptorProviders.isEmpty()) {
            super.write(ctx, msg, promise);
            return;
        }

        final ConnackOutboundProviderInputImpl providerInput =
                new ConnackOutboundProviderInputImpl(serverInformation, channel, clientId);

        final Boolean requestResponseInformation = channel.attr(ChannelAttributes.REQUEST_RESPONSE_INFORMATION).get();
        final ConnackOutboundOutputImpl output = new ConnackOutboundOutputImpl(configurationService, asyncer, connack,
                Objects.requireNonNullElse(requestResponseInformation, false));
        final ConnackOutboundInputImpl input =
                new ConnackOutboundInputImpl(new ConnackPacketImpl(connack), clientId, channel);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final ConnackInterceptorContext interceptorContext = new ConnackInterceptorContext(
                clientId, input, interceptorFuture, connackOutboundInterceptorProviders.size());

        for (final Map.Entry<String, ConnackOutboundInterceptorProvider> entry : connackOutboundInterceptorProviders.entrySet()) {
            if (interceptorFuture.isDone()) {
                // The future is set in case an async interceptor timeout failed
                break;
            }
            final ConnackOutboundInterceptorProvider interceptorProvider = entry.getValue();
            final HiveMQExtension plugin = hiveMQExtensions.getExtension(entry.getKey());

            //disabled extension would be null
            if (plugin == null) {
                interceptorContext.increment(output.connackPrevented());
                continue;
            }
            final ConnackInterceptorTask interceptorTask =
                    new ConnackInterceptorTask(interceptorProvider, providerInput, interceptorFuture, plugin.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }

        final InterceptorFutureCallback callback =
                new InterceptorFutureCallback(output, connack, ctx, promise, eventLog);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());

    }

    private static class InterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull ConnackOutboundOutputImpl output;
        private final @NotNull CONNACK connack;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        private final @NotNull EventLog eventLog;

        InterceptorFutureCallback(
                final @NotNull ConnackOutboundOutputImpl output,
                final @NotNull CONNACK connack,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ChannelPromise promise,
                final @NotNull EventLog eventLog) {

            this.output = output;
            this.connack = connack;
            this.ctx = ctx;
            this.promise = promise;
            this.eventLog = eventLog;
        }

        @Override
        public void onSuccess(final @Nullable Void result) {
            try {
                if (output.connackPrevented()) {
                    eventLog.clientWasDisconnected(
                            ctx.channel(), "Connection prevented by extension in CONNACK outbound interceptor");
                    ctx.channel().close();
                    return;
                }
                final CONNACK finalConnack = CONNACK.mergeConnackPacket(output.getConnackPacket(), connack);
                ctx.writeAndFlush(finalConnack, promise);
            } catch (final Exception e) {
                log.error("Exception while modifying an intercepted CONNACK message.", e);
                ctx.writeAndFlush(connack, promise);
            }
        }

        @Override
        public void onFailure(final @NotNull Throwable t) {
            //should never happen, since the settable future never sets an exception
            ctx.channel().close();
        }
    }

    private class ConnackInterceptorContext extends PluginInOutTaskContext<ConnackOutboundOutputImpl> {

        private final @NotNull ConnackOutboundInputImpl input;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        ConnackInterceptorContext(
                final @NotNull String clientId,
                final @NotNull ConnackOutboundInputImpl input,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final int interceptorCount) {

            super(clientId);
            this.input = input;
            this.interceptorFuture = interceptorFuture;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(final @NotNull ConnackOutboundOutputImpl pluginOutput) {

            if (pluginOutput.isAsync() && pluginOutput.isTimedOut() &&
                    pluginOutput.getTimeoutFallback() == TimeoutFallback.FAILURE) {
                pluginOutput.forciblyDisconnect();
                increment(pluginOutput.connackPrevented());
                return;
            }

            if (pluginOutput.getConnackPacket().isModified()) {
                input.updateConnack(pluginOutput.getConnackPacket());
            }
            increment(pluginOutput.connackPrevented());
        }

        public void increment(final boolean connackPrevented) {
            //we must set the future when no more interceptors are registered
            if (counter.incrementAndGet() == interceptorCount || connackPrevented) {
                interceptorFuture.set(null);
            }
        }
    }

    private class ConnackInterceptorTask
            implements PluginInOutTask<ConnackOutboundInputImpl, ConnackOutboundOutputImpl> {

        private final @NotNull ConnackOutboundInterceptorProvider interceptorProvider;
        private final @NotNull ConnackOutboundProviderInputImpl connackOutboundProviderInput;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final @NotNull String pluginId;

        private ConnackInterceptorTask(
                final @NotNull ConnackOutboundInterceptorProvider interceptorProvider,
                final @NotNull ConnackOutboundProviderInputImpl connackOutboundProviderInput,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final @NotNull String pluginId) {

            this.interceptorProvider = interceptorProvider;
            this.connackOutboundProviderInput = connackOutboundProviderInput;
            this.interceptorFuture = interceptorFuture;
            this.pluginId = pluginId;
        }

        @Override
        public @NotNull ConnackOutboundOutputImpl apply(
                final @NotNull ConnackOutboundInputImpl input,
                final @NotNull ConnackOutboundOutputImpl output) {

            try {
                final ConnackOutboundInterceptor interceptor =
                        interceptorProvider.getConnackOutboundInterceptor(connackOutboundProviderInput);
                if (interceptor != null && !interceptorFuture.isDone()) {
                    interceptor.onOutboundConnack(input, output);
                }
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on connack interception. The exception should be handled by the extension.",
                        pluginId);
                log.debug("Original exception:", e);
                output.forciblyDisconnect();
                Exceptions.rethrowError(e);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptorProvider.getClass().getClassLoader();
        }
    }
}
