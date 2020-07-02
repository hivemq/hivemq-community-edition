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
package com.hivemq.extensions.iteration;

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * @author Christoph Schäbel
 */
public interface AsyncIterator<V> {

    /**
     * Start fetching chunks of data and iterate the results
     */
    void fetchAndIterate();

    /**
     * @return a future that completes when the iteration is finished
     */
    @NotNull CompletableFuture<Void> getFinishedFuture();

    /**
     * Callback for every item that is iterated
     *
     * @param <V> the class of the iterated items
     */
    interface ItemCallback<V> {

        /**
         * @param item the item for this step of the iteration
         * @return a Future that completes when this step of the iteration is done and contains a {@link Boolean} if the
         * iteration should be continued
         */
        @NotNull
        ListenableFuture<Boolean> onItems(@NotNull Collection<V> item);
    }
}
