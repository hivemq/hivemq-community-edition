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
package com.hivemq.common.shutdown;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * A interface for shutdown hooks used for registering HiveMQ shutdown hooks
 * in a proper, clean and manageable way.
 * <p>
 * To register on of the Shutdown Hooks, use the {@link com.hivemq.common.shutdown.ShutdownHooks}
 * class.
 *
 * @author Dominik Obermaier
 */
public interface HiveMQShutdownHook extends Runnable {

    /**
     * The name of the shutdown hook. This name is used for logging purposes
     *
     * @return the name of the HiveMQ shutdown hook
     */
    @NotNull String name();

    /**
     * The {@link HiveMQShutdownHook.Priority} of the shutdown hook.
     *
     * @return the {@link HiveMQShutdownHook.Priority} of the shutdown hook.
     */
    default @NotNull Priority priority() {
        return Priority.DOES_NOT_MATTER;
    }

    enum Priority {
        FIRST(Integer.MAX_VALUE),
        CRITICAL(1_000_000),
        VERY_HIGH(500_000),
        HIGH(100_000),
        MEDIUM(50_000),
        LOW(10_000),
        VERY_LOW(5_000),
        DOES_NOT_MATTER(Integer.MIN_VALUE);

        private final int value;

        Priority(final int value) {

            this.value = value;
        }

        public int getValue() {
            return value;
        }

        @Override
        public String toString() {
            return name() + " (" + getValue() + ")";
        }
    }
}
