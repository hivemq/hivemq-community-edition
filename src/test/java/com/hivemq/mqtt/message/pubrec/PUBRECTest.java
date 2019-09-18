package com.hivemq.mqtt.message.pubrec;

import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extensions.packets.pubrec.ModifiablePubrecPacketImpl;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserPropertiesBuilder;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import static org.junit.Assert.*;

/**
 * @author Yannick Weber
 */
public class PUBRECTest {

    private FullConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
    }

    @Test
    public void test_constructMqtt3() {
        final PUBREC origin =
                new PUBREC(1);
        final ModifiablePubrecPacketImpl packet =
                new ModifiablePubrecPacketImpl(configurationService, origin);
        final PUBREC merged = PUBREC.createPubrecFrom(packet);
        assertNotNull(merged);
        assertNotSame(origin, merged);
        assertPUBRECequals(origin, merged);
    }

    @Test
    public void test_constructMqtt5() {
        final PUBREC origin =
                new PUBREC(
                        1, Mqtt5PubRecReasonCode.NOT_AUTHORIZED, "NotAuthorized",
                        Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubrecPacketImpl packet =
                new ModifiablePubrecPacketImpl(configurationService, origin);
        final PUBREC merged = PUBREC.createPubrecFrom(packet);
        assertNotNull(merged);
        assertNotSame(origin, merged);
        assertPUBRECequals(origin, merged);
    }

    @Test
    public void test_constructMqtt5_withUserProperties() {

        @NotNull final Mqtt5UserPropertiesBuilder builder = Mqtt5UserProperties.builder();
        builder.add(new MqttUserProperty("user1", "value1"));
        builder.add(new MqttUserProperty("user2", "value2"));
        builder.add(new MqttUserProperty("user3", "value3"));
        @NotNull final Mqtt5UserProperties userProperties = builder.build();

        final PUBREC origin =
                new PUBREC(1, Mqtt5PubRecReasonCode.NOT_AUTHORIZED, "NotAuthorized", userProperties);
        final ModifiablePubrecPacketImpl packet =
                new ModifiablePubrecPacketImpl(configurationService, origin);
        final PUBREC merged = PUBREC.createPubrecFrom(packet);
        assertNotNull(merged);
        assertNotSame(origin, merged);
        assertPUBRECequals(origin, merged);
    }

    private void assertPUBRECequals(final @NotNull PUBREC a, final @NotNull PUBREC b) {
        assertEquals(a.getPacketIdentifier(), b.getPacketIdentifier());
        assertEquals(a.getReasonCode(), b.getReasonCode());
        assertEquals(a.getReasonString(), b.getReasonString());
        assertEquals(a.getUserProperties(), b.getUserProperties());
    }

}