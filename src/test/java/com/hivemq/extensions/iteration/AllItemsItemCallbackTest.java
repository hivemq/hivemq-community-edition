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