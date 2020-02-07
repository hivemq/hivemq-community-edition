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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.publish.parameter.PublishInboundInputImpl;
import com.hivemq.extensions.interceptor.publish.parameter.PublishInboundOutputImpl;
import com.hivemq.extensions.packets.publish.PublishPacketImpl;
import com.hivemq.mqtt.handler.disconnect.Mqtt3ServerDisconnector;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.reason.Mqtt5PubAckReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This handler intercepts every inbound PUBLISH message and delegates it to all registered {@link
 * PublishInboundInterceptor}s for a specific client.
 * <p>
 * When delivery of the PUBLISH is prevented, the message will be dropped.
 * <p>
 * When the {@link AckReasonCode} for the PUBACK or PUBREC is not SUCCESS a MQTT 3 client will be disconnected, but an
 * MQTT 5 client receives the PUBACK or PUBREC with the reason code.
 * <p>
 * When the {@link AckReasonCode} is SUCCESS an Acknowledgement will be sent (PUBACK or PUBREC) dependent on the QoS.
 * <p>
 * Same happens for TimeoutFallback.FAILURE if the output is marked as async and timed out.
 * <p>
 * Multiple interceptors are called sequentially, beginning with the one with the highest priority.
 * <p>
 * If any interceptor prevents delivery of a publish, no more interceptor will be called.
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
@ChannelHandler.Sharable
@Singleton
public class IncomingPublishHandler extends SimpleChannelInboundHandler<PUBLISH> {

    private static final Logger log = LoggerFactory.getLogger(IncomingPublishHandler.class);

    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull MessageDroppedService messageDroppedService;
    private final @NotNull PluginAuthorizerService pluginAuthorizerService;
    private final @NotNull Mqtt3ServerDisconnector mqttDisconnector;
    private final @NotNull FullConfigurationService configurationService;

    @Inject
    public IncomingPublishHandler(
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull PluginAuthorizerService pluginAuthorizerService,
            final @NotNull Mqtt3ServerDisconnector mqttDisconnector,
            final @NotNull FullConfigurationService configurationService) {

        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.messageDroppedService = messageDroppedService;
        this.pluginAuthorizerService = pluginAuthorizerService;
        this.mqttDisconnector = mqttDisconnector;
        this.configurationService = configurationService;
    }

    @Override
    public void channelRead0(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBLISH msg) {
        interceptOrDelegate(ctx, msg);
    }

