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
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.HiveMQExtensionEvent;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.ioc.annotation.PluginStartStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Singleton
public class ExtensionLifecycleHandlerImpl implements ExtensionLifecycleHandler {

    private static final Logger log = LoggerFactory.getLogger(ExtensionLifecycleHandlerImpl.class);

    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull ExecutorService pluginStartStopExecutor;

    @Inject
    @VisibleForTesting
    public ExtensionLifecycleHandlerImpl(
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull @PluginStartStop ExecutorService pluginStartStopExecutor) {
        this.hiveMQExtensions = hiveMQExtensions;
        this.pluginStartStopExecutor = pluginStartStopExecutor;
    }

    @Override
    public @NotNull CompletableFuture<Void> handleExtensionEvents(final ImmutableCollection<HiveMQExtensionEvent> hiveMQExtensionEvents) {
        final ImmutableList<HiveMQExtensionEvent> sorted = analyzePluginEvents(hiveMQExtensionEvents);

        CompletableFuture<Void> returnFuture = CompletableFuture.completedFuture(null);
        for (final HiveMQExtensionEvent hiveMQExtensionEvent : sorted) {
            returnFuture = returnFuture
                    .thenComposeAsync((v) -> handlePluginEvent(hiveMQExtensionEvent), pluginStartStopExecutor)
                    .handle((v, t) -> {
                        if (t != null) {
                            log.debug("Exception during Extension Lifecycle event handling", t);
                        }
                        return null;
                    });
        }
        return returnFuture;
    }

    private @NotNull CompletableFuture<Boolean> handlePluginEvent(final @NotNull HiveMQExtensionEvent hiveMQExtensionEvent) {
        switch (hiveMQExtensionEvent.getChange()) {
            case ENABLE:
                return startPlugin(hiveMQExtensionEvent);
            case DISABLE:
                return stopPlugin(hiveMQExtensionEvent.getExtensionId(), hiveMQExtensionEvent.isEmbedded());
            default:
                return CompletableFuture.completedFuture(false);
        }
    }

    private @NotNull CompletableFuture<Boolean> startPlugin(final @NotNull HiveMQExtensionEvent pluginEvent) {
        final String pluginId = pluginEvent.getExtensionId();

        log.debug("Starting {}extension with id \"{}\" at {}", pluginEvent.isEmbedded() ? "embedded " : "", pluginId, pluginEvent.getExtensionFolder());
        return CompletableFuture.supplyAsync(() -> hiveMQExtensions.extensionStart(pluginId), pluginStartStopExecutor);
    }

    private @NotNull CompletableFuture<Boolean> stopPlugin(final @NotNull String pluginId, final boolean embedded) {

        log.debug("Stopping {}extension with id {}", embedded ? "embedded " : "", pluginId);
        return CompletableFuture.supplyAsync(() -> hiveMQExtensions.extensionStop(pluginId, false), pluginStartStopExecutor);
    }

    private @NotNull ImmutableList<HiveMQExtensionEvent> analyzePluginEvents(final ImmutableCollection<HiveMQExtensionEvent> hiveMQExtensionEvents) {
        // here duplicate start priority logging can be added, once it is specified
        return ImmutableList.sortedCopyOf(
                Comparator.comparingInt(HiveMQExtensionEvent::getPriority).reversed(), hiveMQExtensionEvents);
    }
}
