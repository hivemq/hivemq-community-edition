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
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertNotNull;

/**
 * @author Florian Limpöck
 * @since 4.1.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class ChannelDependenciesTest {

    private ChannelDependencies channelDependencies;

    @Mock
    private NoConnectIdleHandler noConnectIdleHandler;

    @Mock
    private ConnectHandler connectHandler;

    @Mock
    private DisconnectHandler disconnectHandler;

    @Mock
    private SubscribeHandler subscribeHandler;

    @Mock
    private UnsubscribeHandler unsubscribeHandler;

    @Mock
    private ChannelGroup channelGroup;

    @Mock
    private FullConfigurationService fullConfigurationService;

    @Mock
    private GlobalTrafficShapingHandler globalTrafficShapingHandler;

    @Mock
    private MetricsHolder metricsHolder;

    @Mock
    private ExceptionHandler exceptionHandler;

    @Mock
    private PingRequestHandler pingRequestHandler;

    @Mock
    private RestrictionsConfigurationService restrictionsConfigurationService;

    @Mock
    private MqttConnectDecoder mqttConnectDecoder;

    @Mock
    private EncoderFactory encoderFactory;

    @Mock
    private EventLog eventLog;

    @Mock
    private SslParameterHandler sslParameterHandler;

    @Mock
    private MqttDecoders mqttDecoders;

    @Mock
    private AuthHandler authHandler;

    @Mock
    private PluginInitializerHandler pluginInitializerHandler;

    @Mock
    private ClientLifecycleEventHandler clientLifecycleEventHandler;

    @Mock
    private AuthInProgressMessageHandler authInProgressMessageHandler;

    @Mock
    private MessageExpiryHandler messageExpiryHandler;

    @Mock
    private IncomingPublishHandler incomingPublishHandler;

    @Mock
    private IncomingSubscribeHandler incomingSubscribeHandler;

    @Mock
    private ConnectionLimiterHandler connectionLimiterHandler;

    @Mock
    private MqttServerDisconnector mqttServerDisconnector;

    @Mock
    private InterceptorHandler interceptorHandler;

    @Mock
    private GlobalMQTTMessageCounter globalMQTTMessageCounter;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        channelDependencies = new ChannelDependencies(
                noConnectIdleHandler,
                () -> connectHandler,
                connectionLimiterHandler,
                disconnectHandler,
                () -> subscribeHandler,
                () -> unsubscribeHandler,
                channelGroup,
                fullConfigurationService,
                globalTrafficShapingHandler,
                metricsHolder,
                exceptionHandler,
                pingRequestHandler,
                restrictionsConfigurationService,
                mqttConnectDecoder,
                eventLog,
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
                globalMQTTMessageCounter);

    }

    @Test
    public void test_all_provided() {

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