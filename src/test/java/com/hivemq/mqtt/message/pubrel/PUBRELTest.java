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
package com.hivemq.mqtt.message.pubrel;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.packets.pubrel.PubrelPacketImpl;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5PubRelReasonCode;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class PUBRELTest {

    @Test
    public void test_constructMqtt3() {
        final PUBREL origin = new PUBREL(1);
        final PubrelPacketImpl packet = new PubrelPacketImpl(origin);

        final PUBREL merged = PUBREL.from(packet);

        assertNotNull(merged);
        assertNotSame(origin, merged);
        assertPUBRELequals(origin, merged);
    }

    @Test
    public void test_constructMqtt5() {
        final PUBREL origin = new PUBREL(
                1, Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND, "reasonString",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
        final PubrelPacketImpl packet = new PubrelPacketImpl(origin);

        final PUBREL merged = PUBREL.from(packet);

        assertNotNull(merged);
        assertNotSame(origin, merged);
        assertPUBRELequals(origin, merged);
    }

    @Test
    public void test_constructMqtt5_withUserProperties() {
        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(
                new MqttUserProperty("user1", "value1"),
                new MqttUserProperty("user2", "value2"),
                new MqttUserProperty("user3", "value3"));

        final PUBREL origin =
                new PUBREL(1, Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND, "reasonString", userProperties);
        final PubrelPacketImpl packet = new PubrelPacketImpl(origin);

        final PUBREL merged = PUBREL.from(packet);

        assertNotNull(merged);
        assertNotSame(origin, merged);
        assertPUBRELequals(origin, merged);
    }

    private void assertPUBRELequals(final @NotNull PUBREL a, final @NotNull PUBREL b) {
        assertEquals(a.getPacketIdentifier(), b.getPacketIdentifier());
        assertEquals(a.getReasonCode(), b.getReasonCode());
        assertEquals(a.getReasonString(), b.getReasonString());
        assertEquals(a.getUserProperties(), b.getUserProperties());
    }
}