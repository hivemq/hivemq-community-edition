package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecInboundInterceptor;
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
import com.hivemq.extensions.packets.pubrec.PubrecPacketImpl;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PubrecInboundInterceptorHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(PubrecInboundInterceptorHandler.class);

    @NotNull
    private final FullConfigurationService configurationService;

    @NotNull
    private final PluginOutPutAsyncer asyncer;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    @NotNull
    private final PluginTaskExecutorService pluginTaskExecutorService;

    @Inject
    public PubrecInboundInterceptorHandler(
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
        if (!handlePubrec(ctx, (PUBREC) msg)) {
            super.channelRead(ctx, msg);
        }
    }

    private boolean handlePubrec(final ChannelHandlerContext ctx, final PUBREC pubrec) {
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
        final PubrecInterceptorContext interceptorContext = new PubrecInterceptorContext(
                PubrecInterceptorTask.class, clientId, input, output, interceptorFuture,
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
            final PubrecInterceptorTask interceptorTask =
                    new PubrecInterceptorTask(interceptor, interceptorFuture, plugin.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }

        final InterceptorFutureCallback callback =
                new InterceptorFutureCallback(output, pubrec, ctx);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
        return true;
    }

    private static class InterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull PubrecInboundOutputImpl output;
        private final @NotNull PUBREC pubrec;
        private final @NotNull ChannelHandlerContext ctx;

        InterceptorFutureCallback(
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

    private static class PubrecInterceptorContext extends PluginInOutTaskContext<PubrecInboundOutputImpl> {

        private final @NotNull PubrecInboundInputImpl input;
        private final @NotNull PubrecInboundOutputImpl output;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PubrecInterceptorContext(
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

    private static class PubrecInterceptorTask implements
            PluginInOutTask<PubrecInboundInputImpl, PubrecInboundOutputImpl> {

        private final @NotNull PubrecInboundInterceptor interceptor;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final @NotNull String pluginId;

        private PubrecInterceptorTask(
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

}