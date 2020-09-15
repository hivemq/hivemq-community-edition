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
package com.hivemq.extensions.client;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;

import java.util.Map;

/**
 * @author Florian Limpöck
*/
public interface ClientAuthenticators {

    /**
     * Put an enhanced authenticator with an extension id for a client
     *
     * @param extensionId   the id of the extension
     * @param authenticator the authenticator to put
     */
    void put(@NotNull String extensionId, @NotNull EnhancedAuthenticator authenticator);

    /**
     * Remove the enhanced authenticator from HiveMQ and all clients.
     *
     * @param pluginClassLoader the classloader of the plugin
     */
    void removeForExtension(@NotNull IsolatedExtensionClassloader pluginClassLoader);

    /**
     * @return a map containing all extensions with their registered enhanced authenticators.
     */
    @NotNull Map<String, EnhancedAuthenticator> getAuthenticatorMap();

}
