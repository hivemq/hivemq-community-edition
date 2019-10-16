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

package com.hivemq.bootstrap.netty;

import com.hivemq.annotations.NotNull;
import com.hivemq.bootstrap.netty.initializer.ListenerAttributeAdderFactory;
import com.hivemq.codec.decoder.MqttConnectDecoder;
import com.hivemq.codec.decoder.MqttDecoders;
import com.hivemq.codec.encoder.EncoderFactory;
import com.hivemq.codec.encoder.MQTTMessageEncoder;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.extensions.handler.*;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.metrics.handler.MetricsInitializer;
import com.hivemq.mqtt.handler.auth.AuthHandler;
import com.hivemq.mqtt.handler.auth.AuthInProgressMessageHandler;
import com.hivemq.mqtt.handler.connect.ConnectHandler;
import com.hivemq.mqtt.handler.connect.ConnectPersistenceUpdateHandler;
import com.hivemq.mqtt.handler.connect.StopReadingAfterConnectHandler;
import com.hivemq.mqtt.handler.disconnect.DisconnectHandler;
import com.hivemq.mqtt.handler.ping.PingRequestHandler;
import com.hivemq.mqtt.handler.publish.DropOutgoingPublishesHandler;
import com.hivemq.mqtt.handler.publish.PublishMessageExpiryHandler;
import com.hivemq.mqtt.handler.publish.PublishUserEventReceivedHandler;
import com.hivemq.mqtt.handler.publish.ReturnMessageIdToPoolHandler;
import com.hivemq.mqtt.handler.publish.qos.QoSReceiverHandler;
import com.hivemq.mqtt.handler.publish.qos.QoSSenderHandler;
import com.hivemq.mqtt.handler.subscribe.SubscribeHandler;
import com.hivemq.mqtt.handler.unsubscribe.UnsubscribeHandler;
import com.hivemq.security.ssl.SslParameterHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Christoph Schäbel
 * @author Dominik Obermaier
 */
public class ChannelDependencies {

    @NotNull
    private final Provider<MetricsInitializer> statisticsInitializer;

    @NotNull
    private final Provider<ConnectHandler> connectHandlerProvider;

    @NotNull
    private final ConnectPersistenceUpdateHandler connectPersistenceUpdateHandler;

    @NotNull
    private final DisconnectHandler disconnectHandler;

    @NotNull
    private final Provider<SubscribeHandler> subscribeHandlerProvider;

    @NotNull
    private final Provider<PublishUserEventReceivedHandler> publishUserEventReceivedHandlerProvider;

    @NotNull
    private final Provider<UnsubscribeHandler> unsubscribeHandlerProvider;

    @NotNull
    private final Provider<QoSReceiverHandler> qoSReceiverHandlerProvider;

    @NotNull
    private final Provider<QoSSenderHandler> qoSSenderHandlerProvider;

    @NotNull
    private final ChannelGroup channelGroup;

    @NotNull
    private final FullConfigurationService fullConfigurationService;

    @NotNull
    private final GlobalTrafficShapingHandler globalTrafficShapingHandler;

    @NotNull
    private final MetricsHolder metricsHolder;

    @NotNull
    private final ExceptionHandler exceptionHandler;

    @NotNull
    private final PingRequestHandler pingRequestHandler;

    @NotNull
    private final RestrictionsConfigurationService restrictionsConfigurationService;

    @NotNull
    private final MqttConnectDecoder mqttConnectDecoder;

    @NotNull
    private final ReturnMessageIdToPoolHandler returnMessageIdToPoolHandler;

    @NotNull
    private final MQTTMessageEncoder mqttMessageEncoder;

    @NotNull
    private final ListenerAttributeAdderFactory listenerAttributeAdderFactory;

    @NotNull
    private final StopReadingAfterConnectHandler stopReadingAfterConnectHandler;

    @NotNull
    private final Provider<DropOutgoingPublishesHandler> dropOutgoingPublishesHandlerProvider;

