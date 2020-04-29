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

package com.hivemq.embedded.internal;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.embedded.EmbeddedHiveMQ;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * @author Georg Held
 */
public class EmbeddedHiveMQImpl implements EmbeddedHiveMQ {

    private final @Nullable Path conf;
    private final @NotNull Path extensions;
    private final @NotNull Path data;

    EmbeddedHiveMQImpl(final @Nullable Path conf, final @NotNull Path extensions, final @NotNull Path data) {

        this.conf = conf;
        this.extensions = extensions;
        this.data = data;
    }

    @Override
    public CompletableFuture<Void> start() {
        return null;
    }

    @Override
    public CompletableFuture<Void> stop() {
        return null;
    }

    @Override
    public MetricRegistry getMetricRegistry() {
        return null;
    }
}
