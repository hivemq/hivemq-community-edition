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
package com.hivemq.mqtt.handler.auth;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ClientConnectionContext;
import com.hivemq.bootstrap.ClientState;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import util.DummyClientConnection;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class MqttAuthSenderTest {

    private @NotNull MqttAuthSender mqttAuthSender;
    private EventLog eventLog;

    @Before
    public void setUp() throws Exception {
        eventLog = mock(EventLog.class);
        mqttAuthSender = new MqttAuthSender(eventLog);
    }

    @Test(expected = NullPointerException.class)
    public void test_send_auth_code_null() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(new DummyClientConnection(channel, null));
        mqttAuthSender.sendAuth(channel, null, null, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason");
    }

    @Test(expected = NullPointerException.class)
    public void test_send_auth_props_null() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(new DummyClientConnection(channel, null));
        mqttAuthSender.sendAuth(channel, null, Mqtt5AuthReasonCode.SUCCESS, null, "reason");
    }

    @Test(expected = NullPointerException.class)
    public void test_send_auth_channel_null() {
        mqttAuthSender.sendAuth(null, null, Mqtt5AuthReasonCode.SUCCESS, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason");
    }

    @Test(expected = NullPointerException.class)
    public void test_send_auth_method_null() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(new DummyClientConnection(channel, null));
        ClientConnection.of(channel).proposeClientState(ClientState.RE_AUTHENTICATING);
        mqttAuthSender.sendAuth(channel, null, Mqtt5AuthReasonCode.SUCCESS, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason");
    }

    @Test
    public void test_send_auth_success() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(new DummyClientConnection(channel, null));
        ClientConnection.of(channel).proposeClientState(ClientState.RE_AUTHENTICATING);
        ClientConnection.of(channel).setAuthMethod("METHOD");
        final ChannelFuture future = mqttAuthSender.sendAuth(channel, null, Mqtt5AuthReasonCode.SUCCESS, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason");

        assertNotNull(future);
        verify(eventLog).clientAuthentication(channel, Mqtt5AuthReasonCode.SUCCESS, false);

    }
}