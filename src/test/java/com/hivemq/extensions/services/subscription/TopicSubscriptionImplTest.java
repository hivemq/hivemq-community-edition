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
package com.hivemq.extensions.services.subscription;

import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.subscribe.Topic;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class TopicSubscriptionImplTest {

    @Test
    public void test_from_topic() {

        final Topic topic = new Topic("topic", QoS.AT_LEAST_ONCE, true,
                true, Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, 1);

        final TopicSubscriptionImpl subscription = new TopicSubscriptionImpl(topic);

        assertEquals("topic", subscription.getTopicFilter());
        assertEquals(Qos.AT_LEAST_ONCE, subscription.getQos());
        assertEquals(true, subscription.getNoLocal());
        assertEquals(true, subscription.getRetainAsPublished());
        assertTrue(subscription.getSubscriptionIdentifier().isPresent());
        assertEquals(1, subscription.getSubscriptionIdentifier().get().intValue());

    }

    @Test
    public void test_convert_to_topic_do_not_send() {

        final Topic topic = new Topic("topic", QoS.AT_LEAST_ONCE, true,
                true, Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, 1);

        final TopicSubscriptionImpl subscription = new TopicSubscriptionImpl(topic);

        final Topic converted = TopicSubscriptionImpl.convertToTopic(subscription);

        assertEquals("topic", converted.getTopic());
        assertEquals(QoS.AT_LEAST_ONCE, converted.getQoS());
        assertEquals(Mqtt5RetainHandling.DO_NOT_SEND, converted.getRetainHandling());
        assertEquals(true, converted.isNoLocal());
        assertEquals(true, converted.isRetainAsPublished());
        assertEquals(1, converted.getSubscriptionIdentifier().intValue());

    }
}