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
import com.hivemq.metrics.handler.GlobalMQTTMessageCounter;
import com.hivemq.mqtt.handler.InterceptorHandler;
import com.hivemq.mqtt.handler.auth.AuthHandler;
import com.hivemq.mqtt.handler.auth.AuthInProgressMessageHandler;
import com.hivemq.mqtt.handler.connect.ConnectHandler;
import com.hivemq.mqtt.handler.connect.ConnectionLimiterHandler;
import com.hivemq.mqtt.handler.connect.NoConnectIdleHandler;
import com.hivemq.mqtt.handler.disconnect.DisconnectHandler;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.handler.ping.PingRequestHandler;
import com.hivemq.mqtt.handler.publish.MessageExpiryHandler;
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

    private final @NotNull NoConnectIdleHandler noConnectIdleHandler;
    private final @NotNull Provider<ConnectHandler> connectHandlerProvider;
    private final @NotNull ConnectionLimiterHandler connectionLimiterHandler;
    private final @NotNull DisconnectHandler disconnectHandler;
    private final @NotNull Provider<SubscribeHandler> subscribeHandlerProvider;
    private final @NotNull Provider<UnsubscribeHandler> unsubscribeHandlerProvider;
    private final @NotNull ChannelGroup channelGroup;
    private final @NotNull FullConfigurationService fullConfigurationService;
    private final @NotNull GlobalTrafficShapingHandler globalTrafficShapingHandler;
    private final @NotNull MetricsHolder metricsHolder;
    private final @NotNull ExceptionHandler exceptionHandler;
    private final @NotNull PingRequestHandler pingRequestHandler;
    private final @NotNull RestrictionsConfigurationService restrictionsConfigurationService;
    private final @NotNull MqttConnectDecoder mqttConnectDecoder;
    private final @NotNull MQTTMessageEncoder mqttMessageEncoder;
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
    private final @NotNull MqttServerDisconnector mqttServerDisconnector;
    private final @NotNull InterceptorHandler interceptorHandler;
    private final @NotNull GlobalMQTTMessageCounter globalMQTTMessageCounter;


    @Inject
    public ChannelDependencies(
            final @NotNull NoConnectIdleHandler noConnectIdleHandler,
            final @NotNull Provider<ConnectHandler> connectHandlerProvider,
            final @NotNull ConnectionLimiterHandler connectionLimiterHandler,
            final @NotNull DisconnectHandler disconnectHandler,
            final @NotNull Provider<SubscribeHandler> subscribeHandlerProvider,
            final @NotNull Provider<UnsubscribeHandler> unsubscribeHandlerProvider,
            final @NotNull ChannelGroup channelGroup,
            final @NotNull FullConfigurationService fullConfigurationService,
            final @NotNull GlobalTrafficShapingHandler globalTrafficShapingHandler,
            final @NotNull MetricsHolder metricsHolder,
            final @NotNull ExceptionHandler exceptionHandler,
            final @NotNull PingRequestHandler pingRequestHandler,
            final @NotNull RestrictionsConfigurationService restrictionsConfigurationService,
            final @NotNull MqttConnectDecoder mqttConnectDecoder,
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
            final @NotNull MqttServerDisconnector mqttServerDisconnector,
            final @NotNull InterceptorHandler interceptorHandler,
            final @NotNull GlobalMQTTMessageCounter globalMQTTMessageCounter) {

        this.noConnectIdleHandler = noConnectIdleHandler;
        this.connectHandlerProvider = connectHandlerProvider;
        this.connectionLimiterHandler = connectionLimiterHandler;
        this.disconnectHandler = disconnectHandler;
        this.subscribeHandlerProvider = subscribeHandlerProvider;
        this.unsubscribeHandlerProvider = unsubscribeHandlerProvider;
        this.channelGroup = channelGroup;
        this.fullConfigurationService = fullConfigurationService;
        this.globalTrafficShapingHandler = globalTrafficShapingHandler;
        this.metricsHolder = metricsHolder;
        this.exceptionHandler = exceptionHandler;
        this.pingRequestHandler = pingRequestHandler;
        this.restrictionsConfigurationService = restrictionsConfigurationService;
        this.mqttConnectDecoder = mqttConnectDecoder;
        this.mqttMessageEncoder = new MQTTMessageEncoder(encoderFactory, globalMQTTMessageCounter);
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
        this.mqttServerDisconnector = mqttServerDisconnector;
        this.interceptorHandler = interceptorHandler;
        this.globalMQTTMessageCounter = globalMQTTMessageCounter;
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
    public UnsubscribeHandler getUnsubscribeHandler() {
        return unsubscribeHandlerProvider.get();
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
    public RestrictionsConfigurationService getRestrictionsConfigurationService() {
        return restrictionsConfigurationService;
    }

    @NotNull
    public MqttConnectDecoder getMqttConnectDecoder() {
        return mqttConnectDecoder;
    }

    @NotNull
    public MQTTMessageEncoder getMqttMessageEncoder() {
        return mqttMessageEncoder;
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
    public MqttServerDisconnector getMqttServerDisconnector() {
        return mqttServerDisconnector;
    }

    @NotNull
    public InterceptorHandler getInterceptorHandler() {
        return interceptorHandler;
    }

    @NotNull
    public GlobalMQTTMessageCounter getGlobalMQTTMessageCounter() {
        return globalMQTTMessageCounter;
    }
}
