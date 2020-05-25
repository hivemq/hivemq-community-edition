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

package com.hivemq.persistence;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistence;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.InitFutureUtilsExecutorRule;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PersistenceShutdownHookTest {

    @Rule
    public InitFutureUtilsExecutorRule initFutureUtilsExecutorRule = new InitFutureUtilsExecutorRule();

    @Mock
    private ClientSessionPersistence clientSessionPersistence;
    @Mock
    private ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence;
    @Mock
    private IncomingMessageFlowPersistence incomingMessageFlowPersistence;
    @Mock
    private RetainedMessagePersistence retainedMessagePersistence;
    @Mock
    private ListeningExecutorService persistenceExecutorService;
    @Mock
    private ListeningScheduledExecutorService persistenceScheduledExecutorService;
    @Mock
    private SingleWriterService singleWriterService;
    @Mock
    private PublishPayloadPersistence payloadPersistence;
    @Mock
    private ListeningScheduledExecutorService payloadPersistenceExecutor;
    @Mock
    private ClientQueuePersistence clientQueuePersistence;

    private PersistenceShutdownHook persistenceShutdownHook;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        persistenceShutdownHook = new PersistenceShutdownHook(clientSessionPersistence, clientSessionSubscriptionPersistence,
                incomingMessageFlowPersistence, retainedMessagePersistence, payloadPersistence,
                clientQueuePersistence, persistenceExecutorService, persistenceScheduledExecutorService,
                payloadPersistenceExecutor, singleWriterService);

        when(clientSessionPersistence.closeDB()).thenReturn(Futures.immediateFuture(null));
        when(clientSessionSubscriptionPersistence.closeDB()).thenReturn(Futures.immediateFuture(null));
        when(retainedMessagePersistence.closeDB()).thenReturn(Futures.immediateFuture(null));
        when(clientQueuePersistence.closeDB()).thenReturn(Futures.immediateFuture(null));
    }

    @Test
    public void test_dbs_closed() throws Exception {
        persistenceShutdownHook.run();
        verify(clientSessionPersistence).closeDB();
        verify(clientSessionSubscriptionPersistence).closeDB();
        verify(incomingMessageFlowPersistence).closeDB();
        verify(retainedMessagePersistence).closeDB();
        verify(payloadPersistence).closeDB();
        verify(persistenceExecutorService).shutdown();
        verify(persistenceScheduledExecutorService).shutdownNow();
        verify(singleWriterService).stop();
    }

    @Test(timeout = 15000)
    public void test_timeout_future_not_returning() throws Exception {

        final SettableFuture<Void> voidSettableFuture = SettableFuture.create();
        when(retainedMessagePersistence.closeDB()).thenReturn(voidSettableFuture);

        InternalConfigurations.PERSISTENCE_SHUTDOWN_TIMEOUT.set(1);

        final long start = System.currentTimeMillis();
        persistenceShutdownHook.run();

        //check that it took less than this test's safety timeout to complete the run
        assertTrue(start + 15000 > System.currentTimeMillis());
    }

    @Test
    public void test_exception() throws Exception {
        when(retainedMessagePersistence.closeDB()).thenReturn(Futures.immediateFailedFuture(new RuntimeException("test")));
        persistenceShutdownHook.run();
    }

}