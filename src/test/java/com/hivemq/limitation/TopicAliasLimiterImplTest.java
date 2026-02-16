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
package com.hivemq.limitation;


import com.hivemq.configuration.service.InternalConfigurations;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class TopicAliasLimiterImplTest {

    private TopicAliasLimiter topicAliasLimiter;

    @Before
    public void setUp() throws Exception {
        InternalConfigurations.TOPIC_ALIAS_GLOBAL_MEMORY_SOFT_LIMIT_BYTES.set(50);
        InternalConfigurations.TOPIC_ALIAS_GLOBAL_MEMORY_HARD_LIMIT_BYTES.set(200);

        topicAliasLimiter = new TopicAliasLimiterImpl();
    }

    @Test
    public void test_init_usage() {
        topicAliasLimiter.initUsage(5);

        assertFalse(topicAliasLimiter.limitExceeded());
        assertTrue(topicAliasLimiter.aliasesAvailable());

        topicAliasLimiter.initUsage(5);

        assertFalse(topicAliasLimiter.limitExceeded());
        assertTrue(topicAliasLimiter.aliasesAvailable());

        topicAliasLimiter.initUsage(5);

        assertFalse(topicAliasLimiter.limitExceeded());
        assertFalse(topicAliasLimiter.aliasesAvailable());
    }

    @Test
    public void test_add_usage() {
        topicAliasLimiter.addUsage(RandomStringUtils.randomAlphanumeric(6));
        assertFalse(topicAliasLimiter.limitExceeded());
        assertFalse(topicAliasLimiter.aliasesAvailable());

        topicAliasLimiter.addUsage(RandomStringUtils.randomAlphanumeric(56));

        assertFalse(topicAliasLimiter.limitExceeded());
        assertFalse(topicAliasLimiter.aliasesAvailable());

        topicAliasLimiter.addUsage(RandomStringUtils.randomAlphanumeric(1));

        assertTrue(topicAliasLimiter.limitExceeded());
        assertFalse(topicAliasLimiter.aliasesAvailable());
    }

    @Test
    public void test_remove_usage() {
        topicAliasLimiter.addUsage(RandomStringUtils.randomAlphanumeric(107));

        topicAliasLimiter.removeUsage(RandomStringUtils.randomAlphanumeric(6));
        assertTrue(topicAliasLimiter.limitExceeded());
        assertFalse(topicAliasLimiter.aliasesAvailable());

        topicAliasLimiter.removeUsage(RandomStringUtils.randomAlphanumeric(1));

        assertFalse(topicAliasLimiter.limitExceeded());
        assertFalse(topicAliasLimiter.aliasesAvailable());

        topicAliasLimiter.removeUsage(RandomStringUtils.randomAlphanumeric(151));

        assertFalse(topicAliasLimiter.limitExceeded());
        assertTrue(topicAliasLimiter.aliasesAvailable());
    }

    @Test
    public void test_finish_usage() {
        final String topic = RandomStringUtils.randomAlphanumeric(6);

        topicAliasLimiter.initUsage(5);

        topicAliasLimiter.addUsage(topic);
        topicAliasLimiter.addUsage(topic);
        topicAliasLimiter.addUsage(topic);
        topicAliasLimiter.addUsage(topic);
        topicAliasLimiter.addUsage(topic);

        topicAliasLimiter.finishUsage(topic, topic, topic, topic, topic);

        assertFalse(topicAliasLimiter.limitExceeded());
        assertTrue(topicAliasLimiter.aliasesAvailable());
    }
}
