package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.PingResponseOutboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.pingresponse.parameter.PingResponseOutboundInputImpl;
import com.hivemq.extensions.interceptor.pingresponse.parameter.PingResponseOutboundOutputImpl;
import com.hivemq.extensions.packets.pingresponse.PingResponsePacketImpl;
import com.hivemq.mqtt.message.PINGRESP;
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
 * @since 4.2.0
 */
@Singleton
@ChannelHandler.Sharable
public class PingRespOutboundInterceptorHandler extends ChannelOutboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(PingRespOutboundInterceptorHandler.class);

    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;

    public PingRespOutboundInterceptorHandler(
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions) {
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
    }

    @Override
    public void write(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg, final @NotNull ChannelPromise promise) throws Exception {
        if (!(msg instanceof PINGRESP)) {
            super.write(ctx, msg, promise);
            return;
        }
        if (!handlePingResponse(ctx,(PINGRESP) msg, promise)) {
            super.write(ctx, msg, promise);
        }
    }

    private boolean handlePingResponse(final @NotNull ChannelHandlerContext ctx, final @NotNull PINGRESP pingresp, final @NotNull ChannelPromise promise) {
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

        final List<PingResponseOutboundInterceptor> pingResponseOutboundInterceptors = clientContext.getPingResponseOutboundInterceptors();
        final PingResponseOutboundInputImpl
                input = new PingResponseOutboundInputImpl(new PingResponsePacketImpl(pingresp), clientId, channel);
        final PingResponseOutboundOutputImpl output = new PingResponseOutboundOutputImpl(asyncer, pingresp);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final PingResponseOutboundInterceptorContext interceptorContext =
                new PingResponseOutboundInterceptorContext(
                        PingResponseOutboundInterceptorTask.class, clientId, output, input, interceptorFuture, pingResponseOutboundInterceptors.size());

        for (final PingResponseOutboundInterceptor interceptor : pingResponseOutboundInterceptors) {
            if (output.deliveryPrevented()) {
                if (!interceptorFuture.isDone()) {
                    interceptorFuture.set(null);
                }
                break;
            }

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader((IsolatedPluginClassloader) interceptor.getClass().getClassLoader());
            if (extension == null) {
                interceptorContext.increment();
                continue;
            }

            final PingResponseOutboundInterceptorTask interceptorTask = new PingResponseOutboundInterceptorTask(interceptor, extension.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(interceptorContext, input, output, interceptorTask);
        }

        final InterceptorFutureCallback callback = new InterceptorFutureCallback(output, ctx);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
        return true;
    }

    private static class PingResponseOutboundInterceptorContext extends
            PluginInOutTaskContext<PingResponseOutboundOutputImpl> {

        private final @NotNull PingResponseOutboundOutputImpl output;
        private final @NotNull PingResponseOutboundInputImpl input;
        final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        private PingResponseOutboundInterceptorContext(final @NotNull Class<?> taskClazz,
                final @NotNull String identifier,
                final @NotNull PingResponseOutboundOutputImpl output,
                final @NotNull PingResponseOutboundInputImpl input,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final int interceptorCount) {
            super(taskClazz, identifier);
            this.output = output;
            this.input = input;
            this.interceptorFuture = interceptorFuture;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(final @NotNull PingResponseOutboundOutputImpl pluginOutput) {
            if (pluginOutput.isAsync() && pluginOutput.isTimedOut() && pluginOutput.getTimeoutFallback() == TimeoutFallback.FAILURE) {
                pluginOutput.forciblyPreventPingResponseDelivery();
            }

            if (counter.incrementAndGet() == interceptorCount || pluginOutput.deliveryPrevented()) {
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
        public PingResponseOutboundOutputImpl apply(final @NotNull PingResponseOutboundInputImpl pingResponseOutboundInput,
                final @NotNull PingResponseOutboundOutputImpl pingResponseOutboundOutput) {
            if (pingResponseOutboundOutput.deliveryPrevented()) {
                return pingResponseOutboundOutput;
            }
            try {
                interceptor.onPingResp(pingResponseOutboundInput, pingResponseOutboundOutput);
            } catch (final Throwable e) {
                log.warn("Uncaught exception was thrown from extension with id \"{}\" on outbound ping response interception. Extensions are responsible for their own exception handling.",
                        pluginId);
                pingResponseOutboundOutput.forciblyPreventPingResponseDelivery();
                Exceptions.rethrowError(e);
            }
            return pingResponseOutboundOutput;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }
    private static class InterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull PingResponseOutboundOutputImpl outboundOutput;
        private final @NotNull ChannelHandlerContext ctx;

        private InterceptorFutureCallback(@NotNull final PingResponseOutboundOutputImpl outboundOutput, @NotNull final ChannelHandlerContext ctx) {
            this.outboundOutput = outboundOutput;
            this.ctx = ctx;
        }

        @Override
        public void onSuccess(@Nullable final Void result) {
            final PINGRESP pingresp = new PINGRESP();
            if (outboundOutput.deliveryPrevented()) {
                ctx.channel().close();
            } else {
                ctx.writeAndFlush(pingresp);
            }
        }

        @Override
        public void onFailure(Throwable t) {
            ctx.channel().close();
        }
    }
}