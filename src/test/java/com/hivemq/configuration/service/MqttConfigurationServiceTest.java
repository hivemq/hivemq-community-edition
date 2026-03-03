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
package com.hivemq.configuration.service;

import org.junit.Test;
import util.EnumTestUtil;

public class MqttConfigurationServiceTest {

    @Test
    public void test_all_queued_messages_strategy_value_of() {
        EnumTestUtil.assertAllValueOf(
                MqttConfigurationService.QueuedMessagesStrategy.class,
                MqttConfigurationService.QueuedMessagesStrategy::getIndex,
                MqttConfigurationService.QueuedMessagesStrategy::valueOf);
    }
}
