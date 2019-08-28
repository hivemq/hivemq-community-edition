package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.puback.PubackInboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.puback.PubackInboundInputImpl;
import com.hivemq.extensions.interceptor.puback.PubackInboundOutputImpl;
import com.hivemq.extensions.packets.puback.PubackPacketImpl;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yannick Weber
 */
@Singleton
@ChannelHandler.Sharable
public class PubackInboundInterceptorHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(PubackInboundInterceptorHandler.class);

    @NotNull
    private final FullConfigurationService configurationService;

    @NotNull
    private final PluginOutPutAsyncer asyncer;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    @NotNull
    private final PluginTaskExecutorService pluginTaskExecutorService;

    @Inject
    public PubackInboundInterceptorHandler(@NotNull final FullConfigurationService configurationService,
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
        if (!(msg instanceof PUBACK)) {
            super.channelRead(ctx, msg);
            return;
        }
        if (!handlePuback(ctx, (PUBACK) msg)) {
            super.channelRead(ctx, msg);
        }
    }

    private boolean handlePuback(final ChannelHandlerContext ctx, final PUBACK puback) {
        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return false;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return false;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null || clientContext.getPubackInboundInterceptors().isEmpty()) {
            return false;
        }
        final List<PubackInboundInterceptor> pubackInboundInterceptors = clientContext.getPubackInboundInterceptors();

        final PubackInboundOutputImpl output = new PubackInboundOutputImpl(configurationService, asyncer, puback);
        final PubackInboundInputImpl
                input = new PubackInboundInputImpl(new PubackPacketImpl(puback), clientId, channel);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final PubackInterceptorContext interceptorContext = new PubackInterceptorContext(
                PubackInterceptorTask.class, clientId, input, output, interceptorFuture,
                pubackInboundInterceptors.size());

        for (final PubackInboundInterceptor interceptor : pubackInboundInterceptors) {

            if (interceptorFuture.isDone()) {
                // The future is set in case an async interceptor timeout failed
                break;
            }

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader((IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            //disabled extension would be null
            if (plugin == null) {
                interceptorContext.increment();
                continue;
            }
            final PubackInterceptorTask interceptorTask =
                    new PubackInterceptorTask(interceptor, interceptorFuture, plugin.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(interceptorContext, input, output, interceptorTask);
        }

        final InterceptorFutureCallback callback =
                new InterceptorFutureCallback(output, puback, ctx);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
        return true;
    }

    private static class InterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull PubackInboundOutputImpl output;
        private final @NotNull PUBACK puback;
        private final @NotNull ChannelHandlerContext ctx;

        InterceptorFutureCallback(final @NotNull PubackInboundOutputImpl output,
                final @NotNull PUBACK puback,
                final @NotNull ChannelHandlerContext ctx) {
            this.output = output;
            this.puback = puback;
            this.ctx = ctx;
        }

        @Override
        public void onSuccess(final @Nullable Void result) {
            try {
                final PUBACK finalPuback = PUBACK.createPubackFrom(output.getPubackPacket());
                ctx.fireChannelRead(finalPuback);
            } catch (final Exception e) {
                log.error("Exception while modifying an intercepted PUBACK message.", e);
                ctx.fireChannelRead(puback);
            }
        }

        @Override
        public void onFailure(final @NotNull Throwable t) {
            //should never happen, since the settable future never sets an exception
            ctx.channel().close();
        }
    }

    private static class PubackInterceptorContext extends PluginInOutTaskContext<PubackInboundOutputImpl> {

        private final @NotNull PubackInboundInputImpl input;
        private final @NotNull PubackInboundOutputImpl output;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PubackInterceptorContext(
                final @NotNull Class<?> taskClazz,
                final @NotNull String clientId,
                final @NotNull PubackInboundInputImpl input,
                final @NotNull PubackInboundOutputImpl output,
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
        public void pluginPost(@NotNull final PubackInboundOutputImpl pluginOutput) {
            if (pluginOutput.getPubackPacket().isModified()) {
                input.updatePuback(pluginOutput.getPubackPacket());
                final PUBACK updatedPuback = PUBACK.createPubackFrom(pluginOutput.getPubackPacket());
                output.update(updatedPuback);
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

    private static class PubackInterceptorTask implements
            PluginInOutTask<PubackInboundInputImpl, PubackInboundOutputImpl> {

        private final @NotNull PubackInboundInterceptor interceptor;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final @NotNull String pluginId;

        private PubackInterceptorTask(
                final @NotNull PubackInboundInterceptor interceptor,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final @NotNull String pluginId) {
            this.interceptor = interceptor;
            this.interceptorFuture = interceptorFuture;
            this.pluginId = pluginId;
        }

        @Override
        public @NotNull PubackInboundOutputImpl apply(final @NotNull PubackInboundInputImpl input, final @NotNull PubackInboundOutputImpl output) {
            try {
                if (!interceptorFuture.isDone()) {
                    interceptor.onInboundPuback(input, output);
                }
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on puback interception. The exception should be handled by the extension.",
                        pluginId);
                log.debug("Original exception:", e);

                // this is needed since the PUBACK could be incompletely modified
                final PUBACK unmodifiedPuback = PUBACK.createPubackFrom(input.getPubackPacket());
                output.update(unmodifiedPuback);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

}
