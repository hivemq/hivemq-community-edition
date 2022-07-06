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

package com.hivemq.extensions.services.executor;

import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.CompletableScheduledFuture;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 4.0.0
 */
public class ManagedExecutorServicePerExtensionTest {

    private final @NotNull ShutdownHooks shutdownHooks = mock(ShutdownHooks.class);
    private final @NotNull IsolatedExtensionClassloader classLoader = mock(IsolatedExtensionClassloader.class);
    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull HiveMQExtension extension = mock(HiveMQExtension.class);

    private @NotNull ManagedExecutorServicePerExtension managedExecutorServicePerExtension;
    private @NotNull GlobalManagedExtensionExecutorService globalManagedPluginExecutorService;

    @Before
    public void setUp() throws Exception {
        InternalConfigurations.MANAGED_EXTENSION_THREAD_POOL_KEEP_ALIVE_SEC.set(60);
        InternalConfigurations.MANAGED_EXTENSION_THREAD_POOL_THREADS_COUNT.set(4);

        when(hiveMQExtensions.getExtensionForClassloader(classLoader)).thenReturn(extension);

        globalManagedPluginExecutorService = new GlobalManagedExtensionExecutorService(shutdownHooks);
        globalManagedPluginExecutorService.postConstruct();

        managedExecutorServicePerExtension = new ManagedExecutorServicePerExtension(globalManagedPluginExecutorService,
                classLoader,
                hiveMQExtensions);
    }

    @After
    public void tearDown() {
        new ManagedPluginExecutorShutdownHook(globalManagedPluginExecutorService, 10).run();
    }

    @Test(expected = UnsupportedOperationException.class)
    @SuppressWarnings("deprecation")
    public void test_shutdown_unsupported() {
        managedExecutorServicePerExtension.shutdown();
    }

    @Test(expected = UnsupportedOperationException.class)
    @SuppressWarnings("deprecation")
    public void test_shutdownNow_unsupported() {
        managedExecutorServicePerExtension.shutdownNow();
    }

    @Test
    public void test_post_construct() {
        assertEquals(4, globalManagedPluginExecutorService.getCorePoolSize());
        assertEquals(60, globalManagedPluginExecutorService.getKeepAliveSeconds());
        assertFalse(managedExecutorServicePerExtension.isShutdown());
        assertFalse(managedExecutorServicePerExtension.isTerminated());
    }

