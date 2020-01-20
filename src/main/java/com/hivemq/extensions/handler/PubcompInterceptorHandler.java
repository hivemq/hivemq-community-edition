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
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompOutboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.pubcomp.PubcompInboundInputImpl;
import com.hivemq.extensions.interceptor.pubcomp.PubcompInboundOutputImpl;
import com.hivemq.extensions.interceptor.pubcomp.PubcompOutboundInputImpl;
import com.hivemq.extensions.interceptor.pubcomp.PubcompOutboundOutputImpl;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
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
 * @author Silvio Giebl
 */
@Singleton
@ChannelHandler.Sharable
public class PubcompInterceptorHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(PubcompInterceptorHandler.class);

    private final @NotNull FullConfigurationService configurationService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull PluginTaskExecutorService executorService;

    @Inject
    public PubcompInterceptorHandler(
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
        if (!(msg instanceof PUBCOMP)) {
            ctx.fireChannelRead(msg);
            return;
        }
        handleInboundPubcomp(ctx, (PUBCOMP) msg);
    }

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull Object msg,
            final @NotNull ChannelPromise promise) {

        if (!(msg instanceof PUBCOMP)) {
            ctx.write(msg, promise);
            return;
        }
        handleOutboundPubcomp(ctx, (PUBCOMP) msg, promise);
    }

    private void handleOutboundPubcomp(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PUBCOMP pubcomp,
            final @NotNull ChannelPromise promise) {

        final Channel channel = ctx.channel();

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.write(pubcomp, promise);
            return;
        }
        final List<PubcompOutboundInterceptor> interceptors = clientContext.getPubcompOutboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.write(pubcomp, promise);
            return;
        }

        final PubcompOutboundOutputImpl output = new PubcompOutboundOutputImpl(configurationService, asyncer, pubcomp);
        final PubcompOutboundInputImpl input = new PubcompOutboundInputImpl(clientId, channel, pubcomp);

        final PubcompOutboundInterceptorContext interceptorContext =
                new PubcompOutboundInterceptorContext(clientId, input, ctx, promise, interceptors.size());

        for (final PubcompOutboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            // disabled extension would be null
            if (extension == null) {
                interceptorContext.increment(output);
                continue;
            }

            final PubcompOutboundInterceptorTask interceptorTask =
                    new PubcompOutboundInterceptorTask(interceptor, extension.getId());

            executorService.handlePluginInOutTaskExecution(interceptorContext, input, output, interceptorTask);
        }
    }

    private void handleInboundPubcomp(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBCOMP pubcomp) {
        final Channel channel = ctx.channel();

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.fireChannelRead(pubcomp);
            return;
        }
        final List<PubcompInboundInterceptor> interceptors = clientContext.getPubcompInboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.fireChannelRead(pubcomp);
            return;
        }

        final PubcompInboundOutputImpl output = new PubcompInboundOutputImpl(configurationService, asyncer, pubcomp);
        final PubcompInboundInputImpl input = new PubcompInboundInputImpl(clientId, channel, pubcomp);

        final PubcompInboundInterceptorContext interceptorContext =
                new PubcompInboundInterceptorContext(clientId, input, ctx, interceptors.size());

        for (final PubcompInboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            // disabled extension would be null
            if (extension == null) {
                interceptorContext.increment(output);
                continue;
            }

            final PubcompInboundInterceptorTask interceptorTask =
                    new PubcompInboundInterceptorTask(interceptor, extension.getId());

            executorService.handlePluginInOutTaskExecution(interceptorContext, input, output, interceptorTask);
        }
    }

    private static class PubcompInboundInterceptorContext extends PluginInOutTaskContext<PubcompInboundOutputImpl> {

        private final @NotNull PubcompInboundInputImpl input;
        private final @NotNull ChannelHandlerContext ctx;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PubcompInboundInterceptorContext(
                final @NotNull String clientId,
                final @NotNull PubcompInboundInputImpl input,
                final @NotNull ChannelHandlerContext ctx,
                final int interceptorCount) {

            super(clientId);
            this.input = input;
            this.ctx = ctx;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(final @NotNull PubcompInboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on inbound PUBCOMP interception.");
                output.update(input.getPubcompPacket());
            } else if (output.getPubcompPacket().isModified()) {
                input.update(output.getPubcompPacket());
            }
            increment(output);
        }

        public void increment(final @NotNull PubcompInboundOutputImpl output) {
            if (counter.incrementAndGet() == interceptorCount) {
                final PUBCOMP finalPubcomp = PUBCOMP.createPubcompFrom(output.getPubcompPacket());
                ctx.fireChannelRead(finalPubcomp);
            }
        }
    }

    private static class PubcompInboundInterceptorTask
            implements PluginInOutTask<PubcompInboundInputImpl, PubcompInboundOutputImpl> {

        private final @NotNull PubcompInboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PubcompInboundInterceptorTask(
                final @NotNull PubcompInboundInterceptor interceptor,
                final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PubcompInboundOutputImpl apply(
                final @NotNull PubcompInboundInputImpl input,
                final @NotNull PubcompInboundOutputImpl output) {

            try {
                interceptor.onInboundPubcomp(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound pubcomp interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original exception: ", e);
                output.update(input.getPubcompPacket());
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

    private static class PubcompOutboundInterceptorContext extends PluginInOutTaskContext<PubcompOutboundOutputImpl> {

        private final @NotNull PubcompOutboundInputImpl input;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PubcompOutboundInterceptorContext(
                final @NotNull String clientId,
                final @NotNull PubcompOutboundInputImpl input,
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
        public void pluginPost(final @NotNull PubcompOutboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on outbound PUBCOMP interception.");
                output.update(input.getPubcompPacket());
            } else if (output.getPubcompPacket().isModified()) {
                input.update(output.getPubcompPacket());
            }
            increment(output);
        }

        public void increment(final @NotNull PubcompOutboundOutputImpl output) {
            if (counter.incrementAndGet() == interceptorCount) {
                final PUBCOMP finalPubcomp = PUBCOMP.createPubcompFrom(output.getPubcompPacket());
                ctx.writeAndFlush(finalPubcomp, promise);
            }
        }
    }

    private static class PubcompOutboundInterceptorTask
            implements PluginInOutTask<PubcompOutboundInputImpl, PubcompOutboundOutputImpl> {

        private final @NotNull PubcompOutboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PubcompOutboundInterceptorTask(
                final @NotNull PubcompOutboundInterceptor interceptor,
                final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PubcompOutboundOutputImpl apply(
                final @NotNull PubcompOutboundInputImpl input,
                final @NotNull PubcompOutboundOutputImpl output) {

            try {
                interceptor.onOutboundPubcomp(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound pubcomp interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original exception: ", e);
                output.update(input.getPubcompPacket());
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }
}
