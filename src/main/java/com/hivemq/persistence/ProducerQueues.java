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

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Daniel Krüger
 */
public interface ProducerQueues {


    <R> @NotNull ListenableFuture<R> submit(@NotNull final String key, @NotNull final SingleWriterServiceImpl.Task<R> task);

    <R> @NotNull ListenableFuture<R> submit(final int bucketIndex, @NotNull final SingleWriterServiceImpl.Task<R> task);


    <R> @Nullable ListenableFuture<R> submit(final int bucketIndex,
                                             @NotNull final SingleWriterServiceImpl.Task<R> task,
                                             @Nullable final SingleWriterServiceImpl.SuccessCallback<R> successCallback,
                                             @Nullable final SingleWriterServiceImpl.FailedCallback failedCallback);

    @NotNull <R> List<ListenableFuture<R>> submitToAllQueues(final @NotNull SingleWriterServiceImpl.Task<R> task);

    @NotNull <R> ListenableFuture<List<R>> submitToAllQueuesAsList(final @NotNull SingleWriterServiceImpl.Task<R> task);

    int getBucket(@NotNull final String key);

    @NotNull ListenableFuture<Void> shutdown(final @Nullable SingleWriterServiceImpl.Task<Void> finalTask);

    @NotNull AtomicLong getTaskCount();

}
