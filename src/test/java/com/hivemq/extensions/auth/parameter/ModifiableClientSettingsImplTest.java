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
package com.hivemq.extensions.auth.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.OverloadProtectionThrottlingLevel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Lukas Brandl
 */
public class ModifiableClientSettingsImplTest {

    @NotNull
    private ModifiableClientSettingsImpl clientSettings;

    @Before
    public void setUp() throws Exception {
        clientSettings = new ModifiableClientSettingsImpl(65535, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_client_receive_max_to_low() {
        clientSettings.setClientReceiveMaximum(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_client_receive_max_to_high() {
        clientSettings.setClientReceiveMaximum(65535 + 1);
    }

    @Test
    public void test_client_receive_not_modified() {
        clientSettings.setClientReceiveMaximum(65535);
        assertFalse(clientSettings.isModified());
    }

    @Test
    public void test_client_receive_modified() {
        assertEquals(65535, clientSettings.getClientReceiveMaximum());
        clientSettings.setClientReceiveMaximum(123);
        assertTrue(clientSettings.isModified());
        assertEquals(123, clientSettings.getClientReceiveMaximum());
    }

    @Test
    public void test_overload_protection_not_modified() {
        clientSettings.setOverloadProtectionThrottlingLevel(OverloadProtectionThrottlingLevel.DEFAULT);
        assertFalse(clientSettings.isModified());
    }

    @Test
    public void test_overload_protection_modified() {
        clientSettings.setOverloadProtectionThrottlingLevel(OverloadProtectionThrottlingLevel.NONE);
        assertTrue(clientSettings.isModified());
    }

    @Test
    public void test_queue_size_modified() {
        assertNull(clientSettings.getQueueSizeMaximum());
        clientSettings.setClientQueueSizeMaximum(123L);
        assertEquals(123L, clientSettings.getQueueSizeMaximum().longValue());
        assertTrue(clientSettings.isModified());
    }

    @Test
    public void test_queue_siz_not_modified() {
        clientSettings = new ModifiableClientSettingsImpl(65535, 1000L);
        assertEquals(1000L, clientSettings.getQueueSizeMaximum().longValue());
        clientSettings.setClientQueueSizeMaximum(1000L);
        assertEquals(1000L, clientSettings.getQueueSizeMaximum().longValue());
        assertFalse(clientSettings.isModified());
    }
}