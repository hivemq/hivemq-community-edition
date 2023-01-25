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
package com.hivemq.persistence;

import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hivemq.persistence.ScheduledCleanUpService.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Brandl
 */
@SuppressWarnings({"unchecked", "NullabilityAnnotations"})
public class ScheduledCleanUpServiceTest {

    private final ListeningScheduledExecutorService scheduledExecutorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());

    @Mock
    private ClientSessionPersistence clientSessionPersistence;

    @Mock
    private ClientSessionSubscriptionPersistence subscriptionPersistence;

    @Mock
    private RetainedMessagePersistence retainedMessagePersistence;

    @Mock
    private MetricsHolder metricsHolder;

    @Mock
    private ClientQueuePersistence clientQueuePersistence;

    private ScheduledCleanUpService scheduledCleanUpService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        scheduledCleanUpService = new ScheduledCleanUpService(scheduledExecutorService, clientSessionPersistence,
                subscriptionPersistence, retainedMessagePersistence, clientQueuePersistence);

        InternalConfigurations.PERSISTENCE_BUCKET_COUNT.set(64);
    }

    @Test
    public void test_clean_up() throws Exception {

        scheduledCleanUpService.cleanUp(0, ScheduledCleanUpService.CLIENT_SESSION_PERSISTENCE_INDEX);
        verify(clientSessionPersistence).cleanUp(eq(ScheduledCleanUpService.CLIENT_SESSION_PERSISTENCE_INDEX));

        scheduledCleanUpService.cleanUp(1, SUBSCRIPTION_PERSISTENCE_INDEX);
        verify(subscriptionPersistence).cleanUp(eq(SUBSCRIPTION_PERSISTENCE_INDEX));

        scheduledCleanUpService.cleanUp(2, RETAINED_MESSAGES_PERSISTENCE_INDEX);
        verify(retainedMessagePersistence).cleanUp(eq(RETAINED_MESSAGES_PERSISTENCE_INDEX));

        scheduledCleanUpService.cleanUp(3, CLIENT_QUEUE_PERSISTENCE_INDEX);
        verify(clientQueuePersistence).cleanUp(eq(CLIENT_QUEUE_PERSISTENCE_INDEX));
    }

    @Test
    public void test_schedule_clean_up() throws Exception {
        final ListeningScheduledExecutorService scheduledExecutorService = mock(ListeningScheduledExecutorService.class);
        scheduledCleanUpService = new ScheduledCleanUpService(scheduledExecutorService, clientSessionPersistence,
                subscriptionPersistence, retainedMessagePersistence, clientQueuePersistence);

        final ArgumentCaptor<ScheduledCleanUpService.CleanUpTask> argumentCaptor = ArgumentCaptor.forClass(ScheduledCleanUpService.CleanUpTask.class);
        when(scheduledExecutorService.schedule(argumentCaptor.capture(), anyLong(), any(TimeUnit.class))).thenReturn(mock(ListenableScheduledFuture.class));


        for (int bucketIndex = 0; bucketIndex < 64; bucketIndex++) {
            for (int persistenceIndex = 0; persistenceIndex < ScheduledCleanUpService.NUMBER_OF_PERSISTENCES; persistenceIndex++) {
                scheduledCleanUpService.scheduleCleanUpTask();
                final ScheduledCleanUpService.CleanUpTask value = argumentCaptor.getValue();
                assertEquals(bucketIndex, value.getBucketIndex());
                assertEquals(persistenceIndex, value.getPersistenceIndex());
            }
        }

        scheduledCleanUpService.scheduleCleanUpTask();
        final ScheduledCleanUpService.CleanUpTask value = argumentCaptor.getValue();
        assertEquals(0, value.getBucketIndex());
        assertEquals(0, value.getPersistenceIndex());
    }

    @Test
    public void test_cleanup_task_rescheduled_in_case_of_exception() throws Exception {
        final ScheduledCleanUpService scheduledCleanUpService = mock(ScheduledCleanUpService.class);
        when(scheduledCleanUpService.cleanUp(anyInt(), anyInt())).thenThrow(new RuntimeException("expected"));
        final ScheduledCleanUpService.CleanUpTask task = new ScheduledCleanUpService.CleanUpTask(scheduledCleanUpService, 0, 0);
        task.call();
        verify(scheduledCleanUpService).scheduleCleanUpTask();
    }
}