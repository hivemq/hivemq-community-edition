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
package com.hivemq.extensions.services.builder;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.subscribe.RetainHandling;
import com.hivemq.extension.sdk.api.packets.subscribe.Subscription;
import com.hivemq.extension.sdk.api.services.builder.TopicSubscriptionBuilder;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.subscription.TopicSubscription;
import com.hivemq.extensions.packets.subscribe.SubscriptionImpl;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.persistence.clientsession.SharedSubscriptionService;
import com.hivemq.persistence.clientsession.SharedSubscriptionServiceImpl.SharedSubscription;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class TopicSubscriptionBuilderImplTest {

    private FullConfigurationService fullConfigurationService;

    private TopicSubscriptionBuilder topicSubscriptionBuilder;

    @Mock
    private SharedSubscriptionService sharedSubscriptionService;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        fullConfigurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        topicSubscriptionBuilder = new TopicSubscriptionBuilderImpl(fullConfigurationService);

    }

    @Test
    public void test_from_subscription() {

        final SubscriptionImpl subscription = new SubscriptionImpl(
                new Topic("topic", QoS.AT_LEAST_ONCE, false, true, Mqtt5RetainHandling.DO_NOT_SEND, null));

        final TopicSubscription topic = topicSubscriptionBuilder.fromSubscription(subscription)
                .subscriptionIdentifier(1)
                .build();

        assertEquals("topic", topic.getTopicFilter());
        assertEquals(Qos.AT_LEAST_ONCE, topic.getQos());
        assertEquals(true, topic.getRetainAsPublished());
        assertEquals(false, topic.getNoLocal());
        assertTrue(topic.getSubscriptionIdentifier().isPresent());
        assertEquals(1, topic.getSubscriptionIdentifier().get().intValue());

    }

    @Test
    public void test_with_all() {

        final TopicSubscription topic = topicSubscriptionBuilder.topicFilter("topic")
                .qos(Qos.AT_LEAST_ONCE)
                .retainAsPublished(true)
                .noLocal(false)
                .subscriptionIdentifier(1)
                .build();

        assertEquals("topic", topic.getTopicFilter());
        assertEquals(Qos.AT_LEAST_ONCE, topic.getQos());
        assertEquals(true, topic.getRetainAsPublished());
        assertEquals(false, topic.getNoLocal());
        assertTrue(topic.getSubscriptionIdentifier().isPresent());
        assertEquals(1, topic.getSubscriptionIdentifier().get().intValue());

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_with_sub_id_to_small() {

        topicSubscriptionBuilder.topicFilter("topic")
                .qos(Qos.AT_LEAST_ONCE)
                .retainAsPublished(true)
                .noLocal(false)
                .subscriptionIdentifier(0)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_with_sub_id_not_allowed() {

        fullConfigurationService.mqttConfiguration().setSubscriptionIdentifierEnabled(false);

        topicSubscriptionBuilder.topicFilter("topic")
                .qos(Qos.AT_LEAST_ONCE)
                .retainAsPublished(true)
                .noLocal(false)
                .subscriptionIdentifier(1)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_with_topic_empty() {

        topicSubscriptionBuilder.topicFilter("")
                .qos(Qos.AT_LEAST_ONCE)
                .retainAsPublished(true)
                .noLocal(false)
                .subscriptionIdentifier(1)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_with_topic_to_large() {

        topicSubscriptionBuilder.topicFilter(RandomStringUtils.randomAlphanumeric(70000))
                .qos(Qos.AT_LEAST_ONCE)
                .retainAsPublished(true)
                .noLocal(false)
                .subscriptionIdentifier(1)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_with_topic_contains_forbidden_wildcard_hashtag() {

        fullConfigurationService.mqttConfiguration().setWildcardSubscriptionsEnabled(false);

        topicSubscriptionBuilder.topicFilter("#")
                .qos(Qos.AT_LEAST_ONCE)
                .retainAsPublished(true)
                .noLocal(false)
                .subscriptionIdentifier(1)
                .build();
    }

    @Test
    public void test_with_topic_contains_allowed_wildcard_hashtag() {
        final TopicSubscription topic = topicSubscriptionBuilder.topicFilter("#")
                .qos(Qos.AT_LEAST_ONCE)
                .retainAsPublished(true)
                .noLocal(false)
                .subscriptionIdentifier(1)
                .build();

        assertEquals("#", topic.getTopicFilter());
    }

    @Test
    public void test_with_topic_contains_allowed_wildcard_plus() {
        final TopicSubscription topic = topicSubscriptionBuilder.topicFilter("+")
                .qos(Qos.AT_LEAST_ONCE)
                .retainAsPublished(true)
                .noLocal(false)
                .subscriptionIdentifier(1)
                .build();

        assertEquals("+", topic.getTopicFilter());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_with_topic_contains_forbidden_wildcard_plus() {

        fullConfigurationService.mqttConfiguration().setWildcardSubscriptionsEnabled(false);

        topicSubscriptionBuilder.topicFilter("topic/a/+/asd")
                .qos(Qos.AT_LEAST_ONCE)
                .retainAsPublished(true)
                .noLocal(false)
                .subscriptionIdentifier(1)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_with_topic_contains_forbidden_shared_sub() {

        fullConfigurationService.mqttConfiguration().setSharedSubscriptionsEnabled(false);

        topicSubscriptionBuilder.topicFilter("$share/group/topic")
                .qos(Qos.AT_LEAST_ONCE)
                .retainAsPublished(true)
                .noLocal(false)
                .subscriptionIdentifier(1)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_with_topic_contains_forbidden_utf8_should_not_char() {

        topicSubscriptionBuilder.topicFilter("topic" + '\u0001');

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_with_topic_contains_forbidden_utf8_must_not_char() {

        topicSubscriptionBuilder.topicFilter("topic" + '\uD800');

    }

    @Test(expected = DoNotImplementException.class)
    public void test_from_subcription_falsely_implemtented() {

        topicSubscriptionBuilder.fromSubscription(new Subscription() {
            @Override
            public @NotNull String getTopicFilter() {
                return null;
            }

            @Override
            public @NotNull Qos getQos() {
                return null;
            }

            @Override
            public @NotNull RetainHandling getRetainHandling() {
                return null;
            }

            @Override
            public boolean getRetainAsPublished() {
                return false;
            }

            @Override
            public boolean getNoLocal() {
                return false;
            }
        });

    }

    @Test
    public void test_with_topic_contains_allowed_shared_sub() {

        final TopicSubscription topic = topicSubscriptionBuilder.topicFilter("$share/group/topic")
                .qos(Qos.AT_LEAST_ONCE)
                .retainAsPublished(true)
                .noLocal(false)
                .subscriptionIdentifier(1)
                .build();

        assertEquals("$share/group/topic", topic.getTopicFilter());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_shared_sub_with_no_local() {

        topicSubscriptionBuilder.topicFilter("$share/group/topic")
                .qos(Qos.AT_LEAST_ONCE)
                .retainAsPublished(true)
                .noLocal(true)
                .subscriptionIdentifier(1)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_with_topic_contains_empty_shared_sub() {

        when(sharedSubscriptionService.checkForSharedSubscription("$share/group/")).thenReturn(new SharedSubscription("", "group"));
        topicSubscriptionBuilder.topicFilter("$share/group/").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_with_topic_bad_char() {

        topicSubscriptionBuilder.topicFilter("123" + "\u0000")
                .qos(Qos.AT_LEAST_ONCE)
                .retainAsPublished(true)
                .noLocal(false)
                .subscriptionIdentifier(1)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_with_sub_id_to_big() {

        topicSubscriptionBuilder.topicFilter("topic")
                .qos(Qos.AT_LEAST_ONCE)
                .retainAsPublished(true)
                .noLocal(false)
                .subscriptionIdentifier(268_435_455 + 1)
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void test_without_topic() {

        topicSubscriptionBuilder
                .qos(Qos.AT_LEAST_ONCE)
                .retainAsPublished(true)
                .noLocal(false)
                .subscriptionIdentifier(1)
                .build();

    }

    @Test
    public void test_without_qos() {

        final TopicSubscription topic = topicSubscriptionBuilder.topicFilter("topic")
                .retainAsPublished(true)
                .noLocal(false)
                .subscriptionIdentifier(1)
                .build();

        assertEquals("topic", topic.getTopicFilter());
        assertEquals(Qos.AT_MOST_ONCE, topic.getQos());
        assertEquals(true, topic.getRetainAsPublished());
        assertEquals(false, topic.getNoLocal());
        assertTrue(topic.getSubscriptionIdentifier().isPresent());
        assertEquals(1, topic.getSubscriptionIdentifier().get().intValue());
    }

    @Test
    public void test_with_min() {

        final TopicSubscription topic = topicSubscriptionBuilder.topicFilter("topic").build();

        assertEquals("topic", topic.getTopicFilter());
        assertEquals(Qos.AT_MOST_ONCE, topic.getQos());
        assertEquals(false, topic.getRetainAsPublished());
        assertEquals(false, topic.getNoLocal());
        assertFalse(topic.getSubscriptionIdentifier().isPresent());
    }
}