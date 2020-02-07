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

package com.hivemq.extensions.packets.general;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Georg Held
 */
public class EmptyUserPropertiesImplTest {

    @Test(timeout = 5000)
    public void test_empty_properties_is_empty() {
        assertInternalPropertiesIsEmpty(EmptyUserPropertiesImpl.INSTANCE);
        assertInternalPropertiesIsEmpty(EmptyUserPropertiesImpl.INSTANCE.consolidate());
    }

    void assertInternalPropertiesIsEmpty(@NotNull final InternalUserProperties userProperties) {
        assertTrue(userProperties.asList().isEmpty());
        assertTrue(userProperties.isEmpty());
        assertTrue(userProperties.getAllForName(RandomStringUtils.random(10)).isEmpty());
        assertFalse(userProperties.getFirst(RandomStringUtils.random(10)).isPresent());
        assertEquals(0, userProperties.toMqtt5UserProperties().size());
    }
}