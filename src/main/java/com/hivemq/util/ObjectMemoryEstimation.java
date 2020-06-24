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

import com.google.common.primitives.ImmutableIntArray;
import com.hivemq.extension.sdk.api.annotations.Nullable;

/**
 * @author Lukas Brandl
 */
public class ObjectMemoryEstimation {

    public static final int OBJECT_SHELL_SIZE = 12; //Class Pointer (4), Flags (4), Locks (4)
    public static final int OBJECT_REF_SIZE = 4;
    public static final int ENUM_OVERHEAD = 4;
    public static final int STRING_OVERHEAD = 38;
    public static final int ARRAY_OVERHEAD = 12;
    public static final int COLLECTION_OVERHEAD = 12;
    public static final int LINKED_LIST_NODE_OVERHEAD = 24;
    public static final int LONG_WRAPPER_SIZE = 24;
    public static final int INT_WRAPPER_SIZE = 16;
    public static final int LONG_SIZE = 8;
    public static final int INT_SIZE = 4;
    public static final int CHAR_SIZE = 2;
    public static final int BOOLEAN_SIZE = 1;

    public static int enumSize() {
        return ENUM_OVERHEAD;
    }

    public static int stringSize(@Nullable final String string) {
        if (string == null) {
            return 0;
        }

        int size = STRING_OVERHEAD;
        size += string.length() * CHAR_SIZE;
        return size;
    }

    public static int byteArraySize(@Nullable final byte[] array) {
        if (array == null) {
            return 0;
        }

        int size = ARRAY_OVERHEAD;
        size += array.length;
        return size;
    }

    public static int immutableIntArraySize(@Nullable final ImmutableIntArray array) {
        if (array == null) {
            return 0;
        }

        int size = ARRAY_OVERHEAD;
        size += intSize(); // start;
        size += intSize(); // end;
        size += array.length() * INT_SIZE;
        return size;
    }

    public static int longWrapperSize() {
        return LONG_WRAPPER_SIZE;
    }

    public static int intWrapperSize() {
        return INT_WRAPPER_SIZE;
    }

    public static int longSize() {
        return LONG_SIZE;
    }

    public static int intSize() {
        return INT_SIZE;
    }

    public static int booleanSize() {
        return BOOLEAN_SIZE;
    }

    public static int objectShellSize() {
        return OBJECT_SHELL_SIZE;
    }

    public static int objectRefSize() {
        return OBJECT_REF_SIZE;
    }

    public static int collectionOverhead() {
        return COLLECTION_OVERHEAD;
    }

    public static int linkedListNodeOverhead() {
        return LINKED_LIST_NODE_OVERHEAD;
    }
}