    @Test
    public void test_execute() throws InterruptedException {
        final CountDownLatch runLatch = new CountDownLatch(1);

        managedExecutorServicePerExtension.execute(runLatch::countDown);

        assertTrue(runLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void test_execute_plugin_stopped() throws Exception {
        final CountDownLatch runLatch = new CountDownLatch(1);

        when(hiveMQExtensions.getExtensionForClassloader(classLoader)).thenReturn(null);

        managedExecutorServicePerExtension.execute(runLatch::countDown);

        assertTrue(runLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void test_schedule_runnable() throws Exception {
        final CountDownLatch runLatch = new CountDownLatch(1);
        final CountDownLatch futureLatch = new CountDownLatch(1);
        final AtomicBoolean delayNotPositive = new AtomicBoolean(false);

        final CompletableScheduledFuture<?> schedule =
                managedExecutorServicePerExtension.schedule(runLatch::countDown, 500, TimeUnit.MILLISECONDS);

        schedule.whenComplete((BiConsumer<Object, Throwable>) (o, throwable) -> {
            delayNotPositive.set(schedule.getDelay(TimeUnit.MILLISECONDS) <= 0);
            futureLatch.countDown();
        });

        assertFalse(runLatch.await(250, TimeUnit.MILLISECONDS));
        assertTrue(runLatch.await(2, TimeUnit.SECONDS));

        futureLatch.await();

        assertTrue(delayNotPositive.get());
    }

    @Test
    public void test_schedule_runnable_plugin_stopped() throws Exception {
        final CountDownLatch runLatch = new CountDownLatch(1);

        when(hiveMQExtensions.getExtensionForClassloader(classLoader)).thenReturn(null);

        final CompletableScheduledFuture<?> schedule =
                managedExecutorServicePerExtension.schedule(runLatch::countDown, 500, TimeUnit.MILLISECONDS);

        assertTrue(runLatch.await(2, TimeUnit.SECONDS));

        schedule.get();

        assertFalse(schedule.isCancelled());
        assertTrue(schedule.isDone());
    }

    @Test(timeout = 5000)
    public void test_schedule_callable() throws Exception {
        final CountDownLatch runLatch = new CountDownLatch(1);
        final CountDownLatch futureLatch = new CountDownLatch(1);

        final CompletableScheduledFuture<String> scheduledFuture = managedExecutorServicePerExtension.schedule(() -> {
            runLatch.countDown();
            return "test";
        }, 50, TimeUnit.MILLISECONDS);

        scheduledFuture.whenComplete((string, throwable) -> {
            if (string.equals("test")) {
                futureLatch.countDown();
            }
        });

        assertFalse(runLatch.await(25, TimeUnit.MILLISECONDS));
        assertFalse(futureLatch.await(0, TimeUnit.MILLISECONDS));
        assertTrue(runLatch.await(2, TimeUnit.SECONDS));
        assertTrue(futureLatch.await(2, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_schedule_callable_cancelled() throws Exception {
        final CountDownLatch runLatch = new CountDownLatch(1);
        final CountDownLatch exceptionLatch = new CountDownLatch(1);

        final CompletableScheduledFuture<String> scheduledFuture = managedExecutorServicePerExtension.schedule(() -> {
            runLatch.countDown();
            return "test";
        }, 10, TimeUnit.MILLISECONDS);

        scheduledFuture.whenComplete((string, throwable) -> {
            // check for cancellation exception
            if (string == null && throwable instanceof CancellationException) {
                exceptionLatch.countDown();
            }
        });

        scheduledFuture.cancel(true);

        assertFalse(runLatch.await(100, TimeUnit.MILLISECONDS));
        assertTrue(exceptionLatch.await(2, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_schedule_callable_plugin_stopped() throws Exception {
        final CountDownLatch runLatch = new CountDownLatch(1);

        when(hiveMQExtensions.getExtensionForClassloader(classLoader)).thenReturn(null);

        final CompletableScheduledFuture<String> scheduledFuture = managedExecutorServicePerExtension.schedule(() -> {
            runLatch.countDown();
            return "test";
        }, 500, TimeUnit.MILLISECONDS);

        assertTrue(runLatch.await(2, TimeUnit.SECONDS));

        assertEquals("test", scheduledFuture.get());

        assertFalse(scheduledFuture.isCancelled());
        assertTrue(scheduledFuture.isDone());
    }

    @Test
    public void test_schedule_at_fixed_rate() throws Exception {
        final CountDownLatch runLatch = new CountDownLatch(10);
        final CountDownLatch completeLatch = new CountDownLatch(1);

        final Runnable task = runLatch::countDown;

        final CompletableScheduledFuture<?> scheduledFuture =
                managedExecutorServicePerExtension.scheduleAtFixedRate(task, 10, 10, TimeUnit.MILLISECONDS);

        assertTrue(runLatch.await(2, TimeUnit.SECONDS));

        final long delay = scheduledFuture.getDelay(TimeUnit.MILLISECONDS);
        assertTrue("bad delay: " + delay, delay <= 10);

        // does not complete normally
        scheduledFuture.whenComplete((object, throwable) -> completeLatch.countDown());

        assertEquals(1, completeLatch.getCount());

        scheduledFuture.cancel(true);

        // completes by cancellation
        assertTrue(completeLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void test_schedule_at_fixed_rate_canceled() throws Exception {
        final CountDownLatch runLatch = new CountDownLatch(10);
        final CountDownLatch completeLatch = new CountDownLatch(1);

        final Runnable task = () -> {
            runLatch.countDown();
            if (runLatch.getCount() == 0) {
                // cancels future
                when(hiveMQExtensions.getExtensionForClassloader(classLoader)).thenReturn(null);
            }
        };

        final CompletableScheduledFuture<?> scheduledFuture =
                managedExecutorServicePerExtension.scheduleAtFixedRate(task, 10, 10, TimeUnit.MILLISECONDS);

        assertTrue(runLatch.await(2, TimeUnit.SECONDS));

        final long delay = scheduledFuture.getDelay(TimeUnit.MILLISECONDS);
        assertTrue("bad delay: " + delay, delay <= 100);

        // does not complete normally
        scheduledFuture.whenComplete((object, throwable) -> completeLatch.countDown());

        assertTrue(completeLatch.await(200, TimeUnit.MILLISECONDS));
        assertEquals(0, completeLatch.getCount());

        final ScheduledFuture<?> delegateFuture =
                ((CompletableScheduledFutureImpl<?>) scheduledFuture).getScheduledFuture();
        assertNotNull(delegateFuture);
        assertTrue(delegateFuture.isCancelled());
    }

    @Test
    public void test_schedule_with_fixed_delay() throws Exception {
        final CountDownLatch runLatch = new CountDownLatch(5);
        final CountDownLatch completeLatch = new CountDownLatch(1);

        final Runnable task = () -> {
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            runLatch.countDown();
        };

        final CompletableScheduledFuture<?> scheduledFuture =
                managedExecutorServicePerExtension.scheduleWithFixedDelay(task, 10, 10, TimeUnit.MILLISECONDS);

        assertTrue(runLatch.await(2, TimeUnit.SECONDS));

        final long delay = scheduledFuture.getDelay(TimeUnit.MILLISECONDS);
        assertTrue("bad delay: " + delay, delay <= 10);


        // does not complete normally
        scheduledFuture.whenComplete((object, throwable) -> completeLatch.countDown());

        assertFalse(completeLatch.await(300, TimeUnit.MILLISECONDS));
        assertEquals(1, completeLatch.getCount());

        scheduledFuture.cancel(true);

        // completes by cancellation
        assertTrue(completeLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void test_schedule_with_fixed_delay_exceptional() throws Exception {
        final CountDownLatch runLatch = new CountDownLatch(5);
        final CountDownLatch exceptionLatch = new CountDownLatch(1);

        final Runnable task = () -> {
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            runLatch.countDown();

            if (runLatch.getCount() == 0) {
                throw new RuntimeException("something is missing");
            }
        };

        final CompletableScheduledFuture<?> scheduledFuture =
                managedExecutorServicePerExtension.scheduleWithFixedDelay(task, 10, 10, TimeUnit.MILLISECONDS);

        assertTrue(runLatch.await(2, TimeUnit.SECONDS));

        final long delay = scheduledFuture.getDelay(TimeUnit.MILLISECONDS);
        assertTrue("bad delay: " + delay, delay <= 10);

        // does complete exceptionally
        scheduledFuture.whenComplete((object, throwable) -> {
            if (throwable != null && throwable.getMessage().equals("something is missing")) {
                exceptionLatch.countDown();
            }
        });

        assertTrue(exceptionLatch.await(300, TimeUnit.MILLISECONDS));
        assertEquals(0, exceptionLatch.getCount());
    }

    @Test
    public void test_await_termination() throws Exception {
        managedExecutorServicePerExtension.schedule(() -> {
            try {
                Thread.sleep(10);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }, 10, TimeUnit.MILLISECONDS);

        assertFalse(managedExecutorServicePerExtension.awaitTermination(100, TimeUnit.MILLISECONDS));

        globalManagedPluginExecutorService.shutdownNow();
        assertTrue(managedExecutorServicePerExtension.awaitTermination(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_submit_callable() throws Exception {
        final CountDownLatch callableLatch = new CountDownLatch(1);

        final Callable<String> callable = () -> "test";

        final CompletableFuture<String> submit = managedExecutorServicePerExtension.submit(callable);

        submit.whenComplete((s, throwable) -> {
            if (s.equals("test")) {
                callableLatch.countDown();
            }
        });

        assertTrue(callableLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void test_submit_callable_exceptional() throws Exception {
        final CountDownLatch callableLatch = new CountDownLatch(1);
        final AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>();

        final Callable<String> callable = () -> {
            throw new NullPointerException("something is missing");
        };

        final CompletableFuture<String> submit = managedExecutorServicePerExtension.submit(callable);

        submit.whenComplete((s, throwable) -> {
            throwableAtomicReference.set(throwable);
            if (s == null) {
                callableLatch.countDown();
            }
        });

        assertTrue(callableLatch.await(1, TimeUnit.SECONDS));

        assertTrue(submit.isCompletedExceptionally());

        assertEquals("something is missing", throwableAtomicReference.get().getMessage());
        assertTrue(throwableAtomicReference.get() instanceof NullPointerException);
    }

    @Test
    public void test_submit_runnable() throws Exception {
        final CountDownLatch runLatch = new CountDownLatch(1);

        final Runnable runnable = () -> {
        };

        final CompletableFuture<?> submit = managedExecutorServicePerExtension.submit(runnable);

        submit.whenComplete((s, throwable) -> runLatch.countDown());

        assertTrue(runLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void test_submit_runnable_exceptional() throws Exception {
        final CountDownLatch runLatch = new CountDownLatch(1);
        final AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>();

        final Runnable runnable = () -> {
            throw new NullPointerException("something is missing");
        };

        final CompletableFuture<?> submit = managedExecutorServicePerExtension.submit(runnable);

        submit.whenComplete((s, throwable) -> {
            throwableAtomicReference.set(throwable);
            runLatch.countDown();
        });

        assertTrue(runLatch.await(1, TimeUnit.SECONDS));

        assertTrue(submit.isCompletedExceptionally());

        assertEquals("something is missing", throwableAtomicReference.get().getMessage());
        assertTrue(throwableAtomicReference.get() instanceof NullPointerException);
    }

    @Test
    public void test_submit_runnable_with_result() throws Exception {
        final CountDownLatch runLatch = new CountDownLatch(1);

        final Runnable runnable = () -> {
        };

        final CompletableFuture<CountDownLatch> submit = managedExecutorServicePerExtension.submit(runnable, runLatch);

        submit.whenComplete((s, throwable) -> s.countDown());

        assertTrue(runLatch.await(1, TimeUnit.SECONDS));
        assertTrue(submit.isDone());
        assertFalse(submit.isCompletedExceptionally());
    }

    @Test
    public void test_submit_runnable_with_result_plugin_stopped() throws Exception {
        final CountDownLatch runLatch = new CountDownLatch(1);

        when(hiveMQExtensions.getExtensionForClassloader(classLoader)).thenReturn(null);

        final Runnable runnable = () -> {
        };

        final CompletableFuture<CountDownLatch> submit = managedExecutorServicePerExtension.submit(runnable, runLatch);

        submit.whenComplete((s, throwable) -> s.countDown());

        assertTrue(runLatch.await(1, TimeUnit.SECONDS));
        assertTrue(submit.isDone());
        assertFalse(submit.isCancelled());
    }

    @Test
    public void test_submit_runnable_with_result_exceptional() throws Exception {
        final CountDownLatch runLatch = new CountDownLatch(1);
        final AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>();

        final Runnable runnable = () -> {
            throw new NullPointerException("something is missing");
        };

        final CompletableFuture<CountDownLatch> submit = managedExecutorServicePerExtension.submit(runnable, runLatch);

        submit.whenComplete((object, throwable) -> {
            throwableAtomicReference.set(throwable);
            // must not be set at exceptional completion
            if (object == null) {
                runLatch.countDown();
            }
        });

        assertTrue(runLatch.await(5, TimeUnit.SECONDS));

        assertTrue(submit.isDone());
        assertTrue(submit.isCompletedExceptionally());
        assertNotNull(throwableAtomicReference);
    }

    @Test
    public void test_invokeAll_callable() throws Exception {
        final CountDownLatch invokeAllLatch = new CountDownLatch(5);

        final List<Callable<String>> callableList =
                Stream.generate((Supplier<Callable<String>>) () -> () -> "test").limit(5).collect(Collectors.toList());

        final List<Future<String>> futures = managedExecutorServicePerExtension.invokeAll(callableList);

        for (final Future<String> future : futures) {
            final CompletableFuture<String> stringCompletableFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return future.get();
                } catch (final InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });

            stringCompletableFuture.whenComplete((s, throwable) -> {
                if (s.equals("test")) {
                    invokeAllLatch.countDown();
                }
            });
        }

        assertTrue(invokeAllLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void test_invokeAll_callable_timeouts() throws Exception {
        final CountDownLatch canceledAllLatch = new CountDownLatch(5);
        final CountDownLatch calledLatch = new CountDownLatch(5);
        final CountDownLatch waitLatch = new CountDownLatch(1);

        final List<Callable<String>> callableList = Stream.generate((Supplier<Callable<String>>) () -> () -> {
            calledLatch.countDown();
            // force timeout
            waitLatch.await();
            return "test";
        }).limit(5).collect(Collectors.toList());

        final List<Future<String>> futures =
                managedExecutorServicePerExtension.invokeAll(callableList, 50, TimeUnit.MILLISECONDS);

        for (final Future<String> future : futures) {
            final CompletableFuture<String> stringCompletableFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return future.get();
                } catch (final InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });

            stringCompletableFuture.whenComplete((s, throwable) -> {
                if (throwable.getCause() instanceof CancellationException) {
                    canceledAllLatch.countDown();
                }
            });
        }

        assertTrue(canceledAllLatch.await(500, TimeUnit.MILLISECONDS));

        waitLatch.countDown();
    }

    @Test(expected = TimeoutException.class)
    public void test_invokeAny_callable_timeouts() throws Exception {
        final CountDownLatch calledLatch = new CountDownLatch(1);

        final List<Callable<String>> callableList = Stream.generate((Supplier<Callable<String>>) () -> () -> {
            // wait 80 milliseconds to guarantee no timeout
            Thread.sleep(150);
            calledLatch.countDown();
            return "test";
        }).limit(5).collect(Collectors.toList());

        managedExecutorServicePerExtension.invokeAny(callableList, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void test_invokeAny_callable_not_timeouts() throws Exception {
        final CountDownLatch calledLatch = new CountDownLatch(1);

        final List<Callable<String>> callableList = Stream.generate((Supplier<Callable<String>>) () -> () -> {
            // wait 20 milliseconds to guarantee no timeout
            Thread.sleep(20);
            calledLatch.countDown();
            return "test";
        }).limit(5).collect(Collectors.toList());

        final String invoked = managedExecutorServicePerExtension.invokeAny(callableList, 100, TimeUnit.MILLISECONDS);

        // we have 4 threads executing tasks the one call will not be called
        assertTrue(calledLatch.await(200, TimeUnit.MILLISECONDS));
        assertEquals(0, calledLatch.getCount());
        assertEquals("test", invoked);
    }

    @Test
    public void test_invokeAny_callable() throws Exception {
        final CountDownLatch calledLatch1 = new CountDownLatch(1);
        final CountDownLatch calledLatch2 = new CountDownLatch(1);

        final List<Callable<String>> callableList = new ArrayList<>();

        callableList.add(() -> {
            Thread.sleep(200);
            calledLatch1.countDown();
            return "test200";
        });
        callableList.add(() -> {
            Thread.sleep(100);
            calledLatch2.countDown();
            return "test100";
        });

        final String invoked = managedExecutorServicePerExtension.invokeAny(callableList);

        // the faster task will return the string
        assertTrue(calledLatch2.await(200, TimeUnit.MILLISECONDS));
        // the slower task will be interrupted
        assertFalse(calledLatch1.await(200, TimeUnit.MILLISECONDS));
        assertEquals("test100", invoked);
    }
}
