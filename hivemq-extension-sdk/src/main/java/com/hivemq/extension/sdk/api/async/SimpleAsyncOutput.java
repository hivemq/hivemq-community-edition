package com.hivemq.extension.sdk.api.async;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.time.Duration;

/**
 * Enables an output object to be processed in a non-blocking way.
 *
 * @author Yannick Weber
 */
@DoNotImplement
public interface SimpleAsyncOutput<T> {

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is handled as failed.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout Timeout that HiveMQ waits for the result of the async operation.
     * @throws UnsupportedOperationException If async is called more than once.
     * @since 4.0.0
     */
    @NotNull Async<T> async(@NotNull Duration timeout);

}
