/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extension.sdk.api.async;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * The interface for the async option of the extension system.
 *
 * @author Christoph Schäbel
 * @author Georg Held
 * @since 4.0.0
 */
@DoNotImplement
public interface Async<T> {

    /**
     * Signal HiveMQ that the async action is done and normal extension operations can resume.
     *
     * @since 4.0.0
     */
    void resume();

    /**
     * Return the original output object.
     *
     * @return The original output object.
     * @since 4.0.0
     */
    @NotNull T getOutput();

    /**
     * Return the current status of the async option.
     *
     * @return The runtime status of the async option.
     * @since 4.0.0
     */
    @NotNull Status getStatus();

    /**
     * Information about the runtime status of an async option.
     * <p>
     * The runtime state of an async option has the following transitions.
     * <pre>
     * +
     * |
     * | AsyncOutput.async(Duration d)
     * |
     * v
     * +---------+                +------+
     * |         | Async.resume() |      |
     * | RUNNING +---------------&gt;+ DONE |
     * |         |                |      |
     * +---------+                +------+
     * |
     * | after Duration d
     * |
     * v
     * +----------+
     * |          |
     * | CANCELED |
     * |          |
     * +----------+
     * </pre>
     *
     * @since 4.0.0
     */
    enum Status {
        /**
         * The async operation is still ongoing.
         *
         * @since 4.0.0
         */
        RUNNING,
        /**
         * The duration for the async operation has timed out.
         *
         * @since 4.0.0
         */
        CANCELED,
        /**
         * The async operation completed before the timeout, with calling {@link Async#resume()}.
         *
         * @since 4.0.0
         */
        DONE
    }
}
