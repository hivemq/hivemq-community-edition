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

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.subscribe.RetainHandling;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.subscribe.Topic;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class ModifiableSubscriptionImplTest {

    private FullConfigurationService configurationService;
    private ModifiableSubscriptionImpl modifiableSubscription;

    @Before
    public void setUp() throws Exception {

        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        modifiableSubscription = new ModifiableSubscriptionImpl(configurationService,
                new Topic("topic", QoS.AT_LEAST_ONCE, false, false, Mqtt5RetainHandling.SEND, 1));

    }

    @Test(expected = NullPointerException.class)
    public void test_set_topic_filter_null() {
        modifiableSubscription.setTopicFilter(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_set_topic_filter_to_long() {
        modifiableSubscription.setTopicFilter(RandomStringUtils.randomAlphanumeric(70000));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_set_topic_filter_empty() {
        modifiableSubscription.setTopicFilter("");
    }

    @Test
    public void test_set_topic_filter_same() {
        modifiableSubscription.setTopicFilter(modifiableSubscription.getTopicFilter());
        assertFalse(modifiableSubscription.isModified());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_set_topic_filter_shared_and_nolocal() {
        modifiableSubscription.setNoLocal(true);
        modifiableSubscription.setTopicFilter("$share/group/topic");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_set_topic_filter_shared_disabled() {
        configurationService.mqttConfiguration().setSharedSubscriptionsEnabled(false);
        modifiableSubscription.setTopicFilter("$share/group/topic");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_set_topic_filter_shared_empty() {
        modifiableSubscription.setTopicFilter("$share/group/");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_set_topic_filter_invalid() {
        modifiableSubscription.setTopicFilter("topic/\u0000");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_set_topic_filter_malformed() {
        modifiableSubscription.setTopicFilter("topic/\uDC00");
    }

    @Test(expected = NullPointerException.class)
    public void test_set_qos_null() {
        modifiableSubscription.setQos(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_set_qos_to_high() {
        configurationService.mqttConfiguration().setMaximumQos(QoS.AT_LEAST_ONCE);
        modifiableSubscription.setQos(Qos.EXACTLY_ONCE);
    }

    @Test
    public void test_set_qos_same() {
        modifiableSubscription.setQos(Qos.AT_LEAST_ONCE);
        assertFalse(modifiableSubscription.isModified());
    }

    @Test
    public void test_set_qos() {
        modifiableSubscription.setQos(Qos.EXACTLY_ONCE);
        assertTrue(modifiableSubscription.isModified());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_set_no_local_to_shared() {
        modifiableSubscription.setTopicFilter("$share/group/topic");
        modifiableSubscription.setNoLocal(true);
    }

    @Test
    public void test_set_no_local_same() {
        modifiableSubscription.setNoLocal(false);
        assertFalse(modifiableSubscription.isModified());
    }

    @Test
    public void test_set_no_local() {
        modifiableSubscription.setNoLocal(true);
        assertTrue(modifiableSubscription.isModified());
    }

    @Test
    public void test_set_retain_as_published_same() {
        modifiableSubscription.setRetainAsPublished(false);
        assertFalse(modifiableSubscription.isModified());
    }

    @Test
    public void test_set_retain_as_published() {
        modifiableSubscription.setRetainAsPublished(true);
        assertTrue(modifiableSubscription.isModified());
    }

    @Test
    public void test_set_retain_handling_same() {
        modifiableSubscription.setRetainHandling(RetainHandling.SEND);
        assertFalse(modifiableSubscription.isModified());
    }

    @Test(expected = NullPointerException.class)
    public void test_set_retain_handling_null() {
        modifiableSubscription.setRetainHandling(null);
        assertFalse(modifiableSubscription.isModified());
    }

    @Test
    public void test_set_retain_handling() {
        modifiableSubscription.setRetainHandling(RetainHandling.DO_NOT_SEND);
        assertTrue(modifiableSubscription.isModified());
    }
}