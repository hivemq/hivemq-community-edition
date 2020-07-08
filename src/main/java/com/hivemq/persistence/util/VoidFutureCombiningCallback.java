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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Georg Held
 */
class VoidFutureCombiningCallback<T> implements FutureCallback<T> {
    private final int size;
    private final SettableFuture<Void> resultFuture;
    private final AtomicInteger count;
    private final Queue<Throwable> throwables = new ConcurrentLinkedQueue<>();

    public VoidFutureCombiningCallback(final int size, final SettableFuture<Void> resultFuture) {
        this.size = size;
        this.resultFuture = resultFuture;

        count = new AtomicInteger(0);
    }

    @Override
    public void onSuccess(@Nullable final T result) {
        final int actualCount = count.incrementAndGet();
        if (actualCount == size) {
            setFuture();
        }
    }

    private void setFuture() {
        if (throwables.isEmpty()) {
            resultFuture.set(null);
        } else if (throwables.size() == 1) {
            resultFuture.setException(throwables.poll());
        } else {
            resultFuture.setException(new BatchedException(throwables));
        }
    }

    @Override
    public void onFailure(final Throwable t) {
        throwables.add(t);

        final int actualCount = count.incrementAndGet();
        if (actualCount == size) {
            setFuture();
        }
    }
}
