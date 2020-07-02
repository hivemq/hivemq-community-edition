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
package com.hivemq.extensions.iteration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.Map;
import java.util.Set;

/**
 * @author Christoph Schäbel
 */
public class ChunkCursor {


    //Map consisting of: persistence bucketId -> last already fetched key from the persistence
    private final @NotNull Map<Integer, String> lastKeys;
    private final @NotNull Set<Integer> finishedBuckets;

    public ChunkCursor() {
        lastKeys = ImmutableMap.of();
        finishedBuckets = ImmutableSet.of();
    }

    public ChunkCursor(@NotNull final Map<Integer, String> lastKeys, @NotNull final Set<Integer> finishedBuckets) {
        this.lastKeys = lastKeys;
        this.finishedBuckets = finishedBuckets;
    }

    @NotNull
    public Map<Integer, String> getLastKeys() {
        return lastKeys;
    }

    @NotNull
    public Set<Integer> getFinishedBuckets() {
        return finishedBuckets;
    }

    @Override
    public String toString() {
        return "ChunkCursor{" +
                "lastKeys=" + lastKeys +
                ", finishedBuckets=" + finishedBuckets +
                '}';
    }
}
