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

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extensions.ExtensionInformationUtil;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.subscribe.parameter.SubscribeInboundInputImpl;
import com.hivemq.extensions.interceptor.subscribe.parameter.SubscribeInboundOutputImpl;
import com.hivemq.extensions.packets.subscribe.ModifiableSubscribePacketImpl;
import com.hivemq.extensions.packets.subscribe.SubscribePacketImpl;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 * @since 4.1.0
 */
@Singleton
public class IncomingSubscribeHandler {

    private static final Logger log = LoggerFactory.getLogger(IncomingSubscribeHandler.class);

    private final @NotNull PluginTaskExecutorService executorService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull PluginAuthorizerService authorizerService;
    private final @NotNull FullConfigurationService configurationService;

    @Inject
    public IncomingSubscribeHandler(
            final @NotNull PluginTaskExecutorService executorService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull PluginAuthorizerService authorizerService,
            final @NotNull FullConfigurationService configurationService) {

        this.executorService = executorService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.authorizerService = authorizerService;
        this.configurationService = configurationService;
    }

    /**
     * intercepts the subscribe message when the channel is active, the client id is set and interceptors are available,
     * otherwise delegates to authorizer
     *
     * @param ctx       the context of the channel handler
     * @param subscribe the subscribe to process
     */
    public void interceptOrDelegate(final @NotNull ChannelHandlerContext ctx, final @NotNull SUBSCRIBE subscribe) {
        final Channel channel = ctx.channel();
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.EXTENSION_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            authorizerService.authorizeSubscriptions(ctx, subscribe);
            return;
        }
        final List<SubscribeInboundInterceptor> interceptors = clientContext.getSubscribeInboundInterceptors();
        if (interceptors.isEmpty()) {
            authorizerService.authorizeSubscriptions(ctx, subscribe);
            return;
        }

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);

        final SubscribePacketImpl packet = new SubscribePacketImpl(subscribe);
        final SubscribeInboundInputImpl input = new SubscribeInboundInputImpl(clientInfo, connectionInfo, packet);
        final ExtensionParameterHolder<SubscribeInboundInputImpl> inputHolder = new ExtensionParameterHolder<>(input);

        final ModifiableSubscribePacketImpl modifiablePacket =
                new ModifiableSubscribePacketImpl(packet, configurationService);
        final SubscribeInboundOutputImpl output = new SubscribeInboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<SubscribeInboundOutputImpl> outputHolder =
                new ExtensionParameterHolder<>(output);

        final SubscribeInboundInterceptorContext context =
                new SubscribeInboundInterceptorContext(clientId, interceptors.size(), ctx, inputHolder, outputHolder);

        for (final SubscribeInboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(interceptor.getClass().getClassLoader());
            if (extension == null) { // disabled extension would be null
                context.finishInterceptor();
                continue;
            }

            final SubscribeInboundInterceptorTask task =
                    new SubscribeInboundInterceptorTask(interceptor, extension.getId());
            executorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
    }

    private class SubscribeInboundInterceptorContext extends PluginInOutTaskContext<SubscribeInboundOutputImpl>
            implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ExtensionParameterHolder<SubscribeInboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<SubscribeInboundOutputImpl> outputHolder;

        SubscribeInboundInterceptorContext(
                final @NotNull String clientId,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ExtensionParameterHolder<SubscribeInboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<SubscribeInboundOutputImpl> outputHolder) {

            super(clientId);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull SubscribeInboundOutputImpl output) {
            if (output.isPreventDelivery()) {
                finishInterceptor();
            } else if (output.isTimedOut() && (output.getTimeoutFallback() == TimeoutFallback.FAILURE)) {
                output.forciblyPreventSubscribeDelivery();
                finishInterceptor();
            } else {
                if (output.getSubscribePacket().isModified()) {
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
            final SubscribeInboundOutputImpl output = outputHolder.get();
            if (output.isPreventDelivery()) {
                prevent(output);
            } else {
                final SUBSCRIBE finalSubscribe = SUBSCRIBE.from(inputHolder.get().getSubscribePacket());
                authorizerService.authorizeSubscriptions(ctx, finalSubscribe);
            }
        }

        private void prevent(final @NotNull SubscribeInboundOutputImpl output) {
            final int size = output.getSubscribePacket().getSubscriptions().size();
            final List<Mqtt5SubAckReasonCode> reasonCodesBuilder = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                reasonCodesBuilder.add(Mqtt5SubAckReasonCode.UNSPECIFIED_ERROR);
            }
            // no need to check mqtt version since the mqtt 3 encoder will just not encode reason string and properties.
            ctx.writeAndFlush(new SUBACK(
                    output.getSubscribePacket().getPacketId(),
                    reasonCodesBuilder,
                    ReasonStrings.SUBACK_EXTENSION_PREVENTED));
        }
    }

    private static class SubscribeInboundInterceptorTask
            implements PluginInOutTask<SubscribeInboundInputImpl, SubscribeInboundOutputImpl> {

        private final @NotNull SubscribeInboundInterceptor interceptor;
        private final @NotNull String extensionId;

        private SubscribeInboundInterceptorTask(
                final @NotNull SubscribeInboundInterceptor interceptor, final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull SubscribeInboundOutputImpl apply(
                final @NotNull SubscribeInboundInputImpl input, final @NotNull SubscribeInboundOutputImpl output) {

            if (output.isPreventDelivery()) {
                // it's already prevented so no further interceptors must be called.
                return output;
            }
            try {
                interceptor.onInboundSubscribe(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound SUBSCRIBE interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original exception:", e);
                output.forciblyPreventSubscribeDelivery();
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
