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

package com.hivemq.persistence.util;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

/**
 * @author Lukas Brandl
 */
public class FutureUtils {

    @Inject
    public static AbstractFutureUtils delegate;

    public static @NotNull ListenableFuture<Void> mergeVoidFutures(
            final @NotNull ListenableFuture<Void> future1, final @NotNull ListenableFuture<Void> future2) {
        return delegate.mergeVoidFutures(future1, future2);
    }

    public static <T> @NotNull ListenableFuture<Void> voidFutureFromList(final @NotNull ImmutableList<ListenableFuture<T>> futureList) {
        return delegate.voidFutureFromList(futureList);
    }

    public static void addExceptionLogger(final @NotNull ListenableFuture<?> listenableFuture) {
        delegate.addExceptionLogger(listenableFuture);
    }

    public static <T> void addPersistenceCallback(
            final @NotNull ListenableFuture<T> future, final @NotNull FutureCallback<? super T> callback) {
        delegate.addPersistenceCallback(future, callback);
    }

    public static <E, C extends Collection<Set<E>>> @NotNull ListenableFuture<Set<E>> combineSetResults(
            final @NotNull ListenableFuture<C> collectionFuture) {
        return delegate.combineSetResults(collectionFuture);
    }
}
