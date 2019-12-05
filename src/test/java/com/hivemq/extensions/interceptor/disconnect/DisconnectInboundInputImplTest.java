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

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableInboundDisconnectPacket;
import com.hivemq.extensions.packets.disconnect.DisconnectPacketImpl;
import com.hivemq.extensions.packets.disconnect.ModifiableInboundDisconnectPacketImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Test;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class DisconnectInboundInputImplTest {

    @Test
    public void test_construction() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final DISCONNECT disconnectPacket = TestMessageUtil.createFullMqtt5Disconnect();
        final DisconnectInboundInputImpl input =
                new DisconnectInboundInputImpl("client", embeddedChannel, disconnectPacket);
        Assert.assertNotNull(input.getClientInformation());
        Assert.assertNotNull(input.getConnectionInformation());
        Assert.assertNotNull(input.getDisconnectPacket());
    }

    @Test
    public void create_DISCONNECT_from_package_test() {
        //
        final DisconnectPacketImpl disconnectPacket =
                new DisconnectPacketImpl(TestMessageUtil.createFullMqtt5Disconnect());
        final DISCONNECT disconnect = new DISCONNECT(Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION, "reason",
                Mqtt5UserProperties.of(
                        new MqttUserProperty("user1", "property1"), new MqttUserProperty("user2", "property2")),
                "server reference", 360);
        final DISCONNECT disconnectFrom = DISCONNECT.createDisconnectFrom(disconnectPacket);
        Assert.assertEquals(disconnect.getUserProperties(), disconnectFrom.getUserProperties());
        Assert.assertEquals(disconnect.getReasonString(), disconnectFrom.getReasonString());
        Assert.assertEquals(disconnect.getServerReference(), disconnect.getServerReference());
        Assert.assertEquals(disconnect.getReasonCode(), disconnect.getReasonCode());
        Assert.assertEquals(disconnect.getSessionExpiryInterval(), disconnect.getSessionExpiryInterval());

    }

    @Test(expected = NullPointerException.class)
    public void test_clientId_null() {
        final DISCONNECT disconnectPacket = TestMessageUtil.createFullMqtt5Disconnect();
        new DisconnectInboundInputImpl(null, new EmbeddedChannel(), disconnectPacket);
    }

    @Test(expected = NullPointerException.class)
    public void test_channel_null() {
        final DISCONNECT disconnectPacket = TestMessageUtil.createFullMqtt5Disconnect();
        new DisconnectInboundInputImpl("client", null, disconnectPacket);
    }

    @Test(expected = NullPointerException.class)
    public void test_packet_null() {
        final DisconnectInboundInputImpl disconnectInboundInput =
                new DisconnectInboundInputImpl(null, new EmbeddedChannel(), null);
    }

    @Test
    public void test_update() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final DISCONNECT disconnectPacket1 = TestMessageUtil.createFullMqtt5Disconnect();
        final DISCONNECT disconnectPacket2 = TestMessageUtil.createFullMqtt5Disconnect();

        final DisconnectInboundInputImpl input =
                new DisconnectInboundInputImpl("client", embeddedChannel, disconnectPacket1);
        input.update(new DisconnectPacketImpl(disconnectPacket2));

        Assert.assertNotSame(input.getDisconnectPacket(), disconnectPacket1);
        Assert.assertNotSame(input.getDisconnectPacket(), disconnectPacket2);
    }

    @Test
    public void test_overrideReasonString() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final FullConfigurationService fullConfigurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();
        final DISCONNECT disconnect = TestMessageUtil.createFullMqtt5Disconnect();

        final ModifiableInboundDisconnectPacket disconnectPacket =
                new ModifiableInboundDisconnectPacketImpl(fullConfigurationService, disconnect, 0);
        disconnectPacket.setReasonString("modified");

        Assert.assertEquals("modified", disconnectPacket.getReasonString().get());
    }

}