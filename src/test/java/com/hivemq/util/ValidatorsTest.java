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

import com.hivemq.configuration.service.entity.ClientWriteBufferProperties;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Georg Held
 */
public class ValidatorsTest {

    @Test
    public void test_valid_thresholds() throws Exception {
        final ClientWriteBufferProperties writeBufferProperties = new ClientWriteBufferProperties(64000, 64000);
        assertSame(writeBufferProperties, Validators.validateWriteBufferProperties(writeBufferProperties));
    }

    @Test
    public void test_default_by_low_equals_zero() throws Exception {
        final ClientWriteBufferProperties writeBufferProperties = new ClientWriteBufferProperties(64000, 0);
        final ClientWriteBufferProperties validated = Validators.validateWriteBufferProperties(writeBufferProperties);
        assertNotSame(writeBufferProperties, validated);
        assertEquals(Validators.DEFAULT_HIGH_THRESHOLD, validated.getHighThreshold());
        assertEquals(Validators.DEFAULT_LOW_THRESHOLD, validated.getLowThreshold());
    }

    @Test
    public void test_default_by_high_less_than_low() throws Exception {
        final ClientWriteBufferProperties writeBufferProperties = new ClientWriteBufferProperties(32000, 64000);
        final ClientWriteBufferProperties validated = Validators.validateWriteBufferProperties(writeBufferProperties);
        assertNotSame(writeBufferProperties, validated);
        assertEquals(Validators.DEFAULT_HIGH_THRESHOLD, validated.getHighThreshold());
        assertEquals(Validators.DEFAULT_LOW_THRESHOLD, validated.getLowThreshold());
    }
}