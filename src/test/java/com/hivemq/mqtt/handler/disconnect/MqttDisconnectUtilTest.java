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

package com.hivemq.mqtt.handler.disconnect;

import com.hivemq.extensions.events.OnServerDisconnectEvent;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("ALL")
public class MqttDisconnectUtilTest {

    private MqttDisconnectUtil mqttDisconnectUtil;

    @Mock
    EventLog eventLog;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mqttDisconnectUtil = new MqttDisconnectUtil(eventLog);
    }

    @Test
    public void test_disconnect_channel() throws InterruptedException {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.PLUGIN_CONNECT_EVENT_SENT).set(true);

        final CountDownLatch eventLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new TestDisconnectEventHandler(eventLatch));
        assertTrue(channel.isActive());

        mqttDisconnectUtil.disconnect(channel, false, false, null, null, Mqtt5UserProperties.NO_USER_PROPERTIES, false);

        assertFalse(channel.isActive());
        assertTrue(eventLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void test_disconnect_channel_with_reason_code() throws InterruptedException {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.PLUGIN_CONNECT_EVENT_SENT).set(true);

        final CountDownLatch eventLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new TestDisconnectEventHandler(eventLatch));
        assertTrue(channel.isActive());

        mqttDisconnectUtil.disconnect(channel, true, false, Mqtt5DisconnectReasonCode.MALFORMED_PACKET, null, Mqtt5UserProperties.NO_USER_PROPERTIES, false);

        assertFalse(channel.isActive());
        assertTrue(eventLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void test_disconnect_channel_with_reason_code_and_reason_string() throws InterruptedException {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.PLUGIN_CONNECT_EVENT_SENT).set(true);

        final CountDownLatch eventLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new TestDisconnectEventHandler(eventLatch));
        assertTrue(channel.isActive());

        mqttDisconnectUtil.disconnect(channel, true, true, Mqtt5DisconnectReasonCode.MALFORMED_PACKET, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES, false);

        assertFalse(channel.isActive());
        assertTrue(eventLatch.await(10, TimeUnit.SECONDS));
    }

    @Test(expected = NullPointerException.class)
    public void test_disconnect_channel_with_reason_code_null() throws InterruptedException {
        final EmbeddedChannel channel = new EmbeddedChannel();
        mqttDisconnectUtil.disconnect(channel, true, false, null, null, Mqtt5UserProperties.NO_USER_PROPERTIES, false);
    }

    @Test
    public void test_log_disconnect() {
        mqttDisconnectUtil.logDisconnect(new EmbeddedChannel(), "log message", "event log message");
        verify(eventLog).clientWasDisconnected(any(Channel.class), eq("event log message"));
    }

    private static class TestDisconnectEventHandler extends SimpleChannelInboundHandler<CONNECT> {
        private final CountDownLatch eventLatch;

        public TestDisconnectEventHandler(CountDownLatch eventLatch) {
            this.eventLatch = eventLatch;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, CONNECT msg) throws Exception {
            ctx.fireChannelRead(msg);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof OnServerDisconnectEvent) {
                eventLatch.countDown();
            }
        }
    }
}