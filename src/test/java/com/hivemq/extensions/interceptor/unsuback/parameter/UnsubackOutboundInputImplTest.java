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
package com.hivemq.extensions.interceptor.unsuback.parameter;

import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Test;
import util.TestMessageUtil;

/**
 * @author Robin Atherton
 */
public class UnsubackOutboundInputImplTest {

    @Test
    public void test_construction() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final UNSUBACK unsuback = TestMessageUtil.createFullMqtt5Unsuback();
        final UnsubackOutboundInputImpl input = new UnsubackOutboundInputImpl("client", embeddedChannel, unsuback);

        Assert.assertNotNull(input.getClientInformation());
        Assert.assertNotNull(input.getConnectionInformation());
        Assert.assertNotNull(input.getUnsubackPacket());
    }

    @Test(expected = NullPointerException.class)
    public void test_clientId_null() {
        final UNSUBACK unsubackPacket = TestMessageUtil.createFullMqtt5Unsuback();
        new UnsubackOutboundInputImpl(null, new EmbeddedChannel(), unsubackPacket);
    }

    @Test(expected = NullPointerException.class)
    public void test_channel_null() {
        final UNSUBACK unsubackPacket = TestMessageUtil.createFullMqtt5Unsuback();
        new UnsubackOutboundInputImpl("client", null, unsubackPacket);
    }

    @Test(expected = NullPointerException.class)
    public void test_packet_null() {
        new UnsubackOutboundInputImpl(null, new EmbeddedChannel(), null);
    }

}