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
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompOutboundInterceptor;
import com.hivemq.extensions.ExtensionInformationUtil;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.pubcomp.parameter.PubcompInboundInputImpl;
import com.hivemq.extensions.interceptor.pubcomp.parameter.PubcompInboundOutputImpl;
import com.hivemq.extensions.interceptor.pubcomp.parameter.PubcompOutboundInputImpl;
import com.hivemq.extensions.interceptor.pubcomp.parameter.PubcompOutboundOutputImpl;
import com.hivemq.extensions.packets.pubcomp.ModifiablePubcompPacketImpl;
import com.hivemq.extensions.packets.pubcomp.PubcompPacketImpl;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
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
public class PubcompInterceptorHandler {

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


    public void handleInboundPubcomp(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBCOMP pubcomp) {
        final Channel channel = ctx.channel();
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.EXTENSION_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.fireChannelRead(pubcomp);
            return;
        }
        final List<PubcompInboundInterceptor> interceptors = clientContext.getPubcompInboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.fireChannelRead(pubcomp);
            return;
        }

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);

        final PubcompPacketImpl packet = new PubcompPacketImpl(pubcomp);
        final PubcompInboundInputImpl input = new PubcompInboundInputImpl(clientInfo, connectionInfo, packet);
        final ExtensionParameterHolder<PubcompInboundInputImpl> inputHolder = new ExtensionParameterHolder<>(input);

        final ModifiablePubcompPacketImpl modifiablePacket =
                new ModifiablePubcompPacketImpl(packet, configurationService);
        final PubcompInboundOutputImpl output = new PubcompInboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<PubcompInboundOutputImpl> outputHolder = new ExtensionParameterHolder<>(output);

        final PubcompInboundInterceptorContext context =
                new PubcompInboundInterceptorContext(clientId, interceptors.size(), ctx, inputHolder, outputHolder);

        for (final PubcompInboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(interceptor.getClass().getClassLoader());
            if (extension == null) { // disabled extension would be null
                context.finishInterceptor();
                continue;
            }

            final PubcompInboundInterceptorTask task =
                    new PubcompInboundInterceptorTask(interceptor, extension.getId());
            executorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
    }

    public void handleOutboundPubcomp(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PUBCOMP pubcomp,
            final @NotNull ChannelPromise promise) {

        final Channel channel = ctx.channel();
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.EXTENSION_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.write(pubcomp, promise);
            return;
        }
        final List<PubcompOutboundInterceptor> interceptors = clientContext.getPubcompOutboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.write(pubcomp, promise);
            return;
        }

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);

        final PubcompPacketImpl packet = new PubcompPacketImpl(pubcomp);
        final PubcompOutboundInputImpl input = new PubcompOutboundInputImpl(clientInfo, connectionInfo, packet);
        final ExtensionParameterHolder<PubcompOutboundInputImpl> inputHolder = new ExtensionParameterHolder<>(input);

        final ModifiablePubcompPacketImpl modifiablePacket =
                new ModifiablePubcompPacketImpl(packet, configurationService);
        final PubcompOutboundOutputImpl output = new PubcompOutboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<PubcompOutboundOutputImpl> outputHolder = new ExtensionParameterHolder<>(output);

        final PubcompOutboundInterceptorContext context = new PubcompOutboundInterceptorContext(
                clientId, interceptors.size(), ctx, promise, inputHolder, outputHolder);

        for (final PubcompOutboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(interceptor.getClass().getClassLoader());
            if (extension == null) { // disabled extension would be null
                context.finishInterceptor();
                continue;
            }

            final PubcompOutboundInterceptorTask task =
                    new PubcompOutboundInterceptorTask(interceptor, extension.getId());
            executorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
    }

    private static class PubcompInboundInterceptorContext extends PluginInOutTaskContext<PubcompInboundOutputImpl>
            implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ExtensionParameterHolder<PubcompInboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<PubcompInboundOutputImpl> outputHolder;

        PubcompInboundInterceptorContext(
                final @NotNull String clientId,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ExtensionParameterHolder<PubcompInboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<PubcompInboundOutputImpl> outputHolder) {

            super(clientId);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull PubcompInboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on inbound PUBCOMP interception. Discarding changes made by the interceptor.");
            } else if (output.isFailed()) {
                log.debug("Exception on inbound PUBCOMP interception. Discarding changes made by the interceptor.");
            } else if (output.getPubcompPacket().isModified()) {
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
            ctx.fireChannelRead(PUBCOMP.from(inputHolder.get().getPubcompPacket()));
        }
    }

    private static class PubcompInboundInterceptorTask
            implements PluginInOutTask<PubcompInboundInputImpl, PubcompInboundOutputImpl> {

        private final @NotNull PubcompInboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PubcompInboundInterceptorTask(
                final @NotNull PubcompInboundInterceptor interceptor, final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PubcompInboundOutputImpl apply(
                final @NotNull PubcompInboundInputImpl input, final @NotNull PubcompInboundOutputImpl output) {

            try {
                interceptor.onInboundPubcomp(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound PUBCOMP interception. " +
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

    private static class PubcompOutboundInterceptorContext extends PluginInOutTaskContext<PubcompOutboundOutputImpl>
            implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        private final @NotNull ExtensionParameterHolder<PubcompOutboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<PubcompOutboundOutputImpl> outputHolder;

        PubcompOutboundInterceptorContext(
                final @NotNull String clientId,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ChannelPromise promise,
                final @NotNull ExtensionParameterHolder<PubcompOutboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<PubcompOutboundOutputImpl> outputHolder) {

            super(clientId);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
            this.promise = promise;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull PubcompOutboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug(
                        "Async timeout on inbound DISCONNECT interception. Discarding changes made by the interceptor.");
            } else if (output.isFailed()) {
                log.debug("Exception on inbound DISCONNECT interception. Discarding changes made by the interceptor.");
            } else if (output.getPubcompPacket().isModified()) {
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
            ctx.writeAndFlush(PUBCOMP.from(inputHolder.get().getPubcompPacket()), promise);
        }
    }

    private static class PubcompOutboundInterceptorTask
            implements PluginInOutTask<PubcompOutboundInputImpl, PubcompOutboundOutputImpl> {

        private final @NotNull PubcompOutboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PubcompOutboundInterceptorTask(
                final @NotNull PubcompOutboundInterceptor interceptor, final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PubcompOutboundOutputImpl apply(
                final @NotNull PubcompOutboundInputImpl input, final @NotNull PubcompOutboundOutputImpl output) {

            try {
                interceptor.onOutboundPubcomp(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound PUBCOMP interception. " +
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
