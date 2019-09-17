package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.PingRequestInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.PingResponseOutboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.pingrequest.parameter.PingRequestInboundInputImpl;
import com.hivemq.extensions.interceptor.pingrequest.parameter.PingRequestInboundOutputImpl;
import com.hivemq.extensions.interceptor.pingresponse.parameter.PingResponseOutboundInputImpl;
import com.hivemq.extensions.interceptor.pingresponse.parameter.PingResponseOutboundOutputImpl;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.PINGRESP;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Robin Atherton
 */
@ChannelHandler.Sharable
@Singleton
public class PingInterceptorHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(PingInterceptorHandler.class);

    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;

    @Inject
    public PingInterceptorHandler(
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions) {
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
    }

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull Object msg,
            final @NotNull ChannelPromise promise)
            throws Exception {
        if (!(msg instanceof PINGRESP)) {
            super.write(ctx, msg, promise);
            return;
        }
        if (!handlePingResponse(ctx)) {
            super.write(ctx, msg, promise);
        }
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (!(msg instanceof PINGREQ)) {
            super.channelRead(ctx, msg);
        } else {
            handlePingRequest(ctx, ((PINGREQ) msg));
        }
    }

    private void handlePingRequest(final @NotNull ChannelHandlerContext ctx, final PINGREQ pingreq) throws Exception {

        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null || clientContext.getPingRequestInboundInterceptors().isEmpty()) {
            super.channelRead(ctx, pingreq);
            return;
        }

        final List<PingRequestInboundInterceptor> pingRequestInboundInterceptors =
                clientContext.getPingRequestInboundInterceptors();
        final PingRequestInboundOutputImpl output = new PingRequestInboundOutputImpl(asyncer);
        final PingRequestInboundInputImpl
                input = new PingRequestInboundInputImpl(clientId, channel);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final PingRequestInboundInterceptorContext interceptorContext =
                new PingRequestInboundInterceptorContext(
                        PingRequestInboundInterceptorTask.class,
                        clientId, interceptorFuture, pingRequestInboundInterceptors.size());

        for (final PingRequestInboundInterceptor interceptor : pingRequestInboundInterceptors) {
            if (!interceptorFuture.isDone()) {
                interceptorFuture.set(null);
            }

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            if (plugin == null) {
                interceptorContext.increment();
                continue;
            }

            final PingRequestInboundInterceptorTask interceptorTask =
                    new PingRequestInboundInterceptorTask(interceptor, plugin.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }

        final PingRequestInterceptorFutureCallback
                callback = new PingRequestInterceptorFutureCallback(ctx);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
    }

    private boolean handlePingResponse(
            final @NotNull ChannelHandlerContext ctx) {
        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return false;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return false;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null || clientContext.getPingResponseOutboundInterceptors().isEmpty()) {
            return false;
        }

        final List<PingResponseOutboundInterceptor> pingResponseOutboundInterceptors =
                clientContext.getPingResponseOutboundInterceptors();
        final PingResponseOutboundInputImpl
                input = new PingResponseOutboundInputImpl(clientId, channel);
        final PingResponseOutboundOutputImpl output = new PingResponseOutboundOutputImpl(asyncer);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final PingResponseOutboundInterceptorContext interceptorContext =
                new PingResponseOutboundInterceptorContext(
                        PingResponseOutboundInterceptorTask.class, clientId, interceptorFuture,
                        pingResponseOutboundInterceptors.size());

        for (final PingResponseOutboundInterceptor interceptor : pingResponseOutboundInterceptors) {
            if (!interceptorFuture.isDone()) {
                interceptorFuture.set(null);
            }

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());
            if (extension == null) {
                interceptorContext.increment();
                continue;
            }

            final PingResponseOutboundInterceptorTask interceptorTask =
                    new PingResponseOutboundInterceptorTask(
                            interceptor, extension.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }

        final PingResponseInterceptorFutureCallback
                callback = new PingResponseInterceptorFutureCallback(ctx);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
        return true;
    }

    private static class PingRequestInboundInterceptorTask implements
            PluginInOutTask<PingRequestInboundInputImpl, PingRequestInboundOutputImpl> {

        private final @NotNull PingRequestInboundInterceptor interceptor;
        private final @NotNull String pluginId;

        PingRequestInboundInterceptorTask(
                final @NotNull PingRequestInboundInterceptor interceptor,
                final @NotNull String pluginId) {
            this.interceptor = interceptor;
            this.pluginId = pluginId;
        }

        @Override
        public PingRequestInboundOutputImpl apply(
                final @NotNull PingRequestInboundInputImpl pingRequestInboundInput,
                final @NotNull PingRequestInboundOutputImpl pingRequestInboundOutput) {
            try {
                interceptor.onInboundPingReq(pingRequestInboundInput, pingRequestInboundOutput);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound ping request interception. Extensions are responsible for their own exception handling.",
                        pluginId);
            }
            return pingRequestInboundOutput;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }

    }

    private static class PingResponseOutboundInterceptorTask implements
            PluginInOutTask<PingResponseOutboundInputImpl, PingResponseOutboundOutputImpl> {

        private final @NotNull PingResponseOutboundInterceptor interceptor;

        private final @NotNull String pluginId;

        public PingResponseOutboundInterceptorTask(
                final @NotNull PingResponseOutboundInterceptor interceptor,
                final @NotNull String pluginId) {
            this.interceptor = interceptor;
            this.pluginId = pluginId;
        }

        @Override
        public PingResponseOutboundOutputImpl apply(
                final @NotNull PingResponseOutboundInputImpl pingResponseOutboundInput,
                final @NotNull PingResponseOutboundOutputImpl pingResponseOutboundOutput) {
            try {
                interceptor.onOutboundPingResp(pingResponseOutboundInput, pingResponseOutboundOutput);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound ping response interception. Extensions are responsible for their own exception handling.",
                        pluginId);
            }
            return pingResponseOutboundOutput;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }

    }

    private static class PingRequestInboundInterceptorContext extends
            PluginInOutTaskContext<PingRequestInboundOutputImpl> {

        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;

        private final @NotNull AtomicInteger counter;

        protected PingRequestInboundInterceptorContext(
                final @NotNull Class<?> taskClazz,
                final @NotNull String identifier,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final int interceptorCount) {
            super(taskClazz, identifier);
            this.interceptorFuture = interceptorFuture;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(final @NotNull PingRequestInboundOutputImpl pluginOutput) {
            increment();
        }

        public void increment() {
            //we must set the future when no more interceptors are registered
            if (counter.incrementAndGet() == interceptorCount) {
                interceptorFuture.set(null);
            }
        }

    }

    private static class PingResponseOutboundInterceptorContext extends
            PluginInOutTaskContext<PingResponseOutboundOutputImpl> {

        final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        private PingResponseOutboundInterceptorContext(
                final @NotNull Class<?> taskClazz,
                final @NotNull String identifier,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final int interceptorCount) {
            super(taskClazz, identifier);
            this.interceptorFuture = interceptorFuture;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(final @NotNull PingResponseOutboundOutputImpl pluginOutput) {
            increment();
        }

        public void increment() {
            //we must set the future when no more interceptors are registered
            if (counter.incrementAndGet() == interceptorCount) {
                interceptorFuture.set(null);
            }
        }

    }

    private static class PingRequestInterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull ChannelHandlerContext ctx;

        PingRequestInterceptorFutureCallback(final @NotNull ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void onSuccess(@Nullable final Void result) {
            final PINGREQ pingreq = new PINGREQ();
            ctx.fireChannelRead(pingreq);
        }

        @Override
        public void onFailure(final @NotNull Throwable t) {
            log.error(t.getMessage());
        }
    }

    private static class PingResponseInterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull ChannelHandlerContext ctx;

        private PingResponseInterceptorFutureCallback(@NotNull final ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void onSuccess(@Nullable final Void result) {
            final PINGRESP pingresp = new PINGRESP();
            ctx.writeAndFlush(pingresp);
        }

        @Override
        public void onFailure(final @NotNull Throwable t) {
            log.error("Intercepting a ping response has failed \n" + t.getMessage());
        }

    }
}






