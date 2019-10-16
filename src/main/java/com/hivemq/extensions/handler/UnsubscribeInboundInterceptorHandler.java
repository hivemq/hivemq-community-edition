package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.UnsubscribeInboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.unsubscribe.parameter.UnsubscribeInboundInputImpl;
import com.hivemq.extensions.interceptor.unsubscribe.parameter.UnsubscribeInboundOutputImpl;
import com.hivemq.extensions.packets.unsubscribe.UnsubscribePacketImpl;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
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
 * @author Robin Atherton
 */
@Singleton
@ChannelHandler.Sharable
public class UnsubscribeInboundInterceptorHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(UnsubscribeInboundInterceptorHandler.class);

    private final @NotNull FullConfigurationService configurationService;

    private final @NotNull PluginOutPutAsyncer asyncer;

    private final @NotNull HiveMQExtensions hiveMQExtensions;

    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;

    @Inject
    public UnsubscribeInboundInterceptorHandler(
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
    public void channelRead(final @NotNull ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (!(msg instanceof UNSUBSCRIBE)) {
            super.channelRead(ctx, msg);
            return;
        }

        final UNSUBSCRIBE unsubscribe = (UNSUBSCRIBE) msg;

        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null || clientContext.getUnsubscribeInboundInterceptors().isEmpty()) {
            super.channelRead(ctx, msg);
            return;
        }

        final List<UnsubscribeInboundInterceptor> unsubscribeInboundInterceptors =
                clientContext.getUnsubscribeInboundInterceptors();

        final UnsubscribeInboundInputImpl input =
                new UnsubscribeInboundInputImpl(new UnsubscribePacketImpl(unsubscribe), clientId, channel);

        final UnsubscribeInboundOutputImpl output =
                new UnsubscribeInboundOutputImpl(asyncer, configurationService, unsubscribe);

        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final UnsubscribeInboundInterceptorContext interceptorContext =
                new UnsubscribeInboundInterceptorContext(
                        UnsubscribeInboundInterceptorTask.class, clientId, input, interceptorFuture,
                        unsubscribeInboundInterceptors.size());

        for (final UnsubscribeInboundInterceptor interceptor : unsubscribeInboundInterceptors) {
            if (interceptorFuture.isDone()) {
                break;
            }

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());
            if (extension == null) {
                interceptorContext.increment();
                continue;
            }

            final UnsubscribeInboundInterceptorTask interceptorTask =
                    new UnsubscribeInboundInterceptorTask(interceptor, interceptorFuture, extension.getId());
            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }

        final UnsubscribeInboundInterceptorFutureCallback callback =
                new UnsubscribeInboundInterceptorFutureCallback(ctx, output, unsubscribe);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());

    }

    private static class UnsubscribeInboundInterceptorContext
            extends PluginInOutTaskContext<UnsubscribeInboundOutputImpl> {

        private final @NotNull UnsubscribeInboundInputImpl input;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        UnsubscribeInboundInterceptorContext(
                @NotNull final Class<?> taskClazz,
                @NotNull final String identifier,
                @NotNull final UnsubscribeInboundInputImpl input,
                @NotNull final SettableFuture<Void> interceptorFuture, final int interceptorCount) {
            super(taskClazz, identifier);
            this.input = input;
            this.interceptorFuture = interceptorFuture;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(
                final @NotNull UnsubscribeInboundOutputImpl pluginOutput) {

            if (pluginOutput.isTimedOut()) {
                log.debug("Async timeout on inbound UNSUBSCRIBE interception.");
                final UNSUBSCRIBE unmodifiedUnsubscribe =
                        UNSUBSCRIBE.createUnsubscribeFrom(input.getUnsubscribePacket());
                pluginOutput.update(unmodifiedUnsubscribe);
            } else if (pluginOutput.getUnsubscribePacket().isModified()) {
                input.updateUnsubscribe(pluginOutput.getUnsubscribePacket());
                final UNSUBSCRIBE updatedUnsubscribe =
                        UNSUBSCRIBE.createUnsubscribeFrom(pluginOutput.getUnsubscribePacket());
                pluginOutput.update(updatedUnsubscribe);
            }
            increment();
        }

        public void increment() {
            if (counter.incrementAndGet() == interceptorCount) {
                interceptorFuture.set(null);
            }
        }
    }

    private static class UnsubscribeInboundInterceptorTask
            implements PluginInOutTask<UnsubscribeInboundInputImpl, UnsubscribeInboundOutputImpl> {

        private final @NotNull UnsubscribeInboundInterceptor interceptor;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final @NotNull String extensionId;

        UnsubscribeInboundInterceptorTask(
                @NotNull final UnsubscribeInboundInterceptor interceptor,
                @NotNull final SettableFuture<Void> interceptorFuture,
                @NotNull final String extensionId) {
            this.interceptor = interceptor;
            this.interceptorFuture = interceptorFuture;
            this.extensionId = extensionId;
        }

        @Override
        public UnsubscribeInboundOutputImpl apply(
                final @NotNull UnsubscribeInboundInputImpl input,
                final @NotNull UnsubscribeInboundOutputImpl output) {
            try {
                if (!interceptorFuture.isDone()) {
                    interceptor.onInboundUnsubscribe(input, output);
                }
            } catch (final Throwable e) {
                log.debug(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound unsubscribe request interception." +
                                "Extensions are responsible for their own exception handling.", extensionId);
                final UNSUBSCRIBE unsubscribe = UNSUBSCRIBE.createUnsubscribeFrom(input.getUnsubscribePacket());
                output.update(unsubscribe);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

    private static class UnsubscribeInboundInterceptorFutureCallback implements FutureCallback<Void> {

        private final UnsubscribeInboundOutputImpl output;
        private final UNSUBSCRIBE unsubscribe;
        private final @NotNull ChannelHandlerContext ctx;

        UnsubscribeInboundInterceptorFutureCallback(
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull UnsubscribeInboundOutputImpl output,
                final @NotNull UNSUBSCRIBE unsubscribe) {
            this.output = output;
            this.unsubscribe = unsubscribe;
            this.ctx = ctx;
        }

        @Override
        public void onSuccess(@Nullable final Void result) {
            try {
                final UNSUBSCRIBE finalUnsubscribe = UNSUBSCRIBE.createUnsubscribeFrom(output.getUnsubscribePacket());
                ctx.fireChannelRead(finalUnsubscribe);
            } catch (final Exception e) {
                log.error("Exception while modifying an intercepted UNSUBSCRIBE message.", e);
                ctx.fireChannelRead(unsubscribe);
            }
        }

        @Override
        public void onFailure(final Throwable t) {
            ctx.channel().close();
        }
    }
}
