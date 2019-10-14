package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelOutboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.pubrel.parameter.PubrelInboundInputImpl;
import com.hivemq.extensions.interceptor.pubrel.parameter.PubrelInboundOutputImpl;
import com.hivemq.extensions.interceptor.pubrel.parameter.PubrelOutboundInputImpl;
import com.hivemq.extensions.interceptor.pubrel.parameter.PubrelOutboundOutputImpl;
import com.hivemq.extensions.packets.pubrel.PubrelPacketImpl;
import com.hivemq.mqtt.message.pubrel.PUBREL;
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
public class PubrelInterceptorHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(PubrelInterceptorHandler.class);

    @NotNull
    private final FullConfigurationService configurationService;

    @NotNull
    private final PluginOutPutAsyncer asyncer;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    @NotNull
    private final PluginTaskExecutorService pluginTaskExecutorService;

    @Inject
    public PubrelInterceptorHandler(
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
        if (!(msg instanceof PUBREL)) {
            super.channelRead(ctx, msg);
            return;
        }
        if (!handleInboundPubrel(ctx, (PUBREL) msg)) {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void write(
            @NotNull final ChannelHandlerContext ctx, @NotNull final Object msg, @NotNull final ChannelPromise promise)
            throws Exception {
        if (!(msg instanceof PUBREL)) {
            super.write(ctx, msg, promise);
            return;
        }
        if (!handleOutboundPubrel(ctx, (PUBREL) msg, promise)) {
            super.write(ctx, msg, promise);
        }

    }

    private boolean handleOutboundPubrel(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PUBREL pubrel,
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
        if (clientContext == null || clientContext.getPubrelOutboundInterceptors().isEmpty()) {
            return false;
        }
        final List<PubrelOutboundInterceptor> pubrelOutboundInterceptors =
                clientContext.getPubrelOutboundInterceptors();

        final PubrelOutboundOutputImpl output = new PubrelOutboundOutputImpl(configurationService, asyncer, pubrel);
        final PubrelOutboundInputImpl
                input = new PubrelOutboundInputImpl(new PubrelPacketImpl(pubrel), clientId, channel);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final PubrelOutboundInterceptorContext interceptorContext =
                new PubrelOutboundInterceptorContext(PubrelOutboundInterceptorTask.class,
                        clientId, input, output, interceptorFuture, pubrelOutboundInterceptors.size());


        for (final PubrelOutboundInterceptor interceptor : pubrelOutboundInterceptors) {

            if (interceptorFuture.isDone()) {
                break;
            }

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            //disabled extension would be null
            if (plugin == null) {
                interceptorContext.increment();
                continue;
            }
            final PubrelOutboundInterceptorTask interceptorTask =
                    new PubrelOutboundInterceptorTask(interceptor, interceptorFuture, plugin.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }

        final OutboundInterceptorFutureCallback callback =
                new OutboundInterceptorFutureCallback(output, pubrel, ctx, promise);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
        return true;
    }

    private boolean handleInboundPubrel(final ChannelHandlerContext ctx, final PUBREL pubrel) {
        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return false;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return false;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null || clientContext.getPubrelInboundInterceptors().isEmpty()) {
            return false;
        }
        final List<PubrelInboundInterceptor> pubrelInboundInterceptors = clientContext.getPubrelInboundInterceptors();

        final PubrelInboundOutputImpl output = new PubrelInboundOutputImpl(configurationService, asyncer, pubrel);
        final PubrelInboundInputImpl
                input = new PubrelInboundInputImpl(new PubrelPacketImpl(pubrel), clientId, channel);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final PubrelInboundInterceptorContext interceptorContext = new PubrelInboundInterceptorContext(
                PubrelInboundInterceptorTask.class, clientId, input, output, interceptorFuture,
                pubrelInboundInterceptors.size());

        for (final PubrelInboundInterceptor interceptor : pubrelInboundInterceptors) {

            if (interceptorFuture.isDone()) {
                break;
            }

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            //disabled extension would be null
            if (plugin == null) {
                interceptorContext.increment();
                continue;
            }
            final PubrelInboundInterceptorTask interceptorTask =
                    new PubrelInboundInterceptorTask(interceptor, interceptorFuture, plugin.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }

        final InboundInterceptorFutureCallback callback =
                new InboundInterceptorFutureCallback(output, pubrel, ctx);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
        return true;
    }

    private static class InboundInterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull PubrelInboundOutputImpl output;
        private final @NotNull PUBREL pubrel;
        private final @NotNull ChannelHandlerContext ctx;

        InboundInterceptorFutureCallback(
                final @NotNull PubrelInboundOutputImpl output,
                final @NotNull PUBREL pubrel,
                final @NotNull ChannelHandlerContext ctx) {
            this.output = output;
            this.pubrel = pubrel;
            this.ctx = ctx;
        }

        @Override
        public void onSuccess(final @Nullable Void result) {
            try {
                final PUBREL finalPubrel = PUBREL.createPubrelFrom(output.getPubrelPacket());
                ctx.fireChannelRead(finalPubrel);
            } catch (final Exception e) {
                log.error("Exception while modifying an intercepted PUBREL message.", e);
                ctx.fireChannelRead(pubrel);
            }
        }

        @Override
        public void onFailure(final @NotNull Throwable t) {
            //should never happen, since the settable future never sets an exception
            ctx.channel().close();
        }
    }

    private static class PubrelInboundInterceptorContext extends PluginInOutTaskContext<PubrelInboundOutputImpl> {

        private final @NotNull PubrelInboundInputImpl input;
        private final @NotNull PubrelInboundOutputImpl output;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PubrelInboundInterceptorContext(
                final @NotNull Class<?> taskClazz,
                final @NotNull String clientId,
                final @NotNull PubrelInboundInputImpl input,
                final @NotNull PubrelInboundOutputImpl output,
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
        public void pluginPost(@NotNull final PubrelInboundOutputImpl pluginOutput) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on inbound PUBREL interception.");
                final PUBREL unmodifiedPubrel = PUBREL.createPubrelFrom(input.getPubrelPacket());
                output.update(unmodifiedPubrel);
            } else if (pluginOutput.getPubrelPacket().isModified()) {
                input.updatePubrel(pluginOutput.getPubrelPacket());
                final PUBREL updatedPubrel = PUBREL.createPubrelFrom(pluginOutput.getPubrelPacket());
                output.update(updatedPubrel);
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

    private static class PubrelInboundInterceptorTask implements
            PluginInOutTask<PubrelInboundInputImpl, PubrelInboundOutputImpl> {

        private final @NotNull PubrelInboundInterceptor interceptor;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final @NotNull String pluginId;

        private PubrelInboundInterceptorTask(
                final @NotNull PubrelInboundInterceptor interceptor,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final @NotNull String pluginId) {
            this.interceptor = interceptor;
            this.interceptorFuture = interceptorFuture;
            this.pluginId = pluginId;
        }

        @Override
        public @NotNull PubrelInboundOutputImpl apply(
                final @NotNull PubrelInboundInputImpl input, final @NotNull PubrelInboundOutputImpl output) {
            try {
                if (!interceptorFuture.isDone()) {
                    interceptor.onInboundPubrel(input, output);
                }
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on pubrel interception. The exception should be handled by the extension.",
                        pluginId);
                log.debug("Original exception:", e);

                // this is needed since the PUBREL could be incompletely modified
                final PUBREL unmodifiedPubrel = PUBREL.createPubrelFrom(input.getPubrelPacket());
                output.update(unmodifiedPubrel);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

    private static class OutboundInterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull PubrelOutboundOutputImpl output;
        private final @NotNull PUBREL pubrel;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;

        OutboundInterceptorFutureCallback(
                final @NotNull PubrelOutboundOutputImpl output,
                final @NotNull PUBREL pubrel,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ChannelPromise promise) {
            this.output = output;
            this.pubrel = pubrel;
            this.ctx = ctx;
            this.promise = promise;
        }

        @Override
        public void onSuccess(final @Nullable Void result) {
            try {
                final PUBREL finalPubrel = PUBREL.createPubrelFrom(output.getPubrelPacket());
                ctx.writeAndFlush(finalPubrel, promise);
            } catch (final Exception e) {
                log.error("Exception while modifying an intercepted PUBREL message.", e);
                ctx.writeAndFlush(pubrel, promise);
            }
        }

        @Override
        public void onFailure(final @NotNull Throwable t) {
            //should never happen, since the settable future never sets an exception
            ctx.channel().close();
        }
    }

    private static class PubrelOutboundInterceptorContext extends PluginInOutTaskContext<PubrelOutboundOutputImpl> {

        private final @NotNull PubrelOutboundInputImpl input;
        private final @NotNull PubrelOutboundOutputImpl output;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PubrelOutboundInterceptorContext(
                final @NotNull Class<?> taskClazz,
                final @NotNull String clientId,
                final @NotNull PubrelOutboundInputImpl input,
                final @NotNull PubrelOutboundOutputImpl output,
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
        public void pluginPost(@NotNull final PubrelOutboundOutputImpl pluginOutput) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on outbound PUBREL interception.");
                final PUBREL unmodifiedPubrel = PUBREL.createPubrelFrom(input.getPubrelPacket());
                output.update(unmodifiedPubrel);
            } else if (pluginOutput.getPubrelPacket().isModified()) {
                input.updatePubrel(pluginOutput.getPubrelPacket());
                final PUBREL updatedPubrel = PUBREL.createPubrelFrom(output.getPubrelPacket());
                output.update(updatedPubrel);
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

    private static class PubrelOutboundInterceptorTask implements
            PluginInOutTask<PubrelOutboundInputImpl, PubrelOutboundOutputImpl> {

        private final @NotNull PubrelOutboundInterceptor interceptor;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final @NotNull String pluginId;

        private PubrelOutboundInterceptorTask(
                final @NotNull PubrelOutboundInterceptor interceptor,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final @NotNull String pluginId) {
            this.interceptor = interceptor;
            this.interceptorFuture = interceptorFuture;
            this.pluginId = pluginId;
        }

        @Override
        public @NotNull PubrelOutboundOutputImpl apply(
                final @NotNull PubrelOutboundInputImpl input, final @NotNull PubrelOutboundOutputImpl output) {
            try {
                if (!interceptorFuture.isDone()) {
                    interceptor.onOutboundPubrel(input, output);
                }
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on pubrel interception. The exception should be handled by the extension.",
                        pluginId);
                log.debug("Original exception:", e);
                final PUBREL unmodifiedPubrel = PUBREL.createPubrelFrom(input.getPubrelPacket());
                output.update(unmodifiedPubrel);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

}
