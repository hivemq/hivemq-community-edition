package com.hivemq.extensions.services.auth;

import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.handler.auth.MqttAuthSender;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.connect.ConnectHandler;
import com.hivemq.mqtt.handler.disconnect.Mqtt5ServerDisconnector;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestMessageUtil;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Florian Limpöck
*/
@SuppressWarnings({"NullabilityAnnotations", "ResultOfMethodCallIgnored"})
public class AuthTaskContextTest {


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
    private Mqtt5ServerDisconnector disconnector;

    @Mock
    private AuthTaskOutput output;

    @Mock
    private MetricsHolder metricsHolder;

    private AuthTaskContext authTaskContext;

    private AUTH auth;
    private EmbeddedChannel embeddedChannel;
    private ModifiableDefaultPermissionsImpl defaultPermissions;
    private ModifiableClientSettingsImpl modifiableClientSettings;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        embeddedChannel = new EmbeddedChannel();
        embeddedChannel.runPendingTasks();

        defaultPermissions = new ModifiableDefaultPermissionsImpl();
        modifiableClientSettings = new ModifiableClientSettingsImpl(30);

        when(ctx.channel()).thenReturn(embeddedChannel);
        when(ctx.executor()).thenReturn(embeddedChannel.eventLoop());
        when(output.getAuthenticationState()).thenReturn(AuthenticationState.NEXT_EXTENSION_OR_DEFAULT);
        when(output.getAuthenticationData()).thenReturn(ByteBuffer.wrap("auth data".getBytes()).asReadOnlyBuffer());
        when(output.getClientSettings()).thenReturn(modifiableClientSettings);
        when(output.getDisconnectedReasonCode()).thenReturn(DisconnectedReasonCode.NOT_AUTHORIZED);
        when(output.isAuthenticatorPresent()).thenReturn(true);

