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

package com.hivemq.extensions.loader;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.annotations.NotNull;
import com.hivemq.extensions.HiveMQPluginEvent;
import com.hivemq.extensions.HiveMQPlugins;
import com.hivemq.extensions.ioc.annotation.PluginStartStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;

/**
 * @author Christoph Schäbel
 * @author Silvio Giebl
 */
@Singleton
public class PluginLifecycleHandlerImpl implements PluginLifecycleHandler {

    private static final Logger log = LoggerFactory.getLogger(PluginLifecycleHandlerImpl.class);

    private final @NotNull HiveMQPlugins hiveMQPlugins;
    private final @NotNull ExecutorService pluginStartStopExecutor;

    @Inject
    PluginLifecycleHandlerImpl(
            final @NotNull HiveMQPlugins hiveMQPlugins,
            final @NotNull @PluginStartStop ExecutorService pluginStartStopExecutor) {

        this.hiveMQPlugins = hiveMQPlugins;
        this.pluginStartStopExecutor = pluginStartStopExecutor;
    }

    @Override
    public void handlePluginEvents(final @NotNull ImmutableList<HiveMQPluginEvent> hiveMQPluginEvents) {
        for (final HiveMQPluginEvent hiveMQPluginEvent : hiveMQPluginEvents) {
            handlePluginEvent(hiveMQPluginEvent);
        }
    }

    @Override
    public @NotNull ListenableFuture<Void> pluginStop(@NotNull final String pluginId) {
        return stopPlugin(pluginId, false);
    }

    private void handlePluginEvent(final HiveMQPluginEvent hiveMQPluginEvent) {
        switch (hiveMQPluginEvent.getChange()) {
            case ENABLE:
                startPlugin(hiveMQPluginEvent);
                break;
            case DISABLE:
                stopPlugin(hiveMQPluginEvent.getPluginId(), true);
                break;
        }
    }

    private void startPlugin(final @NotNull HiveMQPluginEvent pluginEvent) {
        final String pluginId = pluginEvent.getPluginId();

        log.debug("Starting extension with id \"{}\" at {}", pluginId, pluginEvent.getPluginFolder());

        pluginStartStopExecutor.execute(() -> hiveMQPlugins.pluginStart(pluginId));
    }

    private @NotNull ListenableFuture<Void> stopPlugin(final @NotNull String pluginId, final boolean disable) {
        log.debug("{} extension with id {}", disable ? "Disabling" : "Stopping", pluginId);

        final SettableFuture<Void> stoppedFuture = SettableFuture.create();
        pluginStartStopExecutor.execute(() -> {
            hiveMQPlugins.pluginStop(pluginId, disable);
            stoppedFuture.set(null);
        });
        return stoppedFuture;
    }
}
