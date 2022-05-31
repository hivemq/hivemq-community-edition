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

import com.google.common.collect.ImmutableMap;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ClientState;
import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.bootstrap.netty.ChannelHandlerNames;
import com.hivemq.codec.decoder.MQTTMessageDecoder;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.impl.SecurityConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationServiceImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthenticatorProvider;
import com.hivemq.extension.sdk.api.services.auth.provider.EnhancedAuthenticatorProvider;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.auth.parameter.ModifiableClientSettingsImpl;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import com.hivemq.extensions.client.parameter.ServerInformationImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.services.auth.Authenticators;
import com.hivemq.extensions.services.auth.WrappedAuthenticatorProvider;
import com.hivemq.mqtt.handler.auth.AuthInProgressMessageHandler;
import com.hivemq.mqtt.handler.auth.MqttAuthSender;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.connect.ConnectHandler;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import util.TestMessageUtil;

import java.util.Collections;
import java.util.Map;

import static com.hivemq.extensions.handler.PluginAuthenticatorServiceImpl.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class PluginAuthenticatorServiceImplTest {

    private final @NotNull MqttConnacker mqttConnacker = mock(MqttConnacker.class);
    private final @NotNull MqttServerDisconnector mqttServerDisconnector = mock(MqttServerDisconnector.class);
    private final @NotNull FullConfigurationService configurationService = mock(FullConfigurationService.class);
    private final @NotNull Authenticators authenticators = mock(Authenticators.class);
    private final @NotNull ChannelDependencies channelDependencies = mock(ChannelDependencies.class);
    private final @NotNull PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService = mock(PluginTaskExecutorService.class);
    private final @NotNull MqttAuthSender mqttAuthSender = mock(MqttAuthSender.class);
    private final @NotNull ConnectHandler connectHandler = mock(ConnectHandler.class);
    private final @NotNull ChannelHandlerContext channelHandlerContext = mock(ChannelHandlerContext.class);
    private final @NotNull SimpleAuthenticator simpleAuthenticator = mock(SimpleAuthenticator.class);
    private final @NotNull EnhancedAuthenticator enhancedAuthenticator = mock(EnhancedAuthenticator.class);
    private final @NotNull HiveMQExtensions extensions = mock(HiveMQExtensions.class);
    private final @NotNull IsolatedExtensionClassloader classloader1 = mock(IsolatedExtensionClassloader.class);
    private final @NotNull IsolatedExtensionClassloader classloader2 = mock(IsolatedExtensionClassloader.class);

    private @NotNull PluginAuthenticatorService pluginAuthenticatorService;
    private @NotNull EmbeddedChannel channel;
    private @NotNull ClientConnection clientConnection;

    @Before
    public void setUp() throws Exception {
        clientConnection = new ClientConnection(channel, mock(PublishFlushHandler.class));
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        final SecurityConfigurationServiceImpl securityConfig = new SecurityConfigurationServiceImpl();
        channel = new EmbeddedChannel();
        clientConnection = new ClientConnection(channel, mock(PublishFlushHandler.class));
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        clientConnection.setClientId("client");
        clientConnection.setClientReceiveMaximum(100);

        channel.pipeline().addLast(ChannelHandlerNames.MQTT_MESSAGE_DECODER, Mockito.mock(MQTTMessageDecoder.class));

        when(channelHandlerContext.pipeline()).thenReturn(channel.pipeline());
        when(configurationService.securityConfiguration()).thenReturn(securityConfig);
        when(channelHandlerContext.channel()).thenReturn(channel);
        when(channelDependencies.getAuthInProgressMessageHandler()).thenReturn(new AuthInProgressMessageHandler(
                mqttConnacker));

        pluginAuthenticatorService = new PluginAuthenticatorServiceImpl(connectHandler,
                mqttConnacker,
                mqttServerDisconnector,
                mqttAuthSender,
                configurationService,
                authenticators,
                channelDependencies,
                asyncer,
                pluginTaskExecutorService,
                extensions,
                new ServerInformationImpl(new SystemInformationImpl(), new ListenerConfigurationServiceImpl()));
    }

    @After
    public void tearDown() throws Exception {
        InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS.set(false);
    }

    @Test
    public void test_auth_connect_deny_unauthed() {
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(Collections.emptyMap());
        InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS.set(true);
        final CONNECT fullMqtt5Connect = TestMessageUtil.createFullMqtt5Connect();
        final ModifiableClientSettingsImpl clientSettings =
                new ModifiableClientSettingsImpl(fullMqtt5Connect.getReceiveMaximum(), null);

        pluginAuthenticatorService.authenticateConnect(
                channelHandlerContext,
                clientConnection,
                fullMqtt5Connect,
                clientSettings);

        verify(connectHandler).connectSuccessfulUndecided(
                channelHandlerContext,
                clientConnection,
                fullMqtt5Connect,
                clientSettings);
    }

    @Test
    public void test_auth_connect_allow_unauthed() {
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(Collections.emptyMap());
        InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS.set(false);
        final CONNECT fullMqtt5Connect = TestMessageUtil.createFullMqtt5Connect();
        final ModifiableClientSettingsImpl clientSettings =
                new ModifiableClientSettingsImpl(fullMqtt5Connect.getReceiveMaximum(), null);

        pluginAuthenticatorService.authenticateConnect(
                channelHandlerContext,
                clientConnection,
                fullMqtt5Connect,
                clientSettings);

        verify(connectHandler).connectSuccessfulUndecided(
                channelHandlerContext,
                clientConnection,
                fullMqtt5Connect,
                clientSettings);
    }

    @Test
    public void test_auth_connect_simple() {
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(createSimple());
        InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS.set(false);
        final CONNECT fullMqtt5Connect = TestMessageUtil.createFullMqtt5Connect();
        final ModifiableClientSettingsImpl clientSettings =
                new ModifiableClientSettingsImpl(fullMqtt5Connect.getReceiveMaximum(), null);

        pluginAuthenticatorService.authenticateConnect(
                channelHandlerContext,
                clientConnection,
                fullMqtt5Connect,
                clientSettings);

        verify(pluginTaskExecutorService).handlePluginInOutTaskExecution(any(), any(), any(), any());
    }

    @Test
    public void test_auth_connect_enhanced() {
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(createEnhanced());
        InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS.set(false);
        final CONNECT fullMqtt5Connect = TestMessageUtil.createFullMqtt5Connect();
        final ModifiableClientSettingsImpl clientSettings =
                new ModifiableClientSettingsImpl(fullMqtt5Connect.getReceiveMaximum(), null);

        pluginAuthenticatorService.authenticateConnect(
                channelHandlerContext,
                clientConnection,
                fullMqtt5Connect,
                clientSettings);

        verify(pluginTaskExecutorService).handlePluginInOutTaskExecution(any(), any(), any(), any());
    }

    @Test
    public void test_auth_connect_multi() {
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(createMulti());
        InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS.set(false);
        final CONNECT fullMqtt5Connect = TestMessageUtil.createFullMqtt5Connect();
        final ModifiableClientSettingsImpl clientSettings =
                new ModifiableClientSettingsImpl(fullMqtt5Connect.getReceiveMaximum(), null);

        pluginAuthenticatorService.authenticateConnect(
                channelHandlerContext,
                clientConnection,
                fullMqtt5Connect,
                clientSettings);

        verify(pluginTaskExecutorService, times(2)).handlePluginInOutTaskExecution(any(), any(), any(), any());
    }

    @Test
    public void test_auth_reauth_deny_unauthed() {
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(Map.of());
        InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS.set(true);
        clientConnection.setAuthMethod("auth method");
        clientConnection.proposeClientState(ClientState.RE_AUTHENTICATING);
        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();

        pluginAuthenticatorService.authenticateAuth(channelHandlerContext, clientConnection, auth);

        verify(mqttServerDisconnector).disconnect(channel,
                RE_AUTH_FAILED_LOG,
                ReasonStrings.RE_AUTH_FAILED_NO_AUTHENTICATOR,
                Mqtt5DisconnectReasonCode.NOT_AUTHORIZED,
                ReasonStrings.RE_AUTH_FAILED_NO_AUTHENTICATOR,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                true,
                false);
    }

    @Test
    public void test_auth_reauth_deny_unauthed_always() {
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(Map.of());
        InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS.set(true);
        clientConnection.setAuthMethod("auth method");
        clientConnection.proposeClientState(ClientState.RE_AUTHENTICATING);
        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();

        pluginAuthenticatorService.authenticateAuth(channelHandlerContext, clientConnection, auth);

        verify(mqttServerDisconnector).disconnect(channel,
                RE_AUTH_FAILED_LOG,
                ReasonStrings.RE_AUTH_FAILED_NO_AUTHENTICATOR,
                Mqtt5DisconnectReasonCode.NOT_AUTHORIZED,
                ReasonStrings.RE_AUTH_FAILED_NO_AUTHENTICATOR,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                true,
                false);
    }

    @Test
    public void test_auth_reauth_bad_method() {
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(createEnhanced());
        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();
        clientConnection.proposeClientState(ClientState.RE_AUTHENTICATING);

        pluginAuthenticatorService.authenticateAuth(channelHandlerContext, clientConnection, auth);

        verify(mqttServerDisconnector).disconnect(channel,
                DISCONNECT_BAD_AUTHENTICATION_METHOD_LOG_STATEMENT,
                "Different auth method",
                Mqtt5DisconnectReasonCode.BAD_AUTHENTICATION_METHOD,
                String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_AUTH_METHOD, auth.getType().name()),
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                true,
                false);
    }

    @Test
    public void test_auth_reauth_enhanced() {
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(createEnhanced());
        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();
        clientConnection.setAuthMethod(auth.getAuthMethod());
        clientConnection.proposeClientState(ClientState.RE_AUTHENTICATING);

        pluginAuthenticatorService.authenticateAuth(channelHandlerContext, clientConnection, auth);

        verify(pluginTaskExecutorService).handlePluginInOutTaskExecution(any(), any(), any(), any());
    }

    @Test
    public void test_auth_reauth_multi() {
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(createMulti());
        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();
        clientConnection.setAuthMethod(auth.getAuthMethod());
        clientConnection.proposeClientState(ClientState.RE_AUTHENTICATING);

        pluginAuthenticatorService.authenticateAuth(channelHandlerContext, clientConnection, auth);

        // only enhanced should be called
        verify(pluginTaskExecutorService, times(1)).handlePluginInOutTaskExecution(any(), any(), any(), any());
    }

    @Test
    public void test_auth_deny_unauthed() {
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(Map.of());
        InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS.set(true);
        clientConnection.setAuthMethod("auth method");
        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();

        pluginAuthenticatorService.authenticateAuth(channelHandlerContext, clientConnection, auth);

        verify(mqttConnacker).connackError(channel,
                AUTH_FAILED_LOG,
                ReasonStrings.AUTH_FAILED_NO_AUTHENTICATOR,
                Mqtt5ConnAckReasonCode.NOT_AUTHORIZED,
                ReasonStrings.AUTH_FAILED_NO_AUTHENTICATOR,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                true);
    }

    @Test
    public void test_auth_deny_unauthed_always() {
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(Map.of());
        InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS.set(false);
        clientConnection.setAuthMethod("auth method");
        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();

        pluginAuthenticatorService.authenticateAuth(channelHandlerContext, clientConnection, auth);

        verify(mqttConnacker).connackError(channel,
                AUTH_FAILED_LOG,
                ReasonStrings.AUTH_FAILED_NO_AUTHENTICATOR,
                Mqtt5ConnAckReasonCode.NOT_AUTHORIZED,
                ReasonStrings.AUTH_FAILED_NO_AUTHENTICATOR,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                true);
    }

    @Test
    public void test_auth_bad_method() {
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(createEnhanced());
        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();

        pluginAuthenticatorService.authenticateAuth(channelHandlerContext, clientConnection, auth);

        verify(mqttConnacker).connackError(eq(channel),
                eq(CONNACK_BAD_AUTHENTICATION_METHOD_LOG_STATEMENT),
                eq("Different auth method"),
                eq(Mqtt5ConnAckReasonCode.BAD_AUTHENTICATION_METHOD),
                eq(String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_AUTH_METHOD, auth.getType().name())),
                eq(Mqtt5UserProperties.NO_USER_PROPERTIES),
                eq(true));
    }

    @Test
    public void test_auth_enhanced() {
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(createEnhanced());
        final CONNECT connect = TestMessageUtil.createFullMqtt5Connect();
        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();
        clientConnection.setAuthConnect(connect);
        clientConnection.setAuthMethod(auth.getAuthMethod());

        pluginAuthenticatorService.authenticateAuth(channelHandlerContext, clientConnection, auth);

        verify(pluginTaskExecutorService).handlePluginInOutTaskExecution(any(), any(), any(), any());
    }

    @Test
    public void test_auth_multi() {
        when(authenticators.getAuthenticatorProviderMap()).thenReturn(createMulti());
        final CONNECT connect = TestMessageUtil.createFullMqtt5Connect();
        final AUTH auth = TestMessageUtil.createFullMqtt5Auth();
        clientConnection.setAuthConnect(connect);
        clientConnection.setAuthMethod(auth.getAuthMethod());

        pluginAuthenticatorService.authenticateAuth(channelHandlerContext, clientConnection, auth);

        // only enhanced should be called
        verify(pluginTaskExecutorService, times(1)).handlePluginInOutTaskExecution(any(), any(), any(), any());
    }

    private Map<String, WrappedAuthenticatorProvider> createSimple() {
        return ImmutableMap.of(
                "extension1",
                new WrappedAuthenticatorProvider((AuthenticatorProvider) (i -> simpleAuthenticator), classloader1));
    }

    private Map<String, WrappedAuthenticatorProvider> createEnhanced() {
        return ImmutableMap.of(
                "extension1",
                new WrappedAuthenticatorProvider(
                        (EnhancedAuthenticatorProvider) (i -> enhancedAuthenticator),
                        classloader1));
    }

    private Map<String, WrappedAuthenticatorProvider> createMulti() {
        return ImmutableMap.of("extension1",
                new WrappedAuthenticatorProvider((AuthenticatorProvider) (i -> simpleAuthenticator), classloader1),
                "extension2",
                new WrappedAuthenticatorProvider(
                        (EnhancedAuthenticatorProvider) (i -> enhancedAuthenticator),
                        classloader2));
    }
}
