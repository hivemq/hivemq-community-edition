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

import com.hivemq.embedded.internal.EmbeddedExtensionBuilderImpl;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;

/**
 * @author Florian Limpöck
 * @author Christop Schäbel
 * @since CE 2020.5
 */
@DoNotImplement
public interface EmbeddedExtension {

    /**
     * @return a new EmbeddedExtensionBuilder.
     * @since CE 2020.5
     */
    static @NotNull EmbeddedExtensionBuilder builder() {
        return new EmbeddedExtensionBuilderImpl();
    }

    /**
     * @return The unique ID of the extension.
     * @since CE 2020.5
     */
    @NotNull String getId();

    /**
     * @return The human readable name of the extension.
     * @since CE 2020.5
     */
    @NotNull String getName();

    /**
     * @return The version of the extension.
     * @since CE 2020.5
     */
    @NotNull String getVersion();

    /**
     * @return The author of the extension or <code>null</code> if the extension does not provide information about the author.
     * @since CE 2020.5
     */
    @Nullable String getAuthor();

    /**
     * @return the extension's priority. All extensions are called in the order of their priority (highest to lowest).
     * @since CE 2020.5
     */
    int getPriority();

    /**
     * @return the extension's start priority. All extensions are started in the order of their priority (highest to lowest).
     * @since CE 2020.5
     */
    int getStartPriority();

    /**
     * @return the object of the {@link ExtensionMain} implementation of the extension.
     * It must override the {@link ExtensionMain#extensionStart(ExtensionStartInput, ExtensionStartOutput)}
     * and the {@link ExtensionMain#extensionStop(ExtensionStopInput, ExtensionStopOutput)} )} methods.
     *
     * @since CE 2020.5
     */
    @NotNull ExtensionMain getExtensionMain();

}
