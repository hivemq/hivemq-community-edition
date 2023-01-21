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
package com.hivemq.extensions.handler;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.pingreq.PingReqInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pingresp.PingRespOutboundInterceptor;
import com.hivemq.extensions.ExtensionInformationUtil;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
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
import com.hivemq.util.Exceptions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
@Singleton
public class PingInterceptorHandler {

    private static final Logger log = LoggerFactory.getLogger(PingInterceptorHandler.class);

    private final @NotNull PluginTaskExecutorService executorService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;

    @Inject
    public PingInterceptorHandler(
            final @NotNull PluginTaskExecutorService executorService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions) {

        this.executorService = executorService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
    }


    public void handleInboundPingReq(final @NotNull ChannelHandlerContext ctx, final @NotNull PINGREQ pingreq) {
        final Channel channel = ctx.channel();
        final ClientConnection clientConnection = channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        final String clientId = clientConnection.getClientId();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = clientConnection.getExtensionClientContext();
        if (clientContext == null) {
            ctx.fireChannelRead(pingreq);
            return;
        }
        final List<PingReqInboundInterceptor> interceptors = clientContext.getPingReqInboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.fireChannelRead(pingreq);
            return;
        }

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);

        final PingReqInboundInputImpl input = new PingReqInboundInputImpl(clientInfo, connectionInfo);
        final PingReqInboundOutputImpl output = new PingReqInboundOutputImpl(asyncer);
        final PingReqInboundInterceptorContext context =
                new PingReqInboundInterceptorContext(clientId, interceptors.size(), ctx);

        for (final PingReqInboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(interceptor.getClass().getClassLoader());
            if (extension == null) {
                context.finishInterceptor();
                continue;
            }

            final PingReqInboundInterceptorTask task =
                    new PingReqInboundInterceptorTask(interceptor, extension.getId());
            executorService.handlePluginInOutTaskExecution(context, input, output, task);
        }
    }

    public void handleOutboundPingResp(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PINGRESP pingresp,
            final @NotNull ChannelPromise promise) {

        final Channel channel = ctx.channel();
        final ClientConnection clientConnection = channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        final String clientId = clientConnection.getClientId();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = clientConnection.getExtensionClientContext();
        if (clientContext == null) {
            ctx.write(pingresp, promise);
            return;
        }
        final List<PingRespOutboundInterceptor> interceptors = clientContext.getPingRespOutboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.write(pingresp, promise);
            return;
        }

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);

        final PingRespOutboundInputImpl input = new PingRespOutboundInputImpl(clientInfo, connectionInfo);
        final PingRespOutboundOutputImpl output = new PingRespOutboundOutputImpl(asyncer);
        final PingRespOutboundInterceptorContext context =
                new PingRespOutboundInterceptorContext(clientId, interceptors.size(), ctx, promise);

        for (final PingRespOutboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(interceptor.getClass().getClassLoader());
            if (extension == null) {
                context.finishInterceptor();
                continue;
            }

            final PingRespOutboundInterceptorTask task =
                    new PingRespOutboundInterceptorTask(interceptor, extension.getId());
            executorService.handlePluginInOutTaskExecution(context, input, output, task);
        }
    }

    private static class PingReqInboundInterceptorTask
            implements PluginInOutTask<PingReqInboundInputImpl, PingReqInboundOutputImpl> {

        private final @NotNull PingReqInboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PingReqInboundInterceptorTask(
                final @NotNull PingReqInboundInterceptor interceptor, final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PingReqInboundOutputImpl apply(
                final @NotNull PingReqInboundInputImpl input, final @NotNull PingReqInboundOutputImpl output) {

            try {
                interceptor.onInboundPingReq(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound PINGREQ interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId, e);
                Exceptions.rethrowError(e);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

    private static class PingRespOutboundInterceptorTask
            implements PluginInOutTask<PingRespOutboundInputImpl, PingRespOutboundOutputImpl> {

        private final @NotNull PingRespOutboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PingRespOutboundInterceptorTask(
                final @NotNull PingRespOutboundInterceptor interceptor, final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PingRespOutboundOutputImpl apply(
                final @NotNull PingRespOutboundInputImpl input, final @NotNull PingRespOutboundOutputImpl output) {

            try {
                interceptor.onOutboundPingResp(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound PINGRESP interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId, e);
                Exceptions.rethrowError(e);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

    private static class PingReqInboundInterceptorContext extends PluginInOutTaskContext<PingReqInboundOutputImpl>
            implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;

        PingReqInboundInterceptorContext(
                final @NotNull String identifier,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx) {

            super(identifier);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
        }

        @Override
        public void pluginPost(final @NotNull PingReqInboundOutputImpl pluginOutput) {
            finishInterceptor();
        }

        public void finishInterceptor() {
            if (counter.incrementAndGet() == interceptorCount) {
                ctx.executor().execute(this);
            }
        }

        @Override
        public void run() {
            ctx.fireChannelRead(PINGREQ.INSTANCE);
        }
    }

    private static class PingRespOutboundInterceptorContext extends PluginInOutTaskContext<PingRespOutboundOutputImpl>
            implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;

        PingRespOutboundInterceptorContext(
                final @NotNull String identifier,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ChannelPromise promise) {

            super(identifier);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
            this.promise = promise;
        }

        public void pluginPost(final @NotNull PingRespOutboundOutputImpl pluginOutput) {
            finishInterceptor();
        }

        public void finishInterceptor() {
            if (counter.incrementAndGet() == interceptorCount) {
                ctx.executor().execute(this);
            }
        }

        @Override
        public void run() {
            ctx.writeAndFlush(PINGRESP.INSTANCE, promise);
        }
    }
}
