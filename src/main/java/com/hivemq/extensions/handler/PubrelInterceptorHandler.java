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
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelOutboundInterceptor;
import com.hivemq.extensions.ExtensionInformationUtil;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.pubrel.parameter.PubrelInboundInputImpl;
import com.hivemq.extensions.interceptor.pubrel.parameter.PubrelInboundOutputImpl;
import com.hivemq.extensions.interceptor.pubrel.parameter.PubrelOutboundInputImpl;
import com.hivemq.extensions.interceptor.pubrel.parameter.PubrelOutboundOutputImpl;
import com.hivemq.extensions.packets.pubrel.ModifiablePubrelPacketImpl;
import com.hivemq.extensions.packets.pubrel.PubrelPacketImpl;
import com.hivemq.mqtt.message.pubrel.PUBREL;
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
 * @author Silvio Giebl
 */
@Singleton
public class PubrelInterceptorHandler {

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



    public void handleInboundPubrel(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBREL pubrel) {
        final Channel channel = ctx.channel();
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.EXTENSION_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.fireChannelRead(pubrel);
            return;
        }
        final List<PubrelInboundInterceptor> interceptors = clientContext.getPubrelInboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.fireChannelRead(pubrel);
            return;
        }

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);

        final PubrelPacketImpl packet = new PubrelPacketImpl(pubrel);
        final PubrelInboundInputImpl input = new PubrelInboundInputImpl(clientInfo, connectionInfo, packet);
        final ExtensionParameterHolder<PubrelInboundInputImpl> inputHolder = new ExtensionParameterHolder<>(input);

        final ModifiablePubrelPacketImpl modifiablePacket =
                new ModifiablePubrelPacketImpl(packet, configurationService);
        final PubrelInboundOutputImpl output = new PubrelInboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<PubrelInboundOutputImpl> outputHolder = new ExtensionParameterHolder<>(output);

        final PubrelInboundInterceptorContext context =
                new PubrelInboundInterceptorContext(clientId, interceptors.size(), ctx, inputHolder, outputHolder);

        for (final PubrelInboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(interceptor.getClass().getClassLoader());
            if (extension == null) { // disabled extension would be null
                context.finishInterceptor();
                continue;
            }

            final PubrelInboundInterceptorTask task = new PubrelInboundInterceptorTask(interceptor, extension.getId());
            executorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
    }

    public void handleOutboundPubrel(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PUBREL pubrel,
            final @NotNull ChannelPromise promise) {

        final Channel channel = ctx.channel();
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.EXTENSION_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.write(pubrel, promise);
            return;
        }
        final List<PubrelOutboundInterceptor> interceptors = clientContext.getPubrelOutboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.write(pubrel, promise);
            return;
        }

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);

        final PubrelPacketImpl packet = new PubrelPacketImpl(pubrel);
        final PubrelOutboundInputImpl input = new PubrelOutboundInputImpl(clientInfo, connectionInfo, packet);
        final ExtensionParameterHolder<PubrelOutboundInputImpl> inputHolder = new ExtensionParameterHolder<>(input);

        final ModifiablePubrelPacketImpl modifiablePacket =
                new ModifiablePubrelPacketImpl(packet, configurationService);
        final PubrelOutboundOutputImpl output = new PubrelOutboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<PubrelOutboundOutputImpl> outputHolder = new ExtensionParameterHolder<>(output);

        final PubrelOutboundInterceptorContext context = new PubrelOutboundInterceptorContext(
                clientId, interceptors.size(), ctx, promise, inputHolder, outputHolder);

        for (final PubrelOutboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(interceptor.getClass().getClassLoader());
            if (extension == null) { // disabled extension would be null
                context.finishInterceptor();
                continue;
            }

            final PubrelOutboundInterceptorTask task =
                    new PubrelOutboundInterceptorTask(interceptor, extension.getId());
            executorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
    }

    private static class PubrelInboundInterceptorContext extends PluginInOutTaskContext<PubrelInboundOutputImpl>
            implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ExtensionParameterHolder<PubrelInboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<PubrelInboundOutputImpl> outputHolder;

        PubrelInboundInterceptorContext(
                final @NotNull String clientId,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ExtensionParameterHolder<PubrelInboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<PubrelInboundOutputImpl> outputHolder) {

            super(clientId);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull PubrelInboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on inbound PUBREL interception. Discarding changes made by the interceptor.");
            } else if (output.isFailed()) {
                log.debug("Exception on inbound PUBREL interception. Discarding changes made by the interceptor.");
            } else if (output.getPubrelPacket().isModified()) {
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
            ctx.fireChannelRead(PUBREL.from(inputHolder.get().getPubrelPacket()));
        }
    }

    private static class PubrelInboundInterceptorTask
            implements PluginInOutTask<PubrelInboundInputImpl, PubrelInboundOutputImpl> {

        private final @NotNull PubrelInboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PubrelInboundInterceptorTask(
                final @NotNull PubrelInboundInterceptor interceptor, final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PubrelInboundOutputImpl apply(
                final @NotNull PubrelInboundInputImpl input, final @NotNull PubrelInboundOutputImpl output) {

            try {
                interceptor.onInboundPubrel(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound PUBREL interception. " +
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

    private static class PubrelOutboundInterceptorContext extends PluginInOutTaskContext<PubrelOutboundOutputImpl>
            implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        private final @NotNull ExtensionParameterHolder<PubrelOutboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<PubrelOutboundOutputImpl> outputHolder;

        PubrelOutboundInterceptorContext(
                final @NotNull String clientId,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ChannelPromise promise,
                final @NotNull ExtensionParameterHolder<PubrelOutboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<PubrelOutboundOutputImpl> outputHolder) {

            super(clientId);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
            this.promise = promise;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull PubrelOutboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on outbound PUBREL interception. Discarding changes made by the interceptor.");
            } else if (output.isFailed()) {
                log.debug("Exception on outbound PUBREL interception. Discarding changes made by the interceptor.");
            } else if (output.getPubrelPacket().isModified()) {
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
            ctx.writeAndFlush(PUBREL.from(inputHolder.get().getPubrelPacket()), promise);
        }
    }

    private static class PubrelOutboundInterceptorTask
            implements PluginInOutTask<PubrelOutboundInputImpl, PubrelOutboundOutputImpl> {

        private final @NotNull PubrelOutboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PubrelOutboundInterceptorTask(
                final @NotNull PubrelOutboundInterceptor interceptor, final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PubrelOutboundOutputImpl apply(
                final @NotNull PubrelOutboundInputImpl input, final @NotNull PubrelOutboundOutputImpl output) {

            try {
                interceptor.onOutboundPubrel(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound PUBREL interception. " +
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
