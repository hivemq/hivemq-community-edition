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
package com.hivemq.util;

import com.hivemq.extension.sdk.api.annotations.Immutable;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Arrays;
import java.util.Iterator;

/**
 * This int to int map designed for minimal memory overhead for instaces with very few entries.
 * Access is to the map is linear at best, therefore this implementation should not be used if cpu time is a concern.
 * This class is not thread save.
 * Each instance of this IntMap must not be accessed by more than one thread at a time.
 *
 * @author Lukas Brandl
 */
@NotThreadSafe
public class IntMap implements Iterable<IntMap.IntMapEntry> {

    private int[] backingArray = null;

    public IntMap() {
    }

    /**
     * Fill the array with keys from 0 to initialSize mapped to 0.
     * This is intended to avoid garbage when the array is growing.
     *
     * @param initialSize of the backing array
     */
    public IntMap(final int initialSize) {
        backingArray = new int[initialSize * 2];
        for (int i = 0; i < backingArray.length; i += 2) {
            backingArray[i] = i;
        }
    }

    /**
     * Creates a copy of a given IntMap instance
     *
     * @param intMap The instance to be copied
     */
    public IntMap(final IntMap intMap) {
        if (intMap.backingArray == null) {
            this.backingArray = null;
        } else {
            this.backingArray = Arrays.copyOf(intMap.backingArray, intMap.backingArray.length);
        }
    }

    /**
     * @param key is associated with a value
     * @return the value associated with the key or zero
     */
    public int get(final int key) {
        if (backingArray == null) {
            return 0;
        }
        for (int i = 0; i < backingArray.length; i += 2) {
            if (backingArray[i] == key) {
                return backingArray[i + 1];
            }
        }
        return 0;
    }

    /**
     * @param key   is associated with a value
     * @param value will be accessible by the key
     * @return the previous value
     */
    public int put(final int key, final int value) {
        // zero is default, so there is no need to put it
        if (value == 0) {
            return remove(key);
        }
        //shortcut
        if (backingArray == null) {
            backingArray = new int[2];
            backingArray[0] = key;
            backingArray[1] = value;
            return 0;
        }
        for (int i = 0; i < backingArray.length; i += 2) {
            if (backingArray[i] == key) {
                final int previousValue = backingArray[i + 1];
                backingArray[i + 1] = value;
                return previousValue;
            }
        }
        growArray();
        backingArray[backingArray.length - 2] = key;
        backingArray[backingArray.length - 1] = value;
        return 0;
    }

    /**
     * @param key for which the value is incremented
     * @return the previous value
     */
    public int increment(final int key) {
        // Zero is the default, therefore values of zero are not present
        // I case the key that should be incremented is not present it is considered to be zero
        // Therefore we crate a new entry with value 1
        if (backingArray == null) {
            backingArray = new int[2];
            backingArray[0] = key;
            backingArray[1] = 1;
            return 0;
        }
        for (int i = 0; i < backingArray.length; i += 2) {
            if (backingArray[i] == key) {
                final int previousValue = backingArray[i + 1];
                backingArray[i + 1] = backingArray[i + 1] + 1;
                return previousValue;
            }
        }
        put(key, 1);
        return 0;
    }

    /**
     * This method will overwrite the current value at the given index
     * WARNING: It will not check if the array is big enough or even initialised
     *
     * @param key   is associated with a value
     * @param value will be accessible by the key
     * @param index the index at which the entry is put
     */
    public void putUnsafe(final int key, final int value, final int index) {
        final int keyIndex = index * 2;

        backingArray[keyIndex] = key;
        backingArray[keyIndex + 1] = value;
    }


    /**
     * Sets a value for a given key to zero
     *
     * @param key to be removed
     * @return the removed value
     */
    public int remove(final int key) {
        if (backingArray == null) {
            return 0;
        }
        for (int i = 0; i < backingArray.length; i += 2) {
            if (backingArray[i] == key) {
                backingArray[i] = backingArray[backingArray.length - 2];
                final int previousValue = backingArray[i + 1];
                backingArray[i + 1] = backingArray[backingArray.length - 1];
                shrinkArray();
                return previousValue;
            }
        }
        return 0;
    }

    /**
     * Set all values of an other IntMap to the current array, if the value is bigger than the current one
     *
     * @param intMap to be merged
     */
    public void mergeMax(final IntMap intMap) {
        final int[] otherArray = intMap.backingArray;
        if (otherArray == null) {
            return;
        }

        for (int i = 0; i < otherArray.length; i += 2) {
            final int key = otherArray[i];
            final int currentValue = this.get(key);
            final int otherValue = otherArray[i + 1];
            if (otherValue > currentValue) {
                put(key, otherValue);
            }
        }
    }

    /**
     * @return the size of the map
     */
    public int size() {
        if (backingArray == null) {
            return 0;
        }
        return backingArray.length / 2;
    }

    public boolean isEmpty() {
        if (backingArray == null) {
            return true;
        }
        return backingArray.length == 0;
    }

    private void shrinkArray() {
        if (backingArray == null || backingArray.length == 0) {
            return;
        }
        final int[] newArray = new int[backingArray.length - 2];
        System.arraycopy(backingArray, 0, newArray, 0, newArray.length);
        backingArray = newArray;
    }

    private void growArray() {
        final int[] newArray = new int[backingArray.length + 2];
        System.arraycopy(backingArray, 0, newArray, 0, backingArray.length);
        backingArray = newArray;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final IntMap that = (IntMap) o;

        return Arrays.equals(backingArray, that.backingArray);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(backingArray);
    }

    @Override
    public Iterator<IntMapEntry> iterator() {

        return new IntMapIterator(this);
    }

    private static class IntMapIterator implements Iterator<IntMapEntry> {

        private final IntMap map;
        private int nextIndex = 0;

        private IntMapIterator(final IntMap map) {
            this.map = map;
        }

        @Override
        public boolean hasNext() {
            if (map.backingArray == null) {
                return false;
            }
            return nextIndex < map.backingArray.length;
        }

        @Override
        public IntMapEntry next() {
            if (!hasNext()) {
                return null;
            }
            final IntMapEntry entry = new IntMapEntry(map.backingArray[nextIndex], map.backingArray[nextIndex + 1]);
            nextIndex += 2;
            return entry;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Immutable
    public static class IntMapEntry {

        private final int key;
        private final int value;

        private IntMapEntry(final int key, final int value) {
            this.key = key;
            this.value = value;
        }

        public int getKey() {
            return key;
        }

        public int getValue() {
            return value;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final IntMapEntry that = (IntMapEntry) o;

            if (key != that.key) return false;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            int result = key;
            result = 31 * result + value;
            return result;
        }
    }
}
