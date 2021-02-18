package com.hivemq.persistence;

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.List;
import java.util.SplittableRandom;
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

    void execute(final @NotNull SplittableRandom random);


    @NotNull ListenableFuture<Void> shutdown(final @Nullable SingleWriterServiceImpl.Task<Void> finalTask);

    @NotNull AtomicLong getTaskCount();

}
