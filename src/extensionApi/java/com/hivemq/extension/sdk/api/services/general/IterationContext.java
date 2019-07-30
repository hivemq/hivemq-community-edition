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


package com.hivemq.extension.sdk.api.services.general;

/**
 * @author Christoph Schäbel
 * @since 4.2.0
 */
public interface IterationContext {

    /**
     * Aborts the iteration at the current step of the iteration.
     * <p>
     * No further callbacks will be executed and the result future will complete successfully as soon as the current
     * iteration callback returns.
     *
     * @since 4.2.0
     */
    void abortIteration();

}
