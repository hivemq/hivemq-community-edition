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
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptorProvider;
import com.hivemq.extensions.ExtensionInformationUtil;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.parameter.ClientInformationImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.connect.parameter.ConnectInboundInputImpl;
import com.hivemq.extensions.interceptor.connect.parameter.ConnectInboundOutputImpl;
import com.hivemq.extensions.interceptor.connect.parameter.ConnectInboundProviderInputImpl;
import com.hivemq.extensions.packets.connect.ConnectPacketImpl;
import com.hivemq.extensions.packets.connect.ModifiableConnectPacketImpl;
import com.hivemq.extensions.services.interceptor.Interceptors;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Lukas Brandl
 * @author Silvio Giebl
 */
@Singleton
public class ConnectInboundInterceptorHandler {

    private static final Logger log = LoggerFactory.getLogger(ConnectInboundInterceptorHandler.class);

    private final @NotNull FullConfigurationService configurationService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull PluginTaskExecutorService executorService;
    private final @NotNull HivemqId hivemqId;
    private final @NotNull Interceptors interceptors;
    private final @NotNull ServerInformation serverInformation;
    private final @NotNull MqttConnacker connacker;

    @Inject
    public ConnectInboundInterceptorHandler(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull PluginTaskExecutorService executorService,
            final @NotNull HivemqId hivemqId,
            final @NotNull Interceptors interceptors,
            final @NotNull ServerInformation serverInformation,
            final @NotNull MqttConnacker connacker) {

        this.configurationService = configurationService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.executorService = executorService;
        this.hivemqId = hivemqId;
        this.interceptors = interceptors;
        this.serverInformation = serverInformation;
        this.connacker = connacker;
    }


    public void handleInboundConnect(final @NotNull ChannelHandlerContext ctx, final @NotNull CONNECT connect) {
        final Channel channel = ctx.channel();
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ImmutableMap<String, ConnectInboundInterceptorProvider> providers =
                interceptors.connectInboundInterceptorProviders();
        if (providers.isEmpty()) {
            ctx.fireChannelRead(connect);
            return;
        }

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);

        final ConnectInboundProviderInputImpl providerInput =
                new ConnectInboundProviderInputImpl(serverInformation, clientInfo, connectionInfo);

        final long timestamp =
                Objects.requireNonNullElse(channel.attr(ChannelAttributes.CONNECT_RECEIVED_TIMESTAMP).get(),
                        System.currentTimeMillis());
        final ConnectPacketImpl packet = new ConnectPacketImpl(connect, timestamp);
        final ConnectInboundInputImpl input = new ConnectInboundInputImpl(clientInfo, connectionInfo, packet);
        final ExtensionParameterHolder<ConnectInboundInputImpl> inputHolder = new ExtensionParameterHolder<>(input);

        final ModifiableConnectPacketImpl modifiablePacket =
                new ModifiableConnectPacketImpl(packet, configurationService);
        final ConnectInboundOutputImpl output = new ConnectInboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<ConnectInboundOutputImpl> outputHolder = new ExtensionParameterHolder<>(output);

        final ConnectInterceptorContext context =
                new ConnectInterceptorContext(clientId, providers.size(), ctx, inputHolder, outputHolder);

        for (final Map.Entry<String, ConnectInboundInterceptorProvider> entry : providers.entrySet()) {
            final ConnectInboundInterceptorProvider provider = entry.getValue();

            final HiveMQExtension extension = hiveMQExtensions.getExtension(entry.getKey());
            if (extension == null) { // disabled extension would be null
                context.finishInterceptor();
                continue;
            }

            final ConnectInterceptorTask task =
                    new ConnectInterceptorTask(provider, providerInput, extension.getId(), clientId);
            executorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
    }

    private class ConnectInterceptorContext extends PluginInOutTaskContext<ConnectInboundOutputImpl>
            implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ExtensionParameterHolder<ConnectInboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<ConnectInboundOutputImpl> outputHolder;

        ConnectInterceptorContext(
                final @NotNull String clientId,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ExtensionParameterHolder<ConnectInboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<ConnectInboundOutputImpl> outputHolder) {

            super(clientId);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull ConnectInboundOutputImpl output) {
            if (output.isPrevent()) {
                finishInterceptor();
            } else if (output.isTimedOut() && (output.getTimeoutFallback() == TimeoutFallback.FAILURE)) {
                output.prevent(
                        "Connect with client ID " + getIdentifier() + " failed because of an interceptor timeout",
                        "Extension interceptor timeout");
                finishInterceptor();
            } else {
                if (output.getConnectPacket().isModified()) {
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
            final ConnectInboundOutputImpl output = outputHolder.get();
            if (output.isPrevent()) {
                final String logMessage = output.getLogMessage();
                final String reasonString = output.getReasonString();
                connacker.connackError(
                        ctx.channel(),
                        logMessage,
                        logMessage,
                        Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR,
                        reasonString);
            } else {
                final CONNECT connect = CONNECT.from(inputHolder.get().getConnectPacket(), hivemqId.get());
                ctx.channel().attr(ChannelAttributes.CLIENT_ID).set(connect.getClientIdentifier());
                ctx.channel()
                        .attr(ChannelAttributes.EXTENSION_CLIENT_INFORMATION)
                        .set(new ClientInformationImpl(connect.getClientIdentifier()));
                ctx.channel().attr(ChannelAttributes.CLEAN_START).set(connect.isCleanStart());
                ctx.channel().attr(ChannelAttributes.CONNECT_KEEP_ALIVE).set(connect.getKeepAlive());
                ctx.channel().attr(ChannelAttributes.AUTH_USERNAME).set(connect.getUsername());
                ctx.channel().attr(ChannelAttributes.AUTH_PASSWORD).set(connect.getPassword());

                ctx.fireChannelRead(connect);
            }
        }
    }

    private static class ConnectInterceptorTask
            implements PluginInOutTask<ConnectInboundInputImpl, ConnectInboundOutputImpl> {

        private final @NotNull ConnectInboundInterceptorProvider provider;
        private final @NotNull ConnectInboundProviderInputImpl providerInput;
        private final @NotNull String extensionId;
        private final @NotNull String clientId;

        private ConnectInterceptorTask(
                final @NotNull ConnectInboundInterceptorProvider provider,
                final @NotNull ConnectInboundProviderInputImpl providerInput,
                final @NotNull String extensionId,
                final @NotNull String clientId) {

            this.provider = provider;
            this.providerInput = providerInput;
            this.extensionId = extensionId;
            this.clientId = clientId;
        }

        @Override
        public @NotNull ConnectInboundOutputImpl apply(
                final @NotNull ConnectInboundInputImpl input, final @NotNull ConnectInboundOutputImpl output) {

            if (output.isPrevent()) {
                // it's already prevented so no further interceptors must be called.
                return output;
            }
            try {
                final ConnectInboundInterceptor interceptor = provider.getConnectInboundInterceptor(providerInput);
                if (interceptor != null) {
                    interceptor.onConnect(input, output);
                }
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound CONNECT interception. " +
                                "Extensions are responsible for their own exception handling.",
                        extensionId);
                log.debug("Original exception:", e);
                output.prevent(String.format(ReasonStrings.CONNACK_UNSPECIFIED_ERROR_EXTENSION_EXCEPTION, clientId),
                        "Exception in CONNECT inbound interceptor");
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
