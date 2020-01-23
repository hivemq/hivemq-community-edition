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

package com.hivemq.extensions.packets.suback;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserPropertiesBuilder;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Robin Atherton
 */
public class ModifiableSubackPacketImplTest {

    private ModifiableSubackPacketImpl packet;

    private SUBACK original;

    private FullConfigurationService configurationService;
    private List<Mqtt5SubAckReasonCode> originalReasonCodes;
    private List<SubackReasonCode> modifiedReasonCodes;

    @Before
    public void setUp() throws Exception {
        originalReasonCodes = new ArrayList<>();
        originalReasonCodes.add(Mqtt5SubAckReasonCode.GRANTED_QOS_2);
        originalReasonCodes.add(Mqtt5SubAckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR);
        originalReasonCodes.add(Mqtt5SubAckReasonCode.QUOTA_EXCEEDED);
        original = createTestSubAck(1, originalReasonCodes, "reasonCodes");
        packet = createTestSubAckPacket(1, originalReasonCodes, "reasonCodes");

        modifiedReasonCodes = new ArrayList<>();
        modifiedReasonCodes.add(SubackReasonCode.GRANTED_QOS_1);
        modifiedReasonCodes.add(SubackReasonCode.UNSPECIFIED_ERROR);
        modifiedReasonCodes.add(SubackReasonCode.NOT_AUTHORIZED);
    }

    @Test
    public void test_change_all_valid_values() {
        final List<SubackReasonCode> reasonCodes = new ArrayList<>();
        reasonCodes.add(SubackReasonCode.GRANTED_QOS_1);
        reasonCodes.add(SubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR);
        reasonCodes.add(SubackReasonCode.NOT_AUTHORIZED);

        packet.setReasonString("testReasonString");
        packet.setReasonCodes(reasonCodes);

        assertEquals("testReasonString", packet.getReasonString().get());
        assertEquals(SubackReasonCode.GRANTED_QOS_1, packet.getReasonCodes().get(0));
        assertEquals(SubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, packet.getReasonCodes().get(1));
        assertEquals(SubackReasonCode.NOT_AUTHORIZED, packet.getReasonCodes().get(2));
    }

    @Test
    public void test_modify_packet() {
        packet = new ModifiableSubackPacketImpl(configurationService, original);
        packet.setReasonCodes(modifiedReasonCodes);
        assertTrue(packet.isModified());

        packet = new ModifiableSubackPacketImpl(configurationService, original);
        packet.setReasonString("testTestTest");
        assertTrue(packet.isModified());
    }

    @Test
    public void test_set_reason_string_null() {
        packet.setReasonString(null);
        assertTrue(packet.isModified());
    }

    @Test(expected = NullPointerException.class)
    public void test_set_reason_codes_null() {
        packet.setReasonCodes(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_set_reason_codes_element_null() {
        final List<SubackReasonCode> reasonCodes = new ArrayList<>();
        reasonCodes.add(SubackReasonCode.GRANTED_QOS_0);
        reasonCodes.add(null);
        reasonCodes.add(SubackReasonCode.UNSPECIFIED_ERROR);
        try {
            packet.setReasonCodes(reasonCodes);
        } catch (final NullPointerException e) {
            assertEquals("Reason code (at index 1) must never be null.", e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void reasonString_invalid_input() {
        packet.setReasonString("topic" + '\u0001');
    }

    @Test(expected = IllegalArgumentException.class)
    public void reasonString_exceeds_max_length() {
        final StringBuilder s = new StringBuilder("s");
        for (int i = 0; i < 65535; i++) {
            s.append("s");
        }
        packet.setReasonString(s.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_reason_codes_with_different_sizes() {
        final ArrayList<SubackReasonCode> subackReasonCodes = new ArrayList<>();
        subackReasonCodes.add(SubackReasonCode.GRANTED_QOS_0);
        subackReasonCodes.add(SubackReasonCode.SHARED_SUBSCRIPTION_NOT_SUPPORTED);
        packet.setReasonCodes(subackReasonCodes);
    }

    private ModifiableSubackPacketImpl createTestSubAckPacket(
            final int packetIdentifier,
            final List<Mqtt5SubAckReasonCode> reasonCodes,
            final String reasonString) {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        final Mqtt5UserPropertiesBuilder builder =
                Mqtt5UserProperties.builder().add(new MqttUserProperty("test", "test"));
        final Mqtt5UserProperties properties = builder.build();
        final SUBACK suback = new SUBACK(packetIdentifier, reasonCodes, reasonString, properties);
        return new ModifiableSubackPacketImpl(configurationService, suback);
    }

    private SUBACK createTestSubAck(
            final int packetIdentifier,
            final List<Mqtt5SubAckReasonCode> reasonCodes,
            final String reasonString) {
        final Mqtt5UserPropertiesBuilder builder =
                Mqtt5UserProperties.builder().add(new MqttUserProperty("test", "test"));
        final Mqtt5UserProperties properties = builder.build();
        return new SUBACK(packetIdentifier, reasonCodes, reasonString, properties);
    }
}