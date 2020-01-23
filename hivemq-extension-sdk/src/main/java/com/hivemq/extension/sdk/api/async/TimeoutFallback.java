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

/**
 * The TimeoutFallback defines the strategy that will be applied after an {@link Async} operation timed out.
 *
 * @author Christoph Schäbel
 * @author Florian Limpöck
 * @since 4.0.0
 */
public enum TimeoutFallback {

    /**
     * SUCCESS usually means that HiveMQ either sees the outcome as successful or asks the next extension, that also
     * implemented the specific service. <br>
     * The actual behaviour is defined in the specific implementation.
     *
     * @since 4.0.0
     */
    SUCCESS,

    /**
     * FAILURE usually means that HiveMQ aborts any further action. <br>
     * The actual behaviour is defined in the specific implementation.
     *
     * @since 4.0.0
     */
    FAILURE
}
