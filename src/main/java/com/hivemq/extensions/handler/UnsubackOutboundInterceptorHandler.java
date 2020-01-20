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
import com.hivemq.extension.sdk.api.interceptor.unsuback.UnsubackOutboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.unsuback.parameter.UnsubackOutboundInputImpl;
import com.hivemq.extensions.interceptor.unsuback.parameter.UnsubackOutboundOutputImpl;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Robin Atherton
 */
@Singleton
@ChannelHandler.Sharable
public class UnsubackOutboundInterceptorHandler extends ChannelOutboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(UnsubackOutboundInterceptorHandler.class);

    private final @NotNull FullConfigurationService configurationService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull PluginTaskExecutorService executorService;

    @Inject
    public UnsubackOutboundInterceptorHandler(
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
    public void write(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull Object msg,
            final @NotNull ChannelPromise promise) {

        if (!(msg instanceof UNSUBACK)) {
            ctx.write(msg, promise);
            return;
        }
        handleOutboundUnsuback(ctx, (UNSUBACK) msg, promise);
    }

    private void handleOutboundUnsuback(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull UNSUBACK unsuback,
            final @NotNull ChannelPromise promise) {

        final Channel channel = ctx.channel();

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.write(unsuback, promise);
            return;
        }
        final List<UnsubackOutboundInterceptor> interceptors = clientContext.getUnsubackOutboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.write(unsuback, promise);
            return;
        }

        final UnsubackOutboundInputImpl input = new UnsubackOutboundInputImpl(clientId, channel, unsuback);
        final UnsubackOutboundOutputImpl output =
                new UnsubackOutboundOutputImpl(configurationService, asyncer, unsuback);

        final UnsubackOutboundInterceptorContext interceptorContext =
                new UnsubackOutboundInterceptorContext(clientId, input, ctx, promise, interceptors.size());

        for (final UnsubackOutboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            if (extension == null) {
                interceptorContext.increment(output);
                continue;
            }

            final UnsubackOutboundInterceptorTask interceptorTask =
                    new UnsubackOutboundInterceptorTask(interceptor, extension.getId());

            executorService.handlePluginInOutTaskExecution(interceptorContext, input, output, interceptorTask);
        }
    }

    private static class UnsubackOutboundInterceptorContext extends PluginInOutTaskContext<UnsubackOutboundOutputImpl> {

        private final @NotNull UnsubackOutboundInputImpl input;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        UnsubackOutboundInterceptorContext(
                final @NotNull String identifier,
                final @NotNull UnsubackOutboundInputImpl input,
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
        public void pluginPost(final @NotNull UnsubackOutboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on outbound UNSUBACK interception.");
                output.update(input.getUnsubackPacket());
            } else if (output.getUnsubackPacket().isModified()) {
                input.update(output.getUnsubackPacket());
            }
            increment(output);
        }

        public void increment(final @NotNull UnsubackOutboundOutputImpl output) {
            if (counter.incrementAndGet() == interceptorCount) {
                final UNSUBACK unsuback = UNSUBACK.createUnsubackFrom(output.getUnsubackPacket());
                ctx.writeAndFlush(unsuback, promise);
            }
        }
    }

    private static class UnsubackOutboundInterceptorTask
            implements PluginInOutTask<UnsubackOutboundInputImpl, UnsubackOutboundOutputImpl> {

        private final @NotNull UnsubackOutboundInterceptor interceptor;
        private final @NotNull String extensionId;

        UnsubackOutboundInterceptorTask(
                final @NotNull UnsubackOutboundInterceptor interceptor,
                final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull UnsubackOutboundOutputImpl apply(
                final @NotNull UnsubackOutboundInputImpl input,
                final @NotNull UnsubackOutboundOutputImpl output) {

            try {
                interceptor.onOutboundUnsuback(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound UNSUBACK interception. " +
                                "Extensions are responsible to handle their own exceptions.", extensionId);
                log.debug("Original exception: ", e);
                output.update(input.getUnsubackPacket());
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }
}
