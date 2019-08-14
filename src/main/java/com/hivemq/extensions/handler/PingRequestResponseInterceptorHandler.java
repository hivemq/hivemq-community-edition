package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pingrequestresponse.PingRequestResponseInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.pingrequestresponse.PingRequestResponseInputImpl;
import com.hivemq.extensions.interceptor.pingrequestresponse.PingRequestResponseOutputImpl;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.PINGRESP;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Robin Atherton
 */
public class PingRequestResponseInterceptorHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(PingRequestResponseInterceptorHandler.class);

    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;

    public PingRequestResponseInterceptorHandler(
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions) {
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
    }

    @Override
    public void read(final ChannelHandlerContext ctx) throws Exception {
        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null || clientContext.getPingResponseOutboundInterceptors().isEmpty()) {
            return;
        }

        final List<PingRequestResponseInterceptor> pingRequestResponseInterceptors =
                clientContext.getPingRequestResponseInterceptors();

        final PingRequestResponseInputImpl input =
                new PingRequestResponseInputImpl(clientId, channel);
        final PingRequestResponseOutputImpl output =
                new PingRequestResponseOutputImpl(asyncer);

        final SettableFuture<Void> interceptorFuture = SettableFuture.create();

        final PingRequestResponseInterceptorContext interceptorContext =
                new PingRequestResponseInterceptorContext(PingRequestResponseInterceptorTask.class, clientId,
                        interceptorFuture, pingRequestResponseInterceptors.size());

        for (final PingRequestResponseInterceptor interceptor : pingRequestResponseInterceptors) {
            if (!interceptorFuture.isDone()) {
                interceptorFuture.set(null);
            }

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());
            if (extension == null) {
                interceptorContext.increment();
                continue;
            }

            final PingRequestResponseInterceptorTask interceptorTask =
                    new PingRequestResponseInterceptorTask(interceptor, extension.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }

        final InboundInterceptorFutureCallback inboundCallback = new InboundInterceptorFutureCallback(ctx);
        Futures.addCallback(interceptorFuture, inboundCallback, ctx.executor());

    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise)
            throws Exception {
        if (!(msg instanceof PINGRESP)) {
            super.write(ctx, msg, promise);
            return;
        }
        if (!handlePingResponse(ctx)) {
            super.write(ctx, msg, promise);
        }
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

        final List<PingRequestResponseInterceptor> pingRequestResponseInterceptors =
                clientContext.getPingRequestResponseInterceptors();

        final PingRequestResponseInputImpl input =
                new PingRequestResponseInputImpl(clientId, channel);
        final PingRequestResponseOutputImpl output =
                new PingRequestResponseOutputImpl(asyncer);

        final SettableFuture<Void> interceptorFuture = SettableFuture.create();

        final PingRequestResponseInterceptorContext interceptorContext =
                new PingRequestResponseInterceptorContext(PingRequestResponseInterceptorTask.class, clientId,
                        interceptorFuture, pingRequestResponseInterceptors.size());

        for (final PingRequestResponseInterceptor interceptor : pingRequestResponseInterceptors) {
            if (!interceptorFuture.isDone()) {
                interceptorFuture.set(null);
            }

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());
            if (extension == null) {
                interceptorContext.increment();
                continue;
            }

            final PingRequestResponseInterceptorTask interceptorTask =
                    new PingRequestResponseInterceptorTask(interceptor, extension.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }

        final OutboundInterceptorFutureCallback outboundCallback = new OutboundInterceptorFutureCallback(ctx);
        Futures.addCallback(interceptorFuture, outboundCallback, ctx.executor());
        return true;
    }

    private static class PingRequestResponseInterceptorContext extends
            PluginInOutTaskContext<PingRequestResponseOutputImpl> {

        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        protected PingRequestResponseInterceptorContext(
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
        public void pluginPost(final @NotNull PingRequestResponseOutputImpl pluginOutput) {
            if (counter.incrementAndGet() == interceptorCount) {
                interceptorFuture.set(null);
            }
        }

        public void increment() {
            if (counter.incrementAndGet() == interceptorCount) {
                interceptorFuture.set(null);
            }
        }
    }

    private static class PingRequestResponseInterceptorTask implements
            PluginInOutTask<PingRequestResponseInputImpl, PingRequestResponseOutputImpl> {

        private final @NotNull PingRequestResponseInterceptor interceptor;
        private final @NotNull String pluginId;

        PingRequestResponseInterceptorTask(
                final @NotNull PingRequestResponseInterceptor interceptor,
                final @NotNull String pluginId) {
            this.interceptor = interceptor;
            this.pluginId = pluginId;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }

        @Override
        public PingRequestResponseOutputImpl apply(
                final @NotNull PingRequestResponseInputImpl input,
                final @NotNull PingRequestResponseOutputImpl output) {
            try {
                interceptor.onPing(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on in or outbound ping interception. " +
                                "Extensions are responsible for their own exception handling.",
                        pluginId);
                Exceptions.rethrowError(e);
            }
            return output;
        }
    }

    private static class OutboundInterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull ChannelHandlerContext ctx;

        OutboundInterceptorFutureCallback(@NotNull final ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void onSuccess(@Nullable final Void result) {
            final PINGRESP pingresp = new PINGRESP();
            ctx.writeAndFlush(pingresp);
        }

        @Override
        public void onFailure(final Throwable t) {
            ctx.channel().close();
        }
    }

    private static class InboundInterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull ChannelHandlerContext ctx;

        InboundInterceptorFutureCallback(@NotNull final ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void onSuccess(final @Nullable Void result) {
            final PINGREQ pingreq = new PINGREQ();
            ctx.fireChannelRead(pingreq);
        }

        @Override
        public void onFailure(final Throwable t) {

        }
    }

}
