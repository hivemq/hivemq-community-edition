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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class MqttAuthSenderTest {

    @NotNull
    private MqttAuthSender mqttAuthSender;

    @Mock
    private EventLog eventLog;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mqttAuthSender = new MqttAuthSender(eventLog);
    }

    @Test(expected = NullPointerException.class)
    public void test_send_auth_code_null() {
        mqttAuthSender.sendAuth(new EmbeddedChannel(), null, null, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason");
    }

    @Test(expected = NullPointerException.class)
    public void test_send_auth_props_null() {
        mqttAuthSender.sendAuth(new EmbeddedChannel(), null, Mqtt5AuthReasonCode.SUCCESS, null, "reason");
    }

    @Test(expected = NullPointerException.class)
    public void test_send_auth_channel_null() {
        mqttAuthSender.sendAuth(null, null, Mqtt5AuthReasonCode.SUCCESS, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason");
    }

    @Test(expected = NullPointerException.class)
    public void test_send_auth_method_null() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.RE_AUTH_ONGOING).set(true);
        mqttAuthSender.sendAuth(embeddedChannel, null, Mqtt5AuthReasonCode.SUCCESS, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason");
    }

    @Test
    public void test_send_auth_success() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.RE_AUTH_ONGOING).set(true);
        embeddedChannel.attr(ChannelAttributes.AUTH_METHOD).set("METHOD");
        final ChannelFuture future = mqttAuthSender.sendAuth(embeddedChannel, null, Mqtt5AuthReasonCode.SUCCESS, Mqtt5UserProperties.NO_USER_PROPERTIES, "reason");

        assertNotNull(future);
        verify(eventLog).clientAuthentication(embeddedChannel, Mqtt5AuthReasonCode.SUCCESS, false);

    }
}