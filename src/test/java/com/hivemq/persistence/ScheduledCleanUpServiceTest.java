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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.hivemq.persistence.ScheduledCleanUpService.CLIENT_QUEUE_PERSISTENCE_INDEX;
import static com.hivemq.persistence.ScheduledCleanUpService.RETAINED_MESSAGES_PERSISTENCE_INDEX;
import static com.hivemq.persistence.ScheduledCleanUpService.SUBSCRIPTION_PERSISTENCE_INDEX;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Lukas Brandl
 */
@SuppressWarnings({"unchecked", "NullabilityAnnotations"})
public class ScheduledCleanUpServiceTest {

    private final ListeningScheduledExecutorService scheduledExecutorService =
            MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());

    private final @NotNull ClientSessionPersistence clientSessionPersistence = mock();
    private final @NotNull ClientSessionSubscriptionPersistence subscriptionPersistence = mock();
    private final @NotNull RetainedMessagePersistence retainedMessagePersistence = mock();
    private final @NotNull ClientQueuePersistence clientQueuePersistence = mock();

    private ScheduledCleanUpService scheduledCleanUpService;

    @Before
    public void setUp() throws Exception {
        scheduledCleanUpService = new ScheduledCleanUpService(scheduledExecutorService,
                clientSessionPersistence,
                subscriptionPersistence,
                retainedMessagePersistence,
                clientQueuePersistence);

        InternalConfigurations.PERSISTENCE_BUCKET_COUNT.set(64);
    }

    @Test
    public void cleanUp_invokesRespectivePersistenceCleanUps() {
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
    public void scheduleCleanUpTask_whenCalledRepeatedly_iteratesThroughBucketsAndPersistences() {
        final ListeningScheduledExecutorService scheduledExecutorService =
                mock(ListeningScheduledExecutorService.class);
        scheduledCleanUpService = new ScheduledCleanUpService(scheduledExecutorService,
                clientSessionPersistence,
                subscriptionPersistence,
                retainedMessagePersistence,
                clientQueuePersistence);

        final ArgumentCaptor<ScheduledCleanUpService.CleanUpTask> argumentCaptor =
                ArgumentCaptor.forClass(ScheduledCleanUpService.CleanUpTask.class);
        when(scheduledExecutorService.schedule(argumentCaptor.capture(), anyLong(), any(TimeUnit.class))).thenReturn(
                mock(ListenableScheduledFuture.class));

        for (int bucketIndex = 0; bucketIndex < 64; bucketIndex++) {
            for (int persistenceIndex = 0;
                 persistenceIndex < ScheduledCleanUpService.NUMBER_OF_PERSISTENCES;
                 persistenceIndex++) {
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
    public void cleanUpTask_whenAThrowableIsThrown_thenTheNextCleanUpTaskIsScheduled() {
        final ScheduledCleanUpService scheduledCleanUpService = mock(ScheduledCleanUpService.class);
        when(scheduledCleanUpService.cleanUp(anyInt(), anyInt())).thenAnswer(invocation -> {
            throw new Throwable();
        });
        final ScheduledCleanUpService.CleanUpTask task =
                new ScheduledCleanUpService.CleanUpTask(scheduledCleanUpService,
                        scheduledExecutorService,
                        Integer.MAX_VALUE,
                        0,
                        0);
        task.call();
        verify(scheduledCleanUpService).scheduleCleanUpTask();
    }

    @Test
    public void cleanUpTask_whenHittingTheTimeout_thenTheNextCleanUpTaskIsScheduled() {
        final Duration timeout = Duration.ofSeconds(30);
        final ScheduledCleanUpService scheduledCleanUpService = mock(ScheduledCleanUpService.class);
        final AtomicBoolean scheduledNextTask = new AtomicBoolean();
        doAnswer(invocation -> {
            scheduledNextTask.set(true);
            return null;
        }).when(scheduledCleanUpService).scheduleCleanUpTask();
        when(scheduledCleanUpService.cleanUp(anyInt(), anyInt())).thenReturn(Futures.submit(() -> {
            // Because the actual tasks likely ignore being interrupted, just wait here and never terminate
            // until we verified that the next task was scheduled;
            Awaitility.waitAtMost(timeout).until(scheduledNextTask::get);
        }, Executors.newSingleThreadScheduledExecutor()));
        final ScheduledCleanUpService.CleanUpTask task =
                new ScheduledCleanUpService.CleanUpTask(scheduledCleanUpService, scheduledExecutorService, 1, 1, 1);
        task.call();
        Awaitility.waitAtMost(timeout).until(scheduledNextTask::get);
    }
}
