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

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.util.ObjectMemoryEstimation;

import java.util.Collection;

/**
 * @author Florian Limpöck
 */
@Immutable
public class IterablePersistenceEntry<T extends Collection<? extends Sizable>> implements Sizable {

    private final long timestamp;
    private final @NotNull T object;

    private int sizeInMemory = SIZE_NOT_CALCULATED;

    public IterablePersistenceEntry(@NotNull final T object, final long timestamp) {
        this.timestamp = timestamp;
        this.object = object;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @NotNull
    public T getObject() {
        return object;
    }

    @NotNull
    @Override
    public String toString() {
        return object.toString();
    }

    public static int getFixedSize(){
        int size = ObjectMemoryEstimation.objectShellSize(); // object overhead
        size += ObjectMemoryEstimation.longSize(); // timestamp
        size += ObjectMemoryEstimation.intSize(); // sizeInMemory
        size += ObjectMemoryEstimation.collectionOverhead(); // collection overhead
        return size;
    }

    @Override
    public int getEstimatedSize() {

        if (sizeInMemory != SIZE_NOT_CALCULATED){
            return sizeInMemory;
        }

        int size = getFixedSize();
        for (final Sizable item : object) {
            size += ObjectMemoryEstimation.objectRefSize();
            size += item.getEstimatedSize();
        }

        sizeInMemory = size;
        return sizeInMemory;
    }
}
