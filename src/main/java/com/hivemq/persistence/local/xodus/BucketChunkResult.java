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
package com.hivemq.persistence.local.xodus;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

/**
 * @author Christoph Schäbel
 */
public class BucketChunkResult<T> {


    private final @NotNull T value;
    private final boolean finished;
    private final @Nullable String lastKey;
    private final int bucketIndex;

    public BucketChunkResult(@NotNull final T value, final boolean finished, @Nullable final String lastKey, final int bucketIndex) {
        this.value = value;
        this.finished = finished;
        this.lastKey = lastKey;
        this.bucketIndex = bucketIndex;
    }

    @NotNull
    public T getValue() {
        return value;
    }

    public boolean isFinished() {
        return finished;
    }

    public @Nullable String getLastKey() {
        return lastKey;
    }

    public int getBucketIndex() {
        return bucketIndex;
    }

    @Override
    public String toString() {
        return "BucketChunkResult{" +
                "value=" + value +
                ", finished=" + finished +
                ", lastKey='" + lastKey + '\'' +
                ", bucketIndex=" + bucketIndex +
                '}';
    }
}
