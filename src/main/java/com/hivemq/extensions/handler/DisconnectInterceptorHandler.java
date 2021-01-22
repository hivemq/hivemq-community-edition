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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectOutboundInterceptor;
import com.hivemq.extensions.ExtensionInformationUtil;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.disconnect.parameter.DisconnectInboundInputImpl;
import com.hivemq.extensions.interceptor.disconnect.parameter.DisconnectInboundOutputImpl;
import com.hivemq.extensions.interceptor.disconnect.parameter.DisconnectOutboundInputImpl;
import com.hivemq.extensions.interceptor.disconnect.parameter.DisconnectOutboundOutputImpl;
import com.hivemq.extensions.packets.disconnect.DisconnectPacketImpl;
import com.hivemq.extensions.packets.disconnect.ModifiableInboundDisconnectPacketImpl;
import com.hivemq.extensions.packets.disconnect.ModifiableOutboundDisconnectPacketImpl;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
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
public class DisconnectInterceptorHandler {

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

    public void handleInboundDisconnect(
            final @NotNull ChannelHandlerContext ctx, final @NotNull DISCONNECT disconnect) {

        final Channel channel = ctx.channel();
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.EXTENSION_CLIENT_CONTEXT).get();
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

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);
        final Long originalSessionExpiryInterval = channel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).get();

        final DisconnectPacketImpl packet = new DisconnectPacketImpl(disconnect);
        final DisconnectInboundInputImpl input = new DisconnectInboundInputImpl(clientInfo, connectionInfo, packet);
        final ExtensionParameterHolder<DisconnectInboundInputImpl> inputHolder = new ExtensionParameterHolder<>(input);

        final ModifiableInboundDisconnectPacketImpl modifiablePacket =
                new ModifiableInboundDisconnectPacketImpl(packet, configurationService, originalSessionExpiryInterval);
        final DisconnectInboundOutputImpl output = new DisconnectInboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<DisconnectInboundOutputImpl> outputHolder =
                new ExtensionParameterHolder<>(output);

        final DisconnectInboundInterceptorContext context =
                new DisconnectInboundInterceptorContext(clientId, interceptors.size(), ctx, inputHolder, outputHolder);

        for (final DisconnectInboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension =
                    hiveMQExtensions.getExtensionForClassloader(interceptor.getClass().getClassLoader());
            if (extension == null) {
                context.finishInterceptor();
                continue;
            }

            final DisconnectInboundInterceptorTask task =
                    new DisconnectInboundInterceptorTask(interceptor, extension.getId());
            executorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
    }

    public void handleOutboundDisconnect(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull DISCONNECT disconnect,
            final @NotNull ChannelPromise promise) {

        final Channel channel = ctx.channel();
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.EXTENSION_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.write(disconnect, promise);
            return;
        }
        final List<DisconnectOutboundInterceptor> interceptors = clientContext.getDisconnectOutboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.write(disconnect, promise);
            return;
        }

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);

        final DisconnectPacketImpl packet = new DisconnectPacketImpl(disconnect);
        final DisconnectOutboundInputImpl input = new DisconnectOutboundInputImpl(clientInfo, connectionInfo, packet);
        final ExtensionParameterHolder<DisconnectOutboundInputImpl> inputHolder = new ExtensionParameterHolder<>(input);

        final ModifiableOutboundDisconnectPacketImpl modifiablePacket =
                new ModifiableOutboundDisconnectPacketImpl(packet, configurationService);
        final DisconnectOutboundOutputImpl output = new DisconnectOutboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<DisconnectOutboundOutputImpl> outputHolder =
                new ExtensionParameterHolder<>(output);

        final DisconnectOutboundInterceptorContext context = new DisconnectOutboundInterceptorContext(clientId,
                interceptors.size(),
                ctx,
                promise,
                inputHolder,
                outputHolder);

        for (final DisconnectOutboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension =
                    hiveMQExtensions.getExtensionForClassloader(interceptor.getClass().getClassLoader());
            if (extension == null) {
                context.finishInterceptor();
                continue;
            }

            final DisconnectOutboundInterceptorTask task =
                    new DisconnectOutboundInterceptorTask(interceptor, extension.getId());
            executorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
    }

    private static class DisconnectOutboundInterceptorContext
            extends PluginInOutTaskContext<DisconnectOutboundOutputImpl> implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        private final @NotNull ExtensionParameterHolder<DisconnectOutboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<DisconnectOutboundOutputImpl> outputHolder;

        DisconnectOutboundInterceptorContext(
                final @NotNull String identifier,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ChannelPromise promise,
                final @NotNull ExtensionParameterHolder<DisconnectOutboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<DisconnectOutboundOutputImpl> outputHolder) {

            super(identifier);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
            this.promise = promise;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull DisconnectOutboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug(
                        "Async timeout on outbound DISCONNECT interception. Discarding changes made by the interceptor.");
            } else if (output.isFailed()) {
                log.debug("Exception on outbound DISCONNECT interception. Discarding changes made by the interceptor.");
            } else if (output.getDisconnectPacket().isModified()) {
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
            ctx.writeAndFlush(DISCONNECT.from(inputHolder.get().getDisconnectPacket()), promise);
        }
    }

    private static class DisconnectOutboundInterceptorTask
            implements PluginInOutTask<DisconnectOutboundInputImpl, DisconnectOutboundOutputImpl> {

        private final @NotNull DisconnectOutboundInterceptor interceptor;
        private final @NotNull String extensionId;

        DisconnectOutboundInterceptorTask(
                final @NotNull DisconnectOutboundInterceptor interceptor, final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull DisconnectOutboundOutputImpl apply(
                final @NotNull DisconnectOutboundInputImpl input, final @NotNull DisconnectOutboundOutputImpl output) {

            try {
                interceptor.onOutboundDisconnect(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound DISCONNECT interception. " +
                                "Extensions are responsible for their own exception handling.",
                        extensionId);
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

    private static class DisconnectInboundInterceptorContext extends PluginInOutTaskContext<DisconnectInboundOutputImpl>
            implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ExtensionParameterHolder<DisconnectInboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<DisconnectInboundOutputImpl> outputHolder;

        DisconnectInboundInterceptorContext(
                final @NotNull String identifier,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ExtensionParameterHolder<DisconnectInboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<DisconnectInboundOutputImpl> outputHolder) {

            super(identifier);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull DisconnectInboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug(
                        "Async timeout on inbound DISCONNECT interception. Discarding changes made by the interceptor.");
            } else if (output.isFailed()) {
                log.debug("Exception on inbound DISCONNECT interception. Discarding changes made by the interceptor.");
            } else if (output.getDisconnectPacket().isModified()) {
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
            ctx.fireChannelRead(DISCONNECT.from(inputHolder.get().getDisconnectPacket()));
        }
    }

    private static class DisconnectInboundInterceptorTask
            implements PluginInOutTask<DisconnectInboundInputImpl, DisconnectInboundOutputImpl> {

        private final @NotNull DisconnectInboundInterceptor interceptor;
        private final @NotNull String extensionId;

        DisconnectInboundInterceptorTask(
                final @NotNull DisconnectInboundInterceptor interceptor, final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull DisconnectInboundOutputImpl apply(
                final @NotNull DisconnectInboundInputImpl input, final @NotNull DisconnectInboundOutputImpl output) {

            try {
                interceptor.onInboundDisconnect(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound DISCONNECT interception. " +
                                "Extensions are responsible for their own exception handling.",
                        extensionId);
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
