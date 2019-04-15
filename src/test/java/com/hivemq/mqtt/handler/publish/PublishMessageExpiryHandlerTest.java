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

package com.hivemq.mqtt.handler.publish;

import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.LogbackCapturingAppender;
import util.TestMessageUtil;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @since 4.0.1
 */
public class PublishMessageExpiryHandlerTest {

    private PublishMessageExpiryHandler messageExpiryHandler;

    @Mock
    private ChannelHandlerContext ctx;

    private Channel channel;

    LogbackCapturingAppender logCapture;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        messageExpiryHandler = new PublishMessageExpiryHandler();
        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_ID).set("ClientId");
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        when(ctx.channel()).thenReturn(channel);
        logCapture = LogbackCapturingAppender.Factory.weaveInto(PublishMessageExpiryHandler.log);
    }

    @Test
    public void test_message_expired_qos_0() throws Exception {

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.AT_MOST_ONCE);
        publish.setMessageExpiryInterval(1);

        Thread.sleep(2000);

        messageExpiryHandler.write(ctx, publish, channel.newPromise());

        assertEquals(0, publish.getMessageExpiryInterval());
    }

    @Test
    public void test_message_expired_qos_1() throws Exception {

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.AT_LEAST_ONCE);
        publish.setMessageExpiryInterval(1);

        Thread.sleep(2000);

        messageExpiryHandler.write(ctx, publish, channel.newPromise());

        assertEquals(0, publish.getMessageExpiryInterval());
    }

    @Test
    public void test_message_expired_qos_2_not_dup() throws Exception {

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.EXACTLY_ONCE);
        publish.setMessageExpiryInterval(1);

        Thread.sleep(2000);

        messageExpiryHandler.write(ctx, publish, channel.newPromise());

        assertEquals(0, publish.getMessageExpiryInterval());
    }

    @Test
    public void test_message_expired_qos_2_dup() throws Exception {

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.EXACTLY_ONCE);
        publish.setMessageExpiryInterval(1);

        Thread.sleep(2000);

        messageExpiryHandler.write(ctx, publish, channel.newPromise());

        assertEquals(0, publish.getMessageExpiryInterval());

        assertEquals(logCapture.getLastCapturedLog().getFormattedMessage(), "Publish message with topic 'topic' and qos '2' for client 'ClientId' expired after 2 seconds.");
    }

    @Test
    public void test_message_not_expired_not_mqtt_5() throws Exception {

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        final PUBLISH publish = TestMessageUtil.createMqtt3Publish();
        publish.setMessageExpiryInterval(1000);

        Thread.sleep(2000);

        messageExpiryHandler.write(ctx, publish, channel.newPromise());

        assertEquals(1000, publish.getMessageExpiryInterval());

    }
}