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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;

import java.util.Collection;

/**
 * @author Christoph Schäbel
 */
@ThreadSafe
public class ResultBuffer<V> {

    private ChunkResult<V> currentChunk = null;

    @NotNull
    private final NextChunkCallback<V> nextChunkCallback;

    ResultBuffer(@NotNull final NextChunkCallback<V> nextChunkCallback) {
        this.nextChunkCallback = nextChunkCallback;
    }

    synchronized void addChunk(@NotNull final ChunkResult<V> chunk) {
        currentChunk = chunk;
    }

    @Nullable
    synchronized Collection<V> getNextChunk() {

        if (currentChunk == null) {
            return null;
        }

        final ChunkResult<V> chunkResult = currentChunk;
        currentChunk = null;

        if (!chunkResult.isFinished()) {
            nextChunkCallback.fetchNextChunk(chunkResult.getCursor(), this);
        }
        return chunkResult.getResults();
    }

    public synchronized void clean() {
        currentChunk = null;
    }

    public interface NextChunkCallback<V> {

        void fetchNextChunk(@Nullable ChunkCursor cursor, @NotNull ResultBuffer<V> resultBuffer);

    }
}
