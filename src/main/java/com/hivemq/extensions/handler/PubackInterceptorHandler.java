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

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.puback.PubackInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.puback.PubackOutboundInterceptor;
import com.hivemq.extensions.ExtensionInformationUtil;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.puback.parameter.PubackInboundInputImpl;
import com.hivemq.extensions.interceptor.puback.parameter.PubackInboundOutputImpl;
import com.hivemq.extensions.interceptor.puback.parameter.PubackOutboundInputImpl;
import com.hivemq.extensions.interceptor.puback.parameter.PubackOutboundOutputImpl;
import com.hivemq.extensions.packets.puback.ModifiablePubackPacketImpl;
import com.hivemq.extensions.packets.puback.PubackPacketImpl;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
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
 * @author Silvio Giebl
 */
@Singleton
public class PubackInterceptorHandler {

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


    public void handleInboundPuback(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBACK puback) {
        final Channel channel = ctx.channel();
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.EXTENSION_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.fireChannelRead(puback);
            return;
        }
        final List<PubackInboundInterceptor> interceptors = clientContext.getPubackInboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.fireChannelRead(puback);
            return;
        }

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);

        final PubackPacketImpl packet = new PubackPacketImpl(puback);
        final PubackInboundInputImpl input = new PubackInboundInputImpl(clientInfo, connectionInfo, packet);
        final ExtensionParameterHolder<PubackInboundInputImpl> inputHolder = new ExtensionParameterHolder<>(input);

        final ModifiablePubackPacketImpl modifiablePacket =
                new ModifiablePubackPacketImpl(packet, configurationService);
        final PubackInboundOutputImpl output = new PubackInboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<PubackInboundOutputImpl> outputHolder = new ExtensionParameterHolder<>(output);

        final PubackInboundInterceptorContext context =
                new PubackInboundInterceptorContext(clientId, interceptors.size(), ctx, inputHolder, outputHolder);

        for (final PubackInboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(interceptor.getClass().getClassLoader());
            if (extension == null) {
                context.finishInterceptor();
                continue;
            }

            final PubackInboundInterceptorTask task = new PubackInboundInterceptorTask(interceptor, extension.getId());
            executorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
    }

    public void handleOutboundPuback(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PUBACK puback,
            final @NotNull ChannelPromise promise) {

        final Channel channel = ctx.channel();
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.EXTENSION_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.write(puback, promise);
            return;
        }
        final List<PubackOutboundInterceptor> interceptors = clientContext.getPubackOutboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.write(puback, promise);
            return;
        }

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);

        final PubackPacketImpl packet = new PubackPacketImpl(puback);
        final PubackOutboundInputImpl input = new PubackOutboundInputImpl(clientInfo, connectionInfo, packet);
        final ExtensionParameterHolder<PubackOutboundInputImpl> inputHolder = new ExtensionParameterHolder<>(input);

        final ModifiablePubackPacketImpl modifiablePacket =
                new ModifiablePubackPacketImpl(packet, configurationService);
        final PubackOutboundOutputImpl output = new PubackOutboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<PubackOutboundOutputImpl> outputHolder = new ExtensionParameterHolder<>(output);

        final PubackOutboundInterceptorContext context = new PubackOutboundInterceptorContext(
                clientId, interceptors.size(), ctx, promise, inputHolder, outputHolder);

        for (final PubackOutboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(interceptor.getClass().getClassLoader());
            if (extension == null) {
                context.finishInterceptor();
                continue;
            }

            final PubackOutboundInterceptorTask task =
                    new PubackOutboundInterceptorTask(interceptor, extension.getId());
            executorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
    }

    private static class PubackInboundInterceptorContext extends PluginInOutTaskContext<PubackInboundOutputImpl>
            implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ExtensionParameterHolder<PubackInboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<PubackInboundOutputImpl> outputHolder;

        PubackInboundInterceptorContext(
                final @NotNull String clientId,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ExtensionParameterHolder<PubackInboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<PubackInboundOutputImpl> outputHolder) {

            super(clientId);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull PubackInboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on inbound PUBACK interception. Discarding changes made by the interceptor.");
            } else if (output.isFailed()) {
                log.debug("Exception on inbound PUBACK interception. Discarding changes made by the interceptor.");
            } else if (output.getPubackPacket().isModified()) {
                inputHolder.set(inputHolder.get().update(output));
            }
            if (!finishInterceptor()) {
                outputHolder.set(output.update(inputHolder.get()));
            }
        }

        public boolean finishInterceptor() {
            if (counter.incrementAndGet() == interceptorCount) {
                ctx.executor().execute(this);
                return true;
            }
            return false;
        }

        @Override
        public void run() {
            ctx.fireChannelRead(PUBACK.from(inputHolder.get().getPubackPacket()));
        }
    }

    private static class PubackInboundInterceptorTask
            implements PluginInOutTask<PubackInboundInputImpl, PubackInboundOutputImpl> {

        private final @NotNull PubackInboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PubackInboundInterceptorTask(
                final @NotNull PubackInboundInterceptor interceptor, final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PubackInboundOutputImpl apply(
                final @NotNull PubackInboundInputImpl input, final @NotNull PubackInboundOutputImpl output) {

            try {
                interceptor.onInboundPuback(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound PUBACK interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original exception: ", e);
                output.markAsFailed();
                Exceptions.rethrowError(e);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

    private static class PubackOutboundInterceptorContext extends PluginInOutTaskContext<PubackOutboundOutputImpl>
            implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        private final @NotNull ExtensionParameterHolder<PubackOutboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<PubackOutboundOutputImpl> outputHolder;

        PubackOutboundInterceptorContext(
                final @NotNull String clientId,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ChannelPromise promise,
                final @NotNull ExtensionParameterHolder<PubackOutboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<PubackOutboundOutputImpl> outputHolder) {

            super(clientId);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
            this.promise = promise;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull PubackOutboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on outbound PUBACK interception. Discarding changes made by the interceptor.");
            } else if (output.isFailed()) {
                log.debug("Exception on outbound PUBACK interception. Discarding changes made by the interceptor.");
            } else if (output.getPubackPacket().isModified()) {
                inputHolder.set(inputHolder.get().update(output));
            }
            if (!finishInterceptor()) {
                outputHolder.set(output.update(inputHolder.get()));
            }
        }

        public boolean finishInterceptor() {
            if (counter.incrementAndGet() == interceptorCount) {
                ctx.executor().execute(this);
                return true;
            }
            return false;
        }

        @Override
        public void run() {
            ctx.writeAndFlush(PUBACK.from(inputHolder.get().getPubackPacket()), promise);
        }
    }

    private static class PubackOutboundInterceptorTask
            implements PluginInOutTask<PubackOutboundInputImpl, PubackOutboundOutputImpl> {

        private final @NotNull PubackOutboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PubackOutboundInterceptorTask(
                final @NotNull PubackOutboundInterceptor interceptor, final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PubackOutboundOutputImpl apply(
                final @NotNull PubackOutboundInputImpl input, final @NotNull PubackOutboundOutputImpl output) {

            try {
                interceptor.onOutboundPuback(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound PUBACK interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original exception: ", e);
                output.markAsFailed();
                Exceptions.rethrowError(e);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }
}
