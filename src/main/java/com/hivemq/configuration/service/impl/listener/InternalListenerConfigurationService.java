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
package com.hivemq.configuration.service.impl.listener;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.entity.Listener;

/**
 * An internal subinterface with extension for the {@link ListenerConfigurationService}
 *
 * @author Dominik Obermaier
 */
public interface InternalListenerConfigurationService extends ListenerConfigurationService {


    /**
     * An update listener that gets notified as soon as the listener configuration changes
     */
    interface UpdateListener {


        /**
         * This method gets called when the callback listener is registered
         *
         * @param allListeners all available listeners
         */
        void onRegister(@NotNull ImmutableList<Listener> allListeners);

        /**
         * Gets called when a listener config changes.
         *
         * @param newListener  the newly added listener or an empty listener if this method was called
         * @param allListeners all available listeners
         */
        void update(@NotNull final Listener newListener, @NotNull final ImmutableList<Listener> allListeners);
    }


    /**
     * Adds an update listener that will be notified as soon as the listener config changes
     *
     * @param listener the update listener that will be notified as soon as a listener config changes
     */
    void addUpdateListener(@NotNull final UpdateListener listener);

}
