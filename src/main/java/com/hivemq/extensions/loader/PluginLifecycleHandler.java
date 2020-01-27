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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.HiveMQPluginEvent;

/**
 * @author Christoph Schäbel
 */
public interface PluginLifecycleHandler {

    /**
     * Handles extension En-/Disable events and calls the extension's start or stop methods if needed.
     *
     * @param hiveMQPluginEvents {@link ImmutableList} of {@link HiveMQPluginEvent}s which should be processed.
     */
    void handlePluginEvents(final @NotNull ImmutableList<HiveMQPluginEvent> hiveMQPluginEvents);

    /**
     * Stops the extension with the specified extension id.
     * The extension will stay enabled.
     *
     * @param pluginId the pluginId of the extension that should be stopped.
     * @return {@link ListenableFuture} which returns when the extension is stopped.
     */
    @NotNull ListenableFuture<Void> pluginStop(final @NotNull String pluginId);

}
