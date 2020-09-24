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
package com.hivemq.bootstrap.netty.initializer;

import com.google.common.base.Preconditions;
import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.codec.decoder.MQTTMessageDecoder;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.connect.MessageBarrier;
import com.hivemq.mqtt.handler.connect.SubscribeMessageBarrier;
import com.hivemq.mqtt.handler.publish.ChannelInactiveHandler;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.security.exception.SslException;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.*;
import static com.hivemq.configuration.service.InternalConfigurations.DROP_MESSAGES_QOS_0;

/**
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 * @author Silvio Giebl
 */
public abstract class AbstractChannelInitializer extends ChannelInitializer<Channel> {

    private static final Logger log = LoggerFactory.getLogger(AbstractChannelInitializer.class);

    public static final String FIRST_ABSTRACT_HANDLER = ALL_CHANNELS_GROUP_HANDLER;

    @NotNull
    private final ChannelDependencies channelDependencies;
    @NotNull
    private final Listener listener;

    public AbstractChannelInitializer(
            @NotNull final ChannelDependencies channelDependencies,
            @NotNull final Listener listener) {
        this.channelDependencies = channelDependencies;
        this.listener = listener;
    }

    @Override
    protected void initChannel(@NotNull final Channel ch) throws Exception {

        Preconditions.checkNotNull(ch, "Channel must never be null");

        ch.attr(ChannelAttributes.LISTENER).set(listener);

        ch.pipeline().addLast(FIRST_ABSTRACT_HANDLER, new ChannelGroupHandler(channelDependencies.getChannelGroup()));

        ch.pipeline().addLast(GLOBAL_THROTTLING_HANDLER, channelDependencies.getGlobalTrafficShapingHandler());

        ch.pipeline().addLast(MQTT_MESSAGE_DECODER, new MQTTMessageDecoder(channelDependencies));
        ch.pipeline().addLast(MQTT_MESSAGE_ENCODER, channelDependencies.getMqttMessageEncoder());

        addNoConnectIdleHandler(ch);

        ch.pipeline().addLast(MQTT_MESSAGE_BARRIER, new MessageBarrier(channelDependencies.getMqttServerDisconnector()));
        ch.pipeline().addLast(MQTT_SUBSCRIBE_MESSAGE_BARRIER, new SubscribeMessageBarrier());

        // before connack outbound interceptor as it initializes the client context after the connack
        ch.pipeline().addLast(PLUGIN_INITIALIZER_HANDLER, channelDependencies.getPluginInitializerHandler());

        ch.pipeline().addLast(CONNECT_INBOUND_INTERCEPTOR_HANDLER, channelDependencies.getConnectInboundInterceptorHandler());
        ch.pipeline().addLast(CONNACK_OUTBOUND_INTERCEPTOR_HANDLER, channelDependencies.getConnackOutboundInterceptorHandler());
        ch.pipeline().addLast(PING_INTERCEPTOR_HANDLER, channelDependencies.getPingInterceptorHandler());
        ch.pipeline().addLast(PUBLISH_OUTBOUND_INTERCEPTOR_HANDLER, channelDependencies.getPublishOutboundInterceptorHandler());
        ch.pipeline().addLast(PUBACK_INTERCEPTOR_HANDLER, channelDependencies.getPubackInterceptorHandler());
        ch.pipeline().addLast(PUBREC_INTERCEPTOR_HANDLER, channelDependencies.getPubrecInterceptorHandler());
        ch.pipeline().addLast(PUBREL_INTERCEPTOR_HANDLER, channelDependencies.getPubrelInterceptorHandler());
        ch.pipeline().addLast(PUBCOMP_INTERCEPTOR_HANDLER, channelDependencies.getPubcompInterceptorHandler());
        ch.pipeline().addLast(SUBACK_OUTBOUND_INTERCEPTOR_HANDLER, channelDependencies.getSubackOutboundInterceptorHandler());
        ch.pipeline().addLast(UNSUBACK_OUTBOUND_INTERCEPTOR_HANDLER, channelDependencies.getUnsubackOutboundInterceptorHandler());
        // after unsuback outbound interceptor as a unsubscribe can be rejected and then a unsuback is sent
        ch.pipeline().addLast(UNSUBSCRIBE_INBOUND_INTERCEPTOR_HANDLER, channelDependencies.getUnsubscribeInboundInterceptorHandler());
        ch.pipeline().addLast(DISCONNECT_INTERCEPTOR_HANDLER, channelDependencies.getDisconnectInterceptorHandler());

        // after connect inbound interceptor as it intercepts the connect
        ch.pipeline().addLast(CLIENT_LIFECYCLE_EVENT_HANDLER, channelDependencies.getClientLifecycleEventHandler());

        ch.pipeline().addLast(MQTT_AUTH_HANDLER, channelDependencies.getAuthHandler());

        ch.pipeline().addLast(MQTT_MESSAGE_ID_RETURN_HANDLER, channelDependencies.getReturnMessageIdToPoolHandler());

        if (DROP_MESSAGES_QOS_0) {
            ch.pipeline().addLast(DROP_OUTGOING_PUBLISHES_HANDLER, channelDependencies.getDropOutgoingPublishesHandler());
        }

        ch.pipeline().addLast(MESSAGE_EXPIRY_HANDLER, channelDependencies.getPublishMessageExpiryHandler());

        ch.pipeline().addLast(CONNECTION_LIMITER, channelDependencies.getConnectionLimiterHandler());
        ch.pipeline().addLast(MQTT_CONNECT_HANDLER, channelDependencies.getConnectHandler());

        ch.pipeline().addLast(MQTT_CONNECT_PERSISTENCE_HANDLER, channelDependencies.getConnectPersistenceUpdateHandler());
        ch.pipeline().addLast(MQTT_DISCONNECT_HANDLER, channelDependencies.getDisconnectHandler());

        ch.pipeline().addLast(MQTT_QOS_RECEIVER_HANDLER, channelDependencies.getQoSReceiverHandler());
        ch.pipeline().addLast(MQTT_QOS_SENDER_HANDLER, channelDependencies.getQoSSenderHandler());
        ch.pipeline().addLast(MQTT_PINGREQ_HANDLER, channelDependencies.getPingRequestHandler());
        ch.pipeline().addLast(CHANNEL_INACTIVE_HANDLER, new ChannelInactiveHandler());

        ch.pipeline().addLast(INCOMING_PUBLISH_HANDLER, channelDependencies.getIncomingPublishHandler());
        ch.pipeline().addLast(INCOMING_SUBSCRIBE_HANDLER, channelDependencies.getIncomingSubscribeHandler());

        ch.pipeline().addLast(MQTT_SUBSCRIBE_HANDLER, channelDependencies.getSubscribeHandler());
        ch.pipeline().addLast(MQTT_PUBLISH_USER_EVENT_HANDLER, channelDependencies.getPublishUserEventReceivedHandler());

        ch.pipeline().addLast(MQTT_UNSUBSCRIBE_HANDLER, channelDependencies.getUnsubscribeHandler());
        ch.pipeline().addLast(STATISTICS_INITIALIZER, channelDependencies.getStatisticsInitializer());

        addSpecialHandlers(ch);

        ch.pipeline().addLast(EXCEPTION_HANDLER, channelDependencies.getExceptionHandler());
    }

