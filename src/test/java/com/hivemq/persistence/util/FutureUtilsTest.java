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

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Lukas Brandl
 */
public class FutureUtilsTest {

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
}