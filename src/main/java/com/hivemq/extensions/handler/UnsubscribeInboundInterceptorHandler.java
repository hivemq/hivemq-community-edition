/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.extensions.handler;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
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
import com.hivemq.extensions.packets.unsubscribe.ModifiableUnsubscribePacketImpl;
import com.hivemq.mqtt.message.reason.Mqtt5UnsubAckReasonCode;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ReasonStrings;
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
    private final @NotNull PluginTaskExecutorService executorService;

    @Inject
    public UnsubscribeInboundInterceptorHandler(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull PluginTaskExecutorService executorService) {

        this.configurationService = configurationService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.executorService = executorService;
    }

    @Override
    public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) {
        if (!(msg instanceof UNSUBSCRIBE)) {
            ctx.fireChannelRead(msg);
            return;
        }
        handleInboundUnsubscribe(ctx, (UNSUBSCRIBE) msg);
    }

    private void handleInboundUnsubscribe(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull UNSUBSCRIBE unsubscribe) {

        final Channel channel = ctx.channel();

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.fireChannelRead(unsubscribe);
            return;
        }

        final List<UnsubscribeInboundInterceptor> interceptors = clientContext.getUnsubscribeInboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.fireChannelRead(unsubscribe);
            return;
        }

        final UnsubscribeInboundInputImpl input = new UnsubscribeInboundInputImpl(clientId, channel, unsubscribe);
        final UnsubscribeInboundOutputImpl output =
                new UnsubscribeInboundOutputImpl(asyncer, configurationService, unsubscribe);

        final UnsubscribeInboundInterceptorContext interceptorContext =
                new UnsubscribeInboundInterceptorContext(clientId, input, ctx, interceptors.size());

        for (final UnsubscribeInboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());
            if (extension == null) {
                interceptorContext.increment(output);
                continue;
            }

            final UnsubscribeInboundInterceptorTask interceptorTask =
                    new UnsubscribeInboundInterceptorTask(interceptor, extension.getId());
            executorService.handlePluginInOutTaskExecution(interceptorContext, input, output, interceptorTask);
        }
    }

    private static class UnsubscribeInboundInterceptorContext
            extends PluginInOutTaskContext<UnsubscribeInboundOutputImpl> {

        private final @NotNull UnsubscribeInboundInputImpl input;
        private final @NotNull ChannelHandlerContext ctx;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        UnsubscribeInboundInterceptorContext(
                final @NotNull String identifier,
                final @NotNull UnsubscribeInboundInputImpl input,
                final @NotNull ChannelHandlerContext ctx,
                final int interceptorCount) {

            super(identifier);
            this.input = input;
            this.ctx = ctx;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(final @NotNull UnsubscribeInboundOutputImpl output) {
            if (output.isTimedOut()) {
                log.debug("Async timeout on inbound UNSUBSCRIBE interception.");
                output.update(input.getUnsubscribePacket());
                if (output.getTimeoutFallback() == TimeoutFallback.FAILURE) {
                    output.preventDelivery();
                }
            } else if (output.getUnsubscribePacket().isModified()) {
                input.update(output.getUnsubscribePacket());
            }
            increment(output);
        }

        public void increment(final @NotNull UnsubscribeInboundOutputImpl output) {
            if (counter.incrementAndGet() == interceptorCount) {
                if (output.isPreventDelivery()) {
                    prevent(output.getUnsubscribePacket());
                } else {
                    final UNSUBSCRIBE unsubscribe = UNSUBSCRIBE.createUnsubscribeFrom(output.getUnsubscribePacket());
                    ctx.fireChannelRead(unsubscribe);
                }
            }
        }

        private void prevent(final @NotNull ModifiableUnsubscribePacketImpl unsubscribePacket) {
            final ImmutableList.Builder<Mqtt5UnsubAckReasonCode> reasonCodes = ImmutableList.builder();
            for (int i = 0; i < unsubscribePacket.getTopicFilters().size(); i++) {
                reasonCodes.add(Mqtt5UnsubAckReasonCode.UNSPECIFIED_ERROR);
            }
            final UNSUBACK unsuback = new UNSUBACK(unsubscribePacket.getPacketIdentifier(), reasonCodes.build(),
                    ReasonStrings.UNSUBACK_EXTENSION_PREVENTED);
            ctx.writeAndFlush(unsuback);
        }
    }

    private static class UnsubscribeInboundInterceptorTask
            implements PluginInOutTask<UnsubscribeInboundInputImpl, UnsubscribeInboundOutputImpl> {

        private final @NotNull UnsubscribeInboundInterceptor interceptor;
        private final @NotNull String extensionId;

        UnsubscribeInboundInterceptorTask(
                final @NotNull UnsubscribeInboundInterceptor interceptor,
                final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull UnsubscribeInboundOutputImpl apply(
                final @NotNull UnsubscribeInboundInputImpl input,
                final @NotNull UnsubscribeInboundOutputImpl output) {

            if (output.isPreventDelivery()) {
                return output;
            }
            try {
                interceptor.onInboundUnsubscribe(input, output);
            } catch (final Throwable e) {
                log.debug(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound unsubscribe interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original Exception:" + e);
                output.update(input.getUnsubscribePacket());
                output.preventDelivery();
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }
}
