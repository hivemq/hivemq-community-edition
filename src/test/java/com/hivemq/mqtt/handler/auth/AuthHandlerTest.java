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

package com.hivemq.mqtt.handler.auth;

import com.hivemq.extensions.handler.PluginAuthenticatorService;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.disconnect.Mqtt5ServerDisconnector;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.hivemq.mqtt.handler.auth.AuthHandler.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Florian Limpöck
 */
public class AuthHandlerTest {

    @Mock
    private PluginAuthenticatorService pluginAuthenticatorService;
    @Mock
    private MqttConnacker connacker;
    @Mock
    private MqttAuthSender mqttAuthSender;
    @Mock
    private Mqtt5ServerDisconnector disconnector;

    private AuthHandler authHandler;
    private EmbeddedChannel channel;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        channel = new EmbeddedChannel();
        authHandler = new AuthHandler(connacker, mqttAuthSender, disconnector, pluginAuthenticatorService);

        channel.pipeline().addLast(authHandler);
    }

    @After
    public void tearDown() throws Exception {
        verify(mqttAuthSender).logAuth(eq(channel), any(), anyBoolean());
    }

    @Test
    public void test_read_connect_success() {

        channel.writeInbound(new AUTH("auth method", "auth data".getBytes(), Mqtt5AuthReasonCode.SUCCESS, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason"));

        verify(connacker).connackError(
                any(Channel.class), anyString(), anyString(),
                eq(Mqtt5ConnAckReasonCode.PROTOCOL_ERROR),
                eq(String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_REASON_CODE, "AUTH")),
                eq(Mqtt5UserProperties.NO_USER_PROPERTIES),
                eq(true));

    }

    @Test
    public void test_read_reauth_success() {

        channel.attr(ChannelAttributes.RE_AUTH_ONGOING).set(true);

        channel.writeInbound(new AUTH("auth method", "auth data".getBytes(), Mqtt5AuthReasonCode.SUCCESS, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason"));

        verify(disconnector).disconnect(
                channel, SUCCESS_AUTH_RECEIVED_FROM_CLIENT, "Success reason code set in AUTH",
                Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_REASON_CODE, "AUTH"),
                Mqtt5UserProperties.NO_USER_PROPERTIES, true);

    }

    @Test
    public void test_read_reauth_reauth() {

        channel.attr(ChannelAttributes.RE_AUTH_ONGOING).set(true);

        channel.writeInbound(new AUTH("auth method", "auth data".getBytes(), Mqtt5AuthReasonCode.REAUTHENTICATE, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason"));

        verify(disconnector).disconnect(
                channel, REAUTHENTICATE_DURING_RE_AUTH, "REAUTHENTICATE reason code set in AUTH during ongoing re-auth",
                Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_REASON_CODE, "AUTH"),
                Mqtt5UserProperties.NO_USER_PROPERTIES, true);

        verify(pluginAuthenticatorService, never()).authenticateReAuth(any(), any());

    }

    @Test
    public void test_read_auth_reauth() {

        channel.attr(ChannelAttributes.AUTH_ONGOING).set(true);

        channel.writeInbound(new AUTH("auth method", "auth data".getBytes(), Mqtt5AuthReasonCode.REAUTHENTICATE, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason"));

        verify(connacker).connackError(
                any(Channel.class), eq(REAUTHENTICATE_DURING_AUTH), eq("REAUTHENTICATE reason code set in AUTH during ongoing auth"),
                eq(Mqtt5ConnAckReasonCode.PROTOCOL_ERROR),
                eq(String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_REASON_CODE, "AUTH")),
                eq(Mqtt5UserProperties.NO_USER_PROPERTIES),
                eq(true));

        verify(pluginAuthenticatorService, never()).authenticateReAuth(any(), any());

    }

    @Test
    public void test_read_connect_continue() {

        channel.writeInbound(new AUTH("auth method", "auth data".getBytes(), Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason"));
        verify(pluginAuthenticatorService).authenticateAuth(any(), any(), eq(false));

    }

    @Test
    public void test_read_reauth_continue() {

        channel.attr(ChannelAttributes.RE_AUTH_ONGOING).set(true);
        channel.writeInbound(new AUTH("auth method", "auth data".getBytes(), Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason"));
        verify(pluginAuthenticatorService).authenticateAuth(any(), any(), eq(true));

    }

    @Test
    public void test_read_reauth() {

        channel.writeInbound(new AUTH("auth method", "auth data".getBytes(), Mqtt5AuthReasonCode.REAUTHENTICATE, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason"));
        verify(pluginAuthenticatorService).authenticateReAuth(any(), any());
        assertTrue(channel.attr(ChannelAttributes.RE_AUTH_ONGOING).get());

    }
}