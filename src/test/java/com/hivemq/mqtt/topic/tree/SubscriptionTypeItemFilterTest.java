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
package com.hivemq.mqtt.topic.tree;

import com.hivemq.extension.sdk.api.services.subscription.SubscriptionType;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SubscriptionTypeItemFilterTest {

    @Test
    public void test_mode_all() {

        final byte sharedFlag = SubscriptionFlag.getDefaultFlags(true, false, false);
        final byte individualFlag = SubscriptionFlag.getDefaultFlags(false, false, false);

        final SubscriptionTypeItemFilter itemFilter = new SubscriptionTypeItemFilter(SubscriptionType.ALL);

        assertTrue(itemFilter.test(new SubscriberWithQoS("client", 0, individualFlag, 0)));
        assertTrue(itemFilter.test(new SubscriberWithQoS("client", 0, sharedFlag, 0)));
    }

    @Test
    public void test_mode_individual() {

        final byte sharedFlag = SubscriptionFlag.getDefaultFlags(true, false, false);
        final byte individualFlag = SubscriptionFlag.getDefaultFlags(false, false, false);

        final SubscriptionTypeItemFilter itemFilter = new SubscriptionTypeItemFilter(SubscriptionType.INDIVIDUAL);

        assertTrue(itemFilter.test(new SubscriberWithQoS("client", 0, individualFlag, 0)));
        assertFalse(itemFilter.test(new SubscriberWithQoS("client", 0, sharedFlag, 0)));
    }

    @Test
    public void test_mode_shared() {

        final byte sharedFlag = SubscriptionFlag.getDefaultFlags(true, false, false);
        final byte individualFlag = SubscriptionFlag.getDefaultFlags(false, false, false);

        final SubscriptionTypeItemFilter itemFilter = new SubscriptionTypeItemFilter(SubscriptionType.SHARED);

        assertFalse(itemFilter.test(new SubscriberWithQoS("client", 0, individualFlag, 0)));
        assertTrue(itemFilter.test(new SubscriberWithQoS("client", 0, sharedFlag, 0)));
    }

}