        authTaskContext = new AuthTaskContext("client", modifiableClientSettings, defaultPermissions, ctx, authSender, connectHandler, asyncer, 1, mqttConnack, false, true, 30, disconnector, metricsHolder);
    }

    @Test(timeout = 10000)
    public void test_plugin_post_failed_timeout_auth() {

        when(output.isAsync()).thenReturn(true);
        when(output.isTimedOut()).thenReturn(true);
        when(output.getTimeoutFallback()).thenReturn(TimeoutFallback.FAILURE);

        authTaskContext.pluginPost(output);

        embeddedChannel.runPendingTasks();

        verify(output).failByTimeout();
        verify(mqttConnack, timeout(1000)).connackError(any(), anyString(), anyString(), eq(DisconnectedReasonCode.NOT_AUTHORIZED), any());
    }

    @Test(timeout = 10000)
    public void test_plugin_post_failed_timeout_re_auth() {

        authTaskContext = new AuthTaskContext("client", modifiableClientSettings, defaultPermissions, ctx, authSender, connectHandler, asyncer, 1, mqttConnack, true, true, 30, disconnector, metricsHolder);

        when(output.isAsync()).thenReturn(true);
        when(output.isTimedOut()).thenReturn(true);
        when(output.getTimeoutFallback()).thenReturn(TimeoutFallback.FAILURE);
        when(output.getReasonString()).thenReturn("Reason");

        authTaskContext.pluginPost(output);

        embeddedChannel.runPendingTasks();

        verify(output).failByTimeout();
        verify(disconnector, timeout(1000)).disconnect(any(), anyString(), anyString(), any(Mqtt5DisconnectReasonCode.class), anyString());
    }

    @Test(timeout = 10000)
    public void test_plugin_post_not_done() {

        authTaskContext = new AuthTaskContext("client", modifiableClientSettings, defaultPermissions, ctx, authSender, connectHandler, asyncer, 2, mqttConnack, false, true, 30, disconnector, metricsHolder);
        authTaskContext.pluginPost(output);

        verify(output, times(1)).getChangedUserProperties();
    }

    @Test(timeout = 10000)
    public void test_plugin_undecided_auth_present() {

        when(output.getAuthenticationState()).thenReturn(AuthenticationState.UNDECIDED);

        authTaskContext = new AuthTaskContext("client", modifiableClientSettings, defaultPermissions, ctx, authSender, connectHandler, asyncer, 1, mqttConnack, false, true, 30, disconnector, metricsHolder);
        authTaskContext.pluginPost(output);

        verify(output, times(1)).getAuthenticationState();
        verify(output).failAuthentication();
    }

    @Test(timeout = 10000)
    public void test_plugin_post_success_connect_user_props_changed() {

        when(output.getChangedUserProperties()).thenReturn(Mqtt5UserProperties.of(MqttUserProperty.of("key", "val")).getPluginUserProperties());
        when(output.getAuthenticationState()).thenReturn(AuthenticationState.SUCCESS);

        embeddedChannel.attr(ChannelAttributes.AUTH_CONNECT).set(TestMessageUtil.createFullMqtt5Connect());

        authTaskContext.pluginPost(output);

        final Mqtt5UserProperties userProperties = ctx.channel().attr(ChannelAttributes.AUTH_USER_PROPERTIES).get();
        assertEquals(1, userProperties.size());

        embeddedChannel.runPendingTasks();

        verify(connectHandler, timeout(1000)).connectSuccessfulAuthenticated(any(), any(), any());

        final ByteBuffer authData = ctx.channel().attr(ChannelAttributes.AUTH_DATA).get();
        assertNotNull(authData);

    }

    @Test(timeout = 10000)
    public void test_plugin_post_success_reAuth_user_props_changed() {

        authTaskContext = new AuthTaskContext("client", modifiableClientSettings, defaultPermissions, ctx, authSender, connectHandler, asyncer, 1, mqttConnack, true, true, 30, disconnector, metricsHolder);

        when(output.getChangedUserProperties()).thenReturn(Mqtt5UserProperties.of(MqttUserProperty.of("key", "val")).getPluginUserProperties());
        when(output.getAuthenticationState()).thenReturn(AuthenticationState.SUCCESS);

        authTaskContext.pluginPost(output);

        final Mqtt5UserProperties userProperties = ctx.channel().attr(ChannelAttributes.AUTH_USER_PROPERTIES).get();
        assertEquals(1, userProperties.size());

        embeddedChannel.runPendingTasks();

        verify(authSender, timeout(1000)).sendAuth(eq(embeddedChannel),
                isNotNull(),
                eq(Mqtt5AuthReasonCode.SUCCESS),
                eq(userProperties),
                any());

        final ByteBuffer authData = ctx.channel().attr(ChannelAttributes.AUTH_DATA).get();
        assertNull(authData);

    }

    @Test(timeout = 10000)
    public void test_plugin_post_fail() {

        when(output.getAuthenticationState()).thenReturn(AuthenticationState.FAILED);

        authTaskContext.pluginPost(output);

        embeddedChannel.runPendingTasks();

        verify(mqttConnack, timeout(1000)).connackError(any(), anyString(), anyString(), eq(DisconnectedReasonCode.NOT_AUTHORIZED), any());

        final ByteBuffer authData = ctx.channel().attr(ChannelAttributes.AUTH_DATA).get();
        assertNull(authData);

    }

    @Test(timeout = 10000)
    public void test_plugin_post_success_reAuth_without_data() {

        authTaskContext = new AuthTaskContext("client", modifiableClientSettings, defaultPermissions, ctx, authSender, connectHandler, asyncer, 1, mqttConnack, true, true, 30, disconnector, metricsHolder);

        when(output.getAuthenticationData()).thenReturn(null);
        when(output.getAuthenticationState()).thenReturn(AuthenticationState.SUCCESS);

        authTaskContext.pluginPost(output);

        embeddedChannel.runPendingTasks();

        verify(authSender, timeout(1000)).sendAuth(eq(embeddedChannel),
                isNull(),
                eq(Mqtt5AuthReasonCode.SUCCESS),
                isNull(),
                any());

    }

    @Test(timeout = 10000)
    public void test_plugin_post_continue_without_data() {

        authTaskContext = new AuthTaskContext("client", modifiableClientSettings, defaultPermissions, ctx, authSender, connectHandler, asyncer, 1, mqttConnack, true, true, 30, disconnector, metricsHolder);

        when(output.getAuthenticationData()).thenReturn(null);
        when(output.getAuthenticationState()).thenReturn(AuthenticationState.CONTINUE);

        authTaskContext.pluginPost(output);

        embeddedChannel.runPendingTasks();

        verify(authSender, timeout(1000)).sendAuth(eq(embeddedChannel),
                isNull(),
                eq(Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION),
                isNull(),
                any());

    }

    @Test(timeout = 10000)
    public void test_plugin_post_continue_with_data() {

        authTaskContext = new AuthTaskContext("client", modifiableClientSettings, defaultPermissions, ctx, authSender, connectHandler, asyncer, 1, mqttConnack, true, true, 30, disconnector, metricsHolder);

        when(output.getAuthenticationState()).thenReturn(AuthenticationState.CONTINUE);

        authTaskContext.pluginPost(output);

        embeddedChannel.runPendingTasks();

        verify(authSender, timeout(1000)).sendAuth(eq(embeddedChannel),
                isNotNull(),
                eq(Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION),
                isNull(),
                any());

    }
}