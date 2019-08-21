package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.hivemq.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundOutput;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.disconnect.DisconnectOutboundInputImpl;
import com.hivemq.extensions.interceptor.disconnect.DisconnectOutboundOutputImpl;
import com.hivemq.extensions.packets.disconnect.DisconnectPacketImpl;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Robin Atherton
 */
@Singleton
@ChannelHandler.Sharable
public class DisconnectOutboundInterceptorHandler extends ChannelOutboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(DisconnectOutboundInterceptorHandler.class);

    private final @NotNull FullConfigurationService configurationService;

    private final @NotNull PluginOutPutAsyncer asyncer;

    private final @NotNull HiveMQExtensions hiveMQExtensions;

    private final @NotNull PluginTaskExecutorService executorService;

    @Inject
    public DisconnectOutboundInterceptorHandler(
            @NotNull final FullConfigurationService configurationService,
            @NotNull final PluginOutPutAsyncer asyncer,
            @NotNull final HiveMQExtensions hiveMQExtensions,
            @NotNull final PluginTaskExecutorService executorService) {
        this.configurationService = configurationService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.executorService = executorService;
    }

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg, final @NotNull ChannelPromise promise)
            throws Exception {

        if (!(msg instanceof DISCONNECT)) {
            super.write(ctx, msg, promise);
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
        if (clientContext == null || clientContext.getPublishOutboundInterceptors().isEmpty()) {
            return;
        }

        final List<DisconnectOutboundInterceptor> disconnectOutboundInterceptors =
                clientContext.getDisconnectOutboundInterceptor();
        final DisconnectOutboundInputImpl input =
                new DisconnectOutboundInputImpl(new DisconnectPacketImpl(disconnect), clientId, channel);
        final DisconnectOutboundOutputImpl output =
                new DisconnectOutboundOutputImpl(asyncer, configurationService, disconnect);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();

    }

    static class DisconnectOutboundInterceptorContext extends PluginInOutTaskContext<DisconnectOutboundOutputImpl> {

        private final @NotNull DisconnectOutboundOutputImpl output;
        private final @NotNull DisconnectOutboundInputImpl input;
        final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        public DisconnectOutboundInterceptorContext(
                @NotNull final Class<?> taskClazz,
                @NotNull final String identifier,
                @NotNull final DisconnectOutboundOutputImpl output,
                @NotNull final DisconnectOutboundInputImpl input,
                @NotNull final SettableFuture<Void> interceptorFuture, final int interceptorCount,
                @NotNull final AtomicInteger counter) {
            super(taskClazz, identifier);
            this.output = output;
            this.input = input;
            this.interceptorFuture = interceptorFuture;
            this.interceptorCount = interceptorCount;
            this.counter = counter;
        }

        @Override
        public void pluginPost(
                final @NotNull DisconnectOutboundOutputImpl pluginOutput) {
            if (output.getDisconnectPacket().isModified()) {
                input.updateDisconnect(output.getDisconnectPacket());
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

    private class DisconnectOutboundInterceptorTask
            implements PluginInOutTask<DisconnectOutboundInputImpl, DisconnectOutboundOutputImpl> {

        private final @NotNull DisconnectOutboundInterceptor interceptor;
        private final @NotNull String pluginId;

        public DisconnectOutboundInterceptorTask(
                @NotNull final DisconnectOutboundInterceptor interceptor,
                @NotNull final String pluginId) {
            this.interceptor = interceptor;
            this.pluginId = pluginId;
        }

        @Override
        public DisconnectOutboundOutputImpl apply(
                final @NotNull DisconnectOutboundInputImpl disconnectOutboundInput,
                final @NotNull DisconnectOutboundOutputImpl disconnectOutboundOutput) {
            try {
                interceptor.onOutboundDisconnect(disconnectOutboundInput, disconnectOutboundOutput);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound disconnect interception. " +
                                "Extensions are responsible on their own to handle exceptions.", pluginId);
                Exceptions.rethrowError(e);
            }
            return disconnectOutboundOutput;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

    private static class InterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull DisconnectOutboundOutput outboundOutput;
        private final @NotNull DISCONNECT disconnect;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;

        public InterceptorFutureCallback(
                @NotNull final DisconnectOutboundOutput outboundOutput,
                @NotNull final DISCONNECT disconnect,
                @NotNull final ChannelHandlerContext ctx,
                @NotNull final ChannelPromise promise) {
            this.outboundOutput = outboundOutput;
            this.disconnect = disconnect;
            this.ctx = ctx;
            this.promise = promise;
        }

        @Override
        public void onSuccess(@Nullable final Void result) {
            //TODO
            //try {
            //  @NotNull ModifiableDisconnectPacket dc = outboundOutput.getDisconnectPacket();

            //}
        }

        @Override
        public void onFailure(final Throwable t) {

        }
    }
}
