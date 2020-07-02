package com.hivemq.extensions.iteration;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.general.IterationCallback;
import com.hivemq.extensions.services.general.IterationContextImpl;

import java.util.Collection;
import java.util.concurrent.Executor;

/**
 * @author Georg Held
 */
public class AllItemsItemCallback<T> implements AsyncIterator.ItemCallback<T> {

    private final @NotNull Executor callbackExecutor;
    private final @NotNull IterationCallback<T> callback;

    public AllItemsItemCallback(final @NotNull Executor callbackExecutor, final @NotNull IterationCallback<T> callback) {
        this.callbackExecutor = callbackExecutor;
        this.callback = callback;
    }

    @Override
    public @NotNull ListenableFuture<Boolean> onItems(final @NotNull Collection<T> items) {
        final IterationContextImpl iterationContext = new IterationContextImpl();
        final SettableFuture<Boolean> resultFuture = SettableFuture.create();

        callbackExecutor.execute(() -> {

            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(callback.getClass().getClassLoader());
                for (final T item : items) {

                    callback.iterate(iterationContext, item);

                    if (iterationContext.isAborted()) {
                        resultFuture.set(false);
                        break;
                    }
                }
            } catch (final Exception e) {
                resultFuture.setException(e);
            } finally {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
            resultFuture.set(true);
        });

        return resultFuture;
    }
}

