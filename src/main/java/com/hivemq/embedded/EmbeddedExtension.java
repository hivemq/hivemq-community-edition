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

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;

public interface EmbeddedExtension {

    /**
     * @return The unique ID of an extension.
     * @since CE 2020.5
     */
    @NotNull String getId();

    /**
     * @return The human readable name of an extension.
     * @since CE 2020.5
     */
    @NotNull String getName();

    /**
     * @return The version of an extension.
     * @since CE 2020.5
     */
    @NotNull String getVersion();

    /**
     * @return The author of an extension, if the extension provides information about the author.
     * @since CE 2020.5
     */
    @Nullable String getAuthor();

    /**
     * @return the extension's priority, the extension with a higher priority is used first
     * @since CE 2020.5
     */
    int getPriority();

    /**
     * @return the extension's start priority, the extension with a higher priority starts first
     * @since CE 2020.5
     */
    int getStartPriority();

    /**
     * @return an {@link ExtensionMain} which is the main class of the extension.
     * It must override the {@link ExtensionMain#extensionStart(ExtensionStartInput, ExtensionStartOutput)}
     * and the {@link ExtensionMain#extensionStop(ExtensionStopInput, ExtensionStopOutput)} )} methods.
     *
     * @since CE 2020.5
     */
    @NotNull ExtensionMain getExtensionMain();

}
