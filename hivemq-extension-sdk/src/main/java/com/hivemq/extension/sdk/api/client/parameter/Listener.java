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
package com.hivemq.extension.sdk.api.client.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * Holds information about the listener a client uses for a connection to HiveMQ.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@DoNotImplement
public interface Listener {

    /**
     * The port of HiveMQ the client is connected to.
     *
     * @return The port of the listener.
     * @since 4.0.0
     */
    int getPort();

    /**
     * The bind address of HiveMQ the client is connected to.
     *
     * @return The bind address of the listener.
     * @since 4.0.0
     */
    @NotNull String getBindAddress();

    /**
     * The type of the listener the client uses for the connection to HiveMQ.
     *
     * @return The type of the listener.
     * @since 4.0.0
     */
    @NotNull ListenerType getListenerType();

    /**
     * @return The configured or default name of the listener.
     * @since 4.1.0
     */
    @NotNull
    String getName();
}
