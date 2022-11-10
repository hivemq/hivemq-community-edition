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
package com.hivemq.persistence.clientsession.task;

import com.google.common.collect.ImmutableSet;
import com.hivemq.persistence.clientsession.ClientSessionPersistenceImpl;
import com.hivemq.persistence.clientsession.PendingWillMessages;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

/**
 * @author Lukas Brandl
 */
public class ClientSessionCleanUpTaskTest {

    @Mock
    private ClientSessionLocalPersistence localPersistence;

    @Mock
    private ClientSessionPersistenceImpl clientSessionPersistence;

    @Mock
    private PendingWillMessages pendingWillMessages;

    private ClientSessionCleanUpTask task;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        task = new ClientSessionCleanUpTask(localPersistence, clientSessionPersistence, pendingWillMessages);
    }

    @Test
    public void test_clean_up_clean_task() {
        final String clientId = "client";
        Mockito.when(localPersistence.cleanUp(0)).thenReturn(ImmutableSet.of(clientId));
        task.doTask(0);
        verify(pendingWillMessages).sendWillIfPending(clientId);
        verify(clientSessionPersistence).cleanClientData(clientId);
    }
}