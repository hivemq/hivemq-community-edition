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
package com.hivemq.util;

import com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults;
import com.hivemq.mqtt.message.QoS;
import org.junit.Test;

import static com.hivemq.mqtt.message.QoS.*;
import static org.junit.Assert.*;

public class PublishUtilTest {


    @Test
    public void test_correct_qos_equals() throws Exception {

        final QoS result = PublishUtil.getMinQoS(AT_LEAST_ONCE, AT_LEAST_ONCE);

        assertEquals(AT_LEAST_ONCE, result);
    }

    @Test
    public void test_correct_qos_subscription_higher() throws Exception {

        final QoS result = PublishUtil.getMinQoS(AT_LEAST_ONCE, AT_MOST_ONCE);
        assertEquals(AT_MOST_ONCE, result);

        final QoS result2 = PublishUtil.getMinQoS(EXACTLY_ONCE, AT_LEAST_ONCE);
        assertEquals(AT_LEAST_ONCE, result2);

        final QoS result3 = PublishUtil.getMinQoS(EXACTLY_ONCE, AT_MOST_ONCE);
        assertEquals(AT_MOST_ONCE, result3);
    }

    @Test
    public void test_correct_qos_actual_higher_than_subscription() throws Exception {

        final QoS result = PublishUtil.getMinQoS(AT_MOST_ONCE, AT_LEAST_ONCE);
        assertEquals(AT_MOST_ONCE, result);

        final QoS result2 = PublishUtil.getMinQoS(AT_LEAST_ONCE, EXACTLY_ONCE);
        assertEquals(AT_LEAST_ONCE, result2);

        final QoS result3 = PublishUtil.getMinQoS(AT_MOST_ONCE, EXACTLY_ONCE);
        assertEquals(AT_MOST_ONCE, result3);
    }

    @Test
    public void publish_ttl_expired() throws Exception {
        assertTrue(PublishUtil.checkExpiry(System.currentTimeMillis() - 2000, 1));
        assertTrue(PublishUtil.checkExpiry(System.currentTimeMillis() - 10000, 10));
        assertTrue(PublishUtil.checkExpiry(System.currentTimeMillis(), 0));
        assertFalse(PublishUtil.checkExpiry(System.currentTimeMillis() - 2, 1));
        assertFalse(PublishUtil.checkExpiry(System.currentTimeMillis(), 1));
        assertFalse(PublishUtil.checkExpiry(System.currentTimeMillis() - 100000000, MqttConfigurationDefaults.TTL_DISABLED));
    }
}