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
import com.hivemq.extension.sdk.api.interceptor.unsuback.UnsubackOutboundInterceptor;
import com.hivemq.extensions.ExtensionInformationUtil;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.unsuback.parameter.UnsubackOutboundInputImpl;
import com.hivemq.extensions.interceptor.unsuback.parameter.UnsubackOutboundOutputImpl;
import com.hivemq.extensions.packets.unsuback.ModifiableUnsubackPacketImpl;
import com.hivemq.extensions.packets.unsuback.UnsubackPacketImpl;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
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
public class UnsubackOutboundInterceptorHandler {

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

    public void handleOutboundUnsuback(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull UNSUBACK unsuback,
            final @NotNull ChannelPromise promise) {

        final Channel channel = ctx.channel();
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.EXTENSION_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.write(unsuback, promise);
            return;
        }
        final List<UnsubackOutboundInterceptor> interceptors = clientContext.getUnsubackOutboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.write(unsuback, promise);
            return;
        }

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);

        final UnsubackPacketImpl packet = new UnsubackPacketImpl(unsuback);
        final UnsubackOutboundInputImpl input = new UnsubackOutboundInputImpl(clientInfo, connectionInfo, packet);
        final ExtensionParameterHolder<UnsubackOutboundInputImpl> inputHolder = new ExtensionParameterHolder<>(input);

        final ModifiableUnsubackPacketImpl modifiablePacket =
                new ModifiableUnsubackPacketImpl(packet, configurationService);
        final UnsubackOutboundOutputImpl output = new UnsubackOutboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<UnsubackOutboundOutputImpl> outputHolder =
                new ExtensionParameterHolder<>(output);

        final UnsubackOutboundInterceptorContext context = new UnsubackOutboundInterceptorContext(
                clientId, interceptors.size(), ctx, promise, inputHolder, outputHolder);

        for (final UnsubackOutboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(interceptor.getClass().getClassLoader());
            if (extension == null) {
                context.finishInterceptor();
                continue;
            }

            final UnsubackOutboundInterceptorTask task =
                    new UnsubackOutboundInterceptorTask(interceptor, extension.getId());
            executorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
    }

    private static class UnsubackOutboundInterceptorContext extends PluginInOutTaskContext<UnsubackOutboundOutputImpl>
            implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        final @NotNull ExtensionParameterHolder<UnsubackOutboundInputImpl> inputHolder;
        final @NotNull ExtensionParameterHolder<UnsubackOutboundOutputImpl> outputHolder;

        UnsubackOutboundInterceptorContext(
                final @NotNull String identifier,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ChannelPromise promise,
                final @NotNull ExtensionParameterHolder<UnsubackOutboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<UnsubackOutboundOutputImpl> outputHolder) {

            super(identifier);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
            this.promise = promise;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull UnsubackOutboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug(
                        "Async timeout on outbound UNSUBACK interception. Discarding changes made by the interceptor.");
            } else if (output.isFailed()) {
                log.debug("Exception on outbound UNSUBACK interception. Discarding changes made by the interceptor.");
            } else if (output.getUnsubackPacket().isModified()) {
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
            ctx.writeAndFlush(UNSUBACK.from(inputHolder.get().getUnsubackPacket()), promise);
        }
    }

    private static class UnsubackOutboundInterceptorTask
            implements PluginInOutTask<UnsubackOutboundInputImpl, UnsubackOutboundOutputImpl> {

        private final @NotNull UnsubackOutboundInterceptor interceptor;
        private final @NotNull String extensionId;

        UnsubackOutboundInterceptorTask(
                final @NotNull UnsubackOutboundInterceptor interceptor, final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull UnsubackOutboundOutputImpl apply(
                final @NotNull UnsubackOutboundInputImpl input, final @NotNull UnsubackOutboundOutputImpl output) {

            try {
                interceptor.onOutboundUnsuback(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound UNSUBACK interception. " +
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
