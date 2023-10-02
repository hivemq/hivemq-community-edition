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
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.handler.ClientLifecycleEventHandler;
import com.hivemq.extensions.handler.IncomingPublishHandler;
import com.hivemq.extensions.handler.IncomingSubscribeHandler;
import com.hivemq.extensions.handler.PluginInitializerHandler;
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertNotNull;

public class ChannelDependenciesTest {

    @Mock
    private @NotNull NoConnectIdleHandler noConnectIdleHandler;

    @Mock
    private @NotNull ConnectHandler connectHandler;

    @Mock
    private @NotNull DisconnectHandler disconnectHandler;

    @Mock
    private @NotNull SubscribeHandler subscribeHandler;

    @Mock
    private @NotNull UnsubscribeHandler unsubscribeHandler;

    @Mock
    private @NotNull ChannelGroup channelGroup;

    @Mock
    private @NotNull FullConfigurationService fullConfigurationService;

    @Mock
    private @NotNull GlobalTrafficShapingHandler globalTrafficShapingHandler;

    @Mock
    private @NotNull MetricsHolder metricsHolder;

    @Mock
    private @NotNull ExceptionHandler exceptionHandler;

    @Mock
    private @NotNull PingRequestHandler pingRequestHandler;

    @Mock
    private @NotNull RestrictionsConfigurationService restrictionsConfigurationService;

    @Mock
    private @NotNull MqttConnectDecoder mqttConnectDecoder;

    @Mock
    private @NotNull EncoderFactory encoderFactory;

    @Mock
    private @NotNull EventLog eventLog;

    @Mock
    private @NotNull SslParameterHandler sslParameterHandler;

    @Mock
    private @NotNull MqttDecoders mqttDecoders;

    @Mock
    private @NotNull AuthHandler authHandler;

    @Mock
    private @NotNull PluginInitializerHandler pluginInitializerHandler;

    @Mock
    private @NotNull ClientLifecycleEventHandler clientLifecycleEventHandler;

    @Mock
    private @NotNull AuthInProgressMessageHandler authInProgressMessageHandler;

    @Mock
    private @NotNull MessageExpiryHandler messageExpiryHandler;

    @Mock
    private @NotNull IncomingPublishHandler incomingPublishHandler;

    @Mock
    private @NotNull IncomingSubscribeHandler incomingSubscribeHandler;

    @Mock
    private @NotNull ConnectionLimiterHandler connectionLimiterHandler;

    @Mock
    private @NotNull MqttServerDisconnector mqttServerDisconnector;

    @Mock
    private @NotNull InterceptorHandler interceptorHandler;

    @Mock
    private @NotNull GlobalMQTTMessageCounter globalMQTTMessageCounter;

    @Mock
    private @NotNull ShutdownHooks shutdownHooks;

    private @NotNull ChannelDependencies channelDependencies;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        channelDependencies = new ChannelDependencies(noConnectIdleHandler,
                () -> connectHandler,
                connectionLimiterHandler,
                disconnectHandler,
                () -> subscribeHandler,
                unsubscribeHandler,
                channelGroup,
                fullConfigurationService,
                globalTrafficShapingHandler,
                metricsHolder,
                exceptionHandler,
                pingRequestHandler,
                restrictionsConfigurationService,
                mqttConnectDecoder, mqttConnacker, eventLog,
                sslParameterHandler,
                mqttDecoders,
                encoderFactory,
                () -> authHandler,
                authInProgressMessageHandler,
                () -> pluginInitializerHandler,
                () -> clientLifecycleEventHandler,
                () -> incomingPublishHandler,
                () -> incomingSubscribeHandler,
                () -> messageExpiryHandler,
                mqttServerDisconnector,
                interceptorHandler,
                globalMQTTMessageCounter,
                shutdownHooks);
    }

    @Test
    public void getters_returnAllHandlers() {
        assertNotNull(channelDependencies.getNoConnectIdleHandler());
        assertNotNull(channelDependencies.getConnectHandler());
        assertNotNull(channelDependencies.getDisconnectHandler());
        assertNotNull(channelDependencies.getSubscribeHandler());
        assertNotNull(channelDependencies.getUnsubscribeHandler());
        assertNotNull(channelDependencies.getChannelGroup());
        assertNotNull(channelDependencies.getConfigurationService());
        assertNotNull(channelDependencies.getGlobalTrafficShapingHandler());
        assertNotNull(channelDependencies.getMetricsHolder());
        assertNotNull(channelDependencies.getExceptionHandler());
        assertNotNull(channelDependencies.getPingRequestHandler());
        assertNotNull(channelDependencies.getRestrictionsConfigurationService());
        assertNotNull(channelDependencies.getMqttConnectDecoder());
        assertNotNull(channelDependencies.getMqttMessageEncoder());
        assertNotNull(channelDependencies.getPublishMessageExpiryHandler());
        assertNotNull(channelDependencies.getEventLog());
        assertNotNull(channelDependencies.getSslParameterHandler());
        assertNotNull(channelDependencies.getMqttDecoders());
        assertNotNull(channelDependencies.getAuthHandler());
        assertNotNull(channelDependencies.getAuthInProgressMessageHandler());
        assertNotNull(channelDependencies.getPluginInitializerHandler());
        assertNotNull(channelDependencies.getClientLifecycleEventHandler());
        assertNotNull(channelDependencies.getIncomingPublishHandler());
        assertNotNull(channelDependencies.getIncomingSubscribeHandler());
        assertNotNull(channelDependencies.getConnectionLimiterHandler());
        assertNotNull(channelDependencies.getMqttServerDisconnector());
        assertNotNull(channelDependencies.getInterceptorHandler());
        assertNotNull(channelDependencies.getGlobalMQTTMessageCounter());
    }
}
