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
package com.hivemq.extensions.interceptor.pubcomp;

import com.hivemq.extensions.packets.pubcomp.PubcompPacketImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;
import util.TestMessageUtil;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class PubcompInboundInputImplTest {

    @Test
    public void test_construction_success() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PubcompOutboundInputImpl input = new PubcompOutboundInputImpl(
                "client", embeddedChannel, TestMessageUtil.createSuccessPupcomp());
        assertNotNull(input.getClientInformation());
        assertNotNull(input.getConnectionInformation());
        assertNotNull(input.getPubcompPacket());
    }

    @Test(expected = NullPointerException.class)
    public void test_client_id_null() {
        new PubcompOutboundInputImpl(null, new EmbeddedChannel(), TestMessageUtil.createSuccessPupcomp());
    }

    @Test(expected = NullPointerException.class)
    public void test_channel_null() {
        new PubcompOutboundInputImpl("client", null, TestMessageUtil.createSuccessPupcomp());
    }

    @Test(expected = NullPointerException.class)
    public void test_packet_null() {
        new PubcompOutboundInputImpl("client", new EmbeddedChannel(), null);
    }

    @Test
    public void test_update() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);


        final PUBCOMP pubcomPacket1 = TestMessageUtil.createSuccessPupcomp();
        final PUBCOMP pubcomPacket2 = TestMessageUtil.createSuccessPupcomp();

        final PubcompOutboundInputImpl input = new PubcompOutboundInputImpl("client", embeddedChannel, pubcomPacket1);
        input.update(new PubcompPacketImpl(pubcomPacket2));

        assertNotSame(pubcomPacket1, input.getPubcompPacket());
        assertNotSame(pubcomPacket2, input.getPubcompPacket());
    }
}