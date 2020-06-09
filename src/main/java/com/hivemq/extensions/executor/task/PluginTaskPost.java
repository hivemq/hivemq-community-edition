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
package com.hivemq.extensions.executor.task;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * Class containing the necessary callback, for the processing of an extension result in form of a {@link PluginTaskOutput}.
 *
 * @author Georg Held
 * @author Christoph Schäbel
 */
public interface PluginTaskPost<O extends PluginTaskOutput> {

    /**
     * This is assumed to be a non blocking very lightweight callback. It should basically just do a Thread switch into
     * the right ThreadPool and return. All computation should be done afterwards.
     *
     * @param pluginOutput the final result of a {@link PluginTask}, executed through the HiveMQ extension system.
     */
    void pluginPost(@NotNull final O pluginOutput);


    /**
     * A tangible identifier for the async option.
     *
     * @return the string that is used to calculate the Thread number, e.g. a ClientId.
     */
    @NotNull
    String getIdentifier();
}