    /**
     * intercepts the publish message when the channel is active, the client id is set and interceptors are available,
     * otherwise delegates to authorizer
     *
     * @param ctx     the context of the channel handler
     * @param publish the publish to process
     */
    private void interceptOrDelegate(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBLISH publish) {

        final Channel channel = ctx.channel();
        if (!channel.isActive()) {
            return;
        }

        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null) {
            return;
        }

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).get();
        if (clientContext == null || clientContext.getPublishInboundInterceptors().isEmpty()) {
            ctx.executor().execute(() -> pluginAuthorizerService.authorizePublish(ctx, publish));
            return;
        }

        final List<PublishInboundInterceptor> publishInboundInterceptors =
                clientContext.getPublishInboundInterceptors();

        final PublishInboundOutputImpl inboundOutput =
                new PublishInboundOutputImpl(configurationService, asyncer, publish);
        final PublishInboundInputImpl inboundInput =
                new PublishInboundInputImpl(new PublishPacketImpl(publish), clientId, channel);
        final SettableFuture<Void> interceptorFuture = SettableFuture.create();
        final PublishInboundInterceptorContext interceptorContext = new PublishInboundInterceptorContext(
                clientId, inboundOutput, inboundInput, interceptorFuture, publishInboundInterceptors.size());

        for (final PublishInboundInterceptor interceptor : publishInboundInterceptors) {

            //we can stop running interceptors if delivery is prevented.
            if (inboundOutput.isPreventDelivery()) {
                //we do not know if it is already set by an async task so we check it
                if (!interceptorFuture.isDone()) {
                    interceptorFuture.set(null);
                }
                break;
            }

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(
                    (IsolatedPluginClassloader) interceptor.getClass().getClassLoader());

            //disabled extension would be null
            if (plugin == null) {
                interceptorContext.increment();
                continue;
            }

            final PublishInboundInterceptorTask interceptorTask =
                    new PublishInboundInterceptorTask(interceptor, plugin.getId());

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    interceptorContext, inboundInput, inboundOutput, interceptorTask);
        }

        final InterceptorFutureCallback callback =
                new InterceptorFutureCallback(inboundOutput, channel, clientId, publish, ctx, mqttDisconnector,
                        messageDroppedService, pluginAuthorizerService);
        Futures.addCallback(interceptorFuture, callback, ctx.executor());
    }

    private class PublishInboundInterceptorContext extends PluginInOutTaskContext<PublishInboundOutputImpl> {

        private final @NotNull PublishInboundOutputImpl output;
        private final @NotNull PublishInboundInputImpl input;
        private final @NotNull SettableFuture<Void> interceptorFuture;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;

        PublishInboundInterceptorContext(
                final @NotNull String identifier,
                final @NotNull PublishInboundOutputImpl output,
                final @NotNull PublishInboundInputImpl input,
                final @NotNull SettableFuture<Void> interceptorFuture,
                final int interceptorCount) {

            super(identifier);
            this.output = output;
            this.input = input;
            this.interceptorFuture = interceptorFuture;
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public void pluginPost(@NotNull final PublishInboundOutputImpl pluginOutput) {

            if (pluginOutput.isAsync() && pluginOutput.isTimedOut() &&
                    pluginOutput.getTimeoutFallback() == TimeoutFallback.FAILURE) {
                //Timeout fallback failure means publish delivery prevention
                pluginOutput.forciblyPreventPublishDelivery(
                        pluginOutput.getReasonCode(), pluginOutput.getReasonString());
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

    private class PublishInboundInterceptorTask
            implements PluginInOutTask<PublishInboundInputImpl, PublishInboundOutputImpl> {

        private final @NotNull PublishInboundInterceptor interceptor;
        private final @NotNull String pluginId;

        private PublishInboundInterceptorTask(
                final @NotNull PublishInboundInterceptor interceptor,
                final @NotNull String pluginId) {

            this.interceptor = interceptor;
            this.pluginId = pluginId;
        }

        @Override
        public @NotNull PublishInboundOutputImpl apply(
                final @NotNull PublishInboundInputImpl publishInboundInput,
                final @NotNull PublishInboundOutputImpl publishInboundOutput) {

            if (publishInboundOutput.isPreventDelivery()) {
                //it's already prevented so no further interceptors must be called.
                return publishInboundOutput;
            }
            try {
                interceptor.onInboundPublish(publishInboundInput, publishInboundOutput);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound publish interception. Extensions are responsible on their own to handle exceptions.",
                        pluginId);
                publishInboundOutput.forciblyPreventPublishDelivery(
                        publishInboundOutput.getReasonCode(), publishInboundOutput.getReasonString());
                Exceptions.rethrowError(e);
            }
            return publishInboundOutput;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptor.getClass().getClassLoader();
        }
    }

    private static class InterceptorFutureCallback implements FutureCallback<Void> {

        private final @NotNull PublishInboundOutputImpl inboundOutput;
        private final @NotNull Channel channel;
        private final @NotNull String clientId;
        private final @NotNull PUBLISH publish;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull Mqtt3ServerDisconnector mqttDisconnector;
        private final @NotNull MessageDroppedService messageDroppedService;
        private final @NotNull PluginAuthorizerService pluginAuthorizerService;

        InterceptorFutureCallback(
                final @NotNull PublishInboundOutputImpl inboundOutput,
                final @NotNull Channel channel,
                final @NotNull String clientId,
                final @NotNull PUBLISH publish,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull Mqtt3ServerDisconnector mqttDisconnector,
                final @NotNull MessageDroppedService messageDroppedService,
                final @NotNull PluginAuthorizerService pluginAuthorizerService) {

            this.inboundOutput = inboundOutput;
            this.channel = channel;
            this.clientId = clientId;
            this.publish = publish;
            this.ctx = ctx;
            this.mqttDisconnector = mqttDisconnector;
            this.messageDroppedService = messageDroppedService;
            this.pluginAuthorizerService = pluginAuthorizerService;
        }

        @Override
        public void onSuccess(final @Nullable Void result) {
            if (inboundOutput.isPreventDelivery()) {
                dropMessage();
            } else {
                final PUBLISH finalPublish =
                        PUBLISHFactory.mergePublishPacket(inboundOutput.getPublishPacket(), publish);
                ctx.executor().execute(() -> pluginAuthorizerService.authorizePublish(ctx, finalPublish));
            }
        }

        private void dropMessage() {

            final ProtocolVersion protocolVersion = channel.attr(ChannelAttributes.MQTT_VERSION).get();
            //MQTT 3
            if (protocolVersion != ProtocolVersion.MQTTv5) {
                if (inboundOutput.getReasonCode() != AckReasonCode.SUCCESS) {
                    mqttDisconnector.disconnect(
                            channel,
                            "Client '" + clientId +
                                    "' (IP: {}) sent a PUBLISH, but its onward delivery was prevented by a publish inbound interceptor. Disconnecting client.",
                            "Sent PUBLISH, but its onward delivery was prevented by a publish inbound interceptor",
                            null,
                            null);
                } else {

                    switch (publish.getQoS()) {
                        case AT_MOST_ONCE:
                            //no ack for qos 0
                            break;
                        case AT_LEAST_ONCE:
                            ctx.writeAndFlush(new PUBACK(publish.getPacketIdentifier()));
                            break;
                        case EXACTLY_ONCE:
                            ctx.writeAndFlush(new PUBREC(publish.getPacketIdentifier()));
                            break;
                    }

                }

                //MQTT 5
            } else {
                switch (publish.getQoS()) {
                    case AT_MOST_ONCE:
                        //no ack for qos 0
                        break;
                    case AT_LEAST_ONCE:
                        final Mqtt5PubAckReasonCode ackReasonCode =
                                Mqtt5PubAckReasonCode.from(inboundOutput.getReasonCode());
                        ctx.writeAndFlush(new PUBACK(publish.getPacketIdentifier(), ackReasonCode,
                                inboundOutput.getReasonString(), Mqtt5UserProperties.NO_USER_PROPERTIES));
                        break;
                    case EXACTLY_ONCE:
                        final Mqtt5PubRecReasonCode recReasonCode =
                                Mqtt5PubRecReasonCode.from(inboundOutput.getReasonCode());
                        ctx.writeAndFlush(new PUBREC(publish.getPacketIdentifier(), recReasonCode,
                                inboundOutput.getReasonString(), Mqtt5UserProperties.NO_USER_PROPERTIES));
                        break;
                }
            }

            messageDroppedService.extensionPrevented(clientId, publish.getTopic(), publish.getQoS().getQosNumber());
        }

        @Override
        public void onFailure(final @NotNull Throwable t) {
            //should never happen, since the settable future never sets an exception
            ctx.executor().execute(() -> pluginAuthorizerService.authorizePublish(ctx, publish));
        }
    }
}
