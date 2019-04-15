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

package com.hivemq.persistence.util;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.Rule;
import org.junit.Test;
import util.InitFutureUtilsExecutorRule;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

/**
 * @author Lukas Brandl
 */
public class FutureUtilsTest {

    @Rule
    public InitFutureUtilsExecutorRule initFutureUtilsExecutorRule = new InitFutureUtilsExecutorRule();

    @Test
    public void test_void_future_from_any_future_success() throws Exception {
        final SettableFuture<Object> future = SettableFuture.create();

        final ListenableFuture<Void> voidFuture = FutureUtils.voidFutureFromAnyFuture(future);
        assertEquals(false, voidFuture.isDone());
        future.set(new Object());
        assertEquals(true, voidFuture.isDone());
        voidFuture.get();
    }

    @Test(expected = ExecutionException.class)
    public void test_void_future_from_any_future_failure() throws Exception {
        final SettableFuture<Object> future = SettableFuture.create();

        final ListenableFuture<Void> voidFuture = FutureUtils.voidFutureFromAnyFuture(future);
        assertEquals(false, voidFuture.isDone());
        future.setException(new RuntimeException());
        assertEquals(true, voidFuture.isDone());
        voidFuture.get();
    }

    @Test
    public void test_future_merge() throws Exception {
        final SettableFuture<Void> future1 = SettableFuture.create();
        final SettableFuture<Void> future2 = SettableFuture.create();

        final ListenableFuture<Void> resultFuture = FutureUtils.mergeVoidFutures(future1, future2);
        assertEquals(false, resultFuture.isDone());
        future1.set(null);
        assertEquals(false, resultFuture.isDone());
        future2.set(null);
        assertEquals(true, resultFuture.isDone());
    }

    @Test
    public void test_void_future_from_list() throws Exception {
        final SettableFuture<Void> future1 = SettableFuture.create();
        final SettableFuture<Void> future2 = SettableFuture.create();

        final ImmutableList.Builder<ListenableFuture<Void>> builder = ImmutableList.builder();


        builder.add(future1).add(future2);
        final ListenableFuture<Void> resultFuture = FutureUtils.voidFutureFromList(builder.build());
        assertEquals(false, resultFuture.isDone());
        future1.set(null);
        assertEquals(false, resultFuture.isDone());
        future2.set(null);
        assertEquals(true, resultFuture.isDone());
    }

