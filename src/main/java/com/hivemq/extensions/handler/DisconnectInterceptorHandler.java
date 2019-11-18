package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hivemq.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundOutput;
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
import com.hivemq.extensions.interceptor.disconnect.DisconnectOutboundInputImpl;
import com.hivemq.extensions.interceptor.disconnect.DisconnectOutboundOutputImpl;
import com.hivemq.extensions.packets.disconnect.DisconnectPacketImpl;
import com.hivemq.extensions.packets.disconnect.ModifiableOutboundDisconnectPacketImpl;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Robin Atherton
 */
@Singleton
@ChannelHandler.Sharable
public class DisconnectInterceptorHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(DisconnectInterceptorHandler.class);

    private final @NotNull FullConfigurationService configurationService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull PluginTaskExecutorService executorService;

    @Inject
    public DisconnectInterceptorHandler(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull PluginTaskExecutorService executorService) {

        this.configurationService = configurationService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.executorService = executorService;
    }

    @Override
    public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) throws Exception {

        if (!(msg instanceof DISCONNECT)) {
            super.channelRead(ctx, msg);
            return;
        }

        handleRead(ctx, (DISCONNECT) msg);
    }

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg, final @NotNull ChannelPromise promise)
            throws Exception {

        if (!(msg instanceof DISCONNECT)) {
            super.write(ctx, msg, promise);
            return;
        }

        handleWrite(ctx, (DISCONNECT) msg, promise);
    }

    private void handleRead(final @NotNull ChannelHandlerContext ctx, final @NotNull DISCONNECT disconnect)
            throws Exception {

        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            super.channelRead(ctx, disconnect);
            return;
        }
        final List<DisconnectInboundInterceptor> disconnectInboundInterceptors =
                clientContext.getDisconnectInboundInterceptors();
        if (disconnectInboundInterceptors.isEmpty()) {
            super.channelRead(ctx, disconnect);
            return;
        }

        final DisconnectInboundOutputImpl output =
                new DisconnectInboundOutputImpl(configurationService, asyncer, disconnect);

        final DisconnectInboundInputImpl input =
                new DisconnectInboundInputImpl(new DisconnectPacketImpl(disconnect), clientId, channel);

        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final DisconnectInboundInterceptorContext interceptorContext =
                new DisconnectInboundInterceptorContext(
                        DisconnectInboundInterceptorTask.class, clientId, input, interceptorFuture,
                        disconnectInboundInterceptors.size());

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
            executorService.handlePluginInOutTaskExecution(interceptorContext, input, output, interceptorTask);
        }

        final DisconnectInboundInterceptorFutureCallback callback =
                new DisconnectInboundInterceptorFutureCallback(ctx, output, disconnect);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
    }

    private void handleWrite(
            final @NotNull ChannelHandlerContext ctx, final @NotNull DISCONNECT disconnect,
            final @NotNull ChannelPromise promise) throws Exception {

        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null || clientContext.getDisconnectOutboundInterceptors().isEmpty()) {
            super.write(ctx, disconnect, promise);
            return;
        }

        final List<DisconnectOutboundInterceptor> disconnectOutboundInterceptors =
                clientContext.getDisconnectOutboundInterceptors();

        final DisconnectOutboundInputImpl input =
                new DisconnectOutboundInputImpl(new DisconnectPacketImpl(disconnect), clientId, channel);

        final DisconnectOutboundOutputImpl output =
                new DisconnectOutboundOutputImpl(configurationService, asyncer, disconnect);

        final SettableFuture<Void> interceptorFuture = SettableFuture.create();

        final DisconnectOutboundInterceptorContext interceptorContext =
                new DisconnectOutboundInterceptorContext(
                        DisconnectOutboundInterceptorTask.class, clientId, input, interceptorFuture,
                        disconnectOutboundInterceptors.size());

        for (final DisconnectOutboundInterceptor interceptor : disconnectOutboundInterceptors) {

            if (interceptorFuture.isDone()) {
                break;
            }

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            if (extension == null) {
                interceptorContext.increment();
                continue;
            }

            final DisconnectOutboundInterceptorTask interceptorTask =
                    new DisconnectOutboundInterceptorTask(interceptor, interceptorFuture, extension.getId());

            executorService.handlePluginInOutTaskExecution(interceptorContext, input, output, interceptorTask);
        }
        final DisconnectOutboundInterceptorFutureCallback callback =
                new DisconnectOutboundInterceptorFutureCallback(output, disconnect, ctx, promise);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
    }

    static class DisconnectOutboundInterceptorContext extends PluginInOutTaskContext<DisconnectOutboundOutputImpl> {

        private final @NotNull DisconnectOutboundInputImpl input;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        DisconnectOutboundInterceptorContext(
                final @NotNull Class<?> taskClazz,
                final @NotNull String identifier,
                final @NotNull DisconnectOutboundInputImpl input,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final int interceptorCount) {
            super(taskClazz, identifier);
            this.input = input;
            this.interceptorFuture = interceptorFuture;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(final @NotNull DisconnectOutboundOutputImpl pluginOutput) {
            if (pluginOutput.isTimedOut()) {
                log.debug("Async timeout on inbound DISCONNECT interception");
                final DISCONNECT unmodifiedDisconnect = DISCONNECT.createDisconnectFrom(input.getDisconnectPacket());
                pluginOutput.update(unmodifiedDisconnect);
            } else if (pluginOutput.getDisconnectPacket().isModified()) {
                final ModifiableOutboundDisconnectPacketImpl disconnectPacket = pluginOutput.getDisconnectPacket();
                input.updateDisconnect(disconnectPacket);
                pluginOutput.update(disconnectPacket);
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

    private static class DisconnectOutboundInterceptorTask
            implements PluginInOutTask<DisconnectOutboundInputImpl, DisconnectOutboundOutputImpl> {

        private final @NotNull DisconnectOutboundInterceptor interceptor;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final @NotNull String pluginId;

        DisconnectOutboundInterceptorTask(
                final @NotNull DisconnectOutboundInterceptor interceptor,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final @NotNull String pluginId) {
            this.interceptor = interceptor;
            this.interceptorFuture = interceptorFuture;
            this.pluginId = pluginId;
        }

        @Override
        public DisconnectOutboundOutputImpl apply(
                final @NotNull DisconnectOutboundInputImpl input,
                final @NotNull DisconnectOutboundOutputImpl output) {
            try {
                if (!interceptorFuture.isDone()) {
                    interceptor.onOutboundDisconnect(input, output);
                }
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound disconnect interception. " +
                                "Extensions are responsible on their own to handle exceptions.", pluginId);
                log.debug("Original exception: ", e);
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

    private static class DisconnectOutboundInterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull DisconnectOutboundOutput output;
        private final @NotNull DISCONNECT disconnect;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;

        DisconnectOutboundInterceptorFutureCallback(
                final @NotNull DisconnectOutboundOutput output,
                final @NotNull DISCONNECT disconnect,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ChannelPromise promise) {
            this.output = output;
            this.disconnect = disconnect;
            this.ctx = ctx;
            this.promise = promise;
        }

        @Override
        public void onSuccess(@Nullable final Void result) {
            try {
                final DISCONNECT finalDisconnect = DISCONNECT.createDisconnectFrom(output.getDisconnectPacket());
                ctx.writeAndFlush(finalDisconnect, promise);
            } catch (final Exception e) {
                log.error("Exception while modifying an intercepted disconnect message.", e);
                ctx.writeAndFlush(disconnect, promise);
            }
        }

        @Override
        public void onFailure(final Throwable t) {
            ctx.channel().close();
        }
    }

    private static class DisconnectInboundInterceptorContext
            extends PluginInOutTaskContext<DisconnectInboundOutputImpl> {

        private final @NotNull DisconnectInboundInputImpl input;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        DisconnectInboundInterceptorContext(
                final @NotNull Class<?> taskClazz,
                final @NotNull String identifier,
                final @NotNull DisconnectInboundInputImpl input,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final int interceptorCount) {
            super(taskClazz, identifier);
            this.interceptorFuture = interceptorFuture;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.input = input;
        }

        @Override
        public void pluginPost(
                final @NotNull DisconnectInboundOutputImpl pluginOutput) {
            if (pluginOutput.isTimedOut()) {
                log.debug("Async timeout on inbound DISCONNECT interception");
                final DISCONNECT unmodifiedDisconnect = DISCONNECT.createDisconnectFrom(input.getDisconnectPacket());
                pluginOutput.update(unmodifiedDisconnect);
            } else if (pluginOutput.getDisconnectPacket().isModified()) {
                input.updateDisconnect(pluginOutput.getDisconnectPacket());
                final DISCONNECT updatedDisconnect =
                        DISCONNECT.createDisconnectFrom(pluginOutput.getDisconnectPacket());
                pluginOutput.update(updatedDisconnect);
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
                log.debug("Original exception:", e);
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

    private static class DisconnectInboundInterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull DisconnectInboundOutputImpl output;
        private final @NotNull DISCONNECT disconnect;
        private final @NotNull ChannelHandlerContext ctx;

        DisconnectInboundInterceptorFutureCallback(
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
        public void onFailure(final @NotNull Throwable t) {
            ctx.channel().close();
        }
    }
}
