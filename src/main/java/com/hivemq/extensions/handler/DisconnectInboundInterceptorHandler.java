package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.hivemq.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectInboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.disconnect.DisconnectInboundInputImpl;
import com.hivemq.extensions.interceptor.disconnect.DisconnectInboundOutputImpl;
import com.hivemq.extensions.packets.disconnect.DisconnectPacketImpl;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Robin Atherton
 */
@ChannelHandler.Sharable
public class DisconnectInboundInterceptorHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(DisconnectInboundInterceptorHandler.class);

    private final @NotNull FullConfigurationService configurationService;

    private final @NotNull PluginOutPutAsyncer asyncer;

    private final @NotNull HiveMQExtensions hiveMQExtensions;

    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;

    @Inject
    public DisconnectInboundInterceptorHandler(
            @NotNull final FullConfigurationService configurationService,
            @NotNull final PluginOutPutAsyncer asyncer,
            @NotNull final HiveMQExtensions hiveMQExtensions,
            @NotNull final PluginTaskExecutorService pluginTaskExecutorService) {
        this.configurationService = configurationService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.pluginTaskExecutorService = pluginTaskExecutorService;
    }

    @Override
    public void channelRead(
            final @NotNull ChannelHandlerContext ctx, final Object msg) throws Exception {


        if (!(msg instanceof DISCONNECT)) {
            super.channelRead(ctx, msg);
            return;
        }

        final DISCONNECT disconnect = (DISCONNECT) msg;


        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return;
        }


        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null || clientContext.getDisconnectInboundInterceptors().isEmpty()) {
            super.channelRead(ctx, msg);
            return;
        }

        final List<DisconnectInboundInterceptor> disconnectInboundInterceptors =
                clientContext.getDisconnectInboundInterceptors();

        final DisconnectInboundOutputImpl output =
                new DisconnectInboundOutputImpl(configurationService, asyncer, disconnect);
        final DisconnectInboundInputImpl input = new DisconnectInboundInputImpl(disconnect, clientId, channel);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final DisconnectInboundInterceptorContext interceptorContext =
                new DisconnectInboundInterceptorContext(DisconnectInboundInterceptorTask.class, clientId, input, output,
                        interceptorFuture, disconnectInboundInterceptors.size());

        for (final DisconnectInboundInterceptor interceptor : disconnectInboundInterceptors) {
            if (interceptorFuture.isDone()) {
                break;
            }

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());
            if (extension == null) {
                interceptorContext.increment();
                continue;
            }

            final DisconnectInboundInterceptorTask interceptorTask =
                    new DisconnectInboundInterceptorTask(interceptor, interceptorFuture, extension.getId());
            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }

        final DisconnectInterceptorFutureCallback callback =
                new DisconnectInterceptorFutureCallback(ctx, output, disconnect);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
    }

    private static class DisconnectInboundInterceptorContext
            extends PluginInOutTaskContext<DisconnectInboundOutputImpl> {

        private final @NotNull DisconnectInboundInputImpl input;
        private final @NotNull DisconnectInboundOutputImpl output;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        DisconnectInboundInterceptorContext(
                @NotNull final Class<?> taskClazz,
                @NotNull final String identifier,
                @NotNull final DisconnectInboundInputImpl input,
                @NotNull final DisconnectInboundOutputImpl output,
                @NotNull final SettableFuture<Void> interceptorFuture,
                final int interceptorCount) {
            super(taskClazz, identifier);
            this.interceptorFuture = interceptorFuture;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.input = input;
            this.output = output;
        }

        @Override
        public void pluginPost(
                final @NotNull DisconnectInboundOutputImpl pluginOutput) {
            if (pluginOutput.getDisconnectPacket().isModified()) {
                input.updateDisconnect(pluginOutput.getDisconnectPacket());
                output.update(pluginOutput.getDisconnectPacket());
            }

            increment();
        }

        public void increment() {
            //we must set the future when no more interceptors are registered
            if (counter.incrementAndGet() == interceptorCount) {
                interceptorFuture.set(null);
            }
        }
    }

    private static class DisconnectInboundInterceptorTask
            implements PluginInOutTask<DisconnectInboundInputImpl, DisconnectInboundOutputImpl> {

        private final @NotNull DisconnectInboundInterceptor interceptor;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final @NotNull String pluginId;

        private DisconnectInboundInterceptorTask(
                final @NotNull DisconnectInboundInterceptor interceptor,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final @NotNull String pluginId) {
            this.interceptor = interceptor;
            this.interceptorFuture = interceptorFuture;
            this.pluginId = pluginId;
        }

        @Override
        public DisconnectInboundOutputImpl apply(
                final @NotNull DisconnectInboundInputImpl input,
                final @NotNull DisconnectInboundOutputImpl output) {
            try {
                if (!interceptorFuture.isDone()) {
                    interceptor.onInboundDisconnect(input, output);
                }
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound disconnect request interception." +
                                "Extensions are responsible for their own exception handling.",
                        pluginId);
                final DISCONNECT disconnect = DISCONNECT.createDisconnectFrom(input.getDisconnectPacket());
                output.update(disconnect);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

    private static class DisconnectInterceptorFutureCallback implements FutureCallback<Void> {

        private final DisconnectInboundOutputImpl output;
        private final DISCONNECT disconnect;
        private final @NotNull ChannelHandlerContext ctx;

        DisconnectInterceptorFutureCallback(
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull DisconnectInboundOutputImpl output,
                final @NotNull DISCONNECT disconnect) {
            this.ctx = ctx;
            this.output = output;
            this.disconnect = disconnect;
        }

        @Override
        public void onSuccess(final @Nullable Void result) {
            try {
                final DISCONNECT finalDisconnect = DISCONNECT.createDisconnectFrom(output.getDisconnectPacket());
                ctx.fireChannelRead(finalDisconnect);
            } catch (final Exception e) {
                log.error("Exception while modifying an intercepted DISCONNECT message.", e);
                ctx.fireChannelRead(disconnect);
            }
        }

        @Override
        public void onFailure(final Throwable t) {
            ctx.channel().close();
        }
    }
}
