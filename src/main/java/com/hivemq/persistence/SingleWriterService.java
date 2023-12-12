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

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.concurrent.Executor;

public interface SingleWriterService {

    @NotNull ProducerQueues getRetainedMessageQueue();

    @NotNull ProducerQueues getClientSessionQueue();

    @NotNull ProducerQueues getSubscriptionQueue();

    @NotNull ProducerQueues getQueuedMessagesQueue();

    @NotNull ProducerQueues getAttributeStoreQueue();

    @NotNull Executor callbackExecutor(@NotNull final String key);

    int getPersistenceBucketCount();

    void stop();

    interface Task<R> {
        @NotNull R doTask(int bucketIndex);
    }

    interface SuccessCallback<R> {
        void afterTask(@NotNull R result);
    }

    interface FailedCallback {
        void afterTask(@NotNull Throwable exception);
    }
}
