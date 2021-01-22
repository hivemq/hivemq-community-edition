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
import com.hivemq.extension.sdk.api.interceptor.suback.SubackOutboundInterceptor;
import com.hivemq.extensions.ExtensionInformationUtil;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.suback.parameter.SubackOutboundInputImpl;
import com.hivemq.extensions.interceptor.suback.parameter.SubackOutboundOutputImpl;
import com.hivemq.extensions.packets.suback.ModifiableSubackPacketImpl;
import com.hivemq.extensions.packets.suback.SubackPacketImpl;
import com.hivemq.mqtt.message.suback.SUBACK;
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
 * @author Robin Atherton
 * @author Silvio Giebl
 */
@Singleton
public class SubackOutboundInterceptorHandler {

    private static final Logger log = LoggerFactory.getLogger(SubackOutboundInterceptorHandler.class);

    private final @NotNull FullConfigurationService configurationService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull PluginTaskExecutorService executorService;

    @Inject
    public SubackOutboundInterceptorHandler(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull PluginTaskExecutorService executorService) {

        this.configurationService = configurationService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.executorService = executorService;
    }

    public void handleOutboundSuback(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull SUBACK suback,
            final @NotNull ChannelPromise promise) {

        final Channel channel = ctx.channel();
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.EXTENSION_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.write(suback, promise);
            return;
        }
        final List<SubackOutboundInterceptor> interceptors = clientContext.getSubackOutboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.write(suback, promise);
            return;
        }

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);

        final SubackPacketImpl packet = new SubackPacketImpl(suback);
        final SubackOutboundInputImpl input = new SubackOutboundInputImpl(clientInfo, connectionInfo, packet);
        final ExtensionParameterHolder<SubackOutboundInputImpl> inputHolder = new ExtensionParameterHolder<>(input);

        final ModifiableSubackPacketImpl modifiablePacket =
                new ModifiableSubackPacketImpl(packet, configurationService);
        final SubackOutboundOutputImpl output = new SubackOutboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<SubackOutboundOutputImpl> outputHolder = new ExtensionParameterHolder<>(output);

        final SubAckOutboundInterceptorContext context = new SubAckOutboundInterceptorContext(
                clientId, interceptors.size(), ctx, promise, inputHolder, outputHolder);

        for (final SubackOutboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(interceptor.getClass().getClassLoader());
            if (extension == null) {
                context.finishInterceptor();
                continue;
            }

            final SubackOutboundInterceptorTask task =
                    new SubackOutboundInterceptorTask(interceptor, extension.getId());
            executorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
    }

    private static class SubAckOutboundInterceptorContext extends PluginInOutTaskContext<SubackOutboundOutputImpl>
            implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        private final @NotNull ExtensionParameterHolder<SubackOutboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<SubackOutboundOutputImpl> outputHolder;

        SubAckOutboundInterceptorContext(
                final @NotNull String identifier,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ChannelPromise promise,
                final @NotNull ExtensionParameterHolder<SubackOutboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<SubackOutboundOutputImpl> outputHolder) {

            super(identifier);
            this.ctx = ctx;
            this.promise = promise;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull SubackOutboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on outbound SUBACK interception. Discarding changes made by the interceptor.");
            } else if (output.isFailed()) {
                log.debug("Exception on outbound SUBACK interception. Discarding changes made by the interceptor.");
            } else if (output.getSubackPacket().isModified()) {
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
            ctx.writeAndFlush(SUBACK.from(inputHolder.get().getSubackPacket()), promise);
        }
    }

    private static class SubackOutboundInterceptorTask
            implements PluginInOutTask<SubackOutboundInputImpl, SubackOutboundOutputImpl> {

        private final @NotNull SubackOutboundInterceptor interceptor;
        private final @NotNull String extensionId;

        SubackOutboundInterceptorTask(
                final @NotNull SubackOutboundInterceptor interceptor, final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull SubackOutboundOutputImpl apply(
                final @NotNull SubackOutboundInputImpl input, final @NotNull SubackOutboundOutputImpl output) {

            try {
                interceptor.onOutboundSuback(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound SUBACK interception. " +
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
