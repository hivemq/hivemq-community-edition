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

package com.hivemq.persistence.util;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.ioc.annotation.Persistence;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.Set;

/**
 * @author Lukas Brandl
 */
@Singleton
public class FutureUtilsImpl extends AbstractFutureUtils {

    private final @NotNull ListeningExecutorService persistenceExecutorService;

    @Inject
    public FutureUtilsImpl(@Persistence final @NotNull ListeningExecutorService persistenceExecutorService) {
        this.persistenceExecutorService = persistenceExecutorService;
    }

    @Override
    public <T> void addPersistenceCallback(
            final @NotNull ListenableFuture<T> future, final @NotNull FutureCallback<? super T> callback) {
        super.addPersistenceCallback(future, callback, persistenceExecutorService);
    }

    @Override
    public <E, C extends Collection<Set<E>>> @NotNull ListenableFuture<Set<E>> combineSetResults(
            final @NotNull ListenableFuture<C> collectionFuture) {
        return super.combineSetResults(collectionFuture, persistenceExecutorService);
    }
}
