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

package com.hivemq.extensions.packets.subscribe;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.subscribe.ModifiableSubscription;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.subscribe.Mqtt5SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class ModifiableSubscribePacketImplTest {

    private FullConfigurationService configurationService;
    private ModifiableSubscribePacketImpl modifiableSubscribePacket;

    @Before
    public void setUp() throws Exception {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        modifiableSubscribePacket = new ModifiableSubscribePacketImpl(configurationService, new SUBSCRIBE(TestMessageUtil.TEST_USER_PROPERTIES,
                ImmutableList.of(new Topic("topic", QoS.AT_LEAST_ONCE, false, false, Mqtt5RetainHandling.SEND, 1)), 1, 1));
    }

    @Test
    public void test_clear_user_properties() {
        final ModifiableUserPropertiesImpl userProperties = modifiableSubscribePacket.getUserProperties();

        assertFalse(userProperties.asList().isEmpty());

        userProperties.clear();

        assertTrue(userProperties.asList().isEmpty());
    }

    @Test
    public void test_subscribe_packet_not_modified() {

        final ModifiableSubscription modifiableSubscription = modifiableSubscribePacket.getSubscriptions().get(0);
        modifiableSubscription.setTopicFilter(modifiableSubscription.getTopicFilter());
        modifiableSubscription.setQos(modifiableSubscription.getQos());
        modifiableSubscription.setRetainHandling(modifiableSubscription.getRetainHandling());
        modifiableSubscription.setRetainAsPublished(modifiableSubscription.getRetainAsPublished());
        modifiableSubscription.setNoLocal(modifiableSubscription.getNoLocal());


        assertFalse(modifiableSubscribePacket.isModified());
        assertFalse(modifiableSubscribePacket.getUserProperties().isModified());
        assertEquals(1, modifiableSubscribePacket.getPacketId());

    }

    @Test
    public void test_subscribe_packet_modified_by_subscription() {

        final ModifiableSubscription modifiableSubscription = modifiableSubscribePacket.getSubscriptions().get(0);
        modifiableSubscription.setTopicFilter(modifiableSubscription.getTopicFilter() + "/modified");

        assertTrue(modifiableSubscribePacket.isModified());

    }

    @Test
    public void test_subscribe_packet_modified_by_user_properties() {

        modifiableSubscribePacket.getUserProperties().addUserProperty("test123", "prop123");
        assertTrue(modifiableSubscribePacket.isModified());

    }

    @Test
    public void test_subscribe_packet_mqtt5() {

        modifiableSubscribePacket = new ModifiableSubscribePacketImpl(configurationService, new SUBSCRIBE(TestMessageUtil.TEST_USER_PROPERTIES,
                ImmutableList.of(new Topic("topic", QoS.AT_LEAST_ONCE, false, false, Mqtt5RetainHandling.SEND, 1)), 1, 1));

        assertTrue(modifiableSubscribePacket.getSubscriptionIdentifier().isPresent());

    }

    @Test
    public void test_subscribe_packet_mqtt3() {

        modifiableSubscribePacket = new ModifiableSubscribePacketImpl(configurationService, new SUBSCRIBE(TestMessageUtil.TEST_USER_PROPERTIES,
                ImmutableList.of(new Topic("topic", QoS.AT_LEAST_ONCE, false, false, Mqtt5RetainHandling.SEND, null)), 1, Mqtt5SUBSCRIBE.DEFAULT_NO_SUBSCRIPTION_IDENTIFIER));

        assertFalse(modifiableSubscribePacket.getSubscriptionIdentifier().isPresent());

    }

    @Test
    public void test_subscribe_from_modified_packet_mqtt5() {

        final SubscribePacketImpl subscribePacket = new SubscribePacketImpl(modifiableSubscribePacket);

        assertEquals(subscribePacket.getPacketId(), modifiableSubscribePacket.getPacketId());
        assertEquals(subscribePacket.getSubscriptionIdentifier().isPresent(), modifiableSubscribePacket.getSubscriptionIdentifier().isPresent());
        assertEquals(subscribePacket.getUserProperties(), modifiableSubscribePacket.getUserProperties());
        assertEquals(subscribePacket.getSubscriptions(), modifiableSubscribePacket.getSubscriptions());

    }

    @Test
    public void test_subscribe_from_modified_packet_mqtt3() {

        modifiableSubscribePacket = new ModifiableSubscribePacketImpl(configurationService, new SUBSCRIBE(TestMessageUtil.TEST_USER_PROPERTIES,
                ImmutableList.of(new Topic("topic", QoS.AT_LEAST_ONCE, false, false, Mqtt5RetainHandling.SEND, null)), 1, Mqtt5SUBSCRIBE.DEFAULT_NO_SUBSCRIPTION_IDENTIFIER));

        final SubscribePacketImpl subscribePacket = new SubscribePacketImpl(modifiableSubscribePacket);

        assertEquals(subscribePacket.getPacketId(), modifiableSubscribePacket.getPacketId());
        assertEquals(subscribePacket.getSubscriptionIdentifier().isPresent(), modifiableSubscribePacket.getSubscriptionIdentifier().isPresent());
        assertEquals(subscribePacket.getUserProperties(), modifiableSubscribePacket.getUserProperties());
        assertEquals(subscribePacket.getSubscriptions(), modifiableSubscribePacket.getSubscriptions());

    }

    @Test
    public void test_subscribe_from_SUBSCRIBE_mqtt5() {

        final SUBSCRIBE subscribe = new SUBSCRIBE(TestMessageUtil.TEST_USER_PROPERTIES,
                ImmutableList.of(new Topic("topic", QoS.AT_LEAST_ONCE, false, false, Mqtt5RetainHandling.SEND, 1)), 1, 1);

        final SubscribePacketImpl subscribePacket = new SubscribePacketImpl(subscribe);

        assertEquals(subscribePacket.getPacketId(), subscribe.getPacketIdentifier());
        assertEquals(subscribePacket.getSubscriptionIdentifier().get().intValue(), subscribe.getSubscriptionIdentifier());
        assertEquals(subscribePacket.getSubscriptions().get(0).getTopicFilter(), subscribe.getTopics().get(0).getTopic());

    }

    @Test
    public void test_subscribe_from_SUBSCRIBE_mqtt3() {

        final SUBSCRIBE subscribe = new SUBSCRIBE(TestMessageUtil.TEST_USER_PROPERTIES,
                ImmutableList.of(new Topic("topic", QoS.AT_LEAST_ONCE, false, false, Mqtt5RetainHandling.SEND, null)), 1, Mqtt5SUBSCRIBE.DEFAULT_NO_SUBSCRIPTION_IDENTIFIER);

        final SubscribePacketImpl subscribePacket = new SubscribePacketImpl(subscribe);

        assertEquals(subscribePacket.getPacketId(), subscribe.getPacketIdentifier());
        assertFalse(subscribePacket.getSubscriptionIdentifier().isPresent());
        assertEquals(subscribePacket.getSubscriptions().get(0).getTopicFilter(), subscribe.getTopics().get(0).getTopic());

    }
}