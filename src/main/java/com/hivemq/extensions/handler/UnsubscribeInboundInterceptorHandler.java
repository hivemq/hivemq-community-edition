/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.UnsubscribeInboundInterceptor;
import com.hivemq.extensions.ExtensionInformationUtil;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.unsubscribe.parameter.UnsubscribeInboundInputImpl;
import com.hivemq.extensions.interceptor.unsubscribe.parameter.UnsubscribeInboundOutputImpl;
import com.hivemq.extensions.packets.unsubscribe.ModifiableUnsubscribePacketImpl;
import com.hivemq.extensions.packets.unsubscribe.UnsubscribePacketImpl;
import com.hivemq.mqtt.message.reason.Mqtt5UnsubAckReasonCode;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
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
 * @author Silvio Giebl
 */
@Singleton
public class UnsubscribeInboundInterceptorHandler {

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


    public void handleInboundUnsubscribe(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull UNSUBSCRIBE unsubscribe) {

        final Channel channel = ctx.channel();
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.EXTENSION_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.fireChannelRead(unsubscribe);
            return;
        }
        final List<UnsubscribeInboundInterceptor> interceptors = clientContext.getUnsubscribeInboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.fireChannelRead(unsubscribe);
            return;
        }

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);

        final UnsubscribePacketImpl packet = new UnsubscribePacketImpl(unsubscribe);
        final UnsubscribeInboundInputImpl input = new UnsubscribeInboundInputImpl(clientInfo, connectionInfo, packet);
        final ExtensionParameterHolder<UnsubscribeInboundInputImpl> inputHolder = new ExtensionParameterHolder<>(input);

        final ModifiableUnsubscribePacketImpl modifiablePacket =
                new ModifiableUnsubscribePacketImpl(packet, configurationService);
        final UnsubscribeInboundOutputImpl output = new UnsubscribeInboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<UnsubscribeInboundOutputImpl> outputHolder =
                new ExtensionParameterHolder<>(output);

        final UnsubscribeInboundInterceptorContext context =
                new UnsubscribeInboundInterceptorContext(clientId, interceptors.size(), ctx, inputHolder, outputHolder);

        for (final UnsubscribeInboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(interceptor.getClass().getClassLoader());
            if (extension == null) {
                context.finishInterceptor();
                continue;
            }

            final UnsubscribeInboundInterceptorTask task =
                    new UnsubscribeInboundInterceptorTask(interceptor, extension.getId());
            executorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
    }

    private static class UnsubscribeInboundInterceptorContext
            extends PluginInOutTaskContext<UnsubscribeInboundOutputImpl> implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ExtensionParameterHolder<UnsubscribeInboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<UnsubscribeInboundOutputImpl> outputHolder;

        UnsubscribeInboundInterceptorContext(
                final @NotNull String identifier,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ExtensionParameterHolder<UnsubscribeInboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<UnsubscribeInboundOutputImpl> outputHolder) {

            super(identifier);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull UnsubscribeInboundOutputImpl output) {
            if (output.isPreventDelivery()) {
                finishInterceptor();
            } else if (output.isTimedOut() && (output.getTimeoutFallback() == TimeoutFallback.FAILURE)) {
                output.preventDelivery();
                finishInterceptor();
            } else {
                if (output.getUnsubscribePacket().isModified()) {
                    inputHolder.set(inputHolder.get().update(output));
                }
                if (!finishInterceptor()) {
                    outputHolder.set(output.update(inputHolder.get()));
                }
            }
        }

        public boolean finishInterceptor() {
            if (counter.incrementAndGet() == interceptorCount) {
                ctx.executor().execute(this);
                return true;
            }
            return false;
        }

        @Override
        public void run() {
            final UnsubscribeInboundOutputImpl output = outputHolder.get();
            if (output.isPreventDelivery()) {
                prevent(output);
            } else {
                final UNSUBSCRIBE unsubscribe = UNSUBSCRIBE.from(inputHolder.get().getUnsubscribePacket());
                ctx.fireChannelRead(unsubscribe);
            }
        }

        private void prevent(final @NotNull UnsubscribeInboundOutputImpl output) {
            final int size = output.getUnsubscribePacket().getTopicFilters().size();
            final ImmutableList.Builder<Mqtt5UnsubAckReasonCode> reasonCodesBuilder = ImmutableList.builder();
            for (int i = 0; i < size; i++) {
                reasonCodesBuilder.add(Mqtt5UnsubAckReasonCode.UNSPECIFIED_ERROR);
            }
            ctx.channel().writeAndFlush(new UNSUBACK(
                    output.getUnsubscribePacket().getPacketIdentifier(),
                    reasonCodesBuilder.build(),
                    ReasonStrings.UNSUBACK_EXTENSION_PREVENTED));
        }
    }

    private static class UnsubscribeInboundInterceptorTask
            implements PluginInOutTask<UnsubscribeInboundInputImpl, UnsubscribeInboundOutputImpl> {

        private final @NotNull UnsubscribeInboundInterceptor interceptor;
        private final @NotNull String extensionId;

        UnsubscribeInboundInterceptorTask(
                final @NotNull UnsubscribeInboundInterceptor interceptor, final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull UnsubscribeInboundOutputImpl apply(
                final @NotNull UnsubscribeInboundInputImpl input, final @NotNull UnsubscribeInboundOutputImpl output) {

            if (output.isPreventDelivery()) {
                return output;
            }
            try {
                interceptor.onInboundUnsubscribe(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound UNSUBSCRIBE interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original Exception:" + e);
                output.preventDelivery();
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
