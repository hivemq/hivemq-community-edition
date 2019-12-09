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

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.auth.Authenticator;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.extensions.packets.general.ReasonCodeUtil;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.connect.ConnectHandler;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import util.TestChannelAttribute;

import java.time.Duration;

import static com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode.NOT_AUTHORIZED;
import static com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode.UNSPECIFIED_ERROR;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

/**
 * @author Georg Held
 */
@SuppressWarnings("NullabilityAnnotations")
public class ConnectSimpleAuthTaskContextTest {

    @Mock
    private ConnectHandler connectHandler;

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private Channel channel;

    @Mock
    private Attribute<Mqtt5UserProperties> userPropertiesAttribute;

    @Mock
    private MqttConnacker connacker;

    @Mock
    private PluginOutPutAsyncer asyncer;

    @Mock
    private Attribute<Boolean> eventSentAttribute;

    private CONNECT connect;

    private final ModifiableClientSettingsImpl clientSettings = new ModifiableClientSettingsImpl(65535);
    private final ModifiableDefaultPermissions permissions = new ModifiableDefaultPermissionsImpl();

    private final ImmediateEventExecutor eventExecutors = ImmediateEventExecutor.INSTANCE;

    @Before
    public void setUp() {

        MockitoAnnotations.initMocks(this);
        connect = new CONNECT.Mqtt5Builder().withClientIdentifier("client").build();
        when(ctx.channel()).thenReturn(channel);
        when(channel.attr(ChannelAttributes.PLUGIN_DISCONNECT_EVENT_SENT)).thenReturn(eventSentAttribute);
        when(eventSentAttribute.getAndSet(true)).thenReturn(true);

        when(ctx.executor()).thenReturn(eventExecutors);
        when(channel.attr(any(AttributeKey.class))).thenReturn(new TestChannelAttribute(null));
    }

    @Test(timeout = 5000)
    public void test_successful_auth() {

        final ImmutableList<Authenticator> authenticators = ImmutableList.of(new TestAuth());
        final ConnectSimpleAuthTaskContext context =
                new ConnectSimpleAuthTaskContext("client", connectHandler, connacker, ctx, connect, asyncer,
                        authenticators.size(), true, clientSettings, permissions, new AuthenticationContext());


        final ConnectSimpleAuthTaskOutput output = context.get();
        output.authenticateSuccessfully();

        context.pluginPost(output);

        Mockito.verify(connectHandler, timeout(1000).times(1)).connectSuccessfulAuthenticated(same(ctx), same(connect), same(clientSettings));
    }

    @Test(timeout = 5000)
    public void test_failed_auth() {
        final ImmutableList<Authenticator> authenticators = ImmutableList.of(new TestAuth());
        final ConnectSimpleAuthTaskContext context =
                new ConnectSimpleAuthTaskContext("client", connectHandler, connacker, ctx, connect, asyncer,
                        authenticators.size(), true, clientSettings, permissions, new AuthenticationContext());


        final ConnectSimpleAuthTaskOutput output = context.get();
        output.failAuthentication(UNSPECIFIED_ERROR, "reason");

        context.pluginPost(output);

        verify(connacker, times(1)).connackError(same(channel), anyString(), anyString(), same(ReasonCodeUtil.toMqtt5(UNSPECIFIED_ERROR)), same(ReasonCodeUtil.toMqtt3(UNSPECIFIED_ERROR)), eq("reason"), any(Object.class));
    }

    @Test(timeout = 5000)
    public void test_undecided_auth_present() {
        final ImmutableList<Authenticator> authenticators = ImmutableList.of(new TestAuth());
        final ConnectSimpleAuthTaskContext context =
                new ConnectSimpleAuthTaskContext("client", connectHandler, connacker, ctx, connect, asyncer,
                        authenticators.size(), true, clientSettings, permissions, new AuthenticationContext());

        final ConnectSimpleAuthTaskOutput output = context.get();
        output.authenticatorPresent();

        context.pluginPost(output);

        verify(connacker, times(1)).connackError(same(channel), anyString(), anyString(), same(ReasonCodeUtil.toMqtt5(NOT_AUTHORIZED)), same(ReasonCodeUtil.toMqtt3(NOT_AUTHORIZED)), eq("Authentication failed by extension"), any(Object.class));
    }

