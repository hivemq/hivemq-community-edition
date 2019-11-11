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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ModifiableUnsubscribePacketImplTest {

    private FullConfigurationService configurationService;
    private ModifiableUnsubscribePacketImpl modifiableUnsubscribePacket;

    @Before
    public void setUp() throws Exception {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        modifiableUnsubscribePacket = testUnsubscribePacket();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_add_topics_same() {
        modifiableUnsubscribePacket.addTopics("Test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void remove_non_existing_topic() {
        modifiableUnsubscribePacket.removeTopics("Test123");
    }

    @Test
    public void test_add_topics_different() {
        final int sizeBefore = modifiableUnsubscribePacket.getTopics().size();
        modifiableUnsubscribePacket.addTopics("Test/Different");
        final int sizeAfter = modifiableUnsubscribePacket.getTopics().size();
        assertNotEquals(sizeAfter, sizeBefore);
    }

    @Test
    public void remove_existing_topic() {
        final int sizeBefore = modifiableUnsubscribePacket.getTopics().size();
        modifiableUnsubscribePacket.removeTopics("Test");
        final int sizeAfter = modifiableUnsubscribePacket.getTopics().size();
        assertNotEquals(sizeAfter, sizeBefore);
    }

    @Test(expected = IllegalArgumentException.class)
    public void remove_non_existent_topic() {
        final int sizeBefore = modifiableUnsubscribePacket.getTopics().size();
        modifiableUnsubscribePacket.removeTopics("Non Existing  ");
        final int sizeAfter = modifiableUnsubscribePacket.getTopics().size();
        assertNotEquals(sizeAfter, sizeBefore);
    }

    @Test
    public void set_topics() {
        final ArrayList<String> topics = new ArrayList<>();
        topics.add("Test1");
        topics.add("Test2");
        topics.add("Test3");
        modifiableUnsubscribePacket.setTopics(topics);
        assertEquals("Test3", topics.get(2));
    }
    private ModifiableUnsubscribePacketImpl testUnsubscribePacket() {
        final ImmutableList<String> topics = ImmutableList.of("Test", "Test/Topic");
        final @NotNull Mqtt5UserProperties props =
                Mqtt5UserProperties.builder().add(MqttUserProperty.of("Test", "TestValue")).build();
        final UNSUBSCRIBE unsubscribe = new UNSUBSCRIBE(topics, 1, props);
        return new ModifiableUnsubscribePacketImpl(configurationService, unsubscribe);
    }

}