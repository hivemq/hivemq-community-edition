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

import com.hivemq.codec.decoder.MqttConnectDecoder;
import com.hivemq.codec.decoder.MqttDecoders;
import com.hivemq.codec.encoder.EncoderFactory;
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
import com.hivemq.mqtt.handler.connect.NoConnectIdleHandler;
import com.hivemq.mqtt.handler.disconnect.DisconnectHandler;
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
    private MetricsInitializer statisticsInitializer;

    @Mock
    private NoConnectIdleHandler noConnectIdleHandler;

    @Mock
    private ConnectHandler connectHandler;

    @Mock
    private ConnectPersistenceUpdateHandler connectPersistenceUpdateHandler;

    @Mock
    private DisconnectHandler disconnectHandler;

    @Mock
    private SubscribeHandler subscribeHandler;

    @Mock
    private PublishUserEventReceivedHandler publishUserEventReceivedHandler;

    @Mock
    private UnsubscribeHandler unsubscribeHandler;

    @Mock
    private QoSReceiverHandler qoSReceiverHandler;

    @Mock
    private QoSSenderHandler qoSSenderHandler;

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
    private ReturnMessageIdToPoolHandler returnMessageIdToPoolHandler;

    @Mock
    private EncoderFactory encoderFactory;

    @Mock
    private DropOutgoingPublishesHandler dropOutgoingPublishesHandler;

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
    private PublishOutboundInterceptorHandler publishOutboundInterceptorHandler;

    @Mock
    private ConnectInboundInterceptorHandler connectInterceptorHandler;

    @Mock
    private ConnackOutboundInterceptorHandler connackOutboundInterceptorHandler;

    @Mock
    private DisconnectInterceptorHandler disconnectInterceptorHandler;

    @Mock
    private PubackInterceptorHandler pubackInterceptorHandler;

    @Mock
    private PubrecInterceptorHandler pubrecInterceptorHandler;

    @Mock
    private PubrelInterceptorHandler pubrelInterceptorHandler;

    @Mock
    private PubcompInterceptorHandler pubcompInterceptorHandler;

    @Mock
    private SubackOutboundInterceptorHandler subAckOutboundInterceptorHandler;

    @Mock
    private UnsubackOutboundInterceptorHandler unsubackOutboundInterceptorHandler;

    @Mock
    private UnsubscribeInboundInterceptorHandler unsubscribeInboundInterceptorHandler;

    @Mock
    private PingInterceptorHandler pingInterceptorHandler;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        channelDependencies = new ChannelDependencies(
                () -> statisticsInitializer,
                noConnectIdleHandler,
                () -> connectHandler,
                connectPersistenceUpdateHandler,
                disconnectHandler,
                () -> subscribeHandler,
                () -> publishUserEventReceivedHandler,
                () -> unsubscribeHandler,
                () -> qoSReceiverHandler,
                () -> qoSSenderHandler,
                channelGroup,
                fullConfigurationService,
                globalTrafficShapingHandler,
                metricsHolder,
                exceptionHandler,
                pingRequestHandler,
                restrictionsConfigurationService,
                mqttConnectDecoder,
                returnMessageIdToPoolHandler,
                () -> dropOutgoingPublishesHandler,
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
                publishOutboundInterceptorHandler,
                connectInterceptorHandler,
                connackOutboundInterceptorHandler,
                disconnectInterceptorHandler,
                pubackInterceptorHandler,
                pubrecInterceptorHandler,
                pubrelInterceptorHandler,
                pubcompInterceptorHandler,
                subAckOutboundInterceptorHandler,
                unsubackOutboundInterceptorHandler,
                unsubscribeInboundInterceptorHandler,
                pingInterceptorHandler
        );

    }

    @Test
    public void test_all_provided() {

        assertNotNull(channelDependencies.getStatisticsInitializer());
        assertNotNull(channelDependencies.getNoConnectIdleHandler());
        assertNotNull(channelDependencies.getConnectHandler());
        assertNotNull(channelDependencies.getDisconnectHandler());
        assertNotNull(channelDependencies.getSubscribeHandler());
        assertNotNull(channelDependencies.getPublishUserEventReceivedHandler());
        assertNotNull(channelDependencies.getUnsubscribeHandler());
        assertNotNull(channelDependencies.getQoSSenderHandler());
        assertNotNull(channelDependencies.getQoSReceiverHandler());
        assertNotNull(channelDependencies.getChannelGroup());
        assertNotNull(channelDependencies.getConfigurationService());
        assertNotNull(channelDependencies.getGlobalTrafficShapingHandler());
        assertNotNull(channelDependencies.getMetricsHolder());
        assertNotNull(channelDependencies.getExceptionHandler());
        assertNotNull(channelDependencies.getPingRequestHandler());
        assertNotNull(channelDependencies.getConnectPersistenceUpdateHandler());
        assertNotNull(channelDependencies.getRestrictionsConfigurationService());
        assertNotNull(channelDependencies.getMqttConnectDecoder());
        assertNotNull(channelDependencies.getReturnMessageIdToPoolHandler());
        assertNotNull(channelDependencies.getMqttMessageEncoder());
        assertNotNull(channelDependencies.getDropOutgoingPublishesHandler());
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
        assertNotNull(channelDependencies.getConnectInboundInterceptorHandler());
        assertNotNull(channelDependencies.getConnackOutboundInterceptorHandler());
        assertNotNull(channelDependencies.getDisconnectInterceptorHandler());
        assertNotNull(channelDependencies.getPubackInterceptorHandler());
        assertNotNull(channelDependencies.getPubrecInterceptorHandler());
        assertNotNull(channelDependencies.getPubrelInterceptorHandler());
        assertNotNull(channelDependencies.getPubcompInterceptorHandler());
        assertNotNull(channelDependencies.getSubackOutboundInterceptorHandler());
        assertNotNull(channelDependencies.getUnsubackOutboundInterceptorHandler());
        assertNotNull(channelDependencies.getUnsubscribeInboundInterceptorHandler());
        assertNotNull(channelDependencies.getPingInterceptorHandler());
    }
}