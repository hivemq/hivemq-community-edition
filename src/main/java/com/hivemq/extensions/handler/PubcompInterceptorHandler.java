package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompOutboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.pubcomp.parameter.PubcompInboundInputImpl;
import com.hivemq.extensions.interceptor.pubcomp.parameter.PubcompInboundOutputImpl;
import com.hivemq.extensions.interceptor.pubcomp.parameter.PubcompOutboundInputImpl;
import com.hivemq.extensions.interceptor.pubcomp.parameter.PubcompOutboundOutputImpl;
import com.hivemq.extensions.packets.pubcomp.PubcompPacketImpl;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yannick Weber
 */
@ChannelHandler.Sharable
public class PubcompInterceptorHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(PubcompInterceptorHandler.class);

    @NotNull
    private final FullConfigurationService configurationService;

    @NotNull
    private final PluginOutPutAsyncer asyncer;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    @NotNull
    private final PluginTaskExecutorService pluginTaskExecutorService;

    @Inject
    public PubcompInterceptorHandler(
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
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (!(msg instanceof PUBCOMP)) {
            super.channelRead(ctx, msg);
            return;
        }
        if (!handleInboundPubcomp(ctx, (PUBCOMP) msg)) {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void write(
            @NotNull final ChannelHandlerContext ctx, @NotNull final Object msg, @NotNull final ChannelPromise promise)
            throws Exception {
        if (!(msg instanceof PUBCOMP)) {
            super.write(ctx, msg, promise);
            return;
        }
        if (!handleOutboundPubcomp(ctx, (PUBCOMP) msg, promise)) {
            super.write(ctx, msg, promise);
        }

    }

    private boolean handleOutboundPubcomp(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PUBCOMP pubcomp,
            final @NotNull ChannelPromise promise) {
        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return false;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return false;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null || clientContext.getPubcompOutboundInterceptors().isEmpty()) {
            return false;
        }
        final List<PubcompOutboundInterceptor> pubcompOutboundInterceptors =
                clientContext.getPubcompOutboundInterceptors();

        final PubcompOutboundOutputImpl output = new PubcompOutboundOutputImpl(configurationService, asyncer, pubcomp);
        final PubcompOutboundInputImpl
                input = new PubcompOutboundInputImpl(new PubcompPacketImpl(pubcomp), clientId, channel);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final PubcompOutboundInterceptorContext interceptorContext =
                new PubcompOutboundInterceptorContext(PubcompOutboundInterceptorTask.class,
                        clientId, input, output, interceptorFuture, pubcompOutboundInterceptors.size());


        for (final PubcompOutboundInterceptor interceptor : pubcompOutboundInterceptors) {

            if (interceptorFuture.isDone()) {
                // The future is set in case an async interceptor timeout failed
                break;
            }

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            //disabled extension would be null
            if (plugin == null) {
                interceptorContext.increment();
                continue;
            }
            final PubcompOutboundInterceptorTask interceptorTask =
                    new PubcompOutboundInterceptorTask(interceptor, interceptorFuture, plugin.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }

        final OutboundInterceptorFutureCallback callback =
                new OutboundInterceptorFutureCallback(output, pubcomp, ctx, promise);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
        return true;
    }

    private boolean handleInboundPubcomp(final ChannelHandlerContext ctx, final PUBCOMP pubcomp) {
        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return false;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return false;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null || clientContext.getPubcompInboundInterceptors().isEmpty()) {
            return false;
        }
        final List<PubcompInboundInterceptor> pubcompInboundInterceptors =
                clientContext.getPubcompInboundInterceptors();

        final PubcompInboundOutputImpl output = new PubcompInboundOutputImpl(configurationService, asyncer, pubcomp);
        final PubcompInboundInputImpl
                input = new PubcompInboundInputImpl(new PubcompPacketImpl(pubcomp), clientId, channel);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final PubcompInboundInterceptorContext interceptorContext = new PubcompInboundInterceptorContext(
                PubcompInboundInterceptorTask.class, clientId, input, output, interceptorFuture,
                pubcompInboundInterceptors.size());

        for (final PubcompInboundInterceptor interceptor : pubcompInboundInterceptors) {

            if (interceptorFuture.isDone()) {
                // The future is set in case an async interceptor timeout failed
                break;
            }

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            //disabled extension would be null
            if (plugin == null) {
                interceptorContext.increment();
                continue;
            }
            final PubcompInboundInterceptorTask interceptorTask =
                    new PubcompInboundInterceptorTask(interceptor, interceptorFuture, plugin.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }

        final InboundInterceptorFutureCallback callback =
                new InboundInterceptorFutureCallback(output, pubcomp, ctx);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
        return true;
    }

    private static class InboundInterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull PubcompInboundOutputImpl output;
        private final @NotNull PUBCOMP pubcomp;
        private final @NotNull ChannelHandlerContext ctx;

        InboundInterceptorFutureCallback(
                final @NotNull PubcompInboundOutputImpl output,
                final @NotNull PUBCOMP pubcomp,
                final @NotNull ChannelHandlerContext ctx) {
            this.output = output;
            this.pubcomp = pubcomp;
            this.ctx = ctx;
        }

        @Override
        public void onSuccess(final @Nullable Void result) {
            try {
                final PUBCOMP finalPubcomp = PUBCOMP.createPubcompFrom(output.getPubcompPacket());
                ctx.fireChannelRead(finalPubcomp);
            } catch (final Exception e) {
                log.error("Exception while modifying an intercepted PUBCOMP message.", e);
                ctx.fireChannelRead(pubcomp);
            }
        }

        @Override
        public void onFailure(final @NotNull Throwable t) {
            //should never happen, since the settable future never sets an exception
            ctx.channel().close();
        }
    }

    private static class PubcompInboundInterceptorContext extends PluginInOutTaskContext<PubcompInboundOutputImpl> {

        private final @NotNull PubcompInboundInputImpl input;
        private final @NotNull PubcompInboundOutputImpl output;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PubcompInboundInterceptorContext(
                final @NotNull Class<?> taskClazz,
                final @NotNull String clientId,
                final @NotNull PubcompInboundInputImpl input,
                final @NotNull PubcompInboundOutputImpl output,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final int interceptorCount) {
            super(taskClazz, clientId);
            this.input = input;
            this.output = output;
            this.interceptorFuture = interceptorFuture;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(@NotNull final PubcompInboundOutputImpl pluginOutput) {
            if (output.isTimedOut()) {
                log.warn("Async timeout on inbound PUBCOMP interception.");
                final PUBCOMP unmodifiedPubcomp = PUBCOMP.createPubcompFrom(input.getPubcompPacket());
                output.update(unmodifiedPubcomp);
            } else if (pluginOutput.getPubcompPacket().isModified()) {
                input.updatePubcomp(pluginOutput.getPubcompPacket());
                final PUBCOMP updatedPubcomp = PUBCOMP.createPubcompFrom(pluginOutput.getPubcompPacket());
                output.update(updatedPubcomp);
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

    private static class PubcompInboundInterceptorTask implements
            PluginInOutTask<PubcompInboundInputImpl, PubcompInboundOutputImpl> {

        private final @NotNull PubcompInboundInterceptor interceptor;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final @NotNull String pluginId;

        private PubcompInboundInterceptorTask(
                final @NotNull PubcompInboundInterceptor interceptor,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final @NotNull String pluginId) {
            this.interceptor = interceptor;
            this.interceptorFuture = interceptorFuture;
            this.pluginId = pluginId;
        }

        @Override
        public @NotNull PubcompInboundOutputImpl apply(
                final @NotNull PubcompInboundInputImpl input, final @NotNull PubcompInboundOutputImpl output) {
            try {
                if (!interceptorFuture.isDone()) {
                    interceptor.onInboundPubcomp(input, output);
                }
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on pubcomp interception. The exception should be handled by the extension.",
                        pluginId);
                log.debug("Original exception:", e);

                // this is needed since the PUBCOMP could be incompletely modified
                final PUBCOMP unmodifiedPubcomp = PUBCOMP.createPubcompFrom(input.getPubcompPacket());
                output.update(unmodifiedPubcomp);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

    private static class OutboundInterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull PubcompOutboundOutputImpl output;
        private final @NotNull PUBCOMP pubcomp;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;

        OutboundInterceptorFutureCallback(
                final @NotNull PubcompOutboundOutputImpl output,
                final @NotNull PUBCOMP pubcomp,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ChannelPromise promise) {
            this.output = output;
            this.pubcomp = pubcomp;
            this.ctx = ctx;
            this.promise = promise;
        }

        @Override
        public void onSuccess(final @Nullable Void result) {
            try {
                final PUBCOMP finalPubcomp = PUBCOMP.createPubcompFrom(output.getPubcompPacket());
                ctx.writeAndFlush(finalPubcomp, promise);
            } catch (final Exception e) {
                log.error("Exception while modifying an intercepted PUBCOMP message.", e);
                ctx.writeAndFlush(pubcomp, promise);
            }
        }

        @Override
        public void onFailure(final @NotNull Throwable t) {
            //should never happen, since the settable future never sets an exception
            ctx.channel().close();
        }
    }

    private static class PubcompOutboundInterceptorContext extends PluginInOutTaskContext<PubcompOutboundOutputImpl> {

        private final @NotNull PubcompOutboundInputImpl input;
        private final @NotNull PubcompOutboundOutputImpl output;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PubcompOutboundInterceptorContext(
                final @NotNull Class<?> taskClazz,
                final @NotNull String clientId,
                final @NotNull PubcompOutboundInputImpl input,
                final @NotNull PubcompOutboundOutputImpl output,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final int interceptorCount) {
            super(taskClazz, clientId);
            this.input = input;
            this.output = output;
            this.interceptorFuture = interceptorFuture;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(@NotNull final PubcompOutboundOutputImpl pluginOutput) {
            if (output.isTimedOut()) {
                log.warn("Async timeout on outbound PUBCOMP interception.");
                final PUBCOMP unmodifiedPubcomp = PUBCOMP.createPubcompFrom(input.getPubcompPacket());
                output.update(unmodifiedPubcomp);
            } else if (pluginOutput.getPubcompPacket().isModified()) {
                input.updatePubcomp(pluginOutput.getPubcompPacket());
                final PUBCOMP updatedPubcomp = PUBCOMP.createPubcompFrom(output.getPubcompPacket());
                output.update(updatedPubcomp);
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

    private static class PubcompOutboundInterceptorTask implements
            PluginInOutTask<PubcompOutboundInputImpl, PubcompOutboundOutputImpl> {

        private final @NotNull PubcompOutboundInterceptor interceptor;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final @NotNull String pluginId;

        private PubcompOutboundInterceptorTask(
                final @NotNull PubcompOutboundInterceptor interceptor,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final @NotNull String pluginId) {
            this.interceptor = interceptor;
            this.interceptorFuture = interceptorFuture;
            this.pluginId = pluginId;
        }

        @Override
        public @NotNull PubcompOutboundOutputImpl apply(
                final @NotNull PubcompOutboundInputImpl input, final @NotNull PubcompOutboundOutputImpl output) {
            try {
                if (!interceptorFuture.isDone()) {
                    interceptor.onOutboundPubcomp(input, output);
                }
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on pubcomp interception. The exception should be handled by the extension.",
                        pluginId);
                log.debug("Original exception:", e);
                final PUBCOMP unmodifiedPubcomp = PUBCOMP.createPubcompFrom(input.getPubcompPacket());
                output.update(unmodifiedPubcomp);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

}
