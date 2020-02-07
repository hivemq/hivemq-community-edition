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

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelOutboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.pubrel.PubrelInboundInputImpl;
import com.hivemq.extensions.interceptor.pubrel.PubrelInboundOutputImpl;
import com.hivemq.extensions.interceptor.pubrel.PubrelOutboundInputImpl;
import com.hivemq.extensions.interceptor.pubrel.PubrelOutboundOutputImpl;
import com.hivemq.mqtt.message.pubrel.PUBREL;
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
public class PubrelInterceptorHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(PubrelInterceptorHandler.class);

    private final @NotNull FullConfigurationService configurationService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull PluginTaskExecutorService executorService;

    @Inject
    public PubrelInterceptorHandler(
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
        if (!(msg instanceof PUBREL)) {
            ctx.fireChannelRead(msg);
            return;
        }
        handleInboundPubrel(ctx, (PUBREL) msg);
    }

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull Object msg,
            final @NotNull ChannelPromise promise) {

        if (!(msg instanceof PUBREL)) {
            ctx.write(msg, promise);
            return;
        }
        handleOutboundPubrel(ctx, (PUBREL) msg, promise);
    }

    private void handleOutboundPubrel(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PUBREL pubrel,
            final @NotNull ChannelPromise promise) {

        final Channel channel = ctx.channel();

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.write(pubrel, promise);
            return;
        }
        final List<PubrelOutboundInterceptor> interceptors = clientContext.getPubrelOutboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.write(pubrel, promise);
            return;
        }

        final PubrelOutboundOutputImpl output = new PubrelOutboundOutputImpl(configurationService, asyncer, pubrel);
        final PubrelOutboundInputImpl input = new PubrelOutboundInputImpl(clientId, channel, pubrel);

        final PubrelOutboundInterceptorContext interceptorContext =
                new PubrelOutboundInterceptorContext(clientId, input, ctx, promise, interceptors.size());

        for (final PubrelOutboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            // disabled extension would be null
            if (extension == null) {
                interceptorContext.increment(output);
                continue;
            }
            final PubrelOutboundInterceptorTask interceptorTask =
                    new PubrelOutboundInterceptorTask(interceptor, extension.getId());

            executorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }
    }

    private void handleInboundPubrel(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBREL pubrel) {
        final Channel channel = ctx.channel();

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.fireChannelRead(pubrel);
            return;
        }
        final List<PubrelInboundInterceptor> interceptors = clientContext.getPubrelInboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.fireChannelRead(pubrel);
            return;
        }

        final PubrelInboundOutputImpl output = new PubrelInboundOutputImpl(configurationService, asyncer, pubrel);
        final PubrelInboundInputImpl input = new PubrelInboundInputImpl(clientId, channel, pubrel);

        final PubrelInboundInterceptorContext interceptorContext =
                new PubrelInboundInterceptorContext(clientId, input, ctx, interceptors.size());

        for (final PubrelInboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            // disabled extension would be null
            if (extension == null) {
                interceptorContext.increment(output);
                continue;
            }
            final PubrelInboundInterceptorTask interceptorTask =
                    new PubrelInboundInterceptorTask(interceptor, extension.getId());

            executorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }
    }

    private static class PubrelInboundInterceptorContext extends PluginInOutTaskContext<PubrelInboundOutputImpl> {

        private final @NotNull PubrelInboundInputImpl input;
        private final @NotNull ChannelHandlerContext ctx;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PubrelInboundInterceptorContext(
                final @NotNull String clientId,
                final @NotNull PubrelInboundInputImpl input,
                final @NotNull ChannelHandlerContext ctx,
                final int interceptorCount) {

            super(clientId);
            this.input = input;
            this.ctx = ctx;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(final @NotNull PubrelInboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on inbound PUBREL interception.");
                output.update(input.getPubrelPacket());
            } else if (output.getPubrelPacket().isModified()) {
                input.update(output.getPubrelPacket());
            }
            increment(output);
        }

        public void increment(final @NotNull PubrelInboundOutputImpl output) {
            if (counter.incrementAndGet() == interceptorCount) {
                final PUBREL finalPubrel = PUBREL.createPubrelFrom(output.getPubrelPacket());
                ctx.fireChannelRead(finalPubrel);
            }
        }
    }

    private static class PubrelInboundInterceptorTask
            implements PluginInOutTask<PubrelInboundInputImpl, PubrelInboundOutputImpl> {

        private final @NotNull PubrelInboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PubrelInboundInterceptorTask(
                final @NotNull PubrelInboundInterceptor interceptor,
                final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PubrelInboundOutputImpl apply(
                final @NotNull PubrelInboundInputImpl input,
                final @NotNull PubrelInboundOutputImpl output) {

            try {
                interceptor.onInboundPubrel(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound pubrel interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original exception: ", e);
                output.update(input.getPubrelPacket());
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

    private static class PubrelOutboundInterceptorContext extends PluginInOutTaskContext<PubrelOutboundOutputImpl> {

        private final @NotNull PubrelOutboundInputImpl input;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PubrelOutboundInterceptorContext(
                final @NotNull String clientId,
                final @NotNull PubrelOutboundInputImpl input,
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
        public void pluginPost(final @NotNull PubrelOutboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on outbound PUBREL interception.");
                output.update(input.getPubrelPacket());
            } else if (output.getPubrelPacket().isModified()) {
                input.update(output.getPubrelPacket());
            }
            increment(output);
        }

        public void increment(final @NotNull PubrelOutboundOutputImpl output) {
            if (counter.incrementAndGet() == interceptorCount) {
                final PUBREL finalPubrel = PUBREL.createPubrelFrom(output.getPubrelPacket());
                ctx.writeAndFlush(finalPubrel, promise);
            }
        }
    }

    private static class PubrelOutboundInterceptorTask
            implements PluginInOutTask<PubrelOutboundInputImpl, PubrelOutboundOutputImpl> {

        private final @NotNull PubrelOutboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PubrelOutboundInterceptorTask(
                final @NotNull PubrelOutboundInterceptor interceptor,
                final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PubrelOutboundOutputImpl apply(
                final @NotNull PubrelOutboundInputImpl input,
                final @NotNull PubrelOutboundOutputImpl output) {

            try {
                interceptor.onOutboundPubrel(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound pubrel interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original exception: ", e);
                output.update(input.getPubrelPacket());
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }
}