    @NotNull
    private final EventLog eventLog;

    @NotNull
    private final SslParameterHandler sslParameterHandler;

    @NotNull
    private final MqttDecoders mqttDecoders;

    @NotNull
    private final AuthHandler authHandler;

    @NotNull
    private final Provider<PluginInitializerHandler> pluginInitializerHandlerProvider;

    @NotNull
    private final Provider<ClientLifecycleEventHandler> clientLifecycleEventHandlerProvider;

    @NotNull
    private final AuthInProgressMessageHandler authInProgressMessageHandler;

    @NotNull
    private final Provider<PublishMessageExpiryHandler> publishMessageExpiryHandlerProvider;

    @NotNull
    private final Provider<IncomingPublishHandler> incomingPublishHandlerProvider;

    @NotNull
    private final Provider<IncomingSubscribeHandler> incomingSubscribeHandlerProvider;

    @NotNull
    private final PublishOutboundInterceptorHandler publishOutboundInterceptorHandler;

    @NotNull
    private final ConnectInboundInterceptorHandler connectInboundInterceptorHandler;

    @NotNull
    private final ConnackOutboundInterceptorHandler connackOutboundInterceptorHandler;

    @NotNull
    private final UnsubscribeInboundInterceptorHandler unsubscribeInboundInterceptorHandler;

