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
import com.hivemq.extension.sdk.api.interceptor.pingreq.PingReqInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pingresp.PingRespOutboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.pingreq.parameter.PingReqInboundInputImpl;
import com.hivemq.extensions.interceptor.pingreq.parameter.PingReqInboundOutputImpl;
import com.hivemq.extensions.interceptor.pingresp.parameter.PingRespOutboundInputImpl;
import com.hivemq.extensions.interceptor.pingresp.parameter.PingRespOutboundOutputImpl;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.PINGRESP;
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
@ChannelHandler.Sharable
@Singleton
public class PingInterceptorHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(PingInterceptorHandler.class);

    private final @NotNull PluginTaskExecutorService extensionTaskExecutorService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;

    @Inject
    public PingInterceptorHandler(
            final @NotNull PluginTaskExecutorService extensionTaskExecutorService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions) {

        this.extensionTaskExecutorService = extensionTaskExecutorService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
    }

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull Object msg,
            final @NotNull ChannelPromise promise) {

        if (!(msg instanceof PINGRESP)) {
            ctx.write(msg, promise);
            return;
        }
        handleOutboundPingResponse(ctx, (PINGRESP) msg, promise);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final @NotNull Object msg) {
        if (!(msg instanceof PINGREQ)) {
            ctx.fireChannelRead(msg);
            return;
        }
        handleInboundPingRequest(ctx, ((PINGREQ) msg));
    }

    private void handleInboundPingRequest(final @NotNull ChannelHandlerContext ctx, final @NotNull PINGREQ pingreq) {
        final Channel channel = ctx.channel();

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.fireChannelRead(pingreq);
            return;
        }

        final List<PingReqInboundInterceptor> interceptors = clientContext.getPingReqInboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.fireChannelRead(pingreq);
            return;
        }

        final PingReqInboundOutputImpl output = new PingReqInboundOutputImpl(asyncer);
        final PingReqInboundInputImpl input = new PingReqInboundInputImpl(clientId, channel);
        final PingRequestInboundInterceptorContext interceptorContext =
                new PingRequestInboundInterceptorContext(clientId, ctx, interceptors.size());

        for (final PingReqInboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            if (extension == null) {
                interceptorContext.increment();
                continue;
            }

            final PingRequestInboundInterceptorTask interceptorTask =
                    new PingRequestInboundInterceptorTask(interceptor, extension.getId());

            extensionTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }
    }

    private void handleOutboundPingResponse(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PINGRESP pingresp,
            final @NotNull ChannelPromise promise) {

        final Channel channel = ctx.channel();

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.write(pingresp, promise);
            return;
        }

        final List<PingRespOutboundInterceptor> interceptors = clientContext.getPingRespOutboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.write(pingresp, promise);
            return;
        }

        final PingRespOutboundInputImpl input = new PingRespOutboundInputImpl(clientId, channel);
        final PingRespOutboundOutputImpl output = new PingRespOutboundOutputImpl(asyncer);
        final PingResponseOutboundInterceptorContext interceptorContext =
                new PingResponseOutboundInterceptorContext(clientId, ctx, promise, interceptors.size());

        for (final PingRespOutboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            if (extension == null) {
                interceptorContext.increment();
                continue;
            }

            final PingResponseOutboundInterceptorTask interceptorTask =
                    new PingResponseOutboundInterceptorTask(interceptor, extension.getId());

            extensionTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }
    }

    private static class PingRequestInboundInterceptorTask
            implements PluginInOutTask<PingReqInboundInputImpl, PingReqInboundOutputImpl> {

        private final @NotNull PingReqInboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PingRequestInboundInterceptorTask(
                final @NotNull PingReqInboundInterceptor interceptor,
                final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PingReqInboundOutputImpl apply(
                final @NotNull PingReqInboundInputImpl pingRequestInboundInput,
                final @NotNull PingReqInboundOutputImpl pingRequestInboundOutput) {

            try {
                interceptor.onInboundPingReq(pingRequestInboundInput, pingRequestInboundOutput);
            } catch (final Throwable e) {
                log.debug(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound pingreq interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original Exception: ", e);
            }
            return pingRequestInboundOutput;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

    private static class PingResponseOutboundInterceptorTask
            implements PluginInOutTask<PingRespOutboundInputImpl, PingRespOutboundOutputImpl> {

        private final @NotNull PingRespOutboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PingResponseOutboundInterceptorTask(
                final @NotNull PingRespOutboundInterceptor interceptor,
                final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PingRespOutboundOutputImpl apply(
                final @NotNull PingRespOutboundInputImpl pingResponseOutboundInput,
                final @NotNull PingRespOutboundOutputImpl pingResponseOutboundOutput) {

            try {
                interceptor.onOutboundPingResp(pingResponseOutboundInput, pingResponseOutboundOutput);
            } catch (final Throwable e) {
                log.debug(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound pingresp interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original Exception: ", e);
            }
            return pingResponseOutboundOutput;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

    private static class PingRequestInboundInterceptorContext
            extends PluginInOutTaskContext<PingReqInboundOutputImpl> {

        private final @NotNull ChannelHandlerContext ctx;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PingRequestInboundInterceptorContext(
                final @NotNull String identifier,
                final @NotNull ChannelHandlerContext ctx,
                final int interceptorCount) {

            super(identifier);
            this.ctx = ctx;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(final @NotNull PingReqInboundOutputImpl output) {
            increment();
        }

        public void increment() {
            if (counter.incrementAndGet() == interceptorCount) {
                ctx.fireChannelRead(PINGREQ.INSTANCE);
            }
        }
    }

    private static class PingResponseOutboundInterceptorContext
            extends PluginInOutTaskContext<PingRespOutboundOutputImpl> {

        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PingResponseOutboundInterceptorContext(
                final @NotNull String identifier,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ChannelPromise promise,
                final int interceptorCount) {

            super(identifier);
            this.ctx = ctx;
            this.promise = promise;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        public void pluginPost(final @NotNull PingRespOutboundOutputImpl output) {
            increment();
        }

        public void increment() {
            if (counter.incrementAndGet() == interceptorCount) {
                ctx.writeAndFlush(PINGRESP.INSTANCE, promise);
            }
        }
    }
}
