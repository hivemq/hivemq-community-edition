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
package com.hivemq.bootstrap;

import com.hivemq.configuration.service.entity.TcpListener;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ListenerStartupInformationTest {

    @Test(expected = NullPointerException.class)
    public void test_successful_listener_startup_listener_null() throws Exception {
        ListenerStartupInformation.successfulListenerStartup(null);
    }

    @Test
    public void test_successful_listener() throws Exception {
        final TcpListener listener = new TcpListener(1883, "0.0.0.0");
        final ListenerStartupInformation info = ListenerStartupInformation.successfulListenerStartup(listener);

        assertTrue(info.isSuccessful());
        assertSame(listener, info.getListener());
        assertFalse(info.getException().isPresent());
    }

    @Test(expected = NullPointerException.class)
    public void test_failed_listener_startup_listener_null() throws Exception {
        ListenerStartupInformation.failedListenerStartup(null, null);
    }

    @Test
    public void test_failed_listener_no_exception() throws Exception {
        final TcpListener listener = new TcpListener(1883, "0.0.0.0");
        final ListenerStartupInformation info = ListenerStartupInformation.failedListenerStartup(listener, null);

        assertFalse(info.isSuccessful());
        assertSame(listener, info.getListener());
        assertFalse(info.getException().isPresent());
    }

    @Test
    public void test_failed_listener_exception() throws Exception {
        final TcpListener listener = new TcpListener(1883, "0.0.0.0");
        final ListenerStartupInformation info =
                ListenerStartupInformation.failedListenerStartup(listener, new IllegalArgumentException("illegal"));

        assertFalse(info.isSuccessful());
        assertSame(listener, info.getListener());
        assertTrue(info.getException().isPresent());
        assertEquals(IllegalArgumentException.class, info.getException().get().getClass());
    }
}