    @Inject
    public ChannelDependencies(
            @NotNull final Provider<MetricsInitializer> statisticsInitializer,
            @NotNull final Provider<ConnectHandler> connectHandlerProvider,
            @NotNull final ConnectPersistenceUpdateHandler connectPersistenceUpdateHandler,
            @NotNull final DisconnectHandler disconnectHandler,
            @NotNull final Provider<SubscribeHandler> subscribeHandlerProvider,
            @NotNull final Provider<PublishUserEventReceivedHandler> publishUserEventReceivedHandlerProvider,
            @NotNull final Provider<UnsubscribeHandler> unsubscribeHandlerProvider,
            @NotNull final Provider<QoSReceiverHandler> qoSReceiverHandlerProvider,
            @NotNull final Provider<QoSSenderHandler> qoSSenderHandlerProvider,
            @NotNull final ChannelGroup channelGroup,
            @NotNull final FullConfigurationService fullConfigurationService,
            @NotNull final GlobalTrafficShapingHandler globalTrafficShapingHandler,
            @NotNull final MetricsHolder metricsHolder,
            @NotNull final ExceptionHandler exceptionHandler,
            @NotNull final PingRequestHandler pingRequestHandler,
            @NotNull final RestrictionsConfigurationService restrictionsConfigurationService,
            @NotNull final MqttConnectDecoder mqttConnectDecoder,
            @NotNull final ReturnMessageIdToPoolHandler returnMessageIdToPoolHandler,
            @NotNull final StopReadingAfterConnectHandler stopReadingAfterConnectHandler,
            @NotNull final ListenerAttributeAdderFactory listenerAttributeAdderFactory,
            @NotNull final Provider<DropOutgoingPublishesHandler> dropOutgoingPublishesHandlerProvider,
            @NotNull final EventLog eventLog,
            @NotNull final SslParameterHandler sslParameterHandler,
            @NotNull final MqttDecoders mqttDecoders,
            @NotNull final EncoderFactory encoderFactory,
            @NotNull final AuthHandler authHandler,
            @NotNull final AuthInProgressMessageHandler authInProgressMessageHandler,
            @NotNull final Provider<PluginInitializerHandler> pluginInitializerHandlerProvider,
            @NotNull final Provider<ClientLifecycleEventHandler> clientLifecycleEventHandlerProvider,
            @NotNull final Provider<IncomingPublishHandler> incomingPublishHandlerProvider,
            @NotNull final Provider<IncomingSubscribeHandler> incomingSubscribeHandlerProvider,
            @NotNull final Provider<PublishMessageExpiryHandler> publishMessageExpiryHandlerProvider,
            @NotNull final PublishOutboundInterceptorHandler publishOutboundInterceptorHandler,
            @NotNull final ConnectInboundInterceptorHandler connectInboundInterceptorHandler,
            @NotNull final ConnackOutboundInterceptorHandler connackOutboundInterceptorHandler,
            @NotNull final UnsubscribeInboundInterceptorHandler unsubscribeInboundInterceptorHandler) {


        this.statisticsInitializer = statisticsInitializer;
        this.connectHandlerProvider = connectHandlerProvider;
        this.connectPersistenceUpdateHandler = connectPersistenceUpdateHandler;
        this.disconnectHandler = disconnectHandler;
        this.subscribeHandlerProvider = subscribeHandlerProvider;
        this.publishUserEventReceivedHandlerProvider = publishUserEventReceivedHandlerProvider;
        this.unsubscribeHandlerProvider = unsubscribeHandlerProvider;
        this.qoSReceiverHandlerProvider = qoSReceiverHandlerProvider;
        this.qoSSenderHandlerProvider = qoSSenderHandlerProvider;
        this.channelGroup = channelGroup;
        this.fullConfigurationService = fullConfigurationService;
        this.globalTrafficShapingHandler = globalTrafficShapingHandler;
        this.metricsHolder = metricsHolder;
        this.exceptionHandler = exceptionHandler;
        this.pingRequestHandler = pingRequestHandler;
        this.restrictionsConfigurationService = restrictionsConfigurationService;
        this.mqttConnectDecoder = mqttConnectDecoder;
        this.returnMessageIdToPoolHandler = returnMessageIdToPoolHandler;
        this.stopReadingAfterConnectHandler = stopReadingAfterConnectHandler;
        this.mqttMessageEncoder = new MQTTMessageEncoder(encoderFactory);
        this.listenerAttributeAdderFactory = listenerAttributeAdderFactory;
        this.dropOutgoingPublishesHandlerProvider = dropOutgoingPublishesHandlerProvider;
        this.eventLog = eventLog;
        this.sslParameterHandler = sslParameterHandler;
        this.mqttDecoders = mqttDecoders;
        this.authHandler = authHandler;
        this.authInProgressMessageHandler = authInProgressMessageHandler;
        this.pluginInitializerHandlerProvider = pluginInitializerHandlerProvider;
        this.clientLifecycleEventHandlerProvider = clientLifecycleEventHandlerProvider;
        this.incomingPublishHandlerProvider = incomingPublishHandlerProvider;
        this.incomingSubscribeHandlerProvider = incomingSubscribeHandlerProvider;
        this.publishMessageExpiryHandlerProvider = publishMessageExpiryHandlerProvider;
        this.publishOutboundInterceptorHandler = publishOutboundInterceptorHandler;
        this.connectInboundInterceptorHandler = connectInboundInterceptorHandler;
        this.connackOutboundInterceptorHandler = connackOutboundInterceptorHandler;
        this.unsubscribeInboundInterceptorHandler = unsubscribeInboundInterceptorHandler;
    }

    @NotNull
    public MetricsInitializer getStatisticsInitializer() {
        return statisticsInitializer.get();
    }

    @NotNull
    public ConnectHandler getConnectHandler() {
        return connectHandlerProvider.get();
    }

    @NotNull
    public DisconnectHandler getDisconnectHandler() {
        return disconnectHandler;
    }

    @NotNull
    public SubscribeHandler getSubscribeHandler() {
        return subscribeHandlerProvider.get();
    }

    @NotNull
    public PublishUserEventReceivedHandler getPublishUserEventReceivedHandler() {
        return publishUserEventReceivedHandlerProvider.get();
    }

    @NotNull
    public UnsubscribeHandler getUnsubscribeHandler() {
        return unsubscribeHandlerProvider.get();
    }

    @NotNull
    public QoSSenderHandler getQoSSenderHandler() {
        return qoSSenderHandlerProvider.get();
    }

