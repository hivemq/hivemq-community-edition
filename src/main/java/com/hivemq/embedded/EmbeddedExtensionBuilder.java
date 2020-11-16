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

/**
 * @author Florian Limpöck
 * @since CE 2020.5
 */
@DoNotImplement
public interface EmbeddedExtensionBuilder {

    /**
     * @return a new EmbeddedExtensionBuilder.
     * @since CE 2020.5
     */
    static @NotNull EmbeddedExtensionBuilder builder() {
        return new EmbeddedExtensionBuilderImpl();
    }

    /**
     * Sets the unique ID of the extension
     * <p>
     * Required value.
     *
     * @param id The unique ID of an extension
     * @return the builder itself
     * @since CE 2020.5
     */
    @NotNull EmbeddedExtensionBuilder withId(@NotNull String id);

    /**
     * Sets the human readable name of the extension.
     * <p>
     * Required value.
     *
     * @param name The name of the extension
     * @return the builder itself
     * @since CE 2020.5
     */
    @NotNull EmbeddedExtensionBuilder withName(@NotNull String name);

    /**
     * Sets the version of the extension.
     * <p>
     * Required value.
     *
     * @param version The version of the extension
     * @return the builder itself
     * @since CE 2020.5
     */
    @NotNull EmbeddedExtensionBuilder withVersion(@NotNull String version);

    /**
     * Sets the author of the extension.
     * <p>
     * Optional value, no default.
     *
     * @param author The author of the extension
     * @return the builder itself
     * @since CE 2020.5
     */
    @NotNull EmbeddedExtensionBuilder withAuthor(@Nullable String author);

    /**
     * Sets the extension's priority, the extension with a higher priority is used first
     * <p>
     * Optional value, defaulting to '0'
     *
     * @param priority The priority of the extension
     * @return the builder itself
     * @since CE 2020.5
     */
    @NotNull EmbeddedExtensionBuilder withPriority(int priority);

    /**
     * Sets the extension's start priority, the extension with a higher priority starts first
     * <p>
     * Optional value, defaulting to '1000'
     *
     * @param startPriority The start priority of the extension
     * @return the builder itself
     * @since CE 2020.5
     */
    @NotNull EmbeddedExtensionBuilder withStartPriority(int startPriority);

    /**
     * Sets the main class of the extension
     * <p>
     * Required value.
     *
     * @param extensionMain The main class of the extension
     * @return the builder itself
     * @since CE 2020.5
     */
    @NotNull EmbeddedExtensionBuilder withExtensionMain(@NotNull ExtensionMain extensionMain);

    /**
     * Creates an instance of an EmbeddedExtension.
     * <p>
     * required values are:
     * <ul>
     * <li>id</li>
     * <li>name</li>
     * <li>version</li>
     * <li>extensionMain</li>
     * </ul>
     *
     * @return an instance of an {@link EmbeddedExtension}
     * @throws IllegalStateException if any required parameter is missing.
     * @since CE 2020.5
     */
    @NotNull EmbeddedExtension build();

}
