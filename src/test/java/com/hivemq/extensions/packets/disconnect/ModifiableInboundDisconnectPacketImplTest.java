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
package com.hivemq.extensions.packets.disconnect;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserPropertiesBuilder;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import static org.junit.Assert.*;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class ModifiableInboundDisconnectPacketImplTest {

    private ModifiableInboundDisconnectPacketImpl packet;

    private DISCONNECT original;

    private FullConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        original = createTestDisconnect(5);
        packet = new ModifiableInboundDisconnectPacketImpl(configurationService, original, 5);
    }

    @Test
    public void test_change_all_valid_values() {
        packet.setReasonCode(DisconnectReasonCode.NORMAL_DISCONNECTION);
        packet.setReasonString("normal disconnection");
        packet.setSessionExpiryInterval(0L);

        assertEquals("normal disconnection", packet.getReasonString().get());
        assertEquals(DisconnectReasonCode.NORMAL_DISCONNECTION, packet.getReasonCode());
        assertEquals(0, (long) packet.getSessionExpiryInterval().get());
    }

    @Test
    public void test_modify_packet() {
        packet = new ModifiableInboundDisconnectPacketImpl(configurationService, original, 5);
        packet.setReasonCode(DisconnectReasonCode.QUOTA_EXCEEDED);
        assertTrue(packet.isModified());

        packet = new ModifiableInboundDisconnectPacketImpl(configurationService, original, 5);
        packet.setReasonString("DisconnectReasonCode.");
        assertTrue(packet.isModified());

        packet = new ModifiableInboundDisconnectPacketImpl(configurationService, original, 5);
        packet.setSessionExpiryInterval(0L);
        assertTrue(packet.isModified());
    }

    @Test(expected = NullPointerException.class)
    public void reasonCode_null() {
        packet.setReasonCode(null);
    }

    @Test
    public void reasonString_null() {
        packet.setReasonString(null);
        assertFalse(packet.getReasonString().isPresent());
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
    public void sessionExpiryInterval_is_less_than_0() {
        packet.setSessionExpiryInterval(-1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void sessionExpiryInterval_is_greater_than_maximum() {
        packet.setSessionExpiryInterval(Long.MAX_VALUE);
    }

    @Test
    public void sessionExpiryInterval_set_to_zero_and_back() {
        packet.setSessionExpiryInterval(0L);
        packet.setSessionExpiryInterval(5L);
    }

    @Test(expected = IllegalStateException.class)
    public void sessionExpiryInterval_set_to_zero() {
        original = createTestDisconnect(0);
        packet = new ModifiableInboundDisconnectPacketImpl(configurationService, original, 0);
        packet.setSessionExpiryInterval(1L);
    }

    private DISCONNECT createTestDisconnect(final int sessionExpiryInterval) {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        final Mqtt5UserPropertiesBuilder builder =
                Mqtt5UserProperties.builder().add(new MqttUserProperty("test", "test"));
        final Mqtt5UserProperties properties = builder.build();
        return new DISCONNECT(
                Mqtt5DisconnectReasonCode.ADMINISTRATIVE_ACTION, "administrative Action", properties, "serverReference",
                sessionExpiryInterval);
    }
}