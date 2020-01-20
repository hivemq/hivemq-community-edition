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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectOutboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.disconnect.DisconnectInboundInputImpl;
import com.hivemq.extensions.interceptor.disconnect.DisconnectInboundOutputImpl;
import com.hivemq.extensions.interceptor.disconnect.DisconnectOutboundInputImpl;
import com.hivemq.extensions.interceptor.disconnect.DisconnectOutboundOutputImpl;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
@Singleton
@ChannelHandler.Sharable
public class DisconnectInterceptorHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(DisconnectInterceptorHandler.class);

    private final @NotNull FullConfigurationService configurationService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull PluginTaskExecutorService executorService;

    @Inject
    public DisconnectInterceptorHandler(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull PluginTaskExecutorService executorService) {

        this.configurationService = configurationService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.executorService = executorService;
    }

    @Override
    public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) {
        if (!(msg instanceof DISCONNECT)) {
            ctx.fireChannelRead(msg);
            return;
        }
        handleInboundDisconnect(ctx, (DISCONNECT) msg);
    }

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull Object msg,
            final @NotNull ChannelPromise promise) {

        if (!(msg instanceof DISCONNECT)) {
            ctx.write(msg, promise);
            return;
        }
        handleOutboundDisconnect(ctx, (DISCONNECT) msg, promise);
    }

    private void handleInboundDisconnect(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull DISCONNECT disconnect) {

        final Channel channel = ctx.channel();

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.fireChannelRead(disconnect);
            return;
        }
        final List<DisconnectInboundInterceptor> interceptors = clientContext.getDisconnectInboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.fireChannelRead(disconnect);
            return;
        }

        channel.config().setOption(ChannelOption.ALLOW_HALF_CLOSURE, true);

        final Long originalSessionExpiryInterval = channel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).get();
        final DisconnectInboundOutputImpl output = new DisconnectInboundOutputImpl(
                configurationService, asyncer, disconnect, originalSessionExpiryInterval);

        final DisconnectInboundInputImpl input = new DisconnectInboundInputImpl(clientId, channel, disconnect);

        final DisconnectInboundInterceptorContext interceptorContext =
                new DisconnectInboundInterceptorContext(clientId, input, ctx, interceptors.size());

        for (final DisconnectInboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            if (extension == null) {
                interceptorContext.increment(output);
                continue;
            }

            final DisconnectInboundInterceptorTask interceptorTask =
                    new DisconnectInboundInterceptorTask(interceptor, extension.getId());

            executorService.handlePluginInOutTaskExecution(interceptorContext, input, output, interceptorTask);
        }
    }

    private void handleOutboundDisconnect(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull DISCONNECT disconnect,
            final @NotNull ChannelPromise promise) {

        final Channel channel = ctx.channel();

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.write(disconnect, promise);
            return;
        }
        final List<DisconnectOutboundInterceptor> interceptors = clientContext.getDisconnectOutboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.write(disconnect, promise);
            return;
        }

        final DisconnectOutboundInputImpl input = new DisconnectOutboundInputImpl(clientId, channel, disconnect);

        final DisconnectOutboundOutputImpl output =
                new DisconnectOutboundOutputImpl(configurationService, asyncer, disconnect);

        final DisconnectOutboundInterceptorContext interceptorContext =
                new DisconnectOutboundInterceptorContext(clientId, input, ctx, promise, interceptors.size());

        for (final DisconnectOutboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            if (extension == null) {
                interceptorContext.increment(output);
                continue;
            }

            final DisconnectOutboundInterceptorTask interceptorTask =
                    new DisconnectOutboundInterceptorTask(interceptor, extension.getId());

            executorService.handlePluginInOutTaskExecution(interceptorContext, input, output, interceptorTask);
        }
    }

    static class DisconnectOutboundInterceptorContext extends PluginInOutTaskContext<DisconnectOutboundOutputImpl> {

        private final @NotNull DisconnectOutboundInputImpl input;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        DisconnectOutboundInterceptorContext(
                final @NotNull String identifier,
                final @NotNull DisconnectOutboundInputImpl input,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ChannelPromise promise,
                final int interceptorCount) {

            super(identifier);
            this.input = input;
            this.ctx = ctx;
            this.promise = promise;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(final @NotNull DisconnectOutboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on inbound DISCONNECT interception.");
                output.update(input.getDisconnectPacket());
            } else if (output.getDisconnectPacket().isModified()) {
                input.update(output.getDisconnectPacket());
            }
            increment(output);
        }

        public void increment(final @NotNull DisconnectOutboundOutputImpl output) {
            if (counter.incrementAndGet() == interceptorCount) {
                final DISCONNECT finalDisconnect = DISCONNECT.createDisconnectFrom(output.getDisconnectPacket());
                ctx.writeAndFlush(finalDisconnect, promise);
            }
        }
    }

    private static class DisconnectOutboundInterceptorTask
            implements PluginInOutTask<DisconnectOutboundInputImpl, DisconnectOutboundOutputImpl> {

        private final @NotNull DisconnectOutboundInterceptor interceptor;
        private final @NotNull String extensionId;

        DisconnectOutboundInterceptorTask(
                final @NotNull DisconnectOutboundInterceptor interceptor,
                final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull DisconnectOutboundOutputImpl apply(
                final @NotNull DisconnectOutboundInputImpl input,
                final @NotNull DisconnectOutboundOutputImpl output) {

            try {
                interceptor.onOutboundDisconnect(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound disconnect interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original exception: ", e);
                output.update(input.getDisconnectPacket());
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

    private static class DisconnectInboundInterceptorContext
            extends PluginInOutTaskContext<DisconnectInboundOutputImpl> {

        private final @NotNull DisconnectInboundInputImpl input;
        private final @NotNull ChannelHandlerContext ctx;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        DisconnectInboundInterceptorContext(
                final @NotNull String identifier,
                final @NotNull DisconnectInboundInputImpl input,
                final @NotNull ChannelHandlerContext ctx,
                final int interceptorCount) {

            super(identifier);
            this.input = input;
            this.ctx = ctx;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(final @NotNull DisconnectInboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on inbound DISCONNECT interception.");
                output.update(input.getDisconnectPacket());
            } else if (output.getDisconnectPacket().isModified()) {
                input.update(output.getDisconnectPacket());
            }
            increment(output);
        }

        public void increment(final @NotNull DisconnectInboundOutputImpl output) {
            if (counter.incrementAndGet() == interceptorCount) {
                final DISCONNECT finalDisconnect = DISCONNECT.createDisconnectFrom(output.getDisconnectPacket());
                ctx.fireChannelRead(finalDisconnect);
            }
        }
    }

    private static class DisconnectInboundInterceptorTask
            implements PluginInOutTask<DisconnectInboundInputImpl, DisconnectInboundOutputImpl> {

        private final @NotNull DisconnectInboundInterceptor interceptor;
        private final @NotNull String extensionId;

        DisconnectInboundInterceptorTask(
                final @NotNull DisconnectInboundInterceptor interceptor,
                final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull DisconnectInboundOutputImpl apply(
                final @NotNull DisconnectInboundInputImpl input,
                final @NotNull DisconnectInboundOutputImpl output) {

            try {
                interceptor.onInboundDisconnect(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound disconnect interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original exception: ", e);
                output.update(input.getDisconnectPacket());
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }
}
