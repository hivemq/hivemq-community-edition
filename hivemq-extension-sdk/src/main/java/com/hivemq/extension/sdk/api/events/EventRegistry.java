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
package com.hivemq.extension.sdk.api.events;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListener;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListenerProvider;

/**
 * Registry that allows the registration of different Event Listeners. Event Listeners are used to be informed about
 * certain events for clients.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@DoNotImplement
public interface EventRegistry {

    /**
     * Sets a provider for {@link ClientLifecycleEventListener}.
     *
     * @param clientLifecycleEventListenerProvider A custom provider implementation for {@link ClientLifecycleEventListener}.
     * @throws NullPointerException When ClientLifecycleEventListenerProvider is <code>null</code>.
     * @since 4.0.0
     */
    void setClientLifecycleEventListener(@NotNull ClientLifecycleEventListenerProvider clientLifecycleEventListenerProvider);
}