    @Test(timeout = 5000)
    public void test_void_future_from_list_concurrent() throws Exception {
        final ImmutableList.Builder<ListenableFuture<Void>> builder = ImmutableList.builder();

        final CountDownLatch latch = new CountDownLatch(1);
        for (int i = 0; i < 100; i++) {
            final SettableFuture<Void> future = SettableFuture.create();
            builder.add(future);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        latch.await();
                        future.set(null);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }).start();
        }

        final ListenableFuture<Void> resultFuture = FutureUtils.voidFutureFromList(builder.build());
        latch.countDown();
        while (!resultFuture.isDone()) {
            Thread.sleep(10);
        }
    }

    @Test
    public void test_void_future_from_list_one_exception() throws Exception {
        final SettableFuture<Void> future1 = SettableFuture.create();
        final SettableFuture<Void> future2 = SettableFuture.create();
        final ImmutableList.Builder<ListenableFuture<Void>> builder = ImmutableList.builder();
        builder.add(future1).add(future2);

        final ListenableFuture<Void> resultFuture = FutureUtils.voidFutureFromList(builder.build());

        assertEquals(false, resultFuture.isDone());
        future1.set(null);
        assertEquals(false, resultFuture.isDone());
        future2.setException(new NullPointerException());
        assertEquals(true, resultFuture.isDone());

        Exception expected = null;
        try {
            resultFuture.get();
        } catch (final Exception ex) {
            expected = ex;
        }

        assertThat(expected.getCause(), instanceOf(NullPointerException.class));
    }

    @Test
    public void test_void_future_from_list_multiple_exceptions() throws Exception {
        final SettableFuture<Void> future1 = SettableFuture.create();
        final SettableFuture<Void> future2 = SettableFuture.create();

        final ImmutableList.Builder<ListenableFuture<Void>> builder = ImmutableList.builder();
        builder.add(future1).add(future2);

        final ListenableFuture<Void> resultFuture = FutureUtils.voidFutureFromList(builder.build());

        assertEquals(false, resultFuture.isDone());
        future1.setException(new IllegalAccessException());
        assertEquals(false, resultFuture.isDone());
        future2.setException(new NullPointerException());
        assertEquals(true, resultFuture.isDone());

        Exception expected = null;
        try {
            resultFuture.get();
        } catch (final Exception ex) {
            expected = ex;
        }

        assertThat(expected.getCause(), instanceOf(BatchedException.class));
    }

    @Test
    public void test_add_settable_future_callback() throws Exception {
        final SettableFuture<Void> future1 = SettableFuture.create();
        final SettableFuture<Void> future2 = SettableFuture.create();

        FutureUtils.addSettableFutureCallback(future1, future2);
        assertEquals(false, future2.isDone());
        future1.set(null);
        assertEquals(true, future2.isDone());
    }

    @Test
    public void test_multi_future_result() throws Exception {
        final Map<ListenableFuture<Integer>, FutureUtils.Function<Integer>> futureMap = new HashMap<>();
        final Map<Integer, Integer> intMap = new ConcurrentHashMap<>();
        for (int i = 0; i < 5; i++) {
            final int finalI = i;
            futureMap.put(SettableFuture.create(), new FutureUtils.Function<Integer>() {
                @Override
                public void function(final Integer result) {
                    intMap.put(finalI, result);
                }
            });
        }
        final SettableFuture<Map<Integer, Integer>> resultFuture = SettableFuture.create();
        FutureUtils.multiFutureResult(futureMap, intMap, resultFuture);

        assertEquals(false, resultFuture.isDone());
        for (final ListenableFuture<Integer> future : futureMap.keySet()) {
            ((SettableFuture<Integer>) future).set(0);
        }

        assertEquals(true, resultFuture.isDone());

        final Map<Integer, Integer> resultMap = resultFuture.get();
        assertEquals(5, resultMap.size());
        for (final Integer integer : resultMap.values()) {
            assertEquals(0, (int) integer);
        }
    }

    @Test
    public void test_multi_future_failure() throws Exception {
        final Map<ListenableFuture<Integer>, FutureUtils.Function<Integer>> futureMap = new HashMap<>();
        final Map<Integer, Integer> intMap = new ConcurrentHashMap<>();


        final SettableFuture<Integer> future1 = SettableFuture.create();
        futureMap.put(future1, new FutureUtils.Function<Integer>() {
            @Override
            public void function(final Integer result) {
                intMap.put(1, result);
            }
        });

        final SettableFuture<Integer> future2 = SettableFuture.create();
        futureMap.put(future2, new FutureUtils.Function<Integer>() {
            @Override
            public void function(final Integer result) {
                intMap.put(2, result);
            }
        });

        final SettableFuture<Integer> future3 = SettableFuture.create();
        futureMap.put(future3, new FutureUtils.Function<Integer>() {
            @Override
            public void function(final Integer result) {
                intMap.put(3, result);
            }
        });

        final SettableFuture<Map<Integer, Integer>> resultFuture = SettableFuture.create();

        FutureUtils.multiFutureResult(futureMap, intMap, resultFuture);

        assertFalse(resultFuture.isDone());
        future1.setException(new NullPointerException());
        future2.setException(new RuntimeException());
        future3.set(0);

        assertTrue(resultFuture.isDone());

        Exception expected = null;
        try {
            resultFuture.get();
        } catch (final Exception ex) {
            expected = ex;
        }

        assertNotNull(expected);

        final BatchedException batchedException = (BatchedException) expected.getCause();

        assertEquals(2, batchedException.getThrowables().size());
    }
}