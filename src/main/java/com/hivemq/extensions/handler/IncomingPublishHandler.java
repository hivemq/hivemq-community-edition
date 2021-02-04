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
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extensions.ExtensionInformationUtil;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.interceptor.publish.parameter.PublishInboundInputImpl;
import com.hivemq.extensions.interceptor.publish.parameter.PublishInboundOutputImpl;
import com.hivemq.extensions.packets.publish.ModifiablePublishPacketImpl;
import com.hivemq.extensions.packets.publish.PublishPacketImpl;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5PubAckReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
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
 * @author Silvio Giebl
 * @since 4.0.0
 */
@Singleton
public class IncomingPublishHandler {

    private static final Logger log = LoggerFactory.getLogger(IncomingPublishHandler.class);

    private final @NotNull PluginTaskExecutorService executorService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull MessageDroppedService messageDroppedService;
    private final @NotNull PluginAuthorizerService authorizerService;
    private final @NotNull MqttServerDisconnector mqttDisconnector;
    private final @NotNull FullConfigurationService configurationService;

    @Inject
    public IncomingPublishHandler(
            final @NotNull PluginTaskExecutorService executorService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull PluginAuthorizerService authorizerService,
            final @NotNull MqttServerDisconnector mqttDisconnector,
            final @NotNull FullConfigurationService configurationService) {

        this.executorService = executorService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.messageDroppedService = messageDroppedService;
        this.authorizerService = authorizerService;
        this.mqttDisconnector = mqttDisconnector;
        this.configurationService = configurationService;
    }

    /**
     * intercepts the publish message when the channel is active, the client id is set and interceptors are available,
     * otherwise delegates to authorizer
     *
     * @param ctx     the context of the channel handler
     * @param publish the publish to process
     */
    public void interceptOrDelegate(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBLISH publish, final @NotNull String clientId) {
        final Channel channel = ctx.channel();

        final ClientContextImpl clientContext = channel.attr(ChannelAttributes.EXTENSION_CLIENT_CONTEXT).get();
        if (clientContext == null) {
            ctx.executor().execute(() -> authorizerService.authorizePublish(ctx, publish));
            return;
        }
        final List<PublishInboundInterceptor> interceptors = clientContext.getPublishInboundInterceptors();
        if (interceptors.isEmpty()) {
            ctx.executor().execute(() -> authorizerService.authorizePublish(ctx, publish));
            return;
        }

        final ClientInformation clientInfo = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        final ConnectionInformation connectionInfo = ExtensionInformationUtil.getAndSetConnectionInformation(channel);

        final PublishPacketImpl packet = new PublishPacketImpl(publish);
        final PublishInboundInputImpl input = new PublishInboundInputImpl(clientInfo, connectionInfo, packet);
        final ExtensionParameterHolder<PublishInboundInputImpl> inputHolder = new ExtensionParameterHolder<>(input);

        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);
        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<PublishInboundOutputImpl> outputHolder = new ExtensionParameterHolder<>(output);

        final PublishInboundInterceptorContext context = new PublishInboundInterceptorContext(
                clientId, interceptors.size(), ctx, publish, inputHolder, outputHolder);

        for (final PublishInboundInterceptor interceptor : interceptors) {

            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(interceptor.getClass().getClassLoader());
            if (extension == null) { // disabled extension would be null
                context.finishInterceptor();
                continue;
            }

            final PublishInboundInterceptorTask task =
                    new PublishInboundInterceptorTask(interceptor, extension.getId());
            executorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
    }

    private class PublishInboundInterceptorContext extends PluginInOutTaskContext<PublishInboundOutputImpl>
            implements Runnable {

        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull PUBLISH publish;
        private final @NotNull ExtensionParameterHolder<PublishInboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<PublishInboundOutputImpl> outputHolder;

        PublishInboundInterceptorContext(
                final @NotNull String clientId,
                final int interceptorCount,
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull PUBLISH publish,
                final @NotNull ExtensionParameterHolder<PublishInboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<PublishInboundOutputImpl> outputHolder) {

            super(clientId);
            this.interceptorCount = interceptorCount;
            this.counter = new AtomicInteger(0);
            this.ctx = ctx;
            this.publish = publish;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull PublishInboundOutputImpl output) {
            if (output.isPreventDelivery()) {
                finishInterceptor();
            } else if (output.isTimedOut() && (output.getTimeoutFallback() == TimeoutFallback.FAILURE)) {
                output.forciblyPreventPublishDelivery(output.getReasonCode(), output.getReasonString());
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
            final PublishInboundOutputImpl output = outputHolder.get();
            if (output.isPreventDelivery()) {
                dropMessage(output);
            } else {
                final PUBLISH finalPublish = PUBLISHFactory.merge(inputHolder.get().getPublishPacket(), publish);
                authorizerService.authorizePublish(ctx, finalPublish);
            }
        }

        private void dropMessage(final @NotNull PublishInboundOutputImpl output) {
            final Channel channel = ctx.channel();
            final String clientId = getIdentifier();

            final ProtocolVersion protocolVersion = channel.attr(ChannelAttributes.MQTT_VERSION).get();
            //MQTT 3
            if (protocolVersion != ProtocolVersion.MQTTv5) {
                if (output.getReasonCode() != AckReasonCode.SUCCESS) {
                    mqttDisconnector.disconnect(
                            channel,
                            "Client '" + clientId +
                                    "' (IP: {}) sent a PUBLISH, but its onward delivery was prevented by a publish inbound interceptor. Disconnecting client.",
                            "Sent PUBLISH, but its onward delivery was prevented by a publish inbound interceptor",
                            Mqtt5DisconnectReasonCode.ADMINISTRATIVE_ACTION,
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
                                Mqtt5PubAckReasonCode.from(output.getReasonCode());
                        ctx.writeAndFlush(new PUBACK(publish.getPacketIdentifier(), ackReasonCode,
                                output.getReasonString(), Mqtt5UserProperties.NO_USER_PROPERTIES));
                        break;
                    case EXACTLY_ONCE:
                        final Mqtt5PubRecReasonCode recReasonCode =
                                Mqtt5PubRecReasonCode.from(output.getReasonCode());
                        ctx.writeAndFlush(new PUBREC(publish.getPacketIdentifier(), recReasonCode,
                                output.getReasonString(), Mqtt5UserProperties.NO_USER_PROPERTIES));
                        break;
                }
            }

            messageDroppedService.extensionPrevented(clientId, publish.getTopic(), publish.getQoS().getQosNumber());
        }
    }

    private static class PublishInboundInterceptorTask
            implements PluginInOutTask<PublishInboundInputImpl, PublishInboundOutputImpl> {

        private final @NotNull PublishInboundInterceptor interceptor;
        private final @NotNull String extensionId;

        private PublishInboundInterceptorTask(
                final @NotNull PublishInboundInterceptor interceptor, final @NotNull String extensionId) {

            this.interceptor = interceptor;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull PublishInboundOutputImpl apply(
                final @NotNull PublishInboundInputImpl input, final @NotNull PublishInboundOutputImpl output) {

            if (output.isPreventDelivery()) {
                // it's already prevented so no further interceptors must be called.
                return output;
            }
            try {
                interceptor.onInboundPublish(input, output);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on inbound PUBLISH interception. " +
                                "Extensions are responsible for their own exception handling.", extensionId);
                log.debug("Original exception:", e);
                output.forciblyPreventPublishDelivery(output.getReasonCode(), output.getReasonString());
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
