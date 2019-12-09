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

package com.hivemq.extensions.services.auth;

import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.mqtt.handler.auth.MqttAuthSender;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.connect.ConnectHandler;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.EventExecutor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestMessageUtil;

import java.nio.ByteBuffer;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Florian Limpöck
*/
public class ConnectEnhancedAuthTaskContextTest {

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private MqttAuthSender authSender;

    @Mock
    private ConnectHandler connectHandler;

    @Mock
    private PluginOutPutAsyncer asyncer;

    @Mock
    private MqttConnacker mqttConnack;

    @Mock
    private AuthTaskOutput output;

    private ConnectEnhancedAuthTaskContext context;

    private CONNECT connect;
    private EmbeddedChannel embeddedChannel;
    private AuthenticationContext authenticationContext;
    private ModifiableClientSettingsImpl clientSettings;
    private final ModifiableDefaultPermissions permissions = new ModifiableDefaultPermissionsImpl();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        embeddedChannel = new EmbeddedChannel();
        embeddedChannel.runPendingTasks();
        connect = TestMessageUtil.createFullMqtt5Connect();
        clientSettings = new ModifiableClientSettingsImpl(connect.getReceiveMaximum());

        when(ctx.channel()).thenReturn(embeddedChannel);
        when(ctx.executor()).thenReturn(embeddedChannel.eventLoop());
        when(output.getAuthenticationState()).thenReturn(AuthenticationState.NEXT_EXTENSION_OR_DEFAULT);
        when(output.getAuthenticationData()).thenReturn(ByteBuffer.wrap("auth data".getBytes()).asReadOnlyBuffer());
        when(output.getDisconnectedReasonCode()).thenReturn(DisconnectedReasonCode.NOT_AUTHORIZED);
        when(output.getClientSettings()).thenReturn(clientSettings);
        when(output.isAuthenticatorPresent()).thenReturn(true);

