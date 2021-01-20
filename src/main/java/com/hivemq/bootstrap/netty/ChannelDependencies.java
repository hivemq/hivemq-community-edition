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
package com.hivemq.bootstrap.netty;

import com.hivemq.codec.decoder.MqttConnectDecoder;
import com.hivemq.codec.decoder.MqttDecoders;
import com.hivemq.codec.encoder.EncoderFactory;
import com.hivemq.codec.encoder.MQTTMessageEncoder;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.handler.*;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.metrics.handler.MetricsInitializer;
import com.hivemq.mqtt.handler.InterceptorHandler;
import com.hivemq.mqtt.handler.auth.AuthHandler;
import com.hivemq.mqtt.handler.auth.AuthInProgressMessageHandler;
import com.hivemq.mqtt.handler.connect.ConnectHandler;
import com.hivemq.mqtt.handler.connect.ConnectPersistenceUpdateHandler;
import com.hivemq.mqtt.handler.connect.ConnectionLimiterHandler;
import com.hivemq.mqtt.handler.connect.NoConnectIdleHandler;
import com.hivemq.mqtt.handler.disconnect.DisconnectHandler;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.handler.ping.PingRequestHandler;
import com.hivemq.mqtt.handler.publish.DropOutgoingPublishesHandler;
import com.hivemq.mqtt.handler.publish.MessageExpiryHandler;
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

    private final @NotNull Provider<MetricsInitializer> statisticsInitializer;
    private final @NotNull NoConnectIdleHandler noConnectIdleHandler;
    private final @NotNull Provider<ConnectHandler> connectHandlerProvider;
    private final @NotNull ConnectionLimiterHandler connectionLimiterHandler;
    private final @NotNull ConnectPersistenceUpdateHandler connectPersistenceUpdateHandler;
    private final @NotNull DisconnectHandler disconnectHandler;
    private final @NotNull Provider<SubscribeHandler> subscribeHandlerProvider;
    private final @NotNull Provider<PublishUserEventReceivedHandler> publishUserEventReceivedHandlerProvider;
    private final @NotNull Provider<UnsubscribeHandler> unsubscribeHandlerProvider;
    private final @NotNull Provider<QoSReceiverHandler> qoSReceiverHandlerProvider;
    private final @NotNull Provider<QoSSenderHandler> qoSSenderHandlerProvider;
    private final @NotNull ChannelGroup channelGroup;
    private final @NotNull FullConfigurationService fullConfigurationService;
    private final @NotNull GlobalTrafficShapingHandler globalTrafficShapingHandler;
    private final @NotNull MetricsHolder metricsHolder;
    private final @NotNull ExceptionHandler exceptionHandler;
    private final @NotNull PingRequestHandler pingRequestHandler;
    private final @NotNull RestrictionsConfigurationService restrictionsConfigurationService;
    private final @NotNull MqttConnectDecoder mqttConnectDecoder;
    private final @NotNull ReturnMessageIdToPoolHandler returnMessageIdToPoolHandler;
    private final @NotNull MQTTMessageEncoder mqttMessageEncoder;
    private final @NotNull Provider<DropOutgoingPublishesHandler> dropOutgoingPublishesHandlerProvider;
    private final @NotNull EventLog eventLog;
    private final @NotNull SslParameterHandler sslParameterHandler;
    private final @NotNull MqttDecoders mqttDecoders;
    private final @NotNull Provider<AuthHandler> authHandlerProvider;
    private final @NotNull Provider<PluginInitializerHandler> pluginInitializerHandlerProvider;
    private final @NotNull Provider<ClientLifecycleEventHandler> clientLifecycleEventHandlerProvider;
    private final @NotNull AuthInProgressMessageHandler authInProgressMessageHandler;
    private final @NotNull Provider<IncomingPublishHandler> incomingPublishHandlerProvider;
    private final @NotNull Provider<IncomingSubscribeHandler> incomingSubscribeHandlerProvider;
    private final @NotNull Provider<MessageExpiryHandler> publishMessageExpiryHandlerProvider;
    private final @NotNull PublishOutboundInterceptorHandler publishOutboundInterceptorHandler;
    private final @NotNull ConnectInboundInterceptorHandler connectInboundInterceptorHandler;
    private final @NotNull ConnackOutboundInterceptorHandler connackOutboundInterceptorHandler;
    private final @NotNull DisconnectInterceptorHandler disconnectInterceptorHandler;
    private final @NotNull PubackInterceptorHandler pubackInterceptorHandler;
    private final @NotNull PubrecInterceptorHandler pubrecInterceptorHandler;
    private final @NotNull PubrelInterceptorHandler pubrelInterceptorHandler;
    private final @NotNull PubcompInterceptorHandler pubcompInterceptorhandler;
    private final @NotNull SubackOutboundInterceptorHandler subAckOutboundInterceptorHandler;
    private final @NotNull UnsubscribeInboundInterceptorHandler unsubscribeInboundInterceptorHandler;
    private final @NotNull UnsubackOutboundInterceptorHandler unsubackOutboundInterceptorHandler;
    private final @NotNull PingInterceptorHandler pingInterceptorHandler;
    private final @NotNull MqttServerDisconnector mqttServerDisconnector;
    private final @NotNull InterceptorHandler interceptorHandler;


    @Inject
    public ChannelDependencies(
            final @NotNull Provider<MetricsInitializer> statisticsInitializer,
            final @NotNull NoConnectIdleHandler noConnectIdleHandler,
            final @NotNull Provider<ConnectHandler> connectHandlerProvider,
            final @NotNull ConnectionLimiterHandler connectionLimiterHandler,
            final @NotNull ConnectPersistenceUpdateHandler connectPersistenceUpdateHandler,
            final @NotNull DisconnectHandler disconnectHandler,
            final @NotNull Provider<SubscribeHandler> subscribeHandlerProvider,
            final @NotNull Provider<PublishUserEventReceivedHandler> publishUserEventReceivedHandlerProvider,
            final @NotNull Provider<UnsubscribeHandler> unsubscribeHandlerProvider,
            final @NotNull Provider<QoSReceiverHandler> qoSReceiverHandlerProvider,
            final @NotNull Provider<QoSSenderHandler> qoSSenderHandlerProvider,
            final @NotNull ChannelGroup channelGroup,
            final @NotNull FullConfigurationService fullConfigurationService,
            final @NotNull GlobalTrafficShapingHandler globalTrafficShapingHandler,
            final @NotNull MetricsHolder metricsHolder,
            final @NotNull ExceptionHandler exceptionHandler,
            final @NotNull PingRequestHandler pingRequestHandler,
            final @NotNull RestrictionsConfigurationService restrictionsConfigurationService,
            final @NotNull MqttConnectDecoder mqttConnectDecoder,
            final @NotNull ReturnMessageIdToPoolHandler returnMessageIdToPoolHandler,
            final @NotNull Provider<DropOutgoingPublishesHandler> dropOutgoingPublishesHandlerProvider,
            final @NotNull EventLog eventLog,
            final @NotNull SslParameterHandler sslParameterHandler,
            final @NotNull MqttDecoders mqttDecoders,
            final @NotNull EncoderFactory encoderFactory,
            final @NotNull Provider<AuthHandler> authHandlerProvider,
            final @NotNull AuthInProgressMessageHandler authInProgressMessageHandler,
            final @NotNull Provider<PluginInitializerHandler> pluginInitializerHandlerProvider,
            final @NotNull Provider<ClientLifecycleEventHandler> clientLifecycleEventHandlerProvider,
            final @NotNull Provider<IncomingPublishHandler> incomingPublishHandlerProvider,
            final @NotNull Provider<IncomingSubscribeHandler> incomingSubscribeHandlerProvider,
            final @NotNull Provider<MessageExpiryHandler> publishMessageExpiryHandlerProvider,
            final @NotNull PublishOutboundInterceptorHandler publishOutboundInterceptorHandler,
            final @NotNull ConnectInboundInterceptorHandler connectInboundInterceptorHandler,
            final @NotNull ConnackOutboundInterceptorHandler connackOutboundInterceptorHandler,
            final @NotNull DisconnectInterceptorHandler disconnectInterceptorHandler,
            final @NotNull PubackInterceptorHandler pubackInterceptorHandler,
            final @NotNull PubrecInterceptorHandler pubrecInterceptorHandler,
            final @NotNull PubrelInterceptorHandler pubrelInterceptorHandler,
            final @NotNull PubcompInterceptorHandler pubcompInterceptorHandler,
            final @NotNull SubackOutboundInterceptorHandler subAckOutboundInterceptorHandler,
            final @NotNull UnsubackOutboundInterceptorHandler unsubackOutboundInterceptorHandler,
            final @NotNull UnsubscribeInboundInterceptorHandler unsubscribeInboundInterceptorHandler,
            final @NotNull PingInterceptorHandler pingInterceptorHandler,
            final @NotNull MqttServerDisconnector mqttServerDisconnector, @NotNull InterceptorHandler interceptorHandler) {

        this.statisticsInitializer = statisticsInitializer;
        this.noConnectIdleHandler = noConnectIdleHandler;
        this.connectHandlerProvider = connectHandlerProvider;
        this.connectionLimiterHandler = connectionLimiterHandler;
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
        this.mqttMessageEncoder = new MQTTMessageEncoder(encoderFactory);
        this.dropOutgoingPublishesHandlerProvider = dropOutgoingPublishesHandlerProvider;
        this.eventLog = eventLog;
        this.sslParameterHandler = sslParameterHandler;
        this.mqttDecoders = mqttDecoders;
        this.authHandlerProvider = authHandlerProvider;
        this.authInProgressMessageHandler = authInProgressMessageHandler;
        this.pluginInitializerHandlerProvider = pluginInitializerHandlerProvider;
        this.clientLifecycleEventHandlerProvider = clientLifecycleEventHandlerProvider;
        this.incomingPublishHandlerProvider = incomingPublishHandlerProvider;
        this.incomingSubscribeHandlerProvider = incomingSubscribeHandlerProvider;
        this.publishMessageExpiryHandlerProvider = publishMessageExpiryHandlerProvider;
        this.publishOutboundInterceptorHandler = publishOutboundInterceptorHandler;
        this.connectInboundInterceptorHandler = connectInboundInterceptorHandler;
        this.connackOutboundInterceptorHandler = connackOutboundInterceptorHandler;
        this.disconnectInterceptorHandler = disconnectInterceptorHandler;
        this.pubackInterceptorHandler = pubackInterceptorHandler;
        this.pubrecInterceptorHandler = pubrecInterceptorHandler;
        this.pubrelInterceptorHandler = pubrelInterceptorHandler;
        this.pubcompInterceptorhandler = pubcompInterceptorHandler;
        this.subAckOutboundInterceptorHandler = subAckOutboundInterceptorHandler;
        this.unsubackOutboundInterceptorHandler = unsubackOutboundInterceptorHandler;
        this.unsubscribeInboundInterceptorHandler = unsubscribeInboundInterceptorHandler;
        this.pingInterceptorHandler = pingInterceptorHandler;
        this.mqttServerDisconnector = mqttServerDisconnector;
        this.interceptorHandler = interceptorHandler;
    }

    @NotNull
    public MetricsInitializer getStatisticsInitializer() {
        return statisticsInitializer.get();
    }

    @NotNull
    public NoConnectIdleHandler getNoConnectIdleHandler() {
        return noConnectIdleHandler;
    }

    @NotNull
    public ConnectHandler getConnectHandler() {
        return connectHandlerProvider.get();
    }

    @NotNull
    public ConnectionLimiterHandler getConnectionLimiterHandler() {
        return connectionLimiterHandler;
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
    public DropOutgoingPublishesHandler getDropOutgoingPublishesHandler() {
        return dropOutgoingPublishesHandlerProvider.get();
    }

    @NotNull
    public MessageExpiryHandler getPublishMessageExpiryHandler() {
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
        return authHandlerProvider.get();
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
    public DisconnectInterceptorHandler getDisconnectInterceptorHandler() {
        return disconnectInterceptorHandler;
    }

    @NotNull
    public PubackInterceptorHandler getPubackInterceptorHandler() {
        return pubackInterceptorHandler;
    }

    @NotNull
    public PubrecInterceptorHandler getPubrecInterceptorHandler() {
        return pubrecInterceptorHandler;
    }

    @NotNull
    public PubrelInterceptorHandler getPubrelInterceptorHandler() {
        return pubrelInterceptorHandler;
    }

    @NotNull
    public PubcompInterceptorHandler getPubcompInterceptorHandler() {
        return pubcompInterceptorhandler;
    }

    @NotNull
    public SubackOutboundInterceptorHandler getSubackOutboundInterceptorHandler() {
        return subAckOutboundInterceptorHandler;
    }

    @NotNull
    public UnsubackOutboundInterceptorHandler getUnsubackOutboundInterceptorHandler() {
        return unsubackOutboundInterceptorHandler;
    }

    @NotNull
    public UnsubscribeInboundInterceptorHandler getUnsubscribeInboundInterceptorHandler() {
        return unsubscribeInboundInterceptorHandler;
    }

    @NotNull
    public PingInterceptorHandler getPingInterceptorHandler() {
        return pingInterceptorHandler;
    }

    @NotNull
    public MqttServerDisconnector getMqttServerDisconnector() {
        return mqttServerDisconnector;
    }

    @NotNull
    public InterceptorHandler getInterceptorMultiplexer() {
        return interceptorHandler;
    }
}
