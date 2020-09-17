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
package com.hivemq.extensions;

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;

import java.nio.file.Path;

/**
 * @author Georg Held
 * @author Christoph Schäbel
 */
public interface HiveMQExtension {

    String HIVEMQ_EXTENSION_XML_FILE = "hivemq-extension.xml";

    /**
     * Calls the extension's extensionStart() with the given input and output.
     *
     * @param extensionStartInput  the {@link ExtensionStartInput} which is passed to the extension's extensionStart()
     * @param extensionStartOutput the {@link ExtensionStartOutput} which is passed to the extension's extensionStart()
     */
    void start(@NotNull ExtensionStartInput extensionStartInput, @NotNull ExtensionStartOutput extensionStartOutput)
            throws Throwable;

    /**
     * Calls the extension's extensionStop() with the given input and output.
     *
     * @param extensionStopInput  the {@link ExtensionStopInput} which is passed to the extension's extensionStart()
     * @param extensionStopOutput the {@link ExtensionStopOutput} which is passed to the extension's extensionStart()
     */
    void stop(@NotNull ExtensionStopInput extensionStopInput, @NotNull ExtensionStopOutput extensionStopOutput) throws Throwable;

    /**
     * Cleans the extensions resources and optionally disables the extension
     *
     * @param disable if the extension should be disabled
     */
    void clean(final boolean disable);

    /**
     * @return if the extension is enabled
     */
    boolean isEnabled();

    /**
     * Sets the status to disabled for this extension
     */
    void setDisabled();

    /**
     * @return the extension's priority which is taken from the extension's hivemq-extension.xml
     */
    int getPriority();

    /**
     * @return the extension's start priority which is taken from the extension's hivemq-extension.xml
     */
    int getStartPriority();

    /**
     * @return the extension's name which is taken from the extension's hivemq-extension.xml
     */
    @NotNull String getName();

    /**
     * @return the extension's author which is taken from the extension's hivemq-extension.xml
     */
    @Nullable String getAuthor();

    /**
     * @return the extension's version which is taken from the extension's hivemq-extension.xml
     */
    @NotNull String getVersion();

    /**
     * @return the extension's id which is taken from the extension's hivemq-extension.xml
     */
    @NotNull String getId();

    /**
     * @return the {@link Class} of the extension's implementation of the {@link ExtensionMain} class
     */
    @Nullable Class<? extends ExtensionMain> getExtensionMainClazz();

    /**
     * @return the {@link ClassLoader} used to load the {@link ExtensionMain} class
     */
    @Nullable ClassLoader getExtensionClassloader();

    /**
     * @return the path to the extension's folder
     */
    @NotNull Path getExtensionFolderPath();

    /**
     * @return the previously enabled version of the extension, or null if no previous version was enabled
     */
    @Nullable String getPreviousVersion();

    /**
     * Set the previously enabled version of the extension
     *
     * @param previousVersion the previously enabled version of the extension
     */
    void setPreviousVersion(@Nullable String previousVersion);

    /**
     * is it an embedded extension or not?
     *
     * @return true for embedded else false
     */
    boolean isEmbedded();
}
