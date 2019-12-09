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

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.bootstrap.netty.ChannelHandlerNames;
import com.hivemq.codec.decoder.MQTTMessageDecoder;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.impl.SecurityConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationServiceImpl;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthenticatorProvider;
import com.hivemq.extension.sdk.api.services.auth.provider.EnhancedAuthenticatorProvider;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.parameter.AuthenticatorProviderInputFactory;
import com.hivemq.extensions.client.parameter.ServerInformationImpl;
import com.hivemq.extensions.events.OnAuthFailedEvent;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.services.auth.Authenticators;
import com.hivemq.extensions.services.auth.ModifiableClientSettingsImpl;
import com.hivemq.extensions.services.auth.WrappedAuthenticatorProvider;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.handler.auth.AuthInProgressMessageHandler;
import com.hivemq.mqtt.handler.auth.MqttAuthSender;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.connect.ConnectHandler;
import com.hivemq.mqtt.handler.disconnect.Mqtt5ServerDisconnector;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import util.TestMessageUtil;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import static com.hivemq.extensions.handler.PluginAuthenticatorServiceImpl.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Florian Limpöck
*/
@SuppressWarnings("NullabilityAnnotations")
public class PluginAuthenticatorServiceImplTest {

    @Mock
    private MqttConnacker mqttConnacker;
    @Mock
    private Mqtt5ServerDisconnector mqttDisconnectUtil;
    @Mock
    private FullConfigurationService configurationService;
    @Mock
    private Authenticators authenticators;
    @Mock
    private ChannelDependencies channelDependencies;
    @Mock
    private PluginOutPutAsyncer asyncer;
    @Mock
    private PluginTaskExecutorService pluginTaskExecutorService;
    @Mock
    private MqttAuthSender mqttAuthSender;
    @Mock
    private ConnectHandler connectHandler;
    @Mock
    private ChannelHandlerContext channelHandlerContext;

    @Mock
    private SimpleAuthenticator simpleAuthenticator;

    @Mock
    private EnhancedAuthenticator enhancedAuthenticator;

    @Mock
    private HiveMQExtensions extensions;


    @Mock
    private IsolatedPluginClassloader classloader1;
    @Mock
    private IsolatedPluginClassloader classloader2;

    private AuthenticatorProviderInputFactory authenticatorProviderInputFactory;
    private PluginAuthenticatorService pluginAuthenticatorService;
    private SecurityConfigurationServiceImpl securityConfig;
    private EmbeddedChannel embeddedChannel;


    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        securityConfig = new SecurityConfigurationServiceImpl();
        authenticatorProviderInputFactory = new AuthenticatorProviderInputFactory(new ServerInformationImpl(new SystemInformationImpl(), new ListenerConfigurationServiceImpl()));
        embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        embeddedChannel.attr(ChannelAttributes.CLIENT_ID).set("client");
        embeddedChannel.attr(ChannelAttributes.CLIENT_RECEIVE_MAXIMUM).set(100);

        embeddedChannel.pipeline().addLast(ChannelHandlerNames.MQTT_MESSAGE_DECODER, Mockito.mock(MQTTMessageDecoder.class));

        when(channelHandlerContext.pipeline()).thenReturn(embeddedChannel.pipeline());
        when(configurationService.securityConfiguration()).thenReturn(securityConfig);
        when(channelHandlerContext.channel()).thenReturn(embeddedChannel);
        when(channelDependencies.getAuthInProgressMessageHandler()).thenReturn(new AuthInProgressMessageHandler(mqttConnacker));

