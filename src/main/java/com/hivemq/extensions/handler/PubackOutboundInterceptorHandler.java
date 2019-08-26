package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.puback.PubackOutboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.puback.PubackOutboundInputImpl;
import com.hivemq.extensions.interceptor.puback.PubackOutboundOutputImpl;
import com.hivemq.extensions.packets.puback.PubackPacketImpl;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.*;
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
public class PubackOutboundInterceptorHandler extends ChannelOutboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(PubackOutboundInterceptorHandler.class);

    @NotNull
    private final FullConfigurationService configurationService;

    @NotNull
    private final PluginOutPutAsyncer asyncer;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    @NotNull
    private final PluginTaskExecutorService pluginTaskExecutorService;


    @Inject
    public PubackOutboundInterceptorHandler(@NotNull final FullConfigurationService configurationService,
                                            @NotNull final PluginOutPutAsyncer asyncer,
                                            @NotNull final HiveMQExtensions hiveMQExtensions,
                                            @NotNull final PluginTaskExecutorService pluginTaskExecutorService) {
        this.configurationService = configurationService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.pluginTaskExecutorService = pluginTaskExecutorService;
    }

    @Override
    public void write(
            @NotNull final ChannelHandlerContext ctx, @NotNull final Object msg, @NotNull final ChannelPromise promise)
            throws Exception {
        if (!(msg instanceof PUBACK)) {
            super.write(ctx, msg, promise);
            return;
        }
        if (!handlePuback(ctx, (PUBACK) msg, promise)) {
            super.write(ctx, msg, promise);
        }

    }

    private boolean handlePuback(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PUBACK puback,
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
        if (clientContext == null || clientContext.getPubackOutboundInterceptors().isEmpty()) {
            return false;
        }
        final List<PubackOutboundInterceptor> pubackOutboundInterceptors = clientContext.getPubackOutboundInterceptors();

        final PubackOutboundOutputImpl output = new PubackOutboundOutputImpl(configurationService, asyncer, puback);
        final PubackOutboundInputImpl
                input = new PubackOutboundInputImpl(new PubackPacketImpl(puback), clientId, channel);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final PubackInterceptorContext interceptorContext = new PubackInterceptorContext(PubackInterceptorTask.class,
                clientId, input, output, interceptorFuture, pubackOutboundInterceptors.size());


        for (final PubackOutboundInterceptor interceptor : pubackOutboundInterceptors) {

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
            final PubackInterceptorTask interceptorTask = new PubackInterceptorTask(interceptor, interceptorFuture, plugin.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(interceptorContext, input, output, interceptorTask);
        }

        final InterceptorFutureCallback callback = new InterceptorFutureCallback(output, puback, ctx, promise);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
        return true;
    }

    private static class InterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull PubackOutboundOutputImpl output;
        private final @NotNull PUBACK puback;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        InterceptorFutureCallback(final @NotNull PubackOutboundOutputImpl output,
                                  final @NotNull PUBACK puback,
                                  final @NotNull ChannelHandlerContext ctx,
                                  final @NotNull ChannelPromise promise) {
            this.output = output;
            this.puback = puback;
            this.ctx = ctx;
            this.promise = promise;
        }

        @Override
        public void onSuccess(final @Nullable Void result) {
            try {
                final PUBACK finalPuback = PUBACK.createPubackFrom(output.getPubackPacket());
                ctx.writeAndFlush(finalPuback, promise);
            } catch (final Exception e) {
                log.error("Exception while modifying an intercepted PUBACK message.", e);
                ctx.writeAndFlush(puback, promise);
            }
        }

        @Override
        public void onFailure(final @NotNull Throwable t) {
            //should never happen, since the settable future never sets an exception
            ctx.channel().close();
        }
    }

    private static class PubackInterceptorContext extends PluginInOutTaskContext<PubackOutboundOutputImpl> {

        private final @NotNull PubackOutboundInputImpl input;
        private final @NotNull PubackOutboundOutputImpl output;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PubackInterceptorContext(
                final @NotNull Class<?> taskClazz,
                final @NotNull String clientId,
                final @NotNull PubackOutboundInputImpl input,
                final @NotNull PubackOutboundOutputImpl output,
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
        public void pluginPost(@NotNull final PubackOutboundOutputImpl pluginOutput) {
            if (pluginOutput.getPubackPacket().isModified()) {
                input.updatePuback(pluginOutput.getPubackPacket());
                final PUBACK updatedPuback = PUBACK.createPubackFrom(output.getPubackPacket());
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
            PluginInOutTask<PubackOutboundInputImpl, PubackOutboundOutputImpl> {

        private final @NotNull PubackOutboundInterceptor interceptor;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final @NotNull String pluginId;

        private PubackInterceptorTask(
                final @NotNull PubackOutboundInterceptor interceptor,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final @NotNull String pluginId) {
            this.interceptor = interceptor;
            this.interceptorFuture = interceptorFuture;
            this.pluginId = pluginId;
        }

        @Override
        public @NotNull PubackOutboundOutputImpl apply(final @NotNull PubackOutboundInputImpl input, final @NotNull PubackOutboundOutputImpl output) {
            try {
                if (!interceptorFuture.isDone()) {
                    interceptor.onOutboundPuback(input, output);
                }
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on puback interception. The exception should be handled by the extension.",
                        pluginId);
                log.debug("Original exception:", e);
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
