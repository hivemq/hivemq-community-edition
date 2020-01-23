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
package com.hivemq.extension.sdk.api.events.client;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.events.client.parameters.ClientLifecycleEventListenerProviderInput;

/**
 * The {@link ClientLifecycleEventListenerProvider} allows to provide an listener that listens to MQTT client based
 * events.
 * <p>
 * For each client a {@link ClientLifecycleEventListener} can be provided that has custom logic for those events.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
public interface ClientLifecycleEventListenerProvider {

    /**
     * This method is called by HiveMQ every time a new MQTT connection is started.
     * <p>
     * This provider can either supply the same EventListener (stateless or must be thread-safe)<br/>
     * or a new one (stateful, must not be thread-safe) for each MQTT connection.
     *
     * @param clientLifecycleEventListenerProviderInput The {@link ClientLifecycleEventListenerProviderInput}.
     * @return An implementation of {@link ClientLifecycleEventListener}. The return value {@code null} is ignored and
     * has the same effect as if this provider would had not been set for the connecting client.
     * @since 4.0.0
     */
    @Nullable ClientLifecycleEventListener getClientLifecycleEventListener(@NotNull final ClientLifecycleEventListenerProviderInput clientLifecycleEventListenerProviderInput);
}