    @NotNull
    public QoSReceiverHandler getQoSReceiverHandler() {
        return qoSReceiverHandlerProvider.get();
    }

    @NotNull
    public ChannelGroup getChannelGroup() {
        return channelGroup;
    }

    @NotNull
    public FullConfigurationService getConfigurationService() {
        return fullConfigurationService;
    }

    @NotNull
    public GlobalTrafficShapingHandler getGlobalTrafficShapingHandler() {
        return globalTrafficShapingHandler;
    }

    @NotNull
    public MetricsHolder getMetricsHolder() {
        return metricsHolder;
    }

    @NotNull
    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    @NotNull
    public PingRequestHandler getPingRequestHandler() {
        return pingRequestHandler;
    }

    @NotNull
    public ConnectPersistenceUpdateHandler getConnectPersistenceUpdateHandler() {
        return connectPersistenceUpdateHandler;
    }

    @NotNull
    public RestrictionsConfigurationService getRestrictionsConfigurationService() {
        return restrictionsConfigurationService;
    }

    @NotNull
    public MqttConnectDecoder getMqttConnectDecoder() {
        return mqttConnectDecoder;
    }

    @NotNull
    public ReturnMessageIdToPoolHandler getReturnMessageIdToPoolHandler() {
        return returnMessageIdToPoolHandler;
    }

    @NotNull
    public MQTTMessageEncoder getMqttMessageEncoder() {
        return mqttMessageEncoder;
    }

    @NotNull
    public StopReadingAfterConnectHandler getStopReadingAfterConnectHandler() {
        return stopReadingAfterConnectHandler;
    }

    @NotNull
    public ListenerAttributeAdderFactory getListenerAttributeAdderFactory() {
        return listenerAttributeAdderFactory;
    }

    @NotNull
    public DropOutgoingPublishesHandler getDropOutgoingPublishesHandler() {
        return dropOutgoingPublishesHandlerProvider.get();
    }

    @NotNull
    public PublishMessageExpiryHandler getPublishMessageExpiryHandler() {
        return publishMessageExpiryHandlerProvider.get();
    }

    @NotNull
    public EventLog getEventLog() {
        return eventLog;
    }

    @NotNull
    public SslParameterHandler getSslParameterHandler() {
        return sslParameterHandler;
    }

    @NotNull
    public MqttDecoders getMqttDecoders() {
        return mqttDecoders;
    }

    @NotNull
    public AuthHandler getAuthHandler() {
        return authHandler;
    }

    @NotNull
    public AuthInProgressMessageHandler getAuthInProgressMessageHandler() {
        return authInProgressMessageHandler;
    }

    @NotNull
    public PluginInitializerHandler getPluginInitializerHandler() {
        return pluginInitializerHandlerProvider.get();
    }

    @NotNull
    public ClientLifecycleEventHandler getClientLifecycleEventHandler() {
        return clientLifecycleEventHandlerProvider.get();
    }

    @NotNull
    public IncomingPublishHandler getIncomingPublishHandler() {
        return incomingPublishHandlerProvider.get();
    }

    @NotNull
    public IncomingSubscribeHandler getIncomingSubscribeHandler() {
        return incomingSubscribeHandlerProvider.get();
    }

    @NotNull
    public PublishOutboundInterceptorHandler getPublishOutboundInterceptorHandler() {
        return publishOutboundInterceptorHandler;
    }

    @NotNull
    public ConnectInboundInterceptorHandler getConnectInboundInterceptorHandler() {
        return connectInboundInterceptorHandler;
    }

    @NotNull
    public ConnackOutboundInterceptorHandler getConnackOutboundInterceptorHandler() {
        return connackOutboundInterceptorHandler;
    }

    @NotNull
    public UnsubscribeInboundInterceptorHandler getUnsubscribeInboundInterceptorHandler() {
        return unsubscribeInboundInterceptorHandler;
    }
}
