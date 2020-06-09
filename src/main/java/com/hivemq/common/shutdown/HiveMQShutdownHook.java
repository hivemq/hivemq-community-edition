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
public abstract class HiveMQShutdownHook extends Thread {


    /**
     * The name of the shutdown hook. This name is used for logging purposes
     *
     * @return the name of the HiveMQ shutdown hook
     */
    @NotNull
    public abstract String name();


    /**
     * The {@link HiveMQShutdownHook.Priority} of the shutdown hook. This is only relevant if the execution of this
     * shutdown hook is
     * <b>not</b> asynchronous. See {@link HiveMQShutdownHook#isAsynchronous()}
     *
     * @return the {@link HiveMQShutdownHook.Priority} of the shutdown hook.
     */
    @NotNull
    public abstract Priority priority();

    /**
     * If the Shutdown hook should be executed asynchronous. This is only
     * recommended if this shutdown hook does not have any side effects and
     * does not rely on any internal state of HiveMQ.
     * <p>
     * In case you are not sure, it's safe to set this to <code>false</code>
     * <p>
     * <b>Important:</b> If your shutdown hook is going to take a long time, consider making
     * it asynchronous since it will block the whole shutdown
     *
     * @return <code>true</code> if the shutdown hook should be executed asynchronous, <code>false</code> otherwise.
     */
    public abstract boolean isAsynchronous();

    @Override
    public abstract void run();

    public enum Priority {
        FIRST(Integer.MAX_VALUE),
        CRITICAL(1_000_000),
        VERY_HIGH(500_000),
        HIGH(100_000),
        MEDIUM(50_000),
        LOW(10_000),
        VERY_LOW(5_000),
        DOES_NOT_MATTER(Integer.MIN_VALUE);

        private final int intValue;

        Priority(final int intValue) {

            this.intValue = intValue;
        }

        public int getIntValue() {
            return intValue;
        }

        @Override
        public String toString() {
            return name() + " (" + getIntValue() + ")";
        }
    }


}
