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

package com.hivemq.extensions.executor.task;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * The necessary context for the execution of a {@link PluginTask}.
 *
 * @author Georg Held
 */
public interface PluginTaskContext {

    /**
     * @return usually a {@link com.hivemq.extensions.classloader.IsolatedPluginClassloader} for setting the {@link
     * Thread#contextClassLoader} before the {@link PluginTask} is executed.
     */
    @NotNull
    ClassLoader getPluginClassLoader();

    /**
     * @return a String for the internal scheduling of the {@link com.hivemq.extensions.executor.PluginTaskExecutorService}.
     */
    @NotNull
    String getIdentifier();

}