    protected void addNoConnectIdleHandler(@NotNull final Channel ch) {

        //get timeout value from internal config
        final RestrictionsConfigurationService restrictionsConfig =
                channelDependencies.getRestrictionsConfigurationService();

        final long timeoutMillis = restrictionsConfig.noConnectIdleTimeout();

        if (timeoutMillis > 0) {
            final IdleStateHandler idleStateHandler = new IdleStateHandler(timeoutMillis, 0, 0, TimeUnit.MILLISECONDS);

            ch.pipeline().addAfter(MQTT_MESSAGE_ENCODER, NEW_CONNECTION_IDLE_HANDLER, idleStateHandler);
            ch.pipeline()
                    .addAfter(NEW_CONNECTION_IDLE_HANDLER, NO_CONNECT_IDLE_EVENT_HANDLER,
                            channelDependencies.getNoConnectIdleHandler());
        }
    }

    protected abstract void addSpecialHandlers(@NotNull final Channel ch) throws Exception;

    @Override
    public void exceptionCaught(final @NotNull ChannelHandlerContext ctx, @NotNull final Throwable cause) throws Exception {
        if (cause instanceof SslException) {
            log.error(
                    "{}. Disconnecting client {} ", cause.getMessage(),
                    ChannelUtils.getChannelIP(ctx.channel()).or("UNKNOWN"));
            log.debug("Original exception:", cause);
            //We need to close the channel because the initialization wasn't successful
            channelDependencies.getMqttServerDisconnector().disconnect(ctx.channel(),
                    null, //already logged
                    cause.getMessage() != null ? cause.getMessage() : "TLS connection initialization failed.",
                    Mqtt5DisconnectReasonCode.NOT_AUTHORIZED,
                    null,
                    Mqtt5UserProperties.NO_USER_PROPERTIES,
                    false,
                    true);
        } else {

            //Just use the default handler
            super.exceptionCaught(ctx, cause);
        }
    }

}
