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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.subscription.SubscriptionStore;

/**
 * A callback that can be passed to methods in extension stores (e.g. {@link SubscriptionStore}) to lazily iterate over
 * a potentially large result set.
 *
 * @author Christoph Schäbel
 * @since 4.2.0
 */
@FunctionalInterface
public interface IterationCallback<T> {

    /**
     * This method is called for every result that is part of the iteration.
     *
     * @param context An {@link IterationContext} that allows for interaction with the overlaying iteration mechanism.
     * @param value   The value for this single iteration.
     * @since 4.2.0
     */
    void iterate(@NotNull IterationContext context, @NotNull T value);
}
