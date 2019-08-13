package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
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
import com.hivemq.extensions.services.interceptor.Interceptors;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yannick Weber
 * @since 4.2.0
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

    @NotNull
    private final Interceptors interceptors;

    @NotNull
    private final ServerInformation serverInformation;

    @NotNull
    private final EventLog eventLog;


    @Inject
    public PubackOutboundInterceptorHandler(@NotNull final FullConfigurationService configurationService,
                                            @NotNull final PluginOutPutAsyncer asyncer,
                                            @NotNull final HiveMQExtensions hiveMQExtensions,
                                            @NotNull final PluginTaskExecutorService pluginTaskExecutorService,
                                            @NotNull final Interceptors interceptors,
                                            @NotNull final ServerInformation serverInformation,
                                            @NotNull final EventLog eventLog) {
        this.configurationService = configurationService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.interceptors = interceptors;
        this.serverInformation = serverInformation;
        this.eventLog = eventLog;
    }

    @Override
    public void write(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg, final @NotNull ChannelPromise promise) throws Exception {

        if (!(msg instanceof PUBACK)) {
            super.write(ctx, msg, promise);
            return;
        }

        final PUBACK puback = (PUBACK) msg;

        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null || clientContext.getPubackOutboundInterceptors().isEmpty()) {
            super.write(ctx, msg, promise);
            return;
        }
        final List<PubackOutboundInterceptor> pubackOutboundInterceptors = clientContext.getPubackOutboundInterceptors();

        final PubackOutboundOutputImpl output = new PubackOutboundOutputImpl(configurationService, asyncer, puback);
        final PubackOutboundInputImpl
                input = new PubackOutboundInputImpl(new PubackPacketImpl(puback), clientId, channel);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final PubackInterceptorContext interceptorContext = new PubackInterceptorContext(PubackInterceptorTask.class,
                clientId, input, interceptorFuture, pubackOutboundInterceptors.size());


        for (final PubackOutboundInterceptor interceptor : pubackOutboundInterceptors) {

            if (interceptorFuture.isDone()) {
                // The future is set in case an async interceptor timeout failed
                break;
            }

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader((IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            //disabled extension would be null
            if (plugin == null) {
                interceptorContext.increment(output.pubackPrevented());
                continue;
            }
            final PubackInterceptorTask interceptorTask = new PubackInterceptorTask(interceptor, interceptorFuture, plugin.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(interceptorContext, input, output, interceptorTask);
        }

        final InterceptorFutureCallback callback = new InterceptorFutureCallback(output, puback, ctx, promise, eventLog);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());

    }

    private static class InterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull PubackOutboundOutputImpl output;
        private final @NotNull PUBACK puback;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        private final @NotNull EventLog eventLog;

        InterceptorFutureCallback(final @NotNull PubackOutboundOutputImpl output,
                                  final @NotNull PUBACK puback,
                                  final @NotNull ChannelHandlerContext ctx,
                                  final @NotNull ChannelPromise promise,
                                  final @NotNull EventLog eventLog) {
            this.output = output;
            this.puback = puback;
            this.ctx = ctx;
            this.promise = promise;
            this.eventLog = eventLog;
        }

        @Override
        public void onSuccess(final @Nullable Void result) {
            try {
                if (output.pubackPrevented()) {
                    eventLog.clientWasDisconnected(ctx.channel(), "Connection prevented by extension in PUBACK outbound interceptor");
                    ctx.channel().close();
                    return;
                }
                final PUBACK finalPuback = PUBACK.mergePubackPacket(output.getPubackPacket(), puback);
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
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;


        PubackInterceptorContext(final @NotNull Class<?> taskClazz,
                                  final @NotNull String clientId,
                                  final @NotNull PubackOutboundInputImpl input,
                                  final @NotNull SettableFuture<Void> interceptorFuture,
                                  final int interceptorCount) {
            super(taskClazz, clientId);
            this.input = input;
            this.interceptorFuture = interceptorFuture;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(@NotNull final PubackOutboundOutputImpl pluginOutput) {

            if (pluginOutput.isAsync() && pluginOutput.isTimedOut() && pluginOutput.getTimeoutFallback() == TimeoutFallback.FAILURE) {
                pluginOutput.forciblyDisconnect();
                increment(pluginOutput.pubackPrevented());
                return;
            }

            if (pluginOutput.getPubackPacket().isModified()) {
                input.updatePuback(pluginOutput.getPubackPacket());
            }
            increment(pluginOutput.pubackPrevented());
        }

        public void increment(final boolean pubackPrevented) {
            //we must set the future when no more interceptors are registered
            if (counter.incrementAndGet() == interceptorCount || pubackPrevented) {
                interceptorFuture.set(null);
            }
        }
    }

    private static class PubackInterceptorTask implements
            PluginInOutTask<PubackOutboundInputImpl, PubackOutboundOutputImpl> {

        private final @NotNull PubackOutboundInterceptor interceptor;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final @NotNull String pluginId;

        private PubackInterceptorTask(final @NotNull PubackOutboundInterceptor interceptor,
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
                output.forciblyDisconnect();
                Exceptions.rethrowError(e);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }
}
