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
package com.hivemq.extension.sdk.api.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;

import java.util.Map;

/**
 * Input object when an extension is stopped.
 *
 * @author Christoph Schäbel
 * @author Georg Held
 * @since 4.0.0
 */
@Immutable
@DoNotImplement
public interface ExtensionStopInput {

    /**
     * @return A {@link ExtensionInformation} containing detailed information about this extension.
     * @since 4.0.0
     */
    @NotNull ExtensionInformation getExtensionInformation();

    /**
     * Get information about the HiveMQ instance the extension is running in.
     *
     * @return The {@link ServerInformation} of the input.
     * @since 4.2.0
     */
    @NotNull ServerInformation getServerInformation();

    /**
     * @return A {@link Map} of all currently enabled extensions. <br/>
     * The key is the ID of the extension. <br/>
     * The value is a {@link ExtensionInformation} containing detailed information about each extension.
     * @since 4.0.0
     */
    @NotNull Map<String, @NotNull ExtensionInformation> getEnabledExtensions();
}
