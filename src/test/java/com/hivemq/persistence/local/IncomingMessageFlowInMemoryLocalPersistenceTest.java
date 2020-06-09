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
package com.hivemq.persistence.local;

import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.puback.PUBACK;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Lukas Brandl
 * @author Dominik Obermaier
 */
public class IncomingMessageFlowInMemoryLocalPersistenceTest {


    private IncomingMessageFlowInMemoryLocalPersistence persistence;

    @Before
    public void setUp() throws Exception {

        persistence = new IncomingMessageFlowInMemoryLocalPersistence();

    }

    @Test
    public void test_get_no_entry_available() throws Exception {

        final MessageWithID notAvailable = persistence.get("not_available", 1);
        assertEquals(null, notAvailable);
    }

    @Test
    public void test_get_entry() throws Exception {

        final MessageWithID message = new PUBACK(1);
        persistence.addOrReplace("client", 1, message);

        final MessageWithID result = persistence.get("client", 1);

        assertSame(message, result);
    }

    @Test
    public void test_get_entry_deleted() throws Exception {

        final MessageWithID message = new PUBACK(1);
        persistence.addOrReplace("client", 1, message);
        persistence.remove("client", 1);

        final MessageWithID result = persistence.get("client", 1);

        assertEquals(null, result);
    }

    @Test
    public void test_replace_entry() throws Exception {

        final MessageWithID message = new PUBACK(1);
        final MessageWithID message2 = new PUBACK(1);
        persistence.addOrReplace("client", 1, message);
        persistence.addOrReplace("client", 1, message2);

        final MessageWithID result = persistence.get("client", 1);

        assertSame(message2, result);
    }

    @Test
    public void test_remove_nonexistant_entry() throws Exception {
        persistence.remove("nonexistant", 1);

        //Nothing happens
    }

    @Test
    public void test_remove_entry_for_same_client_but_different_message_id() throws Exception {
        persistence.addOrReplace("client", 1, new PUBACK(1));
        persistence.remove("client", 2);

        assertEquals(null, persistence.get("client", 2));
        assertNotNull(null, persistence.get("client", 1));
    }

    @Test
    public void test_remove_entry_same_message_id_but_different_client() throws Exception {
        persistence.addOrReplace("client", 1, new PUBACK(1));
        persistence.remove("client2", 1);

        assertNotNull(null, persistence.get("client", 1));
    }


    @Test
    public void test_delete() throws Exception {
        persistence.addOrReplace("client", 1, new PUBACK(1));
        persistence.addOrReplace("client", 2, new PUBACK(1));
        final MessageWithID message = new PUBACK(1);
        persistence.addOrReplace("client2", 1, message);

        persistence.delete("client");

        assertEquals(null, persistence.get("client", 1));
        assertEquals(null, persistence.get("client", 2));
        assertEquals(message, persistence.get("client2", 1));
    }
}