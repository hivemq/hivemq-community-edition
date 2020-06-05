package com.hivemq.util;

import com.hivemq.extension.sdk.api.annotations.Nullable;

/**
 * @author Lukas Brandl
 */
public class ObjectMemoryEstimation {

    public static int ENUM_OVERHEAD = 24;
    public static int STRING_OVERHEAD = 38;
    public static int ARRAY_OVERHEAD = 12;
    public static int LONG_WRAPPER_SIZE = 16;
    public static int INT_WRAPPER_SIZE = 16;
    public static int LONG_SIZE = 8;
    public static int INT_SIZE = 4;

    public static int enumSize() {
        return ENUM_OVERHEAD;
    }

    public static int stringSize(@Nullable final String string) {
        if (string == null) {
            return 0;
        }

        int size = STRING_OVERHEAD;
        size += string.length() * 2;
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
}
