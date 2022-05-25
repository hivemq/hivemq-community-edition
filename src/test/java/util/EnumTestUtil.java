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
package util;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import static org.junit.Assert.*;

public final class EnumTestUtil {

    public static <T extends Enum<T>> void assertAllValueOf(@NotNull final Class<T> enumClass,
                                                            @NotNull final ToIntFunction<T> toValueFunction,
                                                            @NotNull final IntFunction<T> toEnumFunction) {
        int minValue = Integer.MAX_VALUE;
        int maxValue = Integer.MIN_VALUE;

        for (final T value : enumClass.getEnumConstants()) {
            assertSame(value, toEnumFunction.apply(toValueFunction.applyAsInt(value)));
            minValue = Math.min(minValue, toValueFunction.applyAsInt(value));
            maxValue = Math.max(maxValue, toValueFunction.applyAsInt(value));
        }

        try {
            toEnumFunction.apply(minValue - 1);
            fail("Value should not be allowed: " + (minValue - 1));
        } catch (final IllegalArgumentException ignored) {
            // Expected
        }

        try {
            toEnumFunction.apply(maxValue + 1);
            fail("Value should not be allowed" + (maxValue + 1));
        } catch (final IllegalArgumentException ignored) {
            // Expected
        }
    }

    public static <T extends Enum<T>> void assertAllValueOfWithFallback(@NotNull final Class<T> enumClass,
                                                                        @NotNull final ToIntFunction<T> toValueFunction,
                                                                        @NotNull final IntFunction<T> toEnumFunction,
                                                                        @Nullable final T fallback) {
        int minValue = Integer.MAX_VALUE;
        int maxValue = Integer.MIN_VALUE;

        for (final T value : enumClass.getEnumConstants()) {
            assertSame(value, toEnumFunction.apply(toValueFunction.applyAsInt(value)));
            minValue = Math.min(minValue, toValueFunction.applyAsInt(value));
            maxValue = Math.max(maxValue, toValueFunction.applyAsInt(value));
        }

        assertEquals(fallback, toEnumFunction.apply(minValue - 1));
        assertEquals(fallback, toEnumFunction.apply(maxValue + 1));
    }

    private EnumTestUtil() {
    }
}
