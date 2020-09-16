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
package com.hivemq.embedded;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.embedded.internal.EmbeddedHiveMQBuilderImpl;
import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@DoNotImplement
@ThreadSafe
public interface EmbeddedHiveMQ extends AutoCloseable {

    /**
     * @return a new EmbeddedHiveMQBuilder.
     */
    static @NotNull EmbeddedHiveMQBuilder builder() {
        return new EmbeddedHiveMQBuilderImpl();
    }
    
    /**
     * Start an EmbeddedHiveMQ.
     * <p>
     * This method is idempotent. Calling start again on an already started EmbeddedHiveMQ has no effect.
     * <p>
     * A {@link #stop()}ed EmbeddedHiveMQ can be restarted with this method. If no enduring persistence type such as 
     * file persistence, is configured, the restarted EmbeddedHiveMQ does not retain its state.
     *
     * @return a {@link CompletableFuture} that completes when HiveMQ is started and ready
     */
    @NotNull CompletableFuture<Void> start();

    /**
     * Stop an EmbeddedHiveMQ. Calling stop clears the metric registry returned by {@link #getMetricRegistry()}.
     * <p>
     * This method is idempotent. Calling stop again on an already stopped EmbeddedHiveMQ has no effect.
     * <p>
     * A stopped EmbeddedHiveMQ can be restarted with the {@link #start()} method. If no enduring persistence type such as
     * a file persistence is configured, the restarted EmbeddedHiveMQ does not retain its state.
     *
     * @return a {@link CompletableFuture} that completes when HiveMQ is stopped
     */
    @NotNull CompletableFuture<Void> stop();

    /**
     * Access the metric registry of HiveMQ. The metric registry can be accessed before EmbeddedHiveMQ is started.
     *
     * @return the {@link MetricRegistry} that contains all HiveMQ metrics
     */
    @NotNull MetricRegistry getMetricRegistry();

    /**
     * {@inheritDoc}
     */
    @Override
    void close() throws ExecutionException, InterruptedException;
}
