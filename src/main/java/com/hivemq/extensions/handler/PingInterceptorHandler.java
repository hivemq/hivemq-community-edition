package com.hivemq.extensions.handler;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.PingReqInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.PingRespOutboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.pingrequest.parameter.PingReqInboundInputImpl;
import com.hivemq.extensions.interceptor.pingrequest.parameter.PingReqInboundOutputImpl;
import com.hivemq.extensions.interceptor.pingresponse.parameter.PingRespOutboundInputImpl;
import com.hivemq.extensions.interceptor.pingresponse.parameter.PingRespOutboundOutputImpl;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.PINGRESP;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Robin Atherton
 */
@ChannelHandler.Sharable
@Singleton
public class PingInterceptorHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(PingInterceptorHandler.class);

    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;

    @Inject
    public PingInterceptorHandler(
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions) {
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
    }

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull Object msg,
            final @NotNull ChannelPromise promise)
            throws Exception {
        if (!(msg instanceof PINGRESP)) {
            ctx.write(msg, promise);
            return;
        }
        handleOutboundPingResponse(ctx, (PINGRESP) msg, promise);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (!(msg instanceof PINGREQ)) {
            super.channelRead(ctx, msg);
        } else {
            handleInboundPingRequest(ctx, ((PINGREQ) msg));
        }
    }

    private void handleInboundPingRequest(final @NotNull ChannelHandlerContext ctx, final PINGREQ pingreq)
            throws Exception {

        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null || clientContext.getPingRequestInboundInterceptors().isEmpty()) {
            super.channelRead(ctx, pingreq);
            return;
        }

        final List<PingReqInboundInterceptor> pingReqInboundInterceptors =
                clientContext.getPingRequestInboundInterceptors();
        final PingReqInboundOutputImpl output = new PingReqInboundOutputImpl(asyncer);
        final PingReqInboundInputImpl input = new PingReqInboundInputImpl(clientId, channel);
        final PingRequestInboundInterceptorContext interceptorContext =
                new PingRequestInboundInterceptorContext(
                        PingRequestInboundInterceptorTask.class,
                        clientId, input, ctx, pingReqInboundInterceptors.size());

        for (final PingReqInboundInterceptor interceptor : pingReqInboundInterceptors) {

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            if (plugin == null) {
                interceptorContext.increment();
                continue;
            }

            final PingRequestInboundInterceptorTask interceptorTask =
                    new PingRequestInboundInterceptorTask(interceptor, plugin.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }
    }

    private void handleOutboundPingResponse(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PINGRESP pingresp,
            final @NotNull ChannelPromise promise) {

        final Channel channel = ctx.channel();

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.writeAndFlush(pingresp);
            return;
        }

        final List<PingRespOutboundInterceptor> interceptors = clientContext.getPingResponseOutboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.write(pingresp);
            return;
        }

        final PingRespOutboundInputImpl input = new PingRespOutboundInputImpl(clientId, channel);

        final PingRespOutboundOutputImpl output = new PingRespOutboundOutputImpl(asyncer);

        final PingResponseOutboundInterceptorContext interceptorContext =
                new PingResponseOutboundInterceptorContext(
                        PingResponseOutboundInterceptorTask.class, clientId, input, ctx, promise, interceptors.size());

        for (final PingRespOutboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            if (extension == null) {
                interceptorContext.increment();
                continue;
            }

            final PingResponseOutboundInterceptorTask interceptorTask =
                    new PingResponseOutboundInterceptorTask(interceptor, extension.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, input, output, interceptorTask);
        }
    }

    private static class PingRequestInboundInterceptorTask implements
            PluginInOutTask<PingReqInboundInputImpl, PingReqInboundOutputImpl> {

        private final @NotNull PingReqInboundInterceptor interceptor;
        private final @NotNull String pluginId;

        PingRequestInboundInterceptorTask(
                final @NotNull PingReqInboundInterceptor interceptor,
                final @NotNull String pluginId) {
            this.interceptor = interceptor;
            this.pluginId = pluginId;
        }

        @Override
        public PingReqInboundOutputImpl apply(
                final @NotNull PingReqInboundInputImpl pingRequestInboundInput,
                final @NotNull PingReqInboundOutputImpl pingRequestInboundOutput) {
            try {
                interceptor.onInboundPingReq(pingRequestInboundInput, pingRequestInboundOutput);
            } catch (final Throwable e) {
                log.debug(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound ping request interception. Extensions are responsible for their own exception handling.",
                        pluginId);
                log.debug("Original Exception: ", e);
            }
            return pingRequestInboundOutput;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }

    }

    private static class PingResponseOutboundInterceptorTask implements
            PluginInOutTask<PingRespOutboundInputImpl, PingRespOutboundOutputImpl> {

        private final @NotNull PingRespOutboundInterceptor interceptor;

        private final @NotNull String pluginId;

        public PingResponseOutboundInterceptorTask(
                final @NotNull PingRespOutboundInterceptor interceptor,
                final @NotNull String pluginId) {
            this.interceptor = interceptor;
            this.pluginId = pluginId;
        }

        @Override
        public PingRespOutboundOutputImpl apply(
                final @NotNull PingRespOutboundInputImpl pingResponseOutboundInput,
                final @NotNull PingRespOutboundOutputImpl pingResponseOutboundOutput) {
            try {
                interceptor.onOutboundPingResp(pingResponseOutboundInput, pingResponseOutboundOutput);
            } catch (final Throwable e) {
                log.debug(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound ping response interception. Extensions are responsible for their own exception handling.",
                        pluginId);
                log.debug("Original Exception: ", e);
            }
            return pingResponseOutboundOutput;
        }

        @Override
        public @com.hivemq.annotations.NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }

    }

    private static class PingRequestInboundInterceptorContext extends
            PluginInOutTaskContext<PingReqInboundOutputImpl> {

        private final @com.hivemq.annotations.NotNull ChannelHandlerContext ctx;
        private final int interceptorCount;
        private final @com.hivemq.annotations.NotNull AtomicInteger counter;

        protected PingRequestInboundInterceptorContext(
                final @com.hivemq.annotations.NotNull Class<?> taskClazz,
                final @com.hivemq.annotations.NotNull String identifier,
                final @com.hivemq.annotations.NotNull PingReqInboundInputImpl input,
                final @com.hivemq.annotations.NotNull ChannelHandlerContext ctx,
                final int interceptorCount) {
            super(taskClazz, identifier);
            this.ctx = ctx;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(final @com.hivemq.annotations.NotNull PingReqInboundOutputImpl pluginOutput) {
            increment();
        }

        public void increment() {
            if (counter.incrementAndGet() == interceptorCount) {
                ctx.fireChannelRead(new PINGREQ());
            }
        }

    }

    private static class PingResponseOutboundInterceptorContext
            extends PluginInOutTaskContext<PingRespOutboundOutputImpl> {

        private final @com.hivemq.annotations.NotNull ChannelHandlerContext ctx;
        private final @com.hivemq.annotations.NotNull ChannelPromise promise;
        private final int interceptorCount;
        private final @com.hivemq.annotations.NotNull AtomicInteger counter;

        private PingResponseOutboundInterceptorContext(
                final @com.hivemq.annotations.NotNull Class<?> taskClazz,
                final @com.hivemq.annotations.NotNull String identifier,
                final @com.hivemq.annotations.NotNull PingRespOutboundInputImpl input,
                final @com.hivemq.annotations.NotNull ChannelHandlerContext ctx,
                final @com.hivemq.annotations.NotNull ChannelPromise promise,
                final int interceptorCount) {

            super(taskClazz, identifier);
            this.ctx = ctx;
            this.promise = promise;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        public void pluginPost(final @NotNull PingRespOutboundOutputImpl pluginOutput) {
            increment();
        }

        public void increment() {
            if (counter.incrementAndGet() == interceptorCount) {
                ctx.writeAndFlush(new PINGRESP(), promise);
            }
        }
    }
}






