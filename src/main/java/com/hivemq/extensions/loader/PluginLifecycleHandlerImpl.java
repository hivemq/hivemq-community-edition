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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.HiveMQPluginEvent;
import com.hivemq.extensions.ioc.annotation.PluginStartStop;
import com.hivemq.extensions.services.auth.Authenticators;
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

    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull ExecutorService pluginStartStopExecutor;
    private final @NotNull Authenticators authenticators;

    @Inject
    PluginLifecycleHandlerImpl(
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull @PluginStartStop ExecutorService pluginStartStopExecutor,
            final @NotNull Authenticators authenticators) {

        this.hiveMQExtensions = hiveMQExtensions;
        this.pluginStartStopExecutor = pluginStartStopExecutor;
        this.authenticators = authenticators;
    }

    @Override
    public void handlePluginEvents(final @NotNull ImmutableList<HiveMQPluginEvent> hiveMQPluginEvents) {
        for (final HiveMQPluginEvent hiveMQPluginEvent : hiveMQPluginEvents) {
            handlePluginEvent(hiveMQPluginEvent);
        }
        pluginStartStopExecutor.execute(() -> {
            if (authenticators.getAuthenticatorProviderMap().isEmpty()) {
                log.warn("\n###############################################################################" +
                        "\n# No security extension present, MQTT clients can not connect to this broker. #" +
                        "\n###############################################################################");
            }
        });
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

        pluginStartStopExecutor.execute(() -> hiveMQExtensions.extensionStart(pluginId));
    }

    private @NotNull ListenableFuture<Void> stopPlugin(final @NotNull String pluginId, final boolean disable) {
        log.debug("{} extension with id {}", disable ? "Disabling" : "Stopping", pluginId);

        final SettableFuture<Void> stoppedFuture = SettableFuture.create();
        pluginStartStopExecutor.execute(() -> {
            hiveMQExtensions.extensionStop(pluginId, disable);
            stoppedFuture.set(null);
        });
        return stoppedFuture;
    }
}
