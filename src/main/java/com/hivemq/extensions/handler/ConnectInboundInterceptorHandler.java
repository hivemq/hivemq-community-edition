/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.extensions.handler;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptorProvider;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.parameter.ClientInformationImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.connect.ConnectInboundInputImpl;
import com.hivemq.extensions.interceptor.connect.ConnectInboundOutputImpl;
import com.hivemq.extensions.interceptor.connect.ConnectInboundProviderInputImpl;
import com.hivemq.extensions.packets.connect.ConnectPacketImpl;
import com.hivemq.extensions.services.interceptor.Interceptors;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Lukas Brandl
 */
@ChannelHandler.Sharable
public class ConnectInboundInterceptorHandler extends SimpleChannelInboundHandler<CONNECT> {

    private static final Logger log = LoggerFactory.getLogger(ConnectInboundInterceptorHandler.class);

    @NotNull
    private final FullConfigurationService configurationService;

    @NotNull
    private final PluginOutPutAsyncer asyncer;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    @NotNull
    private final PluginTaskExecutorService pluginTaskExecutorService;

    @NotNull
    private final HivemqId hivemqId;

    @NotNull
    private final Interceptors interceptors;

    @NotNull
    private final ServerInformation serverInformation;

    @NotNull
    private final MqttConnacker connacker;

    @Inject
    public ConnectInboundInterceptorHandler(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
            final @NotNull HivemqId hivemqId,
            final @NotNull Interceptors interceptors,
            final @NotNull ServerInformation serverInformation,
            final @NotNull MqttConnacker connacker) {

        this.configurationService = configurationService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.hivemqId = hivemqId;
        this.interceptors = interceptors;
        this.serverInformation = serverInformation;
        this.connacker = connacker;
    }

    @Override
    public void channelRead0(final @NotNull ChannelHandlerContext ctx, final @NotNull CONNECT connect) {

        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ImmutableMap<String, ConnectInboundInterceptorProvider> connectInterceptorProviders =
                interceptors.connectInboundInterceptorProviders();

        if (connectInterceptorProviders.isEmpty()) {
            ctx.fireChannelRead(connect);
            return;
        }
        final ConnectInboundProviderInputImpl providerInput =
                new ConnectInboundProviderInputImpl(serverInformation, channel, clientId);

        final ConnectInboundOutputImpl output =
                new ConnectInboundOutputImpl(configurationService, asyncer, connect);
        final ConnectInboundInputImpl input =
                new ConnectInboundInputImpl(new ConnectPacketImpl(connect), clientId, channel);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final ConnectInterceptorContext interceptorContext = new ConnectInterceptorContext(
                clientId, channel, input, interceptorFuture, connectInterceptorProviders.size());

        for (final Map.Entry<String, ConnectInboundInterceptorProvider> entry : connectInterceptorProviders.entrySet()) {
            if (interceptorFuture.isDone()) {
                // The future is set in case an async interceptor timeout failed or if an interceptor throws an exception
                break;
            }
            final ConnectInboundInterceptorProvider provider = entry.getValue();
            final HiveMQExtension plugin = hiveMQExtensions.getExtension(entry.getKey());

            //disabled extension would be null
            if (plugin == null) {
                interceptorContext.increment();
                continue;
            }
            final ConnectInterceptorTask interceptorTask = new ConnectInterceptorTask(
                    provider, providerInput, plugin.getId(), channel, interceptorFuture, clientId);

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }

        final InterceptorFutureCallback callback = new InterceptorFutureCallback(output, connect, ctx, hivemqId.get());
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
    }

    private class ConnectInterceptorContext extends PluginInOutTaskContext<ConnectInboundOutputImpl> {

