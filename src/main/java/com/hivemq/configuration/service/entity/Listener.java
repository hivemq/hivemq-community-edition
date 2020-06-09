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
package com.hivemq.configuration.service.entity;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * A marker interface for a listener. Any listener implementation must
 * implement this interface.
 *
 * @author Dominik Obermaier
 * @author Christoph Schaebel
 * @author Georg Held
 * @see TcpListener
 * @see TlsTcpListener
 * @see WebsocketListener
 * @see TlsWebsocketListener
 */
public interface Listener {

    /**
     * @return the port of the listener
     */
    int getPort();

    /**
     * @return the bind address of a listener
     */
    String getBindAddress();

    /**
     * @return the human readable, name of the listener
     */
    String readableName();

    /**
     * @return the name of the listener
     * @since 4.1
     */
    @NotNull
    String getName();

}