    @Test(timeout = 5000)
    public void test_undecided_auth_not_present() {
        final ImmutableList<Authenticator> authenticators = ImmutableList.of(new TestAuth());
        final ConnectSimpleAuthTaskContext context =
                new ConnectSimpleAuthTaskContext("client", connectHandler, connacker, ctx, connect, asyncer,
                        authenticators.size(), true, clientSettings, permissions, new AuthenticationContext());


        final ConnectSimpleAuthTaskOutput output = context.get();

        context.pluginPost(output);

        verify(connectHandler, times(1)).connectSuccessfulUnauthenticated(same(ctx), same(connect), same(clientSettings));
    }

    @Test(timeout = 5000)
    public void test_async_timed_out_auth() {
        final ImmutableList<Authenticator> authenticators = ImmutableList.of(new TestAuth());
        final ConnectSimpleAuthTaskContext context =
                new ConnectSimpleAuthTaskContext("client", connectHandler, connacker, ctx, connect, asyncer,
                        authenticators.size(), true, clientSettings, permissions, new AuthenticationContext());


        final ConnectSimpleAuthTaskOutput output = context.get();
        output.async(Duration.ofSeconds(10));
        output.markAsAsync();
        output.markAsTimedOut();

        context.pluginPost(output);

        verify(connacker, times(1)).connackError(
                same(channel), anyString(), anyString(), same(ReasonCodeUtil.toMqtt5(NOT_AUTHORIZED)),
                same(ReasonCodeUtil.toMqtt3(NOT_AUTHORIZED)), eq("Authentication failed by timeout"), any(Object.class));
    }

    @Test(timeout = 5000)
    public void test_async_timed_out_auth_multiple_auth() {
        final ImmutableList<Authenticator> authenticators = ImmutableList.of(new TestAuth(), new TestAuth());
        final ConnectSimpleAuthTaskContext context =
                new ConnectSimpleAuthTaskContext("client", connectHandler, connacker, ctx, connect, asyncer,
                        authenticators.size(), true, clientSettings, permissions, new AuthenticationContext());


        final ConnectSimpleAuthTaskOutput output = context.get();
        output.async(Duration.ofSeconds(10));
        output.markAsAsync();
        output.markAsTimedOut();

        context.pluginPost(output);
        context.pluginPost(context.get());

        verify(connacker, times(1)).connackError(
                same(channel), anyString(), anyString(), same(ReasonCodeUtil.toMqtt5(NOT_AUTHORIZED)),
                same(ReasonCodeUtil.toMqtt3(NOT_AUTHORIZED)), eq("Authentication failed by timeout"), any(Object.class));
    }

    @Test(timeout = 5000)
    public void test_inconclusive_auth_no_enhanced_available() {
        final ImmutableList<Authenticator> authenticators = ImmutableList.of(new TestAuth());
        final ConnectSimpleAuthTaskContext context =
                new ConnectSimpleAuthTaskContext("client", connectHandler, connacker, ctx, connect, asyncer,
                        authenticators.size(), true, clientSettings, permissions, new AuthenticationContext());


        final ConnectSimpleAuthTaskOutput output = context.get();
        output.nextExtensionOrDefault();

        context.pluginPost(output);

        verify(connacker, times(1)).connackError(same(channel), anyString(), anyString(), same(Mqtt5ConnAckReasonCode.NOT_AUTHORIZED), same(Mqtt3ConnAckReturnCode.REFUSED_NOT_AUTHORIZED), isNull(String.class), any(Object.class));
    }

    @Test(timeout = 5000)
    public void test_inconclusive_auth_enhanced_available() {
        when(channel.hasAttr(eq(ChannelAttributes.AUTH_METHOD))).thenReturn(true);
        final ImmutableList<Authenticator> authenticators = ImmutableList.of(new TestAuth());
        final ConnectSimpleAuthTaskContext context =
                new ConnectSimpleAuthTaskContext("client", connectHandler, connacker, ctx, connect, asyncer,
                        authenticators.size(), true, clientSettings, permissions, new AuthenticationContext());


        final ConnectSimpleAuthTaskOutput output = context.get();

        output.nextExtensionOrDefault();
        context.pluginPost(output);

        verify(connectHandler, times(0)).connectSuccessfulUnauthenticated(same(ctx), same(connect), same(clientSettings));
    }

