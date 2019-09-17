package com.hivemq.mqtt.message.pubrel;

import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extensions.packets.pubrel.ModifiablePubrelPacketImpl;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserPropertiesBuilder;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5PubRelReasonCode;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import static org.junit.Assert.*;

public class PUBRELTest {

    private FullConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
    }

    @Test
    public void test_constructMqtt3() {
        final PUBREL origin =
                new PUBREL(1);
        final ModifiablePubrelPacketImpl packet =
                new ModifiablePubrelPacketImpl(configurationService, origin);
        final PUBREL merged = PUBREL.createPubrelFrom(packet);
        assertNotNull(merged);
        assertNotSame(origin, merged);
        assertPUBRELequals(origin, merged);
    }

    @Test
    public void test_constructMqtt5() {
        final PUBREL origin =
                new PUBREL(
                        1, Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND, "NotAuthorized",
                        Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubrelPacketImpl packet =
                new ModifiablePubrelPacketImpl(configurationService, origin);
        final PUBREL merged = PUBREL.createPubrelFrom(packet);
        assertNotNull(merged);
        assertNotSame(origin, merged);
        assertPUBRELequals(origin, merged);
    }

    @Test
    public void test_constructMqtt5_withUserProperties() {

        @NotNull final Mqtt5UserPropertiesBuilder builder = Mqtt5UserProperties.builder();
        builder.add(new MqttUserProperty("user1", "value1"));
        builder.add(new MqttUserProperty("user2", "value2"));
        builder.add(new MqttUserProperty("user3", "value3"));
        @NotNull final Mqtt5UserProperties userProperties = builder.build();

        final PUBREL origin =
                new PUBREL(1, Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND, "NotAuthorized", userProperties);
        final ModifiablePubrelPacketImpl packet =
                new ModifiablePubrelPacketImpl(configurationService, origin);
        final PUBREL merged = PUBREL.createPubrelFrom(packet);
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