        authenticationContext = new AuthenticationContext();
        context = new ConnectEnhancedAuthTaskContext("client", connectHandler, mqttConnack, ctx, authSender, connect, asyncer, 1, true, 30, clientSettings, permissions, authenticationContext);
    }

    @Test(timeout = 10000)
    public void test_plugin_post_failed_timeout_auth() {

        when(output.isAsync()).thenReturn(true);
        when(output.isTimedOut()).thenReturn(true);
        when(output.getTimeoutFallback()).thenReturn(TimeoutFallback.FAILURE);

        context.pluginPost(output);

        embeddedChannel.runPendingTasks();

        verify(output).failByTimeout();
        verify(mqttConnack, timeout(1000)).connackError(any(), anyString(), anyString(), eq(DisconnectedReasonCode.NOT_AUTHORIZED), any());
    }

    @Test(timeout = 10000)
    public void test_plugin_post_not_done() {

        context = new ConnectEnhancedAuthTaskContext("client", connectHandler, mqttConnack, ctx, authSender, connect, asyncer, 2, true, 30, clientSettings, permissions, authenticationContext);
        context.pluginPost(output);

        verify(output, times(1)).getChangedUserProperties();
    }

    @Test(timeout = 10000)
    public void test_plugin_rejected() {

        context = new ConnectEnhancedAuthTaskContext("client", connectHandler, mqttConnack, ctx, authSender, connect, asyncer, 1, true, 30, clientSettings, permissions, authenticationContext);
        final EventExecutor mock = mock(EventExecutor.class);
        when(ctx.executor()).thenReturn(mock);
        when(ctx.executor().isShutdown()).thenReturn(false);
        doThrow(new RejectedExecutionException("rejected")).when(mock).execute(any(Runnable.class));

        context.pluginPost(output);

        embeddedChannel.runPendingTasks();
        verify(mqttConnack, never()).connackError(any(), anyString(), anyString(), any(Mqtt5ConnAckReasonCode.class), any(Mqtt3ConnAckReturnCode.class), any());
    }


    @Test(timeout = 10000)
    public void test_plugin_undecided_auth_present() {

        when(output.getAuthenticationState()).thenReturn(AuthenticationState.UNDECIDED);

        context = new ConnectEnhancedAuthTaskContext("client", connectHandler, mqttConnack, ctx, authSender, connect, asyncer, 1, true, 30, clientSettings, permissions, authenticationContext);
        context.pluginPost(output);

        verify(output, times(1)).getAuthenticationState();
        verify(output).failAuthentication();
    }

    @Test(timeout = 10000)
    public void test_plugin_undecided_auth_absent() {

        when(output.getAuthenticationState()).thenReturn(AuthenticationState.UNDECIDED);
        when(output.isAuthenticatorPresent()).thenReturn(false);
        when(output.getClientSettings()).thenReturn(clientSettings);

        context = new ConnectEnhancedAuthTaskContext("client", connectHandler, mqttConnack, ctx, authSender, connect, asyncer, 1, true, 30, clientSettings, permissions, authenticationContext);
        context.pluginPost(output);

        embeddedChannel.runPendingTasks();

        verify(connectHandler).connectSuccessfulUnauthenticated(ctx, connect, clientSettings);
    }


    @Test(timeout = 10000)
    public void test_plugin_post_success_connect_user_props_changed() {

        when(output.getChangedUserProperties()).thenReturn(Mqtt5UserProperties.of(MqttUserProperty.of("key", "val")).getPluginUserProperties());
        when(output.getAuthenticationState()).thenReturn(AuthenticationState.SUCCESS);

        embeddedChannel.attr(ChannelAttributes.AUTH_CONNECT).set(TestMessageUtil.createFullMqtt5Connect());

        context.pluginPost(output);

        final Mqtt5UserProperties userProperties = ctx.channel().attr(ChannelAttributes.AUTH_USER_PROPERTIES).get();
        assertEquals(1, userProperties.size());

        embeddedChannel.runPendingTasks();

        verify(connectHandler, timeout(1000)).connectSuccessfulAuthenticated(any(), any(), any());

        final ByteBuffer authData = ctx.channel().attr(ChannelAttributes.AUTH_DATA).get();
        assertNotNull(authData);

    }

    @Test(timeout = 10000)
    public void test_plugin_post_fail() {

        when(output.getAuthenticationState()).thenReturn(AuthenticationState.FAILED);

        context.pluginPost(output);

        embeddedChannel.runPendingTasks();

        verify(mqttConnack, timeout(1000)).connackError(any(), anyString(), anyString(), eq(DisconnectedReasonCode.NOT_AUTHORIZED), any());

        final ByteBuffer authData = ctx.channel().attr(ChannelAttributes.AUTH_DATA).get();
        assertNull(authData);

    }

    @Test(timeout = 10000)
    public void test_plugin_post_continue_with_method() {

        embeddedChannel.attr(ChannelAttributes.AUTH_METHOD).set(connect.getAuthMethod());
        context = new ConnectEnhancedAuthTaskContext("client", connectHandler, mqttConnack, ctx, authSender, connect, asyncer, 1, true, 30, clientSettings, permissions, authenticationContext);

        when(output.getAuthenticationState()).thenReturn(AuthenticationState.CONTINUE);

        context.pluginPost(output);

        embeddedChannel.runPendingTasks();

        verify(authSender, timeout(1000)).sendAuth(eq(embeddedChannel),
                isNotNull(),
                eq(Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION),
                eq(connect.getUserProperties()),
                any());

    }

    @Test(timeout = 10000)
    public void test_plugin_post_continue_without_method() {

        embeddedChannel.attr(ChannelAttributes.AUTH_METHOD).set(null);
        context = new ConnectEnhancedAuthTaskContext("client", connectHandler, mqttConnack, ctx, authSender, connect, asyncer, 1, true, 30, clientSettings, permissions, authenticationContext);

        when(output.getAuthenticationState()).thenReturn(AuthenticationState.CONTINUE);

        context.pluginPost(output);

        embeddedChannel.runPendingTasks();

        verify(mqttConnack, timeout(1000)).connackError(any(), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.NOT_AUTHORIZED), eq(Mqtt3ConnAckReturnCode.REFUSED_NOT_AUTHORIZED), any());

    }

}