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
package com.hivemq.extensions.interceptor.unsubscribe.parameter;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.extensions.packets.unsubscribe.UnsubscribePacketImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;
import util.TestMessageUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * @author Robin Atherton
 */
public class UnsubscribeInboundInputImplTest {

    @Test
    public void test_construction() {
        final ImmutableList<String> topicFilters =
                ImmutableList.of(
                        new Topic("topic", QoS.AT_LEAST_ONCE, false, false, Mqtt5RetainHandling.SEND, null).toString());
        final UNSUBSCRIBE unsubscribe =
                new UNSUBSCRIBE(topicFilters, 1, Mqtt5UserProperties.of(MqttUserProperty.of("Prop", "Value")));

        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        embeddedChannel.attr(ChannelAttributes.CLIENT_ID).set("client");

        final UnsubscribeInboundInputImpl client =
                new UnsubscribeInboundInputImpl("client", embeddedChannel, unsubscribe);

        assertEquals(client.getClientInformation().getClientId(), "client");
        assertEquals(client.getConnectionInformation().getMqttVersion(), MqttVersion.V_5);
    }

    @Test(expected = NullPointerException.class)
    public void test_client_id_null() {
        new UnsubscribeInboundInputImpl(null, new EmbeddedChannel(), TestMessageUtil.createFullMqtt5Unsubscribe());
    }

    @Test(expected = NullPointerException.class)
    public void test_channel_null() {
        new UnsubscribeInboundInputImpl("client", null, TestMessageUtil.createFullMqtt5Unsubscribe());
    }

    @Test(expected = NullPointerException.class)
    public void test_packet_null() {
        new UnsubscribeInboundInputImpl("client", new EmbeddedChannel(), null);
    }

    @Test
    public void test_update() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final UNSUBSCRIBE unsubscribePacket1 = TestMessageUtil.createFullMqtt5Unsubscribe();
        final UNSUBSCRIBE unsubscribePacket2 = TestMessageUtil.createFullMqtt5Unsubscribe();

        final UnsubscribeInboundInputImpl input =
                new UnsubscribeInboundInputImpl("client1", embeddedChannel, unsubscribePacket1);
        input.update(new UnsubscribePacketImpl(unsubscribePacket2));

        assertNotSame(unsubscribePacket1, input.getUnsubscribePacket());
        assertNotSame(unsubscribePacket2, input.getUnsubscribePacket());
    }
}