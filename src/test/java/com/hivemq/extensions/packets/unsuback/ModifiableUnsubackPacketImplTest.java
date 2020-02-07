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
package com.hivemq.extensions.packets.unsuback;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.unsuback.UnsubackReasonCode;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserPropertiesBuilder;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5UnsubAckReasonCode;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
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
public class ModifiableUnsubackPacketImplTest {

    private ModifiableUnsubackPacketImpl packet;

    private UNSUBACK original;

    private FullConfigurationService configurationService;
    private List<Mqtt5UnsubAckReasonCode> originalreasonCodes;
    private List<UnsubackReasonCode> modifiedReasonCodes;

    @Before
    public void setUp() throws Exception {
        originalreasonCodes = new ArrayList<>();
        originalreasonCodes.add(Mqtt5UnsubAckReasonCode.NO_SUBSCRIPTIONS_EXISTED);
        originalreasonCodes.add(Mqtt5UnsubAckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR);
        originalreasonCodes.add(Mqtt5UnsubAckReasonCode.TOPIC_FILTER_INVALID);
        original = createTestUnsuback(1, originalreasonCodes, "reasonCodes");
        packet = createTestUnsubackPacket(1, originalreasonCodes, "reasonCodes");

        modifiedReasonCodes = new ArrayList<>();
        modifiedReasonCodes.add(UnsubackReasonCode.SUCCESS);
        modifiedReasonCodes.add(UnsubackReasonCode.UNSPECIFIED_ERROR);
        modifiedReasonCodes.add(UnsubackReasonCode.NOT_AUTHORIZED);
    }

    @Test
    public void test_change_all_valid_values() {
        final List<UnsubackReasonCode> reasonCodes = new ArrayList<>();
        reasonCodes.add(UnsubackReasonCode.SUCCESS);
        reasonCodes.add(UnsubackReasonCode.UNSPECIFIED_ERROR);
        reasonCodes.add(UnsubackReasonCode.NOT_AUTHORIZED);

        packet.setReasonString("testReasonString");
        packet.setReasonCodes(reasonCodes);

        assertEquals("testReasonString", packet.getReasonString().get());
        assertEquals(UnsubackReasonCode.SUCCESS, packet.getReasonCodes().get(0));
        assertEquals(UnsubackReasonCode.UNSPECIFIED_ERROR, packet.getReasonCodes().get(1));
        assertEquals(UnsubackReasonCode.NOT_AUTHORIZED, packet.getReasonCodes().get(2));
    }

    @Test
    public void test_modify_packet() {
        packet = new ModifiableUnsubackPacketImpl(configurationService, original);
        packet.setReasonCodes(modifiedReasonCodes);
        assertTrue(packet.isModified());

        packet = new ModifiableUnsubackPacketImpl(configurationService, original);
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
        final List<UnsubackReasonCode> reasonCodes = new ArrayList<>();
        reasonCodes.add(UnsubackReasonCode.SUCCESS);
        reasonCodes.add(null);
        reasonCodes.add(UnsubackReasonCode.UNSPECIFIED_ERROR);
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
        final ArrayList<UnsubackReasonCode> unsubackReasonCodes = new ArrayList<>();
        unsubackReasonCodes.add(UnsubackReasonCode.TOPIC_FILTER_INVALID);
        unsubackReasonCodes.add(UnsubackReasonCode.PACKET_IDENTIFIER_IN_USE);
        packet.setReasonCodes(unsubackReasonCodes);
    }

    private @NotNull ModifiableUnsubackPacketImpl createTestUnsubackPacket(
            final int packetIdentifier,
            final @NotNull List<Mqtt5UnsubAckReasonCode> reasonCodes,
            final @NotNull String reasonString) {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        final Mqtt5UserPropertiesBuilder builder =
                Mqtt5UserProperties.builder().add(new MqttUserProperty("test", "test"));
        final Mqtt5UserProperties properties = builder.build();
        final UNSUBACK unsuback = new UNSUBACK(packetIdentifier, reasonCodes, reasonString, properties);
        return new ModifiableUnsubackPacketImpl(configurationService, unsuback);
    }

    private @NotNull UNSUBACK createTestUnsuback(
            final int packetIdentifier,
            final @NotNull List<Mqtt5UnsubAckReasonCode> reasonCodes,
            final @NotNull String reasonString) {
        final Mqtt5UserPropertiesBuilder builder =
                Mqtt5UserProperties.builder().add(new MqttUserProperty("test", "test"));
        final Mqtt5UserProperties properties = builder.build();
        return new UNSUBACK(packetIdentifier, reasonCodes, reasonString, properties);
    }
}