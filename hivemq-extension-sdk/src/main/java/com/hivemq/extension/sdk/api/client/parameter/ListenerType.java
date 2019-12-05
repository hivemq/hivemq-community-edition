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

/**
 * The type of a {@link Listener}.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
public enum ListenerType {

    /**
     * A TCP Listener is used.
     *
     * @since 4.0.0
     */
    TCP_LISTENER,

    /**
     * A TCP Listener with TLS is used.
     *
     * @since 4.0.0
     */
    TLS_TCP_LISTENER,

    /**
     * A Websocket Listener is used.
     *
     * @since 4.0.0
     */
    WEBSOCKET_LISTENER,

    /**
     * A Websocket Listener with TLS is used.
     *
     * @since 4.0.0
     */
    TLS_WEBSOCKET_LISTENER
}
