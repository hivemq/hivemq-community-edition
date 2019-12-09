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
package com.hivemq.extension.sdk.api.services.intializer;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.client.ClientContext;
import com.hivemq.extension.sdk.api.client.parameter.InitializerInput;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;

/**
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@FunctionalInterface
public interface ClientInitializer {

    /**
     * This method is called for every client when the client is connected and a session is either created or exists.
     * Also this method is called for every already connected client, when the extension starts.
     *
     * @param initializerInput The {@link InitializerInput} containing information about, server, client and connection.
     * @param clientContext    The {@link ClientContext} to add/remove: {@link Interceptor}s or {@link
     *                         TopicPermission}s.
     * @since 4.0.0
     */
    void initialize(@NotNull InitializerInput initializerInput, @NotNull ClientContext clientContext);
}
