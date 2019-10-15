package com.hivemq.extensions.executor.task;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.SimpleAsyncOutput;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Yannick Weber
 */
public class AbstractSimpleAsyncOutput<T> implements PluginTaskOutput, SimpleAsyncOutput<T> {

    protected final @NotNull PluginOutPutAsyncer asyncer;

    private final @NotNull AtomicBoolean async = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean called = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean timedOut = new AtomicBoolean(false);
    private final @NotNull SettableFuture<Boolean> asyncFuture = SettableFuture.create();

    public AbstractSimpleAsyncOutput(final @NotNull PluginOutPutAsyncer asyncer) {
        this.asyncer = asyncer;
    }

    @Override
    public @NotNull Async<T> async(final @NotNull Duration timeout) {
        checkCalled();
        //noinspection unchecked: this cast is safe since this implements AsyncOutput and PluginTaskOutput
        return (Async<T>) asyncer.asyncify(this, timeout);
    }

    private void checkCalled() {
        if (!called.compareAndSet(false, true)) {
            throw new UnsupportedOperationException("async must not be called more than once");
        }
    }

    @Override
    public boolean isAsync() {
        return async.get();
    }

    @Override
    public void markAsAsync() {
        async.set(true);
    }

    @Override
    public boolean isTimedOut() {
        return timedOut.get();
    }

    @Override
    public void markAsTimedOut() {
        timedOut.set(true);
    }

    public void resetAsyncStatus() {
        timedOut.set(false);
        async.set(false);
        called.set(false);
    }

    @Override
    public @Nullable SettableFuture<Boolean> getAsyncFuture() {
        return asyncFuture;
    }

    @Override
    public @NotNull TimeoutFallback getTimeoutFallback() {
        return TimeoutFallback.FAILURE;
    }
}
