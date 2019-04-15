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

package com.hivemq.extensions.packets.publish;

import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.util.Bytes;
import org.junit.Test;
import util.TestMessageUtil;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class PublishPacketImplTest {

    private PublishPacketImpl publishPacket;

    @Test
    public void test_values_correct() {

        final PUBLISH fullMqtt5Publish = TestMessageUtil.createFullMqtt5Publish();

        publishPacket = new PublishPacketImpl(fullMqtt5Publish);

        assertEquals(publishPacket.getDupFlag(), fullMqtt5Publish.isDuplicateDelivery());
        assertEquals(publishPacket.getQos(), Qos.valueOf(fullMqtt5Publish.getQoS().getQosNumber()));
        assertEquals(publishPacket.getRetain(), fullMqtt5Publish.isRetain());
        assertEquals(publishPacket.getTopic(), fullMqtt5Publish.getTopic());
        assertEquals(publishPacket.getPacketId(), fullMqtt5Publish.getPacketIdentifier());
        assertEquals(publishPacket.getPayloadFormatIndicator().get(), PayloadFormatIndicator.valueOf(fullMqtt5Publish.getPayloadFormatIndicator().name()));
        assertEquals(publishPacket.getMessageExpiryInterval().get().longValue(), fullMqtt5Publish.getMessageExpiryInterval());
        assertEquals(publishPacket.getResponseTopic().get(), fullMqtt5Publish.getResponseTopic());
        assertArrayEquals(Bytes.getBytesFromReadOnlyBuffer(publishPacket.getCorrelationData()), fullMqtt5Publish.getCorrelationData());
        assertEquals(publishPacket.getSubscriptionIdentifiers(), fullMqtt5Publish.getSubscriptionIdentifiers());
        assertEquals(publishPacket.getContentType().get(), fullMqtt5Publish.getContentType());
        assertArrayEquals(Bytes.getBytesFromReadOnlyBuffer(publishPacket.getPayload()), fullMqtt5Publish.getPayload());
        assertEquals(publishPacket.getUserProperties().asList().size(), fullMqtt5Publish.getUserProperties().getPluginUserProperties().asList().size());

    }
}