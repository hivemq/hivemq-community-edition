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
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecOutboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.pubrec.PubrecInboundInputImpl;
import com.hivemq.extensions.interceptor.pubrec.PubrecInboundOutputImpl;
import com.hivemq.extensions.interceptor.pubrec.PubrecOutboundInputImpl;
import com.hivemq.extensions.interceptor.pubrec.PubrecOutboundOutputImpl;
import com.hivemq.mqtt.message.pubrec.PUBREC;
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
public class PubrecInterceptorHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(PubrecInterceptorHandler.class);

    private final @NotNull FullConfigurationService configurationService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;

    @Inject
    public PubrecInterceptorHandler(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService) {

        this.configurationService = configurationService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.pluginTaskExecutorService = pluginTaskExecutorService;
    }

    @Override
    public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) {
        if (!(msg instanceof PUBREC)) {
            ctx.fireChannelRead(msg);
            return;
        }
        handleInboundPubrec(ctx, (PUBREC) msg);
    }

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull Object msg,
            final @NotNull ChannelPromise promise) {

        if (!(msg instanceof PUBREC)) {
            ctx.write(msg, promise);
            return;
        }
        handleOutboundPubrec(ctx, (PUBREC) msg, promise);
    }

    private void handleOutboundPubrec(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PUBREC pubrec,
            final @NotNull ChannelPromise promise) {

        final Channel channel = ctx.channel();

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.write(pubrec, promise);
            return;
        }
        final List<PubrecOutboundInterceptor> interceptors = clientContext.getPubrecOutboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.write(pubrec, promise);
            return;
        }

        final PubrecOutboundOutputImpl output = new PubrecOutboundOutputImpl(configurationService, asyncer, pubrec);
        final PubrecOutboundInputImpl input = new PubrecOutboundInputImpl(clientId, channel, pubrec);

        final PubrecOutboundInterceptorContext interceptorContext =
                new PubrecOutboundInterceptorContext(clientId, input, ctx, promise, interceptors.size());

        for (final PubrecOutboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            // disabled extension would be null
            if (extension == null) {
                interceptorContext.increment(output);
                continue;
            }

            final PubrecOutboundInterceptorTask
                    interceptorTask = new PubrecOutboundInterceptorTask(interceptor, extension.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }
    }

    private void handleInboundPubrec(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBREC pubrec) {
        final Channel channel = ctx.channel();

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.fireChannelRead(pubrec);
            return;
        }
        final List<PubrecInboundInterceptor> interceptors = clientContext.getPubrecInboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.fireChannelRead(pubrec);
            return;
        }

        final PubrecInboundOutputImpl output = new PubrecInboundOutputImpl(configurationService, asyncer, pubrec);
        final PubrecInboundInputImpl input = new PubrecInboundInputImpl(clientId, channel, pubrec);

        final PubrecInboundInterceptorContext interceptorContext =
                new PubrecInboundInterceptorContext(clientId, input, ctx, interceptors.size());

        for (final PubrecInboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            // disabled extension would be null
            if (extension == null) {
                interceptorContext.increment(output);
                continue;
            }

            final PubrecInboundInterceptorTask interceptorTask =
                    new PubrecInboundInterceptorTask(interceptor, extension.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }
    }

    private static class PubrecInboundInterceptorContext extends PluginInOutTaskContext<PubrecInboundOutputImpl> {

        private final @NotNull PubrecInboundInputImpl input;
        private final @NotNull ChannelHandlerContext ctx;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PubrecInboundInterceptorContext(
                final @NotNull String clientId,
                final @NotNull PubrecInboundInputImpl input,
                final @NotNull ChannelHandlerContext ctx,
                final int interceptorCount) {

            super(clientId);
            this.input = input;
            this.ctx = ctx;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(final @NotNull PubrecInboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on inbound PUBREC interception.");
                output.update(input.getPubrecPacket());
            } else if (output.getPubrecPacket().isModified()) {
                input.update(output.getPubrecPacket());
            }
            increment(output);
        }

        public void increment(final @NotNull PubrecInboundOutputImpl output) {
            if (counter.incrementAndGet() == interceptorCount) {
                final PUBREC finalPubrec = PUBREC.createPubrecFrom(output.getPubrecPacket());
                ctx.fireChannelRead(finalPubrec);
            }
        }
    }

    private static class PubrecInboundInterceptorTask
            implements PluginInOutTask<PubrecInboundInputImpl, PubrecInboundOutputImpl> {

        private final @NotNull PubrecInboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PubrecInboundInterceptorTask(
                final @NotNull PubrecInboundInterceptor interceptor,
                final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PubrecInboundOutputImpl apply(
                final @NotNull PubrecInboundInputImpl input,
                final @NotNull PubrecInboundOutputImpl output) {

            try {
                interceptor.onInboundPubrec(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound pubrec interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original exception: ", e);
                output.update(input.getPubrecPacket());
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

    private static class PubrecOutboundInterceptorContext extends PluginInOutTaskContext<PubrecOutboundOutputImpl> {

        private final @NotNull PubrecOutboundInputImpl input;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PubrecOutboundInterceptorContext(
                final @NotNull String clientId,
                final @NotNull PubrecOutboundInputImpl input,
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
        public void pluginPost(final @NotNull PubrecOutboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on outbound PUBREC interception.");
                output.update(input.getPubrecPacket());
            } else if (output.getPubrecPacket().isModified()) {
                input.update(output.getPubrecPacket());
            }
            increment(output);
        }

        public void increment(final @NotNull PubrecOutboundOutputImpl output) {
            if (counter.incrementAndGet() == interceptorCount) {
                final PUBREC finalPubrec = PUBREC.createPubrecFrom(output.getPubrecPacket());
                ctx.writeAndFlush(finalPubrec, promise);
            }
        }
    }

    private static class PubrecOutboundInterceptorTask
            implements PluginInOutTask<PubrecOutboundInputImpl, PubrecOutboundOutputImpl> {

        private final @NotNull PubrecOutboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PubrecOutboundInterceptorTask(
                final @NotNull PubrecOutboundInterceptor interceptor,
                final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PubrecOutboundOutputImpl apply(
                final @NotNull PubrecOutboundInputImpl input, final @NotNull PubrecOutboundOutputImpl output) {

            try {
                interceptor.onOutboundPubrec(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound pubrec interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original exception: ", e);
                output.update(input.getPubrecPacket());
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }
}