        pluginAuthenticatorService = new PluginAuthenticatorServiceImpl(mqttConnacker,
                mqttDisconnectUtil,
                configurationService,
                authenticators,
                channelDependencies,
                asyncer,
                new MetricsHolder(new MetricRegistry()),
                pluginTaskExecutorService,
                authenticatorProviderInputFactory,
                mqttAuthSender,
                extensions);
    }

    @After
    public void tearDown() throws Exception {
        InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS.set(false);
    }

    @Test
    public void test_auth_connect_deny_unauthed() {

        when(authenticators.getAuthenticatorProviderMap()).thenReturn(Collections.emptyMap());
        InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS.set(true);

        pluginAuthenticatorService = new PluginAuthenticatorServiceImpl(mqttConnacker,
                mqttDisconnectUtil,
                configurationService,
                authenticators,
                channelDependencies,
                asyncer,
                new MetricsHolder(new MetricRegistry()),
                pluginTaskExecutorService,
                authenticatorProviderInputFactory,
                mqttAuthSender,
                extensions);

        final CONNECT fullMqtt5Connect = TestMessageUtil.createFullMqtt5Connect();

        pluginAuthenticatorService.authenticateConnect(connectHandler, channelHandlerContext, fullMqtt5Connect, new ModifiableClientSettingsImpl(fullMqtt5Connect.getReceiveMaximum()));

        verify(mqttConnacker).connackError(
                eq(embeddedChannel),
                eq(PluginAuthenticatorServiceImpl.CONNACK_NO_AUTHENTICATION_LOG_STATEMENT),
                eq("Disconnected not authorized"),
                eq(Mqtt5ConnAckReasonCode.NOT_AUTHORIZED),
                eq(Mqtt3ConnAckReturnCode.REFUSED_NOT_AUTHORIZED),
                eq(ReasonStrings.CONNACK_NOT_AUTHORIZED_NO_AUTHENTICATOR),
                any(OnAuthFailedEvent.class));

    }

    @Test
    public void test_auth_connect_allow_unauthed() {

        when(authenticators.getAuthenticatorProviderMap()).thenReturn(Collections.emptyMap());
        final CONNECT fullMqtt5Connect = TestMessageUtil.createFullMqtt5Connect();
        final ModifiableClientSettingsImpl clientSettings = new ModifiableClientSettingsImpl(fullMqtt5Connect.getReceiveMaximum());

        pluginAuthenticatorService.authenticateConnect(connectHandler, channelHandlerContext, fullMqtt5Connect, clientSettings);

        verify(connectHandler).connectSuccessfulUnauthenticated(channelHandlerContext, fullMqtt5Connect, clientSettings);

    }

    @Test
    public void test_auth_connect_simple() {

        when(authenticators.getAuthenticatorProviderMap()).thenReturn(createSimple());
        final CONNECT fullMqtt5Connect = TestMessageUtil.createFullMqtt5Connect();
        final ModifiableClientSettingsImpl clientSettings = new ModifiableClientSettingsImpl(fullMqtt5Connect.getReceiveMaximum());

        pluginAuthenticatorService.authenticateConnect(connectHandler, channelHandlerContext, fullMqtt5Connect, clientSettings);

        verify(pluginTaskExecutorService).handlePluginInOutTaskExecution(any(PluginInOutTaskContext.class), any(Supplier.class), any(Supplier.class), any(PluginInOutTask.class));

    }

    @Test
    public void test_auth_connect_enhanced() {

        when(authenticators.getAuthenticatorProviderMap()).thenReturn(createEnhanced());
        final CONNECT fullMqtt5Connect = TestMessageUtil.createFullMqtt5Connect();
        final ModifiableClientSettingsImpl clientSettings = new ModifiableClientSettingsImpl(fullMqtt5Connect.getReceiveMaximum());

        pluginAuthenticatorService.authenticateConnect(connectHandler, channelHandlerContext, fullMqtt5Connect, clientSettings);

        verify(pluginTaskExecutorService).handlePluginInOutTaskExecution(any(PluginInOutTaskContext.class), any(Supplier.class), any(Supplier.class), any(PluginInOutTask.class));

    }

    @Test
    public void test_auth_connect_multi() {

        when(authenticators.getAuthenticatorProviderMap()).thenReturn(createMulti());
        final CONNECT fullMqtt5Connect = TestMessageUtil.createFullMqtt5Connect();
        final ModifiableClientSettingsImpl clientSettings = new ModifiableClientSettingsImpl(fullMqtt5Connect.getReceiveMaximum());

        pluginAuthenticatorService.authenticateConnect(connectHandler, channelHandlerContext, fullMqtt5Connect, clientSettings);

        verify(pluginTaskExecutorService, times(2)).handlePluginInOutTaskExecution(any(PluginInOutTaskContext.class), any(Supplier.class), any(Supplier.class), any(PluginInOutTask.class));

    }

    @Test
    public void test_auth_reauth_deny_unauthed() {

        when(authenticators.isEnhancedAvailable()).thenReturn(false);
        InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS.set(true);

        pluginAuthenticatorService = new PluginAuthenticatorServiceImpl(mqttConnacker,
                mqttDisconnectUtil,
                configurationService,
                authenticators,
                channelDependencies,
                asyncer,
                new MetricsHolder(new MetricRegistry()),
                pluginTaskExecutorService,
                authenticatorProviderInputFactory,
                mqttAuthSender,
                extensions);

        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();

        pluginAuthenticatorService.authenticateReAuth(channelHandlerContext, auth);

        verify(mqttDisconnectUtil).disconnect(
                embeddedChannel, DISCONNECT_NO_AUTHENTICATION_LOG_STATEMENT, "Disconnected not authorized",
                Mqtt5DisconnectReasonCode.NOT_AUTHORIZED,
                ReasonStrings.CONNACK_NOT_AUTHORIZED_NO_AUTHENTICATOR);

    }

    @Test
    public void test_auth_reauth_allow_unauthed() {

        when(authenticators.isEnhancedAvailable()).thenReturn(false);
        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();

        pluginAuthenticatorService.authenticateReAuth(channelHandlerContext, auth);

        verify(mqttAuthSender).sendAuth(embeddedChannel, null, Mqtt5AuthReasonCode.SUCCESS, auth.getUserProperties(), null);

    }

    @Test
    public void test_auth_reauth_bad_method() {

        when(authenticators.isEnhancedAvailable()).thenReturn(true);
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(createEnhanced());
        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();

        pluginAuthenticatorService.authenticateReAuth(channelHandlerContext, auth);

        verify(mqttDisconnectUtil).disconnect(
                embeddedChannel, DISCONNECT_BAD_AUTHENTICATION_METHOD_LOG_STATEMENT, "Disconnected not authorized",
                Mqtt5DisconnectReasonCode.BAD_AUTHENTICATION_METHOD,
                String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_AUTH_METHOD, auth.getType().name()));

    }

    @Test
    public void test_auth_reauth_enhanced() {

        when(authenticators.isEnhancedAvailable()).thenReturn(true);
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(createEnhanced());
        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();
        embeddedChannel.attr(ChannelAttributes.AUTH_METHOD).set(auth.getAuthMethod());

        pluginAuthenticatorService.authenticateReAuth(channelHandlerContext, auth);

        verify(pluginTaskExecutorService).handlePluginInOutTaskExecution(any(PluginInOutTaskContext.class), any(Supplier.class), any(Supplier.class), any(PluginInOutTask.class));

    }

    @Test
    public void test_auth_reauth_multi() {

        when(authenticators.isEnhancedAvailable()).thenReturn(true);
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(createMulti());
        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();
        embeddedChannel.attr(ChannelAttributes.AUTH_METHOD).set(auth.getAuthMethod());

        pluginAuthenticatorService.authenticateReAuth(channelHandlerContext, auth);

        //Only Enhanced should be called.
        verify(pluginTaskExecutorService, times(1)).handlePluginInOutTaskExecution(any(PluginInOutTaskContext.class), any(Supplier.class), any(Supplier.class), any(PluginInOutTask.class));

    }

    @Test
    public void test_auth_deny_unauthed() {

        when(authenticators.isEnhancedAvailable()).thenReturn(false);
        InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS.set(true);

        pluginAuthenticatorService = new PluginAuthenticatorServiceImpl(mqttConnacker,
                mqttDisconnectUtil,
                configurationService,
                authenticators,
                channelDependencies,
                asyncer,
                new MetricsHolder(new MetricRegistry()),
                pluginTaskExecutorService,
                authenticatorProviderInputFactory,
                mqttAuthSender,
                extensions);

        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();

        pluginAuthenticatorService.authenticateAuth(connectHandler, channelHandlerContext, auth, false);

        verify(mqttConnacker).connackError(
                eq(embeddedChannel),
                eq(CONNACK_NO_AUTHENTICATION_LOG_STATEMENT),
                eq("Disconnected not authorized"),
                eq(Mqtt5ConnAckReasonCode.NOT_AUTHORIZED),
                eq(Mqtt3ConnAckReturnCode.REFUSED_NOT_AUTHORIZED),
                eq(ReasonStrings.CONNACK_NOT_AUTHORIZED_NO_AUTHENTICATOR),
                any(OnAuthFailedEvent.class));

    }

    @Test
    public void test_auth_allow_unauthed() {

        when(authenticators.isEnhancedAvailable()).thenReturn(false);
        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();

        pluginAuthenticatorService.authenticateAuth(connectHandler, channelHandlerContext, auth, false);

        verify(mqttAuthSender).sendAuth(embeddedChannel, null, Mqtt5AuthReasonCode.SUCCESS, auth.getUserProperties(), null);

    }

    @Test
    public void test_auth_bad_method() {

        when(authenticators.isEnhancedAvailable()).thenReturn(true);
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(createEnhanced());
        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();

        pluginAuthenticatorService.authenticateAuth(connectHandler, channelHandlerContext, auth, false);

        verify(mqttConnacker).connackError(
                eq(embeddedChannel),
                eq(CONNACK_BAD_AUTHENTICATION_METHOD_LOG_STATEMENT),
                eq("Disconnected not authorized"),
                eq(Mqtt5ConnAckReasonCode.BAD_AUTHENTICATION_METHOD),
                eq(null),
                eq(String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_AUTH_METHOD, auth.getType().name())),
                any(OnAuthFailedEvent.class));

    }

    @Test
    public void test_auth_enhanced() {

        when(authenticators.isEnhancedAvailable()).thenReturn(true);
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(createEnhanced());
        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();
        embeddedChannel.attr(ChannelAttributes.AUTH_METHOD).set(auth.getAuthMethod());

        pluginAuthenticatorService.authenticateAuth(connectHandler, channelHandlerContext, auth, false);

        verify(pluginTaskExecutorService).handlePluginInOutTaskExecution(any(PluginInOutTaskContext.class), any(Supplier.class), any(Supplier.class), any(PluginInOutTask.class));

    }

    @Test
    public void test_auth_multi() {

        when(authenticators.isEnhancedAvailable()).thenReturn(true);
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(createMulti());
        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();
        embeddedChannel.attr(ChannelAttributes.AUTH_METHOD).set(auth.getAuthMethod());

        pluginAuthenticatorService.authenticateAuth(connectHandler, channelHandlerContext, auth, false);

        //Only Enhanced should be called.
        verify(pluginTaskExecutorService, times(1)).handlePluginInOutTaskExecution(any(PluginInOutTaskContext.class), any(Supplier.class), any(Supplier.class), any(PluginInOutTask.class));

    }

    private Map<String, WrappedAuthenticatorProvider> createSimple() {
        return ImmutableMap.of("extension1", new WrappedAuthenticatorProvider((AuthenticatorProvider) (i -> simpleAuthenticator), classloader1));
    }

    private Map<String, WrappedAuthenticatorProvider> createEnhanced() {
        return ImmutableMap.of("extension1", new WrappedAuthenticatorProvider((EnhancedAuthenticatorProvider) (i -> enhancedAuthenticator), classloader1));
    }

    private Map<String, WrappedAuthenticatorProvider> createMulti() {
        return ImmutableMap.of("extension1", new WrappedAuthenticatorProvider((AuthenticatorProvider) (i -> simpleAuthenticator), classloader1),
                "extension2", new WrappedAuthenticatorProvider((EnhancedAuthenticatorProvider) (i -> enhancedAuthenticator), classloader2));
    }
}