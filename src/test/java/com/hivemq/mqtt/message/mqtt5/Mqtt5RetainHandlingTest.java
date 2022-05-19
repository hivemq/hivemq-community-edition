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
package com.hivemq.mqtt.message.mqtt5;

import org.junit.Test;
import util.EnumTestUtil;

public class Mqtt5RetainHandlingTest {

    @Test
    public void test_all_fromCode() {
        EnumTestUtil.assertAllValueOfWithFallback(
                Mqtt5RetainHandling.class,
                Mqtt5RetainHandling::getCode,
                Mqtt5RetainHandling::fromCode,
                null);
    }

}
