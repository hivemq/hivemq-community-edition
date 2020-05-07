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

package com.hivemq.embedded;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;

import java.util.concurrent.CompletableFuture;

@DoNotImplement
@ThreadSafe
public interface EmbeddedHiveMQ {

    /**
     * Start an EmbeddedHiveMQ.
     * <p>
     * This method can be called multiple times.
     *
     * @return a {@link CompletableFuture} that completes when HiveMQ is started and ready
     */
    @NotNull CompletableFuture<Void> start();

    /**
     * Stop an EmbeddedHiveMQ. Calling stop clears the metric registry returned by {@link #getMetricRegistry()}.
     * <p>
     * This method can be called multiple times.
     *
     * @return a {@link CompletableFuture} that completes when HiveMQ is stopped
     */
    @NotNull CompletableFuture<Void> stop();

    /**
     * Access HiveMQ's metric registry. The metric registry can be accessed before EmbeddedHiveMQ is started.
     *
     * @return the {@link MetricRegistry} containing all HiveMQ metrics
     */
    @NotNull MetricRegistry getMetricRegistry();
}