        private final @NotNull String clientId;
        private final @NotNull Channel channel;
        private final @NotNull ConnectInboundInputImpl input;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        ConnectInterceptorContext(
                final @NotNull String clientId,
                final @NotNull Channel channel,
                final @NotNull ConnectInboundInputImpl input,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final int interceptorCount) {

            super(clientId);
            this.clientId = clientId;
            this.channel = channel;
            this.input = input;
            this.interceptorFuture = interceptorFuture;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(final @NotNull ConnectInboundOutputImpl pluginOutput) {

            if (pluginOutput.isAsync() && pluginOutput.isTimedOut() &&
                    pluginOutput.getTimeoutFallback() == TimeoutFallback.FAILURE) {
                final String logMessage =
                        "Connect with client ID " + clientId + " failed because of an interceptor timeout";
                connacker.connackError(
                        channel, logMessage, logMessage, Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR,
                        "Extension interceptor timeout");
                interceptorFuture.set(null);
                return;
            }

            if (pluginOutput.getConnectPacket().isModified()) {
                input.updateConnect(pluginOutput.getConnectPacket(), configurationService);
            }

            if (counter.incrementAndGet() == interceptorCount) {
                interceptorFuture.set(null);
            }
        }

        public void increment() {
            //we must set the future when no more interceptors are registered
            if (counter.incrementAndGet() == interceptorCount) {
                interceptorFuture.set(null);
            }
        }
    }

    private class ConnectInterceptorTask implements PluginInOutTask<ConnectInboundInputImpl, ConnectInboundOutputImpl> {

        private final @NotNull ConnectInboundInterceptorProvider provider;
        private final @NotNull ConnectInboundProviderInputImpl providerInput;
        private final @NotNull String pluginId;
        private final @NotNull Channel channel;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final @NotNull String clientId;

        private ConnectInterceptorTask(
                final @NotNull ConnectInboundInterceptorProvider provider,
                final @NotNull ConnectInboundProviderInputImpl providerInput,
                final @NotNull String pluginId,
                final @NotNull Channel channel,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final @NotNull String clientId) {

            this.provider = provider;
            this.providerInput = providerInput;
            this.pluginId = pluginId;
            this.channel = channel;
            this.interceptorFuture = interceptorFuture;
            this.clientId = clientId;
        }

        @Override
        public @NotNull ConnectInboundOutputImpl apply(
                final @NotNull ConnectInboundInputImpl input,
                final @NotNull ConnectInboundOutputImpl output) {

            try {
                final ConnectInboundInterceptor interceptor = provider.getConnectInboundInterceptor(providerInput);
                if (interceptor != null && !interceptorFuture.isDone()) {
                    interceptor.onConnect(input, output);
                }
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on connect interception. The exception should be handled by the extension.",
                        pluginId);
                log.debug("Original exception:", e);
                final String logMessage = "Connect with client ID " + clientId +
                        " failed because of an exception was thrown by an interceptor";
                connacker.connackError(
                        channel, logMessage, logMessage, Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR,
                        "Exception in interceptor");
                interceptorFuture.set(null);
                Exceptions.rethrowError(e);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return provider.getClass().getClassLoader();
        }
    }

    private static class InterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull ConnectInboundOutputImpl output;
        private final @NotNull CONNECT connect;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull String clusterId;

        InterceptorFutureCallback(
                final @NotNull ConnectInboundOutputImpl output,
                final @NotNull CONNECT connect,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull String clusterId) {

            this.output = output;
            this.connect = connect;
            this.ctx = ctx;
            this.clusterId = clusterId;
        }

        @Override
        public void onSuccess(final @Nullable Void result) {
            try {
                final CONNECT finalConnect = CONNECT.mergeConnectPacket(output.getConnectPacket(), connect, clusterId);
                ctx.channel().attr(ChannelAttributes.CLIENT_ID).set(finalConnect.getClientIdentifier());
                ctx.channel()
                        .attr(ChannelAttributes.PLUGIN_CLIENT_INFORMATION)
                        .set(new ClientInformationImpl(finalConnect.getClientIdentifier()));
                ctx.channel().attr(ChannelAttributes.CLEAN_START).set(finalConnect.isCleanStart());
                ctx.channel().attr(ChannelAttributes.CONNECT_KEEP_ALIVE).set(finalConnect.getKeepAlive());
                ctx.channel().attr(ChannelAttributes.AUTH_USERNAME).set(finalConnect.getUsername());
                ctx.channel().attr(ChannelAttributes.AUTH_PASSWORD).set(finalConnect.getPassword());

                ctx.fireChannelRead(finalConnect);
            } catch (final Exception e) {
                log.error("Exception while modifying an intercepted CONNECT message.", e);
                ctx.fireChannelRead(connect);
            }
        }

        @Override
        public void onFailure(final @NotNull Throwable t) {
            //should never happen, since the settable future never sets an exception
            ctx.fireChannelRead(connect);
        }
    }
}
