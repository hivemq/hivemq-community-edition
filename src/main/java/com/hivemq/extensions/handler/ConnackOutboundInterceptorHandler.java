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

import com.google.common.collect.ImmutableMap;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptorProvider;
import com.hivemq.extensions.ExtensionInformationUtil;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.connack.parameter.ConnackOutboundInputImpl;
import com.hivemq.extensions.interceptor.connack.parameter.ConnackOutboundOutputImpl;
import com.hivemq.extensions.interceptor.connack.parameter.ConnackOutboundProviderInputImpl;
import com.hivemq.extensions.packets.connack.ConnackPacketImpl;
import com.hivemq.extensions.packets.connack.ModifiableConnackPacketImpl;
import com.hivemq.extensions.services.interceptor.Interceptors;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 * @since 4.2.0
 */
@Singleton
public class ConnackOutboundInterceptorHandler {

    private static final Logger log = LoggerFactory.getLogger(ConnackOutboundInterceptorHandler.class);

    private final @NotNull FullConfigurationService configurationService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull PluginTaskExecutorService executorService;
    private final @NotNull Interceptors interceptors;
    private final @NotNull ServerInformation serverInformation;
    private final @NotNull EventLog eventLog;

    @Inject
    public ConnackOutboundInterceptorHandler(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull PluginTaskExecutorService executorService,
            final @NotNull Interceptors interceptors,
            final @NotNull ServerInformation serverInformation,
            final @NotNull EventLog eventLog) {

        this.configurationService = configurationService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.executorService = executorService;
        this.interceptors = interceptors;
        this.serverInformation = serverInformation;
        this.eventLog = eventLog;
    }

    public void handleOutboundConnack(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull CONNACK connack,
            final @NotNull ChannelPromise promise) {

        final Channel channel = ctx.channel();
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            ctx.write(connack, promise);
            return;
        }

        final ImmutableMap<String, ConnackOutboundInterceptorProvider> providers =
                interceptors.connackOutboundInterceptorProviders();
        if (providers.isEmpty()) {
            ctx.write(connack, promise);
            return;
        }

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);
        final boolean requestResponseInformation =
                Objects.requireNonNullElse(channel.attr(ChannelAttributes.REQUEST_RESPONSE_INFORMATION).get(), false);

        final ConnackOutboundProviderInputImpl providerInput =
                new ConnackOutboundProviderInputImpl(serverInformation, clientInfo, connectionInfo);

        final ConnackPacketImpl packet = new ConnackPacketImpl(connack);
        final ConnackOutboundInputImpl input = new ConnackOutboundInputImpl(clientInfo, connectionInfo, packet);
        final ExtensionParameterHolder<ConnackOutboundInputImpl> inputHolder = new ExtensionParameterHolder<>(input);

        final ModifiableConnackPacketImpl modifiablePacket =
                new ModifiableConnackPacketImpl(packet, configurationService, requestResponseInformation);
        final ConnackOutboundOutputImpl output = new ConnackOutboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<ConnackOutboundOutputImpl> outputHolder = new ExtensionParameterHolder<>(output);

        final ConnackInterceptorContext context =
                new ConnackInterceptorContext(clientId, providers.size(), ctx, promise, inputHolder, outputHolder);

        for (final Map.Entry<String, ConnackOutboundInterceptorProvider> entry : providers.entrySet()) {
            final ConnackOutboundInterceptorProvider provider = entry.getValue();

            final HiveMQExtension extension = hiveMQExtensions.getExtension(entry.getKey());
            if (extension == null) { // disabled extension would be null
                context.finishInterceptor();
                continue;
            }

            final ConnackInterceptorTask task = new ConnackInterceptorTask(provider, providerInput, extension.getId());
            executorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
    }

    private class ConnackInterceptorContext extends PluginInOutTaskContext<ConnackOutboundOutputImpl>
            implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        private final @NotNull ExtensionParameterHolder<ConnackOutboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<ConnackOutboundOutputImpl> outputHolder;

        ConnackInterceptorContext(
                final @NotNull String clientId,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ChannelPromise promise,
                final @NotNull ExtensionParameterHolder<ConnackOutboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<ConnackOutboundOutputImpl> outputHolder) {

            super(clientId);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
            this.promise = promise;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull ConnackOutboundOutputImpl output) {
            if (output.isPrevent()) {
                finishInterceptor();
            } else if (output.isTimedOut() && (output.getTimeoutFallback() == TimeoutFallback.FAILURE)) {
                output.prevent();
                finishInterceptor();
            } else {
                if (output.getConnackPacket().isModified()) {
                    inputHolder.set(inputHolder.get().update(output));
                }
                if (!finishInterceptor()) {
                    outputHolder.set(output.update(inputHolder.get()));
                }
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
            if (outputHolder.get().isPrevent()) {
                eventLog.clientWasDisconnected(
                        ctx.channel(), "Connection prevented by extension in CONNACK outbound interceptor");
                ctx.channel().close();
            } else {
                ctx.writeAndFlush(CONNACK.from(inputHolder.get().getConnackPacket()), promise);
            }
        }
    }

    private static class ConnackInterceptorTask
            implements PluginInOutTask<ConnackOutboundInputImpl, ConnackOutboundOutputImpl> {

        private final @NotNull ConnackOutboundInterceptorProvider provider;
        private final @NotNull ConnackOutboundProviderInputImpl providerInput;
        private final @NotNull String extensionId;

        private ConnackInterceptorTask(
                final @NotNull ConnackOutboundInterceptorProvider provider,
                final @NotNull ConnackOutboundProviderInputImpl providerInput,
                final @NotNull String extensionId) {

            this.provider = provider;
            this.providerInput = providerInput;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull ConnackOutboundOutputImpl apply(
                final @NotNull ConnackOutboundInputImpl input, final @NotNull ConnackOutboundOutputImpl output) {

            if (output.isPrevent()) {
                // it's already prevented so no further interceptors must be called.
                return output;
            }
            try {
                final ConnackOutboundInterceptor interceptor = provider.getConnackOutboundInterceptor(providerInput);
                if (interceptor != null) {
                    interceptor.onOutboundConnack(input, output);
                }
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound CONNACK interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original exception:", e);
                output.prevent();
                Exceptions.rethrowError(e);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return provider.getClass().getClassLoader();
        }
    }
}
