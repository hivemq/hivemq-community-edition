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
package util;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.persistence.util.FutureUtilsImpl;
import org.junit.rules.ExternalResource;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Lukas Brandl
 * @author Silvio Giebl
 */
public class InitFutureUtilsExecutorRule extends ExternalResource {

    private final boolean threadPoolExecutor;
    private ListeningExecutorService persistenceExecutor;

    public InitFutureUtilsExecutorRule() {
        this(false);
    }

    public InitFutureUtilsExecutorRule(final boolean threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @Override
    protected void before() throws Throwable {
        final Field field = FutureUtils.class.getDeclaredField("delegate");
        field.setAccessible(true);
        if (threadPoolExecutor) {
            persistenceExecutor = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(1));
        } else {
            persistenceExecutor = MoreExecutors.newDirectExecutorService();
        }
        field.set(null, new FutureUtilsImpl(persistenceExecutor));
    }

    public void waitForPersistenceTask(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        persistenceExecutor.submit(new Runnable() {
            @Override
            public void run() {
            }
        }).get(timeout, unit);
    }

    public ListeningExecutorService getPersistenceExecutor() {
        return persistenceExecutor;
    }

    public void setPersistenceExecutor(final ListeningExecutorService executor) throws NoSuchFieldException, IllegalAccessException {
        final Field field = FutureUtils.class.getDeclaredField("delegate");
        field.setAccessible(true);
        field.set(null, new FutureUtilsImpl(executor));
    }
}
