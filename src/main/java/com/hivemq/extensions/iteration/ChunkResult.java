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

import java.util.Collection;

/**
 * @author Christoph Schäbel
 */
public class ChunkResult<V> {

    private final @NotNull Collection<V> results;
    private final @Nullable ChunkCursor cursor;
    private final boolean finished;

    public ChunkResult(final @NotNull Collection<V> results, final @Nullable ChunkCursor cursor, final boolean finished) {
        this.results = results;
        this.cursor = cursor;
        this.finished = finished;
    }

    public @NotNull Collection<V> getResults() {
        return results;
    }

    public @Nullable ChunkCursor getCursor() {
        return cursor;
    }

    public boolean isFinished() {
        return finished;
    }

    @Override
    public String toString() {
        return "ChunkResult{" +
                "results=" + results +
                ", cursor=" + cursor +
                ", finished=" + finished +
                '}';
    }
}
