package com.hivemq.extensions.packets.unsubscribe;

import com.google.common.collect.ImmutableList;
import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * @author Robin Atherton
 */
public class ModifiableUnsubscribePacketImplTest {

    private FullConfigurationService configurationService;
    private ModifiableUnsubscribePacketImpl modifiableUnsubscribePacket;

    @Before
    public void setUp() throws Exception {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        modifiableUnsubscribePacket = testUnsubscribePacket();
    }

    @Test
    public void test_add_topics_same() {
        modifiableUnsubscribePacket.addTopics("Test");
        assertEquals(2, modifiableUnsubscribePacket.getTopics().size());
        assertFalse(modifiableUnsubscribePacket.isModified());
    }

    @Test
    public void test_add_topics_different() {
        modifiableUnsubscribePacket.addTopics("Test/Different");
        assertEquals(3, modifiableUnsubscribePacket.getTopics().size());
        assertTrue(modifiableUnsubscribePacket.isModified());
    }

    @Test
    public void remove_existing_topic() {
        modifiableUnsubscribePacket.removeTopics("Test");
        assertEquals(1, modifiableUnsubscribePacket.getTopics().size());
        assertTrue(modifiableUnsubscribePacket.isModified());
    }

    @Test
    public void remove_non_existent_topic() {
        modifiableUnsubscribePacket.removeTopics("Non Existing");
        assertEquals(2, modifiableUnsubscribePacket.getTopics().size());
        assertFalse(modifiableUnsubscribePacket.isModified());
    }

    @Test
    public void set_topics() {
        final ArrayList<String> topics = new ArrayList<>();
        topics.add("Test1");
        topics.add("Test2");
        topics.add("Test3");
        modifiableUnsubscribePacket.addTopics("Test1");
        modifiableUnsubscribePacket.addTopics("Test2");
        modifiableUnsubscribePacket.addTopics("Test3");
        assertEquals(5, modifiableUnsubscribePacket.getTopics().size());
        modifiableUnsubscribePacket.setTopics(topics);
        assertEquals(3, modifiableUnsubscribePacket.getTopics().size());

    }

    private ModifiableUnsubscribePacketImpl testUnsubscribePacket() {
        final ImmutableList<String> topics = ImmutableList.of("Test", "Test/Topic");
        final @NotNull Mqtt5UserProperties props =
                Mqtt5UserProperties.builder().add(MqttUserProperty.of("Test", "TestValue")).build();
        final UNSUBSCRIBE unsubscribe = new UNSUBSCRIBE(topics, 1, props);
        return new ModifiableUnsubscribePacketImpl(configurationService, unsubscribe);
    }

}