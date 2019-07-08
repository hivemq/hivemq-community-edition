package com.hivemq.extensions.interceptor.subscribe.parameter;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.extensions.packets.subscribe.SubscribePacketImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.subscribe.Mqtt5SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;
import util.TestMessageUtil;

import static org.junit.Assert.assertEquals;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class SubscribeInboundInputImplTest {

    @Test
    public void test_construction() {

        final SUBSCRIBE subscribe = new SUBSCRIBE(TestMessageUtil.TEST_USER_PROPERTIES,
                ImmutableList.of(new Topic("topic", QoS.AT_LEAST_ONCE, false, false, Mqtt5RetainHandling.SEND, null)), 1, Mqtt5SUBSCRIBE.DEFAULT_NO_SUBSCRIPTION_IDENTIFIER);

        final SubscribePacketImpl subscribePacket = new SubscribePacketImpl(subscribe);

        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        embeddedChannel.attr(ChannelAttributes.CLIENT_ID).set("client");

        final SubscribeInboundInputImpl client = new SubscribeInboundInputImpl(subscribePacket, "client", embeddedChannel);

        assertEquals(client.getClientInformation().getClientId(), "client");
        assertEquals(client.getConnectionInformation().getMqttVersion(), MqttVersion.V_5);

    }
}