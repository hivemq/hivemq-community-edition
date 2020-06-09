/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import com.hivemq.util.ObjectMemoryEstimation;
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

    @Test
    public void test_estimated_size() {

        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(
                new MqttUserProperty("user1", "value1"),
                new MqttUserProperty("user2", "value2"),
                new MqttUserProperty("user3", "value3"));

        final int userPropertiesSize =
                3 * (
                24 + // overhead
                ObjectMemoryEstimation.stringSize("userx") +
                ObjectMemoryEstimation.stringSize("valuex")
                );

        final String reasonString = "reasonString";
        final int reasonStringSize = ObjectMemoryEstimation.stringSize(reasonString);

        final PUBREL pubrel1 = new PUBREL(1);
        final PUBREL pubrel2 = new PUBREL(1, 100L, 100L);
        final PUBREL pubrel3 =
                new PUBREL(1,
                        Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND,
                        reasonString,
                        userProperties);

        final PUBREL pubrel4 = new PUBREL(1,
                Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND,
                reasonString,
                userProperties,
                100L,
                100L);

        final int fixedSize = ObjectMemoryEstimation.objectShellSize() +
                ObjectMemoryEstimation.intSize() + // sizeInMemory
                ObjectMemoryEstimation.intSize() + // packet id
                ObjectMemoryEstimation.enumSize() + // reason code
                ObjectMemoryEstimation.longWrapperSize() + //publish timestamp
                ObjectMemoryEstimation.longWrapperSize() + //expiry interval
                24; //user props overhead

        final int pubrel3Size = fixedSize + reasonStringSize + userPropertiesSize;
        final int pubrel4Size = fixedSize + reasonStringSize + userPropertiesSize;

        assertEquals(fixedSize, pubrel1.getEstimatedSizeInMemory());
        assertEquals(fixedSize, pubrel2.getEstimatedSizeInMemory());
        assertEquals(pubrel3Size, pubrel3.getEstimatedSizeInMemory());
        assertEquals(pubrel4Size, pubrel4.getEstimatedSizeInMemory());

    }
}