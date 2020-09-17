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
import com.hivemq.extension.sdk.api.auth.Authorizer;
import com.hivemq.extension.sdk.api.auth.PublishAuthorizer;
import com.hivemq.extension.sdk.api.auth.SubscriptionAuthorizer;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;

import java.util.Map;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public interface ClientAuthorizers {

    /**
     * Put an {@link Authorizer} for a specific extension for all clients.
     *
     * @param pluginId   the id of the extension.
     * @param authorizer the authorizer to put.
     */
    void put(@NotNull String pluginId, @NotNull Authorizer authorizer);

    /**
     * Remove all authorizers for a specific extension defined by its classloader.
     *
     * @param pluginClassLoader the classloader of the extension.
     */
    void removeAllForPlugin(@NotNull IsolatedExtensionClassloader pluginClassLoader);

    /**
     * @return a map of all subscription authorizers per extension id.
     */
    @NotNull Map<String, SubscriptionAuthorizer> getSubscriptionAuthorizersMap();

    /**
     * @return a map of all publish authorizers per extension id.
     */
    @NotNull Map<String, PublishAuthorizer> getPublishAuthorizersMap();
}
