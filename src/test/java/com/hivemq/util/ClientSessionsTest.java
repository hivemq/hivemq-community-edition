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

import com.hivemq.persistence.clientsession.ClientSession;
import org.junit.Test;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_MAX;
import static org.junit.Assert.assertEquals;

/**
 * @author Lukas Brandl
 */
public class ClientSessionsTest {

    @Test
    public void test_expired() {
        final ClientSession session1 = new ClientSession(false, 10);
        assertEquals(true, ClientSessions.isExpired(session1, 10001));
        assertEquals(false, ClientSessions.isExpired(session1, 9999));

        final ClientSession sessionExactlyExpired = new ClientSession(false, 10);
        assertEquals(true, ClientSessions.isExpired(sessionExactlyExpired, 10000));

        final ClientSession session2 = new ClientSession(true, 10);
        assertEquals(false, ClientSessions.isExpired(session2, 11000));
        final ClientSession session3 = new ClientSession(false, 10);
        assertEquals(true, ClientSessions.isExpired(session3, 11000));
        final ClientSession session4 = new ClientSession(false, SESSION_EXPIRY_MAX);
        assertEquals(false, ClientSessions.isExpired(session4, 11000));

    }
}