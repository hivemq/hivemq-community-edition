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
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author Lukas Brandl
 */
public class FutureUtils {

    private static final Logger log = LoggerFactory.getLogger(FutureUtils.class);

    public static ListenableFuture<Void> voidFutureFromList(final ImmutableList<ListenableFuture<Void>> futures) {
        final SettableFuture<Void> result = SettableFuture.create();
        Futures.whenAllComplete(futures).call((Callable<Void>) () -> {
            final List<Throwable> throwables = new ArrayList<>();
            for (final ListenableFuture<Void> future : futures) {
                // The callback is executed immediately because the future is already completed.
                Futures.addCallback(future, new FutureCallback<>() {
                    @Override
                    public void onSuccess(final Void entry) {

                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        throwables.add(t);
                    }
                }, MoreExecutors.directExecutor());
            }
            if (throwables.isEmpty()) {
                result.set(null);
            } else if (throwables.size() == 1) {
                result.setException(throwables.get(0));
            } else {
                result.setException(new BatchedException(throwables));
            }
            return null;
        }, MoreExecutors.directExecutor());
        return result;
    }

    public static void addExceptionLogger(final ListenableFuture<?> listenableFuture) {
        Futures.addCallback(listenableFuture, new FutureCallback<Object>() {
            @Override
            public void onSuccess(@Nullable final Object o) {
                //no op
            }

            @Override
            public void onFailure(final Throwable throwable) {
                log.error("Uncaught exception", throwable);
            }
        }, MoreExecutors.directExecutor());
    }
}
