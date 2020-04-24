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

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.publish.parameter.PublishOutboundInputImpl;
import com.hivemq.extensions.interceptor.publish.parameter.PublishOutboundOutputImpl;
import com.hivemq.extensions.packets.publish.ModifiableOutboundPublishImpl;
import com.hivemq.extensions.packets.publish.PublishPacketImpl;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
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
 * @author Lukas Brandl
 * @author Silvio Giebl
 */
@Singleton
@ChannelHandler.Sharable
public class PublishOutboundInterceptorHandler extends ChannelOutboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(PublishOutboundInterceptorHandler.class);

    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull FullConfigurationService configurationService;
    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull MessageDroppedService messageDroppedService;

    @Inject
    public PublishOutboundInterceptorHandler(
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull MessageDroppedService messageDroppedService) {

        this.asyncer = asyncer;
        this.configurationService = configurationService;
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.hiveMQExtensions = hiveMQExtensions;
        this.messageDroppedService = messageDroppedService;
    }

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull Object msg,
            final @NotNull ChannelPromise promise) {

        if (!(msg instanceof PUBLISH)) {
            ctx.write(msg, promise);
            return;
        }
        handlePublish(ctx, (PUBLISH) msg, promise);
    }

    // Returns true if the publish is handled by the outbound interceptor handling
    private void handlePublish(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PUBLISH publish,
            final @NotNull ChannelPromise promise) {

        final Channel channel = ctx.channel();
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.write(publish, promise);
            return;
        }
        final List<PublishOutboundInterceptor> interceptors = clientContext.getPublishOutboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.write(publish, promise);
            return;
        }

        final ClientInformation clientInfo = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = PluginInformationUtil.getAndSetConnectionInformation(channel);

        final PublishPacketImpl packet = new PublishPacketImpl(publish);
        final PublishOutboundInputImpl input = new PublishOutboundInputImpl(clientInfo, connectionInfo, packet);
        final ExtensionParameterHolder<PublishOutboundInputImpl> inputHolder = new ExtensionParameterHolder<>(input);

        final ModifiableOutboundPublishImpl modifiablePacket =
                new ModifiableOutboundPublishImpl(packet, configurationService);
        final PublishOutboundOutputImpl output = new PublishOutboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<PublishOutboundOutputImpl> outputHolder = new ExtensionParameterHolder<>(output);

        final PublishOutboundInterceptorContext context = new PublishOutboundInterceptorContext(
                clientId, interceptors.size(), ctx, promise, publish, inputHolder, outputHolder);

        for (final PublishOutboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());
            if (extension == null) { // disabled extension would be null
                context.finishInterceptor();
                continue;
            }

            final PublishOutboundInterceptorTask task =
                    new PublishOutboundInterceptorTask(interceptor, extension.getId());
            pluginTaskExecutorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
    }

    private class PublishOutboundInterceptorContext extends PluginInOutTaskContext<PublishOutboundOutputImpl>
            implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ChannelPromise promise;
        private final @NotNull PUBLISH publish;
        private final @NotNull ExtensionParameterHolder<PublishOutboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<PublishOutboundOutputImpl> outputHolder;

        PublishOutboundInterceptorContext(
                final @NotNull String identifier,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ChannelPromise promise,
                final @NotNull PUBLISH publish,
                final @NotNull ExtensionParameterHolder<PublishOutboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<PublishOutboundOutputImpl> outputHolder) {

            super(identifier);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
            this.promise = promise;
            this.publish = publish;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull PublishOutboundOutputImpl output) {
            if (output.isPreventDelivery()) {
                finishInterceptor();
            } else if (output.isTimedOut() && (output.getTimeoutFallback() == TimeoutFallback.FAILURE)) {
                output.forciblyPreventPublishDelivery();
                finishInterceptor();
            } else {
                if (output.getPublishPacket().isModified()) {
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
            if (outputHolder.get().isPreventDelivery()) {
                messageDroppedService.extensionPrevented(
                        getIdentifier(), publish.getTopic(), publish.getQoS().getQosNumber());
                promise.setSuccess();
                if (publish instanceof PublishWithFuture) {
                    ((PublishWithFuture) publish).getFuture().set(PublishStatus.DELIVERED);
                }
            } else {
                final PUBLISH mergedPublish = PUBLISHFactory.merge(inputHolder.get().getPublishPacket(), publish);
                ctx.writeAndFlush(mergedPublish, promise);
            }
        }
    }

    private static class PublishOutboundInterceptorTask
            implements PluginInOutTask<PublishOutboundInputImpl, PublishOutboundOutputImpl> {

        private final @NotNull PublishOutboundInterceptor interceptor;
        private final @NotNull String extensionId;

        private PublishOutboundInterceptorTask(
                final @NotNull PublishOutboundInterceptor interceptor, final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PublishOutboundOutputImpl apply(
                final @NotNull PublishOutboundInputImpl input, final @NotNull PublishOutboundOutputImpl output) {

            if (output.isPreventDelivery()) {
                // it's already prevented so no further interceptors must be called.
                return output;
            }
            try {
                interceptor.onOutboundPublish(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on outbound PUBLISH interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original exception:", e);
                output.forciblyPreventPublishDelivery();
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
