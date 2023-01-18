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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.ioc.annotation.Persistence;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import com.hivemq.persistence.util.FutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.configuration.service.InternalConfigurations.*;

/**
 * This service is used to remove full remove tombstones that are older than a certain amount of time
 * It is also used to check if the time to live of publishes, retained messages or client session is expired and mark
 * those that are expired as tombstones
 *
 * @author Lukas Brandl
 */
@Singleton
public class ScheduledCleanUpService {

    static final int NUMBER_OF_PERSISTENCES = 4;

    /**
     * The counter index that is associated with the client session persistence in the clean up job scheduling logic
     */
    public static final int CLIENT_SESSION_PERSISTENCE_INDEX = 0;

    /**
     * The counter index that is associated with the subscription persistence in the clean up job scheduling logic
     */
    public static final int SUBSCRIPTION_PERSISTENCE_INDEX = 1;

    /**
     * The counter index that is associated with the retained messages persistence in the clean up job scheduling logic
     */
    public static final int RETAINED_MESSAGES_PERSISTENCE_INDEX = 2;

    /**
     * The counter index that is associated with the client queue persistence in the clean up job scheduling logic
     */
    public static final int CLIENT_QUEUE_PERSISTENCE_INDEX = 3;


    private static final Logger log = LoggerFactory.getLogger(ScheduledCleanUpService.class);

    private final @NotNull ListeningScheduledExecutorService scheduledExecutorService;
    private final @NotNull ClientSessionPersistence clientSessionPersistence;
    private final @NotNull ClientSessionSubscriptionPersistence subscriptionPersistence;
    private final @NotNull RetainedMessagePersistence retainedMessagePersistence;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;

    private int bucketIndex = 0;
    private int persistenceIndex = 0;
    private final int persistenceBucketCount;
    private final int cleanUpJobSchedule;

    @Inject
    public ScheduledCleanUpService(final @NotNull @Persistence ListeningScheduledExecutorService scheduledExecutorService,
                                   final @NotNull ClientSessionPersistence clientSessionPersistence,
                                   final @NotNull ClientSessionSubscriptionPersistence subscriptionPersistence,
                                   final @NotNull RetainedMessagePersistence retainedMessagePersistence,
                                   final @NotNull ClientQueuePersistence clientQueuePersistence) {

        this.scheduledExecutorService = scheduledExecutorService;
        this.clientSessionPersistence = clientSessionPersistence;
        this.subscriptionPersistence = subscriptionPersistence;
        this.retainedMessagePersistence = retainedMessagePersistence;
        this.clientQueuePersistence = clientQueuePersistence;
        this.persistenceBucketCount = PERSISTENCE_BUCKET_COUNT.get();
        this.cleanUpJobSchedule = INTERVAL_BETWEEN_CLEANUP_JOBS_SEC.get();
    }

    @PostConstruct
    public void postConstruct() {
        for (int i = 0; i < CLEANUP_JOB_PARALLELISM; i++) {
            scheduleCleanUpTask();
        }
    }

    @VisibleForTesting
    synchronized void scheduleCleanUpTask() {
        if (scheduledExecutorService.isShutdown()) {
            return;
        }
        final ListenableScheduledFuture<Void> schedule = scheduledExecutorService.schedule(
                new CleanUpTask(this, bucketIndex, persistenceIndex),
                cleanUpJobSchedule,
                TimeUnit.SECONDS);
        persistenceIndex = (persistenceIndex + 1) % NUMBER_OF_PERSISTENCES;
        if (persistenceIndex == 0) {
            bucketIndex = (bucketIndex + 1) % persistenceBucketCount;
        }
        FutureUtils.addExceptionLogger(schedule);

    }

    public ListenableFuture<Void> cleanUp(final int bucketIndex, final int persistenceIndex) {
        switch (persistenceIndex) {
            case CLIENT_SESSION_PERSISTENCE_INDEX:
                return clientSessionPersistence.cleanUp(bucketIndex);
            case SUBSCRIPTION_PERSISTENCE_INDEX:
                return subscriptionPersistence.cleanUp(bucketIndex);
            case RETAINED_MESSAGES_PERSISTENCE_INDEX:
                return retainedMessagePersistence.cleanUp(bucketIndex);
            case CLIENT_QUEUE_PERSISTENCE_INDEX:
                return clientQueuePersistence.cleanUp(bucketIndex);
            default:
                log.error("Unknown persistence index " + persistenceIndex);
                return Futures.immediateFuture(null);
        }
    }

    @VisibleForTesting
    static final class CleanUpTask implements Callable<Void> {
        private final @NotNull ScheduledCleanUpService scheduledCleanUpService;
        private final int bucketIndex;
        private final int persistenceIndex;

        CleanUpTask(@NotNull final ScheduledCleanUpService scheduledCleanUpService,
                    final int bucketIndex,
                    final int persistenceIndex) {
            checkNotNull(scheduledCleanUpService, "Clean up service must not be null");
            this.scheduledCleanUpService = scheduledCleanUpService;
            this.bucketIndex = bucketIndex;
            this.persistenceIndex = persistenceIndex;
        }

        @Override
        public Void call() {
            try {
                final ListenableFuture<Void> future = scheduledCleanUpService.cleanUp(bucketIndex, persistenceIndex);
                Futures.addCallback(future, new FutureCallback<>() {

                    @Override
                    public void onSuccess(final @Nullable Void aVoid) {
                            scheduledCleanUpService.scheduleCleanUpTask();
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        log.error("Exception during cleanup.", throwable);
                        scheduledCleanUpService.scheduleCleanUpTask();
                    }
                }, MoreExecutors.directExecutor());
            } catch (final Exception e) {
                log.error("Exception in clean up job ", e);
                scheduledCleanUpService.scheduleCleanUpTask();
            }
            return null;
        }

        public int getBucketIndex() {
            return bucketIndex;
        }

        public int getPersistenceIndex() {
            return persistenceIndex;
        }
    }
}
