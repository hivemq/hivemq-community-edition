package com.hivemq.extensions.handler;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptorProvider;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
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
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
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
            @NotNull final FullConfigurationService configurationService,
            @NotNull final PluginOutPutAsyncer asyncer,
            @NotNull final HiveMQExtensions hiveMQExtensions,
            @NotNull final PluginTaskExecutorService pluginTaskExecutorService,
            @NotNull final HivemqId hivemqId,
            @NotNull final Interceptors interceptors,
            @NotNull final ServerInformation serverInformation,
            @NotNull final MqttConnacker connacker) {
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
    public void channelRead0(@NotNull final ChannelHandlerContext ctx, @NotNull final CONNECT connect)
            throws Exception {

        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ImmutableMap<String, ConnectInboundInterceptorProvider> connectInterceptorProviders =
                interceptors.connectInterceptorProviders();

        if (connectInterceptorProviders.isEmpty()) {
            ctx.fireChannelRead(connect);
            return;
        }
        final ConnectInboundProviderInputImpl providerInput =
                new ConnectInboundProviderInputImpl(serverInformation, channel, clientId);
        final ImmutableMap.Builder<String, ConnectInboundInterceptor> builder = ImmutableMap.builder();
        for (final Map.Entry<String, ConnectInboundInterceptorProvider> providerEntry : connectInterceptorProviders.entrySet()) {
            final ConnectInboundInterceptor interceptor = providerEntry.getValue().getConnectInterceptor(providerInput);
            if (interceptor != null) {
                builder.put(providerEntry.getKey(), interceptor);
            }
        }
        final ImmutableMap<String, ConnectInboundInterceptor> connectInterceptors = builder.build();

        final ConnectInboundOutputImpl output =
                new ConnectInboundOutputImpl(configurationService, asyncer, connect);
        final ConnectInboundInputImpl input =
                new ConnectInboundInputImpl(new ConnectPacketImpl(connect), clientId, channel);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final ConnectInterceptorContext interceptorContext = new ConnectInterceptorContext(ConnectInterceptorTask.class,
                clientId, channel, input, interceptorFuture, connectInterceptors.size());

        for (final Map.Entry<String, ConnectInboundInterceptor> entry : connectInterceptors.entrySet()) {
            if (interceptorFuture.isDone()) {
                // The future is set in case an async interceptor timeout failed or if an interceptor throws an exception
                break;
            }
            final ConnectInboundInterceptor interceptor = entry.getValue();
            final HiveMQExtension plugin = hiveMQExtensions.getExtension(entry.getKey());

            //disabled extension would be null
            if (plugin == null) {
                interceptorContext.increment();
                continue;
            }
            final ConnectInterceptorTask interceptorTask = new ConnectInterceptorTask(interceptor, plugin.getId(), channel,
                    interceptorFuture, clientId);

            final boolean executionSuccessful =
                    pluginTaskExecutorService.handlePluginInOutTaskExecution(interceptorContext, input, output,
                            interceptorTask);

            //we need to increment since extension post method would not be called.
            if (!executionSuccessful) {

                String className = interceptor.getClass().getSimpleName();

                //may happen if interface not implemented.
                if (className.isEmpty()) {
                    className = "ConnectInboundInterceptor";
                }

                log.warn("Extension task queue full. Ignoring '{}' from extension '{}'", className, plugin.getId());
                interceptorContext.increment();
            }
        }

        final InterceptorFutureCallback callback = new InterceptorFutureCallback(output, connect, ctx, hivemqId.get());
        Futures.addCallback(interceptorFuture, callback, ctx.executor());

    }

    private class ConnectInterceptorContext extends PluginInOutTaskContext<ConnectInboundOutputImpl> {

        private final @NotNull Channel channel;
        private final @NotNull ConnectInboundInputImpl input;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull String clientId;

        ConnectInterceptorContext(
                final @NotNull Class<?> taskClazz,
                final @NotNull String clientId,
                final @NotNull Channel channel,
                final @NotNull ConnectInboundInputImpl input,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final int interceptorCount) {
            super(taskClazz, clientId);
            this.clientId = clientId;
            this.channel = channel;
            this.input = input;
            this.interceptorFuture = interceptorFuture;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(@NotNull final ConnectInboundOutputImpl pluginOutput) {

            if (pluginOutput.isAsync() && pluginOutput.isTimedOut() &&
                    pluginOutput.getTimeoutFallback() == TimeoutFallback.FAILURE) {
                final String logMessage =
                        "Connect with client ID " + clientId + " failed because of an interceptor timeout";
                connacker.connackError(channel, logMessage, logMessage, Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR,
                        Mqtt3ConnAckReturnCode.REFUSED_NOT_AUTHORIZED, "Extension interceptor timeout");
                interceptorFuture.set(null);
                return;
            }

            if (pluginOutput.getConnectPacket().isModified()) {
                input.updateConnect(pluginOutput.getConnectPacket());
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

    private class ConnectInterceptorTask
            implements PluginInOutTask<ConnectInboundInputImpl, ConnectInboundOutputImpl> {

        private final @NotNull ConnectInboundInterceptor interceptor;
        private final @NotNull String pluginId;
        private final @NotNull Channel channel;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final @NotNull String clientId;

        private ConnectInterceptorTask(
                final @NotNull ConnectInboundInterceptor interceptor,
                final @NotNull String pluginId,
                final @NotNull Channel channel,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final @NotNull String clientId) {
            this.interceptor = interceptor;
            this.pluginId = pluginId;
            this.channel = channel;
            this.interceptorFuture = interceptorFuture;
            this.clientId = clientId;
        }

        @Override
        public @NotNull ConnectInboundOutputImpl apply(
                final @NotNull ConnectInboundInputImpl input, final @NotNull ConnectInboundOutputImpl output) {
            try {
                interceptor.onConnect(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on connect interception. The exception should be handled by the extension.",
                        pluginId);
                log.debug("Original exception:", e);
                final String logMessage = "Connect with client ID " + clientId + " failed because of an exception was thrown by an interceptor";
                connacker.connackError(channel, logMessage, logMessage, Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR,
                        Mqtt3ConnAckReturnCode.REFUSED_NOT_AUTHORIZED, "Exception in interceptor");
                interceptorFuture.set(null);
                Exceptions.rethrowError(e);
            }
            return output;
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
