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
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecOutboundInterceptor;
import com.hivemq.extensions.ExtensionInformationUtil;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.pubrec.parameter.PubrecInboundInputImpl;
import com.hivemq.extensions.interceptor.pubrec.parameter.PubrecInboundOutputImpl;
import com.hivemq.extensions.interceptor.pubrec.parameter.PubrecOutboundInputImpl;
import com.hivemq.extensions.interceptor.pubrec.parameter.PubrecOutboundOutputImpl;
import com.hivemq.extensions.packets.pubrec.ModifiablePubrecPacketImpl;
import com.hivemq.extensions.packets.pubrec.PubrecPacketImpl;
import com.hivemq.mqtt.message.pubrec.PUBREC;
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
public class PubrecInterceptorHandler {

    private static final Logger log = LoggerFactory.getLogger(PubrecInterceptorHandler.class);

    private final @NotNull FullConfigurationService configurationService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull PluginTaskExecutorService executorService;

    @Inject
    public PubrecInterceptorHandler(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull PluginTaskExecutorService executorService) {

        this.configurationService = configurationService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.executorService = executorService;
    }


    public void handleInboundPubrec(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBREC pubrec) {
        final Channel channel = ctx.channel();
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.EXTENSION_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.fireChannelRead(pubrec);
            return;
        }
        final List<PubrecInboundInterceptor> interceptors = clientContext.getPubrecInboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.fireChannelRead(pubrec);
            return;
        }

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);

        final PubrecPacketImpl packet = new PubrecPacketImpl(pubrec);
        final PubrecInboundInputImpl input = new PubrecInboundInputImpl(clientInfo, connectionInfo, packet);
        final ExtensionParameterHolder<PubrecInboundInputImpl> inputHolder = new ExtensionParameterHolder<>(input);

        final ModifiablePubrecPacketImpl modifiablePacket =
                new ModifiablePubrecPacketImpl(packet, configurationService);
        final PubrecInboundOutputImpl output = new PubrecInboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<PubrecInboundOutputImpl> outputHolder = new ExtensionParameterHolder<>(output);

        final PubrecInboundInterceptorContext context =
                new PubrecInboundInterceptorContext(clientId, interceptors.size(), ctx, inputHolder, outputHolder);

        for (final PubrecInboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(interceptor.getClass().getClassLoader());
            if (extension == null) { // disabled extension would be null
                context.finishInterceptor();
                continue;
            }

            final PubrecInboundInterceptorTask task = new PubrecInboundInterceptorTask(interceptor, extension.getId());
            executorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
    }

    public void handleOutboundPubrec(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PUBREC pubrec,
            final @NotNull ChannelPromise promise) {

        final Channel channel = ctx.channel();
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.EXTENSION_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.write(pubrec, promise);
            return;
        }
        final List<PubrecOutboundInterceptor> interceptors = clientContext.getPubrecOutboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.write(pubrec, promise);
            return;
        }

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);

        final PubrecPacketImpl packet = new PubrecPacketImpl(pubrec);
        final PubrecOutboundInputImpl input = new PubrecOutboundInputImpl(clientInfo, connectionInfo, packet);
        final ExtensionParameterHolder<PubrecOutboundInputImpl> inputHolder = new ExtensionParameterHolder<>(input);

        final ModifiablePubrecPacketImpl modifiablePacket =
                new ModifiablePubrecPacketImpl(packet, configurationService);
        final PubrecOutboundOutputImpl output = new PubrecOutboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<PubrecOutboundOutputImpl> outputHolder = new ExtensionParameterHolder<>(output);

        final PubrecOutboundInterceptorContext context = new PubrecOutboundInterceptorContext(
                clientId, interceptors.size(), ctx, promise, inputHolder, outputHolder);

        for (final PubrecOutboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(interceptor.getClass().getClassLoader());
            if (extension == null) { // disabled extension would be null
                context.finishInterceptor();
                continue;
            }

            final PubrecOutboundInterceptorTask task =
                    new PubrecOutboundInterceptorTask(interceptor, extension.getId());
            executorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
    }

    private static class PubrecInboundInterceptorContext extends PluginInOutTaskContext<PubrecInboundOutputImpl>
            implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ExtensionParameterHolder<PubrecInboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<PubrecInboundOutputImpl> outputHolder;

        PubrecInboundInterceptorContext(
                final @NotNull String clientId,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ExtensionParameterHolder<PubrecInboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<PubrecInboundOutputImpl> outputHolder) {

            super(clientId);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull PubrecInboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on inbound PUBREC interception. Discarding changes made by the interceptor.");
            } else if (output.isFailed()) {
                log.debug("Exception on inbound PUBREC interception. Discarding changes made by the interceptor.");
            } else if (output.getPubrecPacket().isModified()) {
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
            ctx.fireChannelRead(PUBREC.from(inputHolder.get().getPubrecPacket()));
        }
    }

    private static class PubrecInboundInterceptorTask
            implements PluginInOutTask<PubrecInboundInputImpl, PubrecInboundOutputImpl> {

        private final @NotNull PubrecInboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PubrecInboundInterceptorTask(
                final @NotNull PubrecInboundInterceptor interceptor, final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PubrecInboundOutputImpl apply(
                final @NotNull PubrecInboundInputImpl input, final @NotNull PubrecInboundOutputImpl output) {

            try {
                interceptor.onInboundPubrec(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound PUBREC interception. " +
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

    private static class PubrecOutboundInterceptorContext extends PluginInOutTaskContext<PubrecOutboundOutputImpl>
            implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        private final @NotNull ExtensionParameterHolder<PubrecOutboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<PubrecOutboundOutputImpl> outputHolder;

        PubrecOutboundInterceptorContext(
                final @NotNull String clientId,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ChannelPromise promise,
                final @NotNull ExtensionParameterHolder<PubrecOutboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<PubrecOutboundOutputImpl> outputHolder) {

            super(clientId);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
            this.promise = promise;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull PubrecOutboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on outbound PUBREC interception. Discarding changes made by the interceptor.");
            } else if (output.isFailed()) {
                log.debug("Exception on outbound PUBREC interception. Discarding changes made by the interceptor.");
            } else if (output.getPubrecPacket().isModified()) {
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
            ctx.writeAndFlush(PUBREC.from(inputHolder.get().getPubrecPacket()), promise);
        }
    }

    private static class PubrecOutboundInterceptorTask
            implements PluginInOutTask<PubrecOutboundInputImpl, PubrecOutboundOutputImpl> {

        private final @NotNull PubrecOutboundInterceptor interceptor;
        private final @NotNull String extensionId;

        PubrecOutboundInterceptorTask(
                final @NotNull PubrecOutboundInterceptor interceptor, final @NotNull String extensionId) {

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
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound PUBREC interception. " +
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
