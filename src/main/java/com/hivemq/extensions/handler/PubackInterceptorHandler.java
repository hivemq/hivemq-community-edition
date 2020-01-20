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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.puback.PubackInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.puback.PubackOutboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.puback.PubackInboundInputImpl;
import com.hivemq.extensions.interceptor.puback.PubackInboundOutputImpl;
import com.hivemq.extensions.interceptor.puback.PubackOutboundInputImpl;
import com.hivemq.extensions.interceptor.puback.PubackOutboundOutputImpl;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yannick Weber
 * @author Robin Atherton
 */
@Singleton
@ChannelHandler.Sharable
public class PubackInterceptorHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(PubackInterceptorHandler.class);

    private final @NotNull FullConfigurationService configurationService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull PluginTaskExecutorService executorService;

    @Inject
    public PubackInterceptorHandler(
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
        if (!(msg instanceof PUBACK)) {
            ctx.fireChannelRead(msg);
            return;
        }
        handleInboundPuback(ctx, (PUBACK) msg);
    }

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull Object msg,
            final @NotNull ChannelPromise promise) {

        if (!(msg instanceof PUBACK)) {
            ctx.write(msg, promise);
            return;
        }
        handleOutboundPuback(ctx, (PUBACK) msg, promise);
    }

    private void handleInboundPuback(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBACK puback) {
        final Channel channel = ctx.channel();

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.fireChannelRead(puback);
            return;
        }
        final List<PubackInboundInterceptor> interceptors = clientContext.getPubackInboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.fireChannelRead(puback);
            return;
        }

        final PubackInboundOutputImpl output = new PubackInboundOutputImpl(configurationService, asyncer, puback);
        final PubackInboundInputImpl input = new PubackInboundInputImpl(clientId, channel, puback);

        final PubackInboundInterceptorContext interceptorContext =
                new PubackInboundInterceptorContext(clientId, input, ctx, interceptors.size());

        for (final PubackInboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            if (extension == null) {
                interceptorContext.increment(output);
                continue;
            }

            final PubackInboundInterceptorTask interceptorTask =
                    new PubackInboundInterceptorTask(interceptor, extension.getId());

            executorService.handlePluginInOutTaskExecution(interceptorContext, input, output, interceptorTask);
        }
    }

    private void handleOutboundPuback(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PUBACK puback,
            final @NotNull ChannelPromise promise) {

        final Channel channel = ctx.channel();

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.write(puback, promise);
            return;
        }
        final List<PubackOutboundInterceptor> interceptors = clientContext.getPubackOutboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.write(puback, promise);
            return;
        }

        final PubackOutboundOutputImpl output = new PubackOutboundOutputImpl(configurationService, asyncer, puback);
        final PubackOutboundInputImpl input = new PubackOutboundInputImpl(clientId, channel, puback);

        final PubackOutboundInterceptorContext interceptorContext =
                new PubackOutboundInterceptorContext(clientId, input, ctx, promise, interceptors.size());

        for (final PubackOutboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            if (extension == null) {
                interceptorContext.increment(output);
                continue;
            }

            final PubackOutboundInterceptorTask interceptorTask =
                    new PubackOutboundInterceptorTask(interceptor, extension.getId());

            executorService.handlePluginInOutTaskExecution(interceptorContext, input, output, interceptorTask);
        }
    }

    private static class PubackInboundInterceptorContext extends PluginInOutTaskContext<PubackInboundOutputImpl> {

        private final @NotNull PubackInboundInputImpl input;
        private final @NotNull ChannelHandlerContext ctx;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PubackInboundInterceptorContext(
                final @NotNull String clientId,
                final @NotNull PubackInboundInputImpl input,
                final @NotNull ChannelHandlerContext ctx,
                final int interceptorCount) {

            super(clientId);
            this.input = input;
            this.ctx = ctx;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(final @NotNull PubackInboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on inbound PUBACK interception.");
                output.update(input.getPubackPacket());
            } else if (output.getPubackPacket().isModified()) {
                input.update(output.getPubackPacket());
            }
            increment(output);
        }

        public void increment(final @NotNull PubackInboundOutputImpl output) {
            if (counter.incrementAndGet() == interceptorCount) {
                final PUBACK finalPuback = PUBACK.createPubackFrom(output.getPubackPacket());
                ctx.fireChannelRead(finalPuback);
            }
        }
    }

    private static class PubackInboundInterceptorTask
            implements PluginInOutTask<PubackInboundInputImpl, PubackInboundOutputImpl> {

        private final @NotNull PubackInboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PubackInboundInterceptorTask(
                final @NotNull PubackInboundInterceptor interceptor,
                final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PubackInboundOutputImpl apply(
                final @NotNull PubackInboundInputImpl input,
                final @NotNull PubackInboundOutputImpl output) {

            try {
                interceptor.onInboundPuback(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound puback interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original exception: ", e);
                output.update(input.getPubackPacket());
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

    private static class PubackOutboundInterceptorContext extends PluginInOutTaskContext<PubackOutboundOutputImpl> {

        private final @NotNull PubackOutboundInputImpl input;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PubackOutboundInterceptorContext(
                final @NotNull String clientId,
                final @NotNull PubackOutboundInputImpl input,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ChannelPromise promise,
                final int interceptorCount) {

            super(clientId);
            this.input = input;
            this.ctx = ctx;
            this.promise = promise;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(final @NotNull PubackOutboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on outbound PUBACK interception.");
                output.update(input.getPubackPacket());
            } else if (output.getPubackPacket().isModified()) {
                input.update(output.getPubackPacket());
            }
            increment(output);
        }

        public void increment(final @NotNull PubackOutboundOutputImpl output) {
            if (counter.incrementAndGet() == interceptorCount) {
                final PUBACK finalPubAck = PUBACK.createPubackFrom(output.getPubackPacket());
                ctx.writeAndFlush(finalPubAck, promise);
            }
        }
    }

    private static class PubackOutboundInterceptorTask
            implements PluginInOutTask<PubackOutboundInputImpl, PubackOutboundOutputImpl> {

        private final @NotNull PubackOutboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PubackOutboundInterceptorTask(
                final @NotNull PubackOutboundInterceptor interceptor,
                final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PubackOutboundOutputImpl apply(
                final @NotNull PubackOutboundInputImpl input,
                final @NotNull PubackOutboundOutputImpl output) {

            try {
                interceptor.onOutboundPuback(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound puback interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original exception: ", e);
                output.update(input.getPubackPacket());
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }
}
