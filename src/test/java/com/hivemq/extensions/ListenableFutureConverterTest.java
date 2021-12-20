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
package com.hivemq.extensions;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import util.TestException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ListenableFutureConverterTest {

    @Mock
    private RetainedMessagePersistence retainedMessagePersistence;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test_cancel() {

        final ListenableFuture<Void> voidListenableFuture = SettableFuture.create();

        final CompletableFuture<Void> voidCompletableFuture = ListenableFutureConverter.toCompletable(voidListenableFuture, MoreExecutors.directExecutor());

        assertFalse(voidCompletableFuture.isCancelled());
        assertFalse(voidListenableFuture.isCancelled());

        voidCompletableFuture.cancel(true);
        assertTrue(voidListenableFuture.isCancelled());

    }

    @Test
    public void test_conversion_failed_not_null() throws ExecutionException, InterruptedException {

        final SettableFuture<Void> voidListenableFuture = SettableFuture.create();

        final Function<Void, String> functionMock = Mockito.mock(Function.class);

        final CompletableFuture<String> voidCompletableFuture = ListenableFutureConverter.toCompletable(voidListenableFuture, functionMock, false, MoreExecutors.directExecutor());

        when(functionMock.apply(null)).thenThrow(new RuntimeException("TEST"));

        voidListenableFuture.set(null);

        assertTrue(voidCompletableFuture.isCompletedExceptionally());

    }

    @Test
    public void test_conversion_nullable_null_result() throws ExecutionException, InterruptedException {

        final SettableFuture<Void> voidListenableFuture = SettableFuture.create();

        final Function<Void, String> functionMock = Mockito.mock(Function.class);

        final CompletableFuture<String> voidCompletableFuture = ListenableFutureConverter.toCompletable(voidListenableFuture, functionMock, true, MoreExecutors.directExecutor());

        //apply must not be called, if result nullable and null
        when(functionMock.apply(any(Void.class))).thenThrow(new RuntimeException("TEST"));

        voidListenableFuture.set(null);

        assertTrue(voidCompletableFuture.isDone());
        assertFalse(voidCompletableFuture.isCompletedExceptionally());

        assertNull(voidCompletableFuture.get());

    }

    @Test
    public void test_conversion_nullable_not_null_result_converted() throws ExecutionException, InterruptedException {

        final SettableFuture<Integer> voidListenableFuture = SettableFuture.create();

        final Function<Integer, String> functionMock = x -> "" + x;

        final CompletableFuture<String> voidCompletableFuture = ListenableFutureConverter.toCompletable(voidListenableFuture, functionMock, true, MoreExecutors.directExecutor());

        voidListenableFuture.set(5);

        assertTrue(voidCompletableFuture.isDone());
        assertFalse(voidCompletableFuture.isCompletedExceptionally());

        assertEquals("5", voidCompletableFuture.get());

    }

    @Test
    public void test_conversion_nullable_not_null_result_no_converter() throws ExecutionException, InterruptedException {

        final SettableFuture<Integer> voidListenableFuture = SettableFuture.create();

        final CompletableFuture<Integer> voidCompletableFuture = ListenableFutureConverter.toCompletable(voidListenableFuture, MoreExecutors.directExecutor());

        voidListenableFuture.set(5);

        assertTrue(voidCompletableFuture.isDone());
        assertFalse(voidCompletableFuture.isCompletedExceptionally());

        assertEquals(5, voidCompletableFuture.get().intValue());

    }

    @Test
    public void test_conversion_nullable_null_result_no_converter() throws ExecutionException, InterruptedException {

        final SettableFuture<Integer> voidListenableFuture = SettableFuture.create();

        final CompletableFuture<Integer> voidCompletableFuture = ListenableFutureConverter.toCompletable(voidListenableFuture, MoreExecutors.directExecutor());

        voidListenableFuture.set(null);

        assertTrue(voidCompletableFuture.isDone());
        assertFalse(voidCompletableFuture.isCompletedExceptionally());

        assertEquals(null, voidCompletableFuture.get());

    }

    @Test
    public void test_conversion_nullable_null_result_with_converter() throws ExecutionException, InterruptedException {

        final SettableFuture<Integer> voidListenableFuture = SettableFuture.create();

        final Function<Integer, String> functionMock = x -> "" + x;

        final CompletableFuture<String> voidCompletableFuture = ListenableFutureConverter.toCompletable(voidListenableFuture, functionMock, MoreExecutors.directExecutor());

        voidListenableFuture.set(null);

        assertTrue(voidCompletableFuture.isDone());
        assertFalse(voidCompletableFuture.isCompletedExceptionally());

        assertEquals(null, voidCompletableFuture.get());

    }

    @Test(expected = ExecutionException.class)
    public void test_execution_failed() throws ExecutionException, InterruptedException {

        final SettableFuture<Void> voidListenableFuture = SettableFuture.create();

        final Function<Void, String> functionMock = Mockito.mock(Function.class);

        final CompletableFuture<String> voidCompletableFuture = ListenableFutureConverter.toCompletable(voidListenableFuture, functionMock, MoreExecutors.directExecutor());

        voidListenableFuture.setException(TestException.INSTANCE);

        voidCompletableFuture.get();

    }

}