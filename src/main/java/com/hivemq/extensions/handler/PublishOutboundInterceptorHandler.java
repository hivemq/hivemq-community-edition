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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.publish.parameter.PublishOutboundInputImpl;
import com.hivemq.extensions.interceptor.publish.parameter.PublishOutboundOutputImpl;
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
 */
@ChannelHandler.Sharable
@Singleton
public class PublishOutboundInterceptorHandler extends ChannelOutboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(PublishOutboundInterceptorHandler.class);

    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull FullConfigurationService configurationService;
    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull MessageDroppedService messageDroppedService;

    @Inject
    public PublishOutboundInterceptorHandler(@NotNull final PluginOutPutAsyncer asyncer,
                                             @NotNull final FullConfigurationService configurationService,
                                             @NotNull final PluginTaskExecutorService pluginTaskExecutorService,
                                             @NotNull final HiveMQExtensions hiveMQExtensions,
                                             @NotNull final MessageDroppedService messageDroppedService) {
        this.asyncer = asyncer;
        this.configurationService = configurationService;
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.hiveMQExtensions = hiveMQExtensions;
        this.messageDroppedService = messageDroppedService;
    }

    @Override
    public void write(@NotNull final ChannelHandlerContext ctx, @NotNull final Object msg, @NotNull final ChannelPromise promise) throws Exception {
        if (!(msg instanceof PUBLISH)) {
            super.write(ctx, msg, promise);
            return;
        }
        if (!handlePublish(ctx, (PUBLISH) msg, promise)) {
            super.write(ctx, msg, promise);
        }

    }

    // Returns true if the publish is handled by the outbound interceptor handling
    private boolean handlePublish(@NotNull final ChannelHandlerContext ctx, @NotNull final PUBLISH publish, @NotNull final ChannelPromise promise) {
        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return false;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return false;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null || clientContext.getPublishOutboundInterceptors().isEmpty()) {
            return false;
        }

        final List<PublishOutboundInterceptor> publishOutboundInterceptors = clientContext.getPublishOutboundInterceptors();
        final PublishOutboundInputImpl input = new PublishOutboundInputImpl(new PublishPacketImpl(publish), clientId, channel);
        final PublishOutboundOutputImpl output = new PublishOutboundOutputImpl(configurationService, asyncer, publish);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final PublishOutboundInterceptorContext interceptorContext = new PublishOutboundInterceptorContext(
                PublishOutboundInterceptorTask.class, clientId, output, input, interceptorFuture, publishOutboundInterceptors.size()
        );

        for (final PublishOutboundInterceptor interceptor : publishOutboundInterceptors) {

            //we can stop running interceptors if delivery is prevented.
            if (output.isPreventDelivery()) {
                //we do not know if it is already set by an async task so we check it
                if (!interceptorFuture.isDone()) {
                    interceptorFuture.set(null);
                }
                break;
            }

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader((IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            //disabled extension would be null
            if (extension == null) {
                interceptorContext.increment();
                continue;
            }

            final PublishOutboundInterceptorTask interceptorTask = new PublishOutboundInterceptorTask(interceptor, extension.getId());

            final boolean executionSuccessful = pluginTaskExecutorService.handlePluginInOutTaskExecution(interceptorContext, input, output, interceptorTask);

            //we need to increment since extension post method would not be called.
            if (!executionSuccessful) {

                String className = interceptor.getClass().getSimpleName();

                //may happen if interface not implemented.
                if (className.isEmpty()) {
                    className = "PublishOutboundInterceptor";
                }

                log.warn("Extension task queue full. Ignoring '{}' from extension '{}'", className, extension.getId());
                interceptorContext.increment();
            }
        }

        final SettableFuture<PublishStatus> publishFuture;
        if (publish instanceof PublishWithFuture) {
            publishFuture = ((PublishWithFuture) publish).getFuture();
        } else {
            publishFuture = null;
        }
        final InterceptorFutureCallback callback = new InterceptorFutureCallback(output, clientId, publish, ctx, messageDroppedService,
                publishFuture, promise);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
        return true;
    }

    class PublishOutboundInterceptorContext extends PluginInOutTaskContext<PublishOutboundOutputImpl> {

        private final @NotNull PublishOutboundOutputImpl output;
        private final @NotNull PublishOutboundInputImpl input;
        @VisibleForTesting
        final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PublishOutboundInterceptorContext(final @NotNull Class<?> taskClazz,
                                          final @NotNull String identifier,
                                          final @NotNull PublishOutboundOutputImpl output,
                                          final @NotNull PublishOutboundInputImpl input,
                                          final @NotNull SettableFuture<Void> interceptorFuture,
                                          final int interceptorCount) {
            super(taskClazz, identifier);
            this.output = output;
            this.input = input;
            this.interceptorFuture = interceptorFuture;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(@NotNull final PublishOutboundOutputImpl pluginOutput) {

            if (pluginOutput.isAsync() && pluginOutput.isTimedOut() && pluginOutput.getTimeoutFallback() == TimeoutFallback.FAILURE) {
                //Timeout fallback failure means publish delivery prevention
                pluginOutput.forciblyPreventPublishDelivery();
            }

            if (output.getPublishPacket().isModified()) {
                input.updatePublish(output.getPublishPacket());
            }

            if (counter.incrementAndGet() == interceptorCount || pluginOutput.isPreventDelivery()) {
                interceptorFuture.set(null);
            }
        }

        public void increment() {
            //we must set the future when no more interceptors are registered
            if (counter.incrementAndGet() == interceptorCount) {
                interceptorFuture.set(null);
            }
        }
    }

    private class PublishOutboundInterceptorTask implements PluginInOutTask<PublishOutboundInputImpl, PublishOutboundOutputImpl> {

        private final @NotNull PublishOutboundInterceptor interceptor;
        private final @NotNull String pluginId;

        private PublishOutboundInterceptorTask(final @NotNull PublishOutboundInterceptor interceptor,
                                               final @NotNull String pluginId) {
            this.interceptor = interceptor;
            this.pluginId = pluginId;
        }

        @Override
        public @NotNull PublishOutboundOutputImpl apply(final @NotNull PublishOutboundInputImpl publishOutboundInput,
                                                        final @NotNull PublishOutboundOutputImpl publishOutboundOutput) {
            if (publishOutboundOutput.isPreventDelivery()) {
                //it's already prevented so no further interceptors must be called.
                return publishOutboundOutput;
            }
            try {
                interceptor.onOutboundPublish(publishOutboundInput, publishOutboundOutput);
            } catch (final Throwable e) {
                log.warn("Uncaught exception was thrown from extension with id \"{}\" on outbound publish interception. " +
                        "Extensions are responsible on their own to handle exceptions.", pluginId);
                Exceptions.rethrowError(e);
            }
            return publishOutboundOutput;
        }
    }

    private static class InterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull PublishOutboundOutputImpl outboundOutput;
        private final @NotNull String clientId;
        private final @NotNull PUBLISH publish;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull MessageDroppedService messageDroppedService;
        private final @Nullable SettableFuture<PublishStatus> publishFuture;
        private final @NotNull ChannelPromise promise;

        InterceptorFutureCallback(
                final @NotNull PublishOutboundOutputImpl outboundOutput,
                final @NotNull String clientId,
                final @NotNull PUBLISH publish,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull MessageDroppedService messageDroppedService,
                final @Nullable SettableFuture<PublishStatus> publishFuture,
                final @NotNull ChannelPromise promise) {
            this.outboundOutput = outboundOutput;
            this.clientId = clientId;
            this.publish = publish;
            this.ctx = ctx;
            this.messageDroppedService = messageDroppedService;
            this.publishFuture = publishFuture;
            this.promise = promise;
        }

        @Override
        public void onSuccess(final @Nullable Void result) {
            if (outboundOutput.isPreventDelivery()) {
                messageDroppedService.extensionPrevented(clientId, publish.getTopic(), publish.getQoS().getQosNumber());
                promise.setSuccess();
                if (publishFuture != null) {
                    publishFuture.set(PublishStatus.DELIVERED);
                }
            } else {
                final PUBLISH mergedPublish = PUBLISHFactory.mergePublishPacket(outboundOutput.getPublishPacket(), publish);
                ctx.writeAndFlush(mergedPublish);
            }
        }

        @Override
        public void onFailure(final @NotNull Throwable t) {
            //should never happen, since the settable future never sets an exception
            Exceptions.rethrowError("Exception in publish outbound interceptor handling. ", t);
            ctx.writeAndFlush(publish);
        }
    }
}
