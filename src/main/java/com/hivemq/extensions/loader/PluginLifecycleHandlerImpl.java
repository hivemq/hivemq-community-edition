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
package com.hivemq.extensions.loader;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.HiveMQPluginEvent;
import com.hivemq.extensions.ioc.annotation.PluginStartStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * @author Christoph Schäbel
 * @author Silvio Giebl
 * @author Georg Held
 */
@Singleton
public class PluginLifecycleHandlerImpl implements PluginLifecycleHandler {

    private static final Logger log = LoggerFactory.getLogger(PluginLifecycleHandlerImpl.class);

    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull ExecutorService pluginStartStopExecutor;

    @Inject
    @VisibleForTesting
    public PluginLifecycleHandlerImpl(
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull @PluginStartStop ExecutorService pluginStartStopExecutor) {

        this.hiveMQExtensions = hiveMQExtensions;
        this.pluginStartStopExecutor = pluginStartStopExecutor;
    }

    @Override
    public @NotNull CompletableFuture<Void> handlePluginEvents(final @NotNull ImmutableList<HiveMQPluginEvent> hiveMQPluginEvents) {

        final ImmutableList<HiveMQPluginEvent> sorted = analyzePluginEvents(hiveMQPluginEvents);

        CompletableFuture<Void> returnFuture = CompletableFuture.completedFuture(null);

        for (final HiveMQPluginEvent hiveMQPluginEvent : sorted) {
            returnFuture = returnFuture
                    .thenComposeAsync((v) -> handlePluginEvent(hiveMQPluginEvent), pluginStartStopExecutor)
                    .handle((v, t) -> {
                        if (t != null) {
                            log.debug("Exception during Extension Lifecycle event handling", t);
                        }
                        return null;
                    });

        }
        return returnFuture;
    }

    private @NotNull CompletableFuture<Boolean> handlePluginEvent(final @NotNull HiveMQPluginEvent hiveMQPluginEvent) {
        switch (hiveMQPluginEvent.getChange()) {
            case ENABLE:
                return startPlugin(hiveMQPluginEvent);
            case DISABLE:
                return stopPlugin(hiveMQPluginEvent.getPluginId());
            default:
                return CompletableFuture.completedFuture(false);
        }
    }

    private @NotNull CompletableFuture<Boolean> startPlugin(final @NotNull HiveMQPluginEvent pluginEvent) {
        final String pluginId = pluginEvent.getPluginId();

        log.debug("Starting extension with id \"{}\" at {}", pluginId, pluginEvent.getPluginFolder());
        return CompletableFuture.supplyAsync(() -> hiveMQExtensions.extensionStart(pluginId), pluginStartStopExecutor);
    }

    private @NotNull CompletableFuture<Boolean> stopPlugin(final @NotNull String pluginId) {

        log.debug("Stopping extension with id {}", pluginId);
        return CompletableFuture.supplyAsync(() -> hiveMQExtensions.extensionStop(pluginId, false), pluginStartStopExecutor);
    }

    private @NotNull ImmutableList<HiveMQPluginEvent> analyzePluginEvents(final @NotNull ImmutableList<HiveMQPluginEvent> hiveMQPluginEvents) {
        // Here duplicate start priority logging can be added, once it is specified
        return ImmutableList.sortedCopyOf(
                Comparator.comparingInt(HiveMQPluginEvent::getPriority).reversed(), hiveMQPluginEvents);
    }
}
