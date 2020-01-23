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
package com.hivemq.extensions.interceptor.disconnect;

import com.hivemq.extensions.packets.disconnect.DisconnectPacketImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Test;
import util.TestMessageUtil;

public class DisconnectOutboundInputImplTest {

    @Test
    public void test_construction() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final DISCONNECT disconnectPacket = TestMessageUtil.createFullMqtt5Disconnect();
        final DisconnectOutboundInputImpl input =
                new DisconnectOutboundInputImpl("client", embeddedChannel, disconnectPacket);
        Assert.assertNotNull(input.getClientInformation());
        Assert.assertNotNull(input.getConnectionInformation());
        Assert.assertNotNull(input.getDisconnectPacket());
    }

    @Test(expected = NullPointerException.class)
    public void test_clientId_null() {
        final DISCONNECT disconnectPacket = TestMessageUtil.createFullMqtt5Disconnect();
        new DisconnectOutboundInputImpl(null, new EmbeddedChannel(), disconnectPacket);
    }

    @Test(expected = NullPointerException.class)
    public void test_channel_null() {
        final DISCONNECT disconnectPacket = TestMessageUtil.createFullMqtt5Disconnect();
        new DisconnectOutboundInputImpl("client", null, disconnectPacket);
    }

    @Test(expected = NullPointerException.class)
    public void test_packet_null() {
        new DisconnectOutboundInputImpl(null, new EmbeddedChannel(), null);
    }

    @Test
    public void test_update() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final DISCONNECT disconnectPacket1 = TestMessageUtil.createFullMqtt5Disconnect();
        final DISCONNECT disconnectPacket2 = TestMessageUtil.createFullMqtt5Disconnect();

        final DisconnectOutboundInputImpl input =
                new DisconnectOutboundInputImpl("client", embeddedChannel, disconnectPacket1);
        input.update(new DisconnectPacketImpl(disconnectPacket2));

        Assert.assertNotSame(input.getDisconnectPacket(), disconnectPacket1);
        Assert.assertNotSame(input.getDisconnectPacket(), disconnectPacket2);
    }
}