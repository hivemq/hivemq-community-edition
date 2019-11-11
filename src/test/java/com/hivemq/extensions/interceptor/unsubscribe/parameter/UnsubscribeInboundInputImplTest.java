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

import static org.junit.Assert.assertEquals;

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
}