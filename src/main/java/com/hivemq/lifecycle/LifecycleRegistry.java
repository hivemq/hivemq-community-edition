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
package com.hivemq.lifecycle;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.util.ThreadFactoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dominik Obermaier
 */
@Singleton
public class LifecycleRegistry {

    private static final Logger log = LoggerFactory.getLogger(LifecycleRegistry.class);


    private final List<PreDestroyInvokable> preDestroyInvokables;

    LifecycleRegistry() {
        final List<PreDestroyInvokable> preDestroyInvokables = new ArrayList<>();
        this.preDestroyInvokables = Collections.synchronizedList(preDestroyInvokables);
    }

    public void addPreDestroyMethod(@NotNull final Method preDestroyMethod, @NotNull final Object onObject) {
        checkNotNull(preDestroyMethod);
        checkNotNull(onObject);

        preDestroyInvokables.add(new PreDestroyInvokable(preDestroyMethod, onObject));
    }


    /**
     * Executes the preDestroy methods in parallel. This method does not block and
     * you have to synchronize yourself if you want so with the returned {@link com.google.common.util.concurrent.ListenableFuture}
     * <p>
     * There are no guarantees of the return type when you wait for the future. Most likely you'll get Void or null, though.
     *
     * @return a {@link com.google.common.util.concurrent.ListenableFuture} of all preDestroy executions
     */
    public ListenableFuture<?> executePreDestroy() {
        final ExecutorService executorService = Executors.newFixedThreadPool(3, ThreadFactoryUtil.create("PreDestroy-%d"));

        final ListeningExecutorService executorService1 = MoreExecutors.listeningDecorator(executorService);

        final List<ListenableFuture<?>> futures = new ArrayList<>();

        for (final PreDestroyInvokable preDestroyInvokable : preDestroyInvokables) {
            futures.add(executorService1.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        preDestroyInvokable.getPreDestroyMethod().invoke(preDestroyInvokable.getOnObject());
                    } catch (final IllegalAccessException | InvocationTargetException e) {
                        log.error("Could not execute preDestroy method for class {}", preDestroyInvokable.getOnObject().getClass(), e);
                    }
                }
            }));
        }

        return Futures.allAsList(futures);
    }


    private static final class PreDestroyInvokable {
        private final Method preDestroyMethod;
        private final Object onObject;

        public PreDestroyInvokable(@NotNull final Method preDestroyMethod, @NotNull final Object onObject) {
            this.preDestroyMethod = preDestroyMethod;
            this.onObject = onObject;
        }

        public Method getPreDestroyMethod() {
            return preDestroyMethod;
        }

        public Object getOnObject() {
            return onObject;
        }
    }
}