    @Test(timeout = 5000)
    public void test_connect_userproperties_are_not_available() {
        connect.setUserProperties(Mqtt5UserProperties.of(new MqttUserProperty("one", "one"), new MqttUserProperty("one", "two")));

        when(channel.attr(ChannelAttributes.AUTH_USER_PROPERTIES)).thenReturn(userPropertiesAttribute);

        final ImmutableList<Authenticator> authenticators = ImmutableList.of(new TestAuth(), new TestAuth());
        final ConnectSimpleAuthTaskContext context =
                new ConnectSimpleAuthTaskContext("client", connectHandler, connacker, ctx, connect, asyncer,
                        authenticators.size(), true, clientSettings, permissions, new AuthenticationContext());

        final ConnectSimpleAuthTaskOutput output = context.get();
        final ModifiableUserProperties outboundUserProperties = output.getOutboundUserProperties();

        assertEquals(0, outboundUserProperties.asList().size());

        output.nextExtensionOrDefault();
        context.pluginPost(output);

        final ConnectSimpleAuthTaskOutput output2 = context.get();
        final ModifiableUserProperties outboundUserProperties2 = output2.getOutboundUserProperties();

        assertEquals(0, outboundUserProperties2.asList().size());

        output2.authenticateSuccessfully();

        context.pluginPost(output2);

        final ConnectSimpleAuthTaskOutput output3 = context.get();

        final ModifiableUserProperties outboundUserProperties3 = output3.getOutboundUserProperties();

        assertEquals(0, outboundUserProperties3.asList().size());

        verify(connectHandler, times(1)).connectSuccessfulAuthenticated(same(ctx), same(connect), any(ModifiableClientSettingsImpl.class));

        assertEquals(2, connect.getUserProperties().size());
    }


    @Test(timeout = 5000)
    public void test_wait_for_last_task_done() {
        final ImmutableList<Authenticator> authenticators = ImmutableList.of(new TestAuth(), new TestAuth(), new TestAuth());
        final ConnectSimpleAuthTaskContext context =
                new ConnectSimpleAuthTaskContext("client", connectHandler, connacker, ctx, connect, asyncer,
                        authenticators.size(), true, clientSettings, permissions, new AuthenticationContext());

        final ConnectSimpleAuthTaskOutput output = context.get();

        output.authenticateSuccessfully();


        context.pluginPost(output);
        context.pluginPost(output);
        Mockito.verify(connectHandler, never()).connectSuccessfulAuthenticated(any(ChannelHandlerContext.class), any(CONNECT.class), any(ModifiableClientSettingsImpl.class));
        context.pluginPost(output);

        Mockito.verify(connectHandler, times(1)).connectSuccessfulAuthenticated(same(ctx), same(connect), same(clientSettings));
    }

    @Test(timeout = 5000)
    @SuppressWarnings("unchecked")
    public void test_permissions_present_after_successful_auth() {

        final Attribute attr = mock(Attribute.class);
        when(channel.attr(ChannelAttributes.AUTH_PERMISSIONS)).thenReturn(attr);

        final ImmutableList<Authenticator> authenticators = ImmutableList.of(new TestAuth());
        final ConnectSimpleAuthTaskContext context =
                new ConnectSimpleAuthTaskContext("client", connectHandler, connacker, ctx, connect, asyncer,
                        authenticators.size(), true, clientSettings, permissions, new AuthenticationContext());


        final ConnectSimpleAuthTaskOutput output = context.get();
        output.authenticateSuccessfully();

        context.pluginPost(output);

        Mockito.verify(attr, times(1)).set(any());
    }


    @Test(timeout = 5000)
    @SuppressWarnings("unchecked")
    public void test_permissions_not_present_after_inconclusive_auth() {

        final Attribute attr = mock(Attribute.class);
        when(channel.attr(ChannelAttributes.AUTH_PERMISSIONS)).thenReturn(attr);

        when(channel.hasAttr(eq(ChannelAttributes.AUTH_METHOD))).thenReturn(true);

        final ImmutableList<Authenticator> authenticators = ImmutableList.of(new TestAuth());
        final ConnectSimpleAuthTaskContext context =
                new ConnectSimpleAuthTaskContext("client", connectHandler, connacker, ctx, connect, asyncer,
                        authenticators.size(), true, clientSettings, permissions, new AuthenticationContext());

        final ConnectSimpleAuthTaskOutput output = context.get();
        output.nextExtensionOrDefault();

        context.pluginPost(output);

        Mockito.verify(attr, times(0)).set(any());
    }

    private static class TestAuth implements SimpleAuthenticator {

        @Override
        public void onConnect(final SimpleAuthInput input, final SimpleAuthOutput output) {
            output.authenticateSuccessfully();
        }
    }
}