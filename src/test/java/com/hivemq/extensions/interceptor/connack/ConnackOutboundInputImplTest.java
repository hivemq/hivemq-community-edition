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

package com.hivemq.extensions.interceptor.connack;

import com.hivemq.extensions.packets.connack.ConnackPacketImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;
import util.TestMessageUtil;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class ConnackOutboundInputImplTest {

    @Test
    public void test_construction_success() {

        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final ConnackOutboundInputImpl input = new ConnackOutboundInputImpl(new ConnackPacketImpl(TestMessageUtil.createFullMqtt5Connack()), "client", embeddedChannel);
        assertNotNull(input.get().getClientInformation());
        assertNotNull(input.get().getConnackPacket());
        assertNotNull(input.get().getConnectionInformation());

    }

    @Test(expected = NullPointerException.class)
    public void test_client_id_null() {
        new ConnackOutboundInputImpl(new ConnackPacketImpl(TestMessageUtil.createFullMqtt5Connack()), null, new EmbeddedChannel());
    }

    @Test(expected = NullPointerException.class)
    public void test_channel_null() {
        new ConnackOutboundInputImpl(new ConnackPacketImpl(TestMessageUtil.createFullMqtt5Connack()), "asd", null);
    }

    @Test(expected = NullPointerException.class)
    public void test_packet_null() {
        new ConnackOutboundInputImpl(null, "asd", new EmbeddedChannel());
    }

    @Test
    public void test_update() {

        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        final ConnackPacketImpl connackPacket1 = new ConnackPacketImpl(TestMessageUtil.createFullMqtt5Connack());
        final ConnackPacketImpl connackPacket2 = new ConnackPacketImpl(TestMessageUtil.createFullMqtt5Connack());

        final ConnackOutboundInputImpl input = new ConnackOutboundInputImpl(connackPacket1, "client", embeddedChannel);

        input.updateConnack(connackPacket2);

        assertNotEquals(connackPacket2, input.getConnackPacket());
        assertNotEquals(connackPacket1, input.getConnackPacket());


    }
}