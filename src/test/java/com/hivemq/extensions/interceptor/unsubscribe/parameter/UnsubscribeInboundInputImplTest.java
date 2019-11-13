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

        final UnsubscribePacketImpl unsubscribePacket = new UnsubscribePacketImpl(unsubscribe);
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        embeddedChannel.attr(ChannelAttributes.CLIENT_ID).set("client");

        final UnsubscribeInboundInputImpl client =
                new UnsubscribeInboundInputImpl(unsubscribePacket, "client", embeddedChannel);

        assertEquals(client.getClientInformation().getClientId(), "client");
        assertEquals(client.getConnectionInformation().getMqttVersion(), MqttVersion.V_5);
    }

    @Test(expected = NullPointerException.class)
    public void test_client_id_null() {
        final UnsubscribePacketImpl unsubscribePacket =
                new UnsubscribePacketImpl(TestMessageUtil.createFullMqtt5Unsubscribe());
        final UnsubscribeInboundInputImpl input =
                new UnsubscribeInboundInputImpl(unsubscribePacket, null, new EmbeddedChannel());
    }

    @Test(expected = NullPointerException.class)
    public void test_channel_null() {
        final UnsubscribePacketImpl unsubscribePacket =
                new UnsubscribePacketImpl(TestMessageUtil.createFullMqtt5Unsubscribe());
        final UnsubscribeInboundInputImpl input = new UnsubscribeInboundInputImpl(unsubscribePacket, "client", null);
    }

    @Test(expected = NullPointerException.class)
    public void test_packet_null() {
        final UnsubscribeInboundInputImpl input =
                new UnsubscribeInboundInputImpl(null, "client", new EmbeddedChannel());
    }

    @Test
    public void test_update() {
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final UnsubscribePacketImpl unsubscribePacket1 =
                new UnsubscribePacketImpl(TestMessageUtil.createFullMqtt5Unsubscribe());
        final UnsubscribePacketImpl unsubscribePacket2 =
                new UnsubscribePacketImpl(TestMessageUtil.createFullMqtt5Unsubscribe());

        final UnsubscribeInboundInputImpl input =
                new UnsubscribeInboundInputImpl(unsubscribePacket1, "client1", embeddedChannel);
        input.updateUnsubscribe(unsubscribePacket2);

        assertNotSame(unsubscribePacket1, input.getUnsubscribePacket());
        assertNotSame(unsubscribePacket2, input.getUnsubscribePacket());
    }

}