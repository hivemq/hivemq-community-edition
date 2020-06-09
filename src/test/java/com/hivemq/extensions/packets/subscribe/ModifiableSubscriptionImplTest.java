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
package com.hivemq.extensions.packets.subscribe;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.subscribe.RetainHandling;
import com.hivemq.mqtt.message.QoS;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
public class ModifiableSubscriptionImplTest {

    private @NotNull FullConfigurationService configurationService;

    @Before
    public void setUp() {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
    }

    @Test
    public void setTopicFilter() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        assertFalse(modifiableSubscription.isModified());

        modifiableSubscription.setTopicFilter("test");

        assertTrue(modifiableSubscription.isModified());
        assertEquals("test", modifiableSubscription.getTopicFilter());
    }

    @Test
    public void setTopicFilter_same() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        assertFalse(modifiableSubscription.isModified());

        modifiableSubscription.setTopicFilter("topic");

        assertFalse(modifiableSubscription.isModified());
    }

    @Test(expected = NullPointerException.class)
    public void setTopicFilter_null() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        modifiableSubscription.setTopicFilter(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTopicFilter_toLong() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        modifiableSubscription.setTopicFilter(RandomStringUtils.randomAlphanumeric(70000));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTopicFilter_empty() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        modifiableSubscription.setTopicFilter("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTopicFilter_sharedAndNoLocal() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, true);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        modifiableSubscription.setTopicFilter("$share/group/topic");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTopicFilter_sharedDisabled() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        configurationService.mqttConfiguration().setSharedSubscriptionsEnabled(false);
        modifiableSubscription.setTopicFilter("$share/group/topic");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTopicFilter_sharedEmpty() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        modifiableSubscription.setTopicFilter("$share/group/");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTopicFilter_invalid() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        modifiableSubscription.setTopicFilter("topic/\u0000");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTopicFilter_malformed() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        modifiableSubscription.setTopicFilter("topic/\uDC00");
    }

    @Test
    public void setQos() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        assertFalse(modifiableSubscription.isModified());

        modifiableSubscription.setQos(Qos.EXACTLY_ONCE);

        assertTrue(modifiableSubscription.isModified());
        assertEquals(Qos.EXACTLY_ONCE, modifiableSubscription.getQos());
    }

    @Test
    public void setQos_same() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        assertFalse(modifiableSubscription.isModified());

        modifiableSubscription.setQos(Qos.AT_LEAST_ONCE);

        assertFalse(modifiableSubscription.isModified());
        assertEquals(Qos.AT_LEAST_ONCE, modifiableSubscription.getQos());
    }

    @Test(expected = NullPointerException.class)
    public void setQos_null() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        modifiableSubscription.setQos(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setQos_tooHigh() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        configurationService.mqttConfiguration().setMaximumQos(QoS.AT_LEAST_ONCE);
        modifiableSubscription.setQos(Qos.EXACTLY_ONCE);
    }

    @Test
    public void setRetainHandling() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        assertFalse(modifiableSubscription.isModified());

        modifiableSubscription.setRetainHandling(RetainHandling.DO_NOT_SEND);

        assertTrue(modifiableSubscription.isModified());
        assertEquals(RetainHandling.DO_NOT_SEND, modifiableSubscription.getRetainHandling());
    }

    @Test
    public void setRetainHandling_same() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        assertFalse(modifiableSubscription.isModified());

        modifiableSubscription.setRetainHandling(RetainHandling.SEND);

        assertFalse(modifiableSubscription.isModified());
        assertEquals(RetainHandling.SEND, modifiableSubscription.getRetainHandling());
    }

    @Test(expected = NullPointerException.class)
    public void setRetainHandling_null() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        modifiableSubscription.setRetainHandling(null);
    }

    @Test
    public void setNoLocal() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        assertFalse(modifiableSubscription.isModified());

        modifiableSubscription.setNoLocal(true);

        assertTrue(modifiableSubscription.isModified());
        assertEquals(true, modifiableSubscription.getNoLocal());
    }

    @Test
    public void setNoLocal_same() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        assertFalse(modifiableSubscription.isModified());

        modifiableSubscription.setNoLocal(false);

        assertFalse(modifiableSubscription.isModified());
        assertEquals(false, modifiableSubscription.getNoLocal());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setNoLocal_shared() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("$share/group/topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        modifiableSubscription.setNoLocal(true);
    }

    @Test
    public void setRetainAsPublished() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        assertFalse(modifiableSubscription.isModified());

        modifiableSubscription.setRetainAsPublished(true);

        assertTrue(modifiableSubscription.isModified());
        assertEquals(true, modifiableSubscription.getRetainAsPublished());
    }

    @Test
    public void setRetainAsPublished_same() {
        final SubscriptionImpl subscription =
                new SubscriptionImpl("topic", Qos.AT_LEAST_ONCE, RetainHandling.SEND, false, false);
        final ModifiableSubscriptionImpl modifiableSubscription =
                new ModifiableSubscriptionImpl(subscription, configurationService);

        assertFalse(modifiableSubscription.isModified());

        modifiableSubscription.setRetainAsPublished(false);

        assertFalse(modifiableSubscription.isModified());
        assertEquals(false, modifiableSubscription.getRetainAsPublished());
    }
}