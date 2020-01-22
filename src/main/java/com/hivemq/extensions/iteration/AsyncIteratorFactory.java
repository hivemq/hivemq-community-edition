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
package com.hivemq.extensions.iteration;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.util.ThreadFactoryUtil;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Christoph Schäbel
 */
@LazySingleton
public class AsyncIteratorFactory {

    private final @NotNull ExecutorService executorService;

    @Inject
    public AsyncIteratorFactory() {
        executorService = Executors.newFixedThreadPool(4, ThreadFactoryUtil.create("async-iterator-executor-%d"));
    }

    @NotNull
    public <K, V> AsyncIterator<K, V> createIterator(
            @NotNull final FetchCallback<K, V> fetchCallback,
            @NotNull final AsyncIterator.ItemCallback<V> iterationCallback) {

        return new AsyncLocalChunkIterator<K, V>(fetchCallback, iterationCallback, executorService);
    }

}
