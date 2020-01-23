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
package com.hivemq.extensions.services.general;

import com.hivemq.extension.sdk.api.services.general.IterationContext;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Christoph Schäbel
 */
public class IterationContextImpl implements IterationContext {

    private final AtomicBoolean aborted = new AtomicBoolean(false);

    @Override
    public void abortIteration() {
        aborted.set(true);
    }

    public boolean isAborted() {
        return aborted.get();
    }
}
