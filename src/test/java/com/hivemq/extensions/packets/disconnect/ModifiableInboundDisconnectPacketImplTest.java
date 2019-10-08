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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ModifiableInboundDisconnectPacketImplTest {

    private ModifiableInboundDisconnectPacketImpl packet;

    private DISCONNECT original;

    private FullConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        final Mqtt5UserPropertiesBuilder builder = Mqtt5UserProperties.builder().add(new MqttUserProperty("test", "test"));
        final Mqtt5UserProperties properties = builder.build();
        original = new DISCONNECT(Mqtt5DisconnectReasonCode.ADMINISTRATIVE_ACTION, "administrative Action", properties, "serverReference", 5);
        packet = new ModifiableInboundDisconnectPacketImpl(configurationService, original);
    }

    @Test
    public void test_change_all_valid_values() {
        packet.setReasonCode(DisconnectReasonCode.NORMAL_DISCONNECTION);
        packet.setReasonString("normal disconnection");
        packet.setServerReference("test server reference");
        packet.setSessionExpiryInterval(0);

        assertEquals("normal disconnection", packet.getReasonString());
        assertEquals("test server reference", packet.getServerReference());
        assertEquals(DisconnectReasonCode.NORMAL_DISCONNECTION, packet.getReasonCode());
        assertEquals(0, packet.getSessionExpiryInterval());
    }

    @Test
    public void test_modify_packet() {
        packet = new ModifiableInboundDisconnectPacketImpl(configurationService, original);
        packet.setReasonCode(DisconnectReasonCode.BAD_AUTHENTICATION_METHOD);
        assertTrue(packet.isModified());

        packet = new ModifiableInboundDisconnectPacketImpl(configurationService, original);
        packet.setReasonString("DisconnectReasonCode.");
        assertTrue(packet.isModified());

        packet = new ModifiableInboundDisconnectPacketImpl(configurationService, original);
        packet.setSessionExpiryInterval(0);
        assertTrue(packet.isModified());

        packet = new ModifiableInboundDisconnectPacketImpl(configurationService, original);
        packet.setServerReference("server reference changed");
        assertTrue(packet.isModified());
    }

    @Test(expected = NullPointerException.class)
    public void reasonCode_null() {
        packet.setReasonCode(null);
    }

    @Test(expected = NullPointerException.class)
    public void reasonString_null() {
        packet.setReasonString(null);
    }

    @Test(expected = NullPointerException.class)
    public void serverReference_null() {
        packet.setServerReference(null);
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
    public void serverReference_invalid_input() {
        packet.setServerReference("topic" + '\u0001');
    }

    @Test(expected = IllegalArgumentException.class)
    public void serverReference_exceeds_max_length() {
        final StringBuilder s = new StringBuilder("s");
        for (int i = 0; i < 65535; i++) {
            s.append("s");
        }
        packet.setServerReference(s.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void sessionExpiryInterval_is_less_than_0() {
        packet.setSessionExpiryInterval(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void sessionExpiryInterval_is_greater_than_maximum() {
        packet.setSessionExpiryInterval(Long.MAX_VALUE);
    }

    @Test(expected = IllegalStateException.class)
    public void sessionExpiryInterval_was_already_zero() {
        packet.setSessionExpiryInterval(0);
        packet.setSessionExpiryInterval(1);
    }
}