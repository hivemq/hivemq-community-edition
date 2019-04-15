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

package com.hivemq.extensions.interceptor.publish.parameter;

import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extensions.packets.publish.PublishPacketImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.util.Bytes;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;
import util.TestMessageUtil;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings({"NullabilityAnnotations", "OptionalGetWithoutIsPresent"})
public class PublishInboundInputImplTest {

    @Test
    public void test_values_set_correctly() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PublishInboundInputImpl publishInboundInput = new PublishInboundInputImpl(new PublishPacketImpl(TestMessageUtil.createFullMqtt5Publish()), "client", channel);

        assertEquals("client", publishInboundInput.getClientInformation().getClientId());
        assertEquals(true, publishInboundInput.getPublishPacket().getDupFlag());
        assertEquals(Qos.EXACTLY_ONCE, publishInboundInput.getPublishPacket().getQos());
        assertEquals(true, publishInboundInput.getPublishPacket().getRetain());
        assertEquals("topic", publishInboundInput.getPublishPacket().getTopic());
        assertEquals(1, publishInboundInput.getPublishPacket().getPacketId());
        assertEquals(PayloadFormatIndicator.UTF_8, publishInboundInput.getPublishPacket().getPayloadFormatIndicator().get());
        assertEquals(360, publishInboundInput.getPublishPacket().getMessageExpiryInterval().get().longValue());
        assertEquals("response topic", publishInboundInput.getPublishPacket().getResponseTopic().get());
        assertArrayEquals("correlation data".getBytes(), Bytes.getBytesFromReadOnlyBuffer(publishInboundInput.getPublishPacket().getCorrelationData()));
        assertArrayEquals("payload".getBytes(), Bytes.getBytesFromReadOnlyBuffer(publishInboundInput.getPublishPacket().getPayload()));
        assertEquals("content type", publishInboundInput.getPublishPacket().getContentType().get());
        assertEquals(3, publishInboundInput.getPublishPacket().getSubscriptionIdentifiers().size());
        assertEquals(2, publishInboundInput.getPublishPacket().getUserProperties().asList().size());

        assertNotNull(publishInboundInput.getConnectionInformation());
        assertEquals(publishInboundInput, publishInboundInput.get());


    }

    @Test
    public void test_values_set_correctly_empty_optionals() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final PublishInboundInputImpl publishInboundInput = new PublishInboundInputImpl(new PublishPacketImpl(TestMessageUtil.createMqtt5Publish("topic")), "client", channel);

        assertEquals("client", publishInboundInput.getClientInformation().getClientId());
        assertEquals(false, publishInboundInput.getPublishPacket().getDupFlag());
        assertEquals(Qos.AT_LEAST_ONCE, publishInboundInput.getPublishPacket().getQos());
        assertEquals(false, publishInboundInput.getPublishPacket().getRetain());
        assertEquals("topic", publishInboundInput.getPublishPacket().getTopic());
        assertEquals(1, publishInboundInput.getPublishPacket().getPacketId());
        assertEquals(Optional.empty(), publishInboundInput.getPublishPacket().getPayloadFormatIndicator());
        assertEquals(Optional.empty(), publishInboundInput.getPublishPacket().getMessageExpiryInterval());
        assertEquals(Optional.empty(), publishInboundInput.getPublishPacket().getResponseTopic());
        assertEquals(Optional.empty(), publishInboundInput.getPublishPacket().getCorrelationData());
        assertArrayEquals("payload".getBytes(), Bytes.getBytesFromReadOnlyBuffer(publishInboundInput.getPublishPacket().getPayload()));
        assertEquals(Optional.empty(), publishInboundInput.getPublishPacket().getContentType());
        assertEquals(Collections.emptyList(), publishInboundInput.getPublishPacket().getSubscriptionIdentifiers());
        assertEquals(0, publishInboundInput.getPublishPacket().getUserProperties().asList().size());

        assertNotNull(publishInboundInput.getConnectionInformation());
        assertEquals(publishInboundInput, publishInboundInput.get());


    }
}