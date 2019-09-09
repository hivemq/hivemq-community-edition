package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecOutboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.pubrec.PubrecInboundInputImpl;
import com.hivemq.extensions.interceptor.pubrec.PubrecInboundOutputImpl;
import com.hivemq.extensions.interceptor.pubrec.PubrecOutboundInputImpl;
import com.hivemq.extensions.interceptor.pubrec.PubrecOutboundOutputImpl;
import com.hivemq.extensions.packets.pubrec.PubrecPacketImpl;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PubrecInterceptorHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(PubrecInterceptorHandler.class);

    @NotNull
    private final FullConfigurationService configurationService;

    @NotNull
    private final PluginOutPutAsyncer asyncer;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    @NotNull
    private final PluginTaskExecutorService pluginTaskExecutorService;

    @Inject
    public PubrecInterceptorHandler(
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
        if (!(msg instanceof PUBREC)) {
            super.channelRead(ctx, msg);
            return;
        }
        if (!handleInboundPubrec(ctx, (PUBREC) msg)) {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void write(
            @NotNull final ChannelHandlerContext ctx, @NotNull final Object msg, @NotNull final ChannelPromise promise)
            throws Exception {
        if (!(msg instanceof PUBREC)) {
            super.write(ctx, msg, promise);
            return;
        }
        if (!handleOutboundPubrec(ctx, (PUBREC) msg, promise)) {
            super.write(ctx, msg, promise);
        }
    }

    private boolean handleOutboundPubrec(
            final @NotNull ChannelHandlerContext ctx, final @NotNull PUBREC pubrec,
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
        if (clientContext == null || clientContext.getPubrecOutboundInterceptors().isEmpty()) {
            return false;
        }
        final List<PubrecOutboundInterceptor> pubrecOutboundInterceptors =
                clientContext.getPubrecOutboundInterceptors();

        final PubrecOutboundOutputImpl output = new PubrecOutboundOutputImpl(configurationService, asyncer, pubrec);

        final PubrecOutboundInputImpl
                input = new PubrecOutboundInputImpl(new PubrecPacketImpl(pubrec), clientId, channel);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final PubrecOutboundInterceptorContext interceptorContext = new PubrecOutboundInterceptorContext(
                PubrecOutboundInterceptorTask.class, clientId, input, output, interceptorFuture,
                pubrecOutboundInterceptors.size());

        for (final PubrecOutboundInterceptor interceptor : pubrecOutboundInterceptors) {

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
            final PubrecOutboundInterceptorTask
                    interceptorTask = new PubrecOutboundInterceptorTask(interceptor, interceptorFuture, plugin.getId());


            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }

        final InterceptorOutboundFutureCallback callback =
                new InterceptorOutboundFutureCallback(output, pubrec, ctx, promise);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
        return true;
    }

    private boolean handleInboundPubrec(final ChannelHandlerContext ctx, final PUBREC pubrec) {
        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return false;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return false;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null || clientContext.getPubrecInboundInterceptors().isEmpty()) {
            return false;
        }
        final List<PubrecInboundInterceptor> pubrecInboundInterceptors = clientContext.getPubrecInboundInterceptors();

        final PubrecInboundOutputImpl output = new PubrecInboundOutputImpl(configurationService, asyncer, pubrec);
        final PubrecInboundInputImpl
                input = new PubrecInboundInputImpl(new PubrecPacketImpl(pubrec), clientId, channel);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final PubrecInboundInterceptorContext
                interceptorContext = new PubrecInboundInterceptorContext(
                PubrecInboundInterceptorTask.class, clientId, input, output, interceptorFuture,
                pubrecInboundInterceptors.size());

        for (final PubrecInboundInterceptor interceptor : pubrecInboundInterceptors) {

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
            final PubrecInboundInterceptorTask interceptorTask =
                    new PubrecInboundInterceptorTask(interceptor, interceptorFuture, plugin.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }

        final InboundInterceptorFutureCallback callback =
                new InboundInterceptorFutureCallback(output, pubrec, ctx);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
        return true;
    }

    private static class InboundInterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull PubrecInboundOutputImpl output;
        private final @NotNull PUBREC pubrec;
        private final @NotNull ChannelHandlerContext ctx;

        InboundInterceptorFutureCallback(
                final @NotNull PubrecInboundOutputImpl output,
                final @NotNull PUBREC pubrec,
                final @NotNull ChannelHandlerContext ctx) {
            this.output = output;
            this.pubrec = pubrec;
            this.ctx = ctx;
        }

        @Override
        public void onSuccess(final @Nullable Void result) {
            try {
                final PUBREC finalPubrec = PUBREC.createPubrecFrom(output.getPubrecPacket());
                ctx.fireChannelRead(finalPubrec);
            } catch (final Exception e) {
                log.error("Exception while modifying an intercepted PUBREC message.", e);
                ctx.fireChannelRead(pubrec);
            }
        }

        @Override
        public void onFailure(final @NotNull Throwable t) {
            //should never happen, since the settable future never sets an exception
            ctx.channel().close();
        }
    }

    private static class PubrecInboundInterceptorContext extends PluginInOutTaskContext<PubrecInboundOutputImpl> {

        private final @NotNull PubrecInboundInputImpl input;
        private final @NotNull PubrecInboundOutputImpl output;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PubrecInboundInterceptorContext(
                final @NotNull Class<?> taskClazz,
                final @NotNull String clientId,
                final @NotNull PubrecInboundInputImpl input,
                final @NotNull PubrecInboundOutputImpl output,
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
        public void pluginPost(@NotNull final PubrecInboundOutputImpl pluginOutput) {
            if (pluginOutput.getPubrecPacket().isModified()) {
                input.updatePubrec(pluginOutput.getPubrecPacket());
                final PUBREC updatedPubrec = PUBREC.createPubrecFrom(pluginOutput.getPubrecPacket());
                output.update(updatedPubrec);
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

    private static class PubrecInboundInterceptorTask implements
            PluginInOutTask<PubrecInboundInputImpl, PubrecInboundOutputImpl> {

        private final @NotNull PubrecInboundInterceptor interceptor;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final @NotNull String pluginId;

        private PubrecInboundInterceptorTask(
                final @NotNull PubrecInboundInterceptor interceptor,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final @NotNull String pluginId) {
            this.interceptor = interceptor;
            this.interceptorFuture = interceptorFuture;
            this.pluginId = pluginId;
        }

        @Override
        public @NotNull PubrecInboundOutputImpl apply(
                final @NotNull PubrecInboundInputImpl input, final @NotNull PubrecInboundOutputImpl output) {
            try {
                if (!interceptorFuture.isDone()) {
                    interceptor.onInboundPubrec(input, output);
                }
                if (output.isTimedOut()) {
                    log.warn("Async timeout on inbound PUBREC interception.");
                    final PUBREC unmodifiedPubrec = PUBREC.createPubrecFrom(input.getPubrecPacket());
                    output.update(unmodifiedPubrec);
                }
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on pubrec interception. The exception should " +
                                "be handled by the extension.",
                        pluginId);
                log.debug("Original exception:", e);

                // this is needed since the PUBREC could be incompletely modified
                final PUBREC unmodifiedPubrec = PUBREC.createPubrecFrom(input.getPubrecPacket());
                output.update(unmodifiedPubrec);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

    private static class InterceptorOutboundFutureCallback implements FutureCallback<Void> {

        private final @NotNull PubrecOutboundOutputImpl output;
        private final @NotNull PUBREC pubrec;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;

        InterceptorOutboundFutureCallback(
                final @NotNull PubrecOutboundOutputImpl output,
                final @NotNull PUBREC pubrec,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ChannelPromise promise) {
            this.output = output;
            this.pubrec = pubrec;
            this.ctx = ctx;
            this.promise = promise;
        }

        @Override
        public void onSuccess(final @Nullable Void result) {
            try {
                final PUBREC finalpubrec = PUBREC.createPubrecFrom(output.getPubrecPacket());
                ctx.writeAndFlush(finalpubrec, promise);
            } catch (final Exception e) {
                log.error("Exception while modifying an intercepted pubrec message.", e);
                ctx.writeAndFlush(pubrec, promise);
            }
        }

        @Override
        public void onFailure(final @NotNull Throwable t) {
            //should never happen, since the settable future never sets an exception
            ctx.channel().close();
        }
    }

    private static class PubrecOutboundInterceptorContext extends PluginInOutTaskContext<PubrecOutboundOutputImpl> {

        private final @NotNull PubrecOutboundInputImpl input;
        private final @NotNull PubrecOutboundOutputImpl output;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PubrecOutboundInterceptorContext(
                final @NotNull Class<?> taskClazz,
                final @NotNull String clientId,
                final @NotNull PubrecOutboundInputImpl input,
                final @NotNull PubrecOutboundOutputImpl output,
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
        public void pluginPost(@NotNull final PubrecOutboundOutputImpl pluginOutput) {
            if (pluginOutput.getPubrecPacket().isModified()) {
                input.updatePubrec(pluginOutput.getPubrecPacket());
                final PUBREC modifiedPubrec = PUBREC.createPubrecFrom(pluginOutput.getPubrecPacket());
                output.update(modifiedPubrec);
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

    private static class PubrecOutboundInterceptorTask implements
            PluginInOutTask<PubrecOutboundInputImpl, PubrecOutboundOutputImpl> {

        private final @NotNull PubrecOutboundInterceptor interceptor;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final @NotNull String pluginId;

        private PubrecOutboundInterceptorTask(
                final @NotNull PubrecOutboundInterceptor interceptor,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final @NotNull String pluginId) {
            this.interceptor = interceptor;
            this.interceptorFuture = interceptorFuture;
            this.pluginId = pluginId;
        }

        @Override
        public @NotNull PubrecOutboundOutputImpl apply(
                final @NotNull PubrecOutboundInputImpl input, final @NotNull PubrecOutboundOutputImpl output) {

            try {
                if (!interceptorFuture.isDone()) {
                    interceptor.onOutboundPubrec(input, output);
                }
                if (output.isTimedOut()) {
                    log.warn("Async timeout on outbound PUBREC interception.");
                    final PUBREC unmodifiedPubrec = PUBREC.createPubrecFrom(input.getPubrecPacket());
                    output.update(unmodifiedPubrec);
                }
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on PUBREC interception. The exception should be handled by the extension.",
                        pluginId);
                log.debug("Original exception:", e);

                // this is needed since the interceptor can throw an exception, while modifying the packet and leave it in an invalid state
                final PUBREC unmodifiedPubrec = PUBREC.createPubrecFrom(input.getPubrecPacket());
                output.update(unmodifiedPubrec);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }
}
