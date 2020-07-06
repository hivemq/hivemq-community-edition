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
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AllItemsItemCallbackTest {

    @Test
    public void iterateAllItems() throws ExecutionException, InterruptedException {
        final List<String> input = Arrays.asList("1", "2", "3");
        final List<String> output = new ArrayList<>();

        final AllItemsItemCallback<String> stringCallback = new AllItemsItemCallback<>(MoreExecutors.directExecutor(), (ctx, item) -> {
            output.add(item);
        });

        final ListenableFuture<Boolean> future = stringCallback.onItems(input);

        assertTrue(future.get());
        assertEquals(input, output);
    }

    @Test
    public void contextCancelled() throws ExecutionException, InterruptedException {
        final List<String> input = Arrays.asList("1", "2", "3");
        final List<String> output = new ArrayList<>();

        final AllItemsItemCallback<String> stringCallback = new AllItemsItemCallback<>(MoreExecutors.directExecutor(), (ctx, item) -> {
            ctx.abortIteration();
            output.add(item);
        });

        final ListenableFuture<Boolean> future = stringCallback.onItems(input);

        assertFalse(future.get());
        assertEquals(1, output.size());
    }
